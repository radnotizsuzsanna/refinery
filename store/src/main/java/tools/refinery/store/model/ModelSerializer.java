package tools.refinery.store.model;

import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.map.internal.MapDelta;
import tools.refinery.store.map.internal.MapTransaction;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Sushi, Inci
 *
 */
public class ModelSerializer {
	HashMap<Class<?>, SerializerStrategy<?>> serializerStrategyMap;

	public ModelSerializer(){
		this.serializerStrategyMap = new HashMap<>();
	}

	public <T> void addStrategy(Class<T> valueType, SerializerStrategy<T> strategy){
		serializerStrategyMap.put(valueType, strategy);

	}


	public void write(ModelStore store, DataOutputStream relations, HashMap<Relation<?>, DataOutputStream> streams) throws IOException {
		if (store instanceof ModelStoreImpl impl) {
			for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : impl.stores.entrySet()) {
				DataRepresentation<?, ?> dataRepresentation = entry.getKey();
				if (dataRepresentation instanceof Relation<?> relation) {
					VersionedMapStore<?, ?> mapStore = entry.getValue();
					if (mapStore instanceof VersionedMapStoreDeltaImpl<?, ?> deltaStore) {
						writeRelation(relation, deltaStore, relations, streams);
					} else {
						throw new UnsupportedOperationException("Only delta stores are supported!");
					}
				} else {
					throw new UnsupportedOperationException(
							"Only Relation representations are supported during serialization.");
				}
			}
			relations.flush();
			relations.close();
		}
	}

	/**
	 * Serializes a relation with transactions stored in a delta map store.
	 *
	 * @param relation The relation to serialize.
	 * @param rawVersionedMapStore The store to serialize. Must have {@link Tuple} keys and <code>T</code> values.
	 * @param relations The stream to write relation metadata to.
	 * @param streams The streams to write relation contents to.
	 * @param <T> The type of values to serialize.
	 * @throws IOException When the serialization fails.
	 */
	private <T> void writeRelation(Relation<T> relation,
								   VersionedMapStoreDeltaImpl<?, ?> rawVersionedMapStore,
								   DataOutputStream relations,
								   HashMap<Relation<?>, DataOutputStream> streams) throws IOException {
		// Guaranteed to succeed by the precondition of this method.
		@SuppressWarnings("unchecked")
		VersionedMapStoreDeltaImpl<Tuple, T> versionedMapStore = (VersionedMapStoreDeltaImpl<Tuple, T>) rawVersionedMapStore;
		//Writes out Relation ValueType
		Class<T> valueTypeClass = relation.getValueType();
		String valueTypeString = valueTypeClass.toString();
		valueTypeString = valueTypeString.replace("class ", "");
		byte[] valueTypeByte = valueTypeString.getBytes(StandardCharsets.UTF_8);
		relations.writeInt(valueTypeByte.length);
		relations.write(valueTypeByte);
		System.out.println("\nWriting Relation valueType: " + valueTypeString);

		// Get the strategy. This will always match the type of <code>valueTypeClass</code> because we store
		// the matching serializer for each value type in <code>serializerStrategyMap</code>.
		@SuppressWarnings("unchecked")
		SerializerStrategy<T> serializerStrategy = (SerializerStrategy<T>) serializerStrategyMap.get(valueTypeClass);

		//Writes out Relation name
		String name = relation.getName();
		byte[] nameByte = name.getBytes(StandardCharsets.UTF_8);
		relations.writeInt(nameByte.length);
		relations.write(nameByte);
		System.out.println("\nWriting Relation name: " + relation.getName());

		//Writes out Relation arity
		relations.writeInt(relation.getArity());
		System.out.println("Writing Relation arity: " + relation.getArity());

		//Writes out defaultValue
		serializerStrategy.writeValue(relations, relation.getDefaultValue());
		System.out.println("Writing Relation defaultValue: " + relation.getDefaultValue());

		writeDeltaStore(relation, versionedMapStore, streams.get(relation), serializerStrategy);
	}

	public ModelStoreWithError read(DataInputStream relations, HashMap<Relation<?>, DataInputStream> streams) throws IOException, ClassNotFoundException {
		Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> stores = new HashMap<>();
		ModelStoreWithError modelStoreWithError = new ModelStoreWithError(null);
		try{
			while(relations.available()!=0){
				//Reads Relation valueType
				int length = relations.readInt();
				byte[] valueTypeByte = new byte[length];
				relations.readFully(valueTypeByte);
				String valueTypeString = new String(valueTypeByte, StandardCharsets.UTF_8);
				Class<?> valueTypeClass = Class.forName(valueTypeString);
				System.out.println("\nReading Relation valueType: " + valueTypeString);

				//TODO ez igy nagyon csunya? :( A "?" helyére nem tudom eltenni a típust?
				DataRepresentationVersionMapStorePair<Tuple, ?> pair = readRelation(relations, streams, valueTypeClass, modelStoreWithError);
				if (pair == null) {
					return new ModelStoreWithError(null, new Exception("Could not read a relation"),-1,null);
					//TODO ha a relációt sem sikerült beolvasni,akkor mi legyen
				}
				//Creates ModelStore from Relation and VersionedMapStoreDeltaImpl
				stores.put(pair.dataRepresentation(), pair.mapStore());
			}
		}
		catch (IOException e){
			if(e.getMessage() == null) throw new IOException("Incomplete Relation in file");
			else throw  e;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		long lastVersion = 2;
		Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> newStores = new HashMap<>();
		for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : stores.entrySet()){
			newStores.put(entry.getKey(), (VersionedMapStore<?, ?>) entry.getValue().createMap(lastVersion));
		}

		ModelStore store = new ModelStoreImpl(stores);
		modelStoreWithError.modelStore = store; //TODO ne így
		relations.close();
		return modelStoreWithError;
	}

	/**
	 * Deserializes a relation with transactions stored in a delta map store.
	 *
	 * @param relations The stream to read relation metadata from.
	 * @param streams   The streams to read relation contents from.
	 * @param <T>       The type of values to deserialize.
	 * @return The model store with the deserialized data.
	 * @throws IOException When the deserialization fails.
	 */
	private <T> DataRepresentationVersionMapStorePair readRelation(DataInputStream relations,
																   HashMap<Relation<?>, DataInputStream> streams,
																   Class<T> valueTypeClass, ModelStoreWithError modelStoreWithError) throws IOException {
		@SuppressWarnings("unchecked")
		SerializerStrategy<T> serializerStrategy = (SerializerStrategy<T>) serializerStrategyMap.get(valueTypeClass);

		String name = new String("");
		int arity = 0;
		T defaultValue = null;

		try {
			//Reads Relation name
			int length = relations.readInt();
			byte[] nameByte = new byte[length];
			relations.readFully(nameByte);
			name = new String(nameByte, StandardCharsets.UTF_8);
			System.out.println("\nReading Relation name: " + name);

			//Reads Relation arity
			arity = relations.readInt();
			System.out.println("Reading Relation arity: " + arity);

			//Reads defaultValue
			defaultValue = serializerStrategy.readValue(relations);
			System.out.println("Reading Relation defaultValue: " + defaultValue);
		}catch (IOException exception){
			return null;
		}

		//Creates Relation
		var relation = new Relation<T>(name, arity, valueTypeClass, defaultValue);
		System.out.println("Relation created: " + relation.getName());

		//Creates VersionedMapStoreDeltaImp
		DataInputStream data = streams.get(relation);
		var mapStore = readDeltaStore(relation, data, serializerStrategy, modelStoreWithError);
		if (modelStoreWithError.exception == null) modelStoreWithError.setGuiltyRelation(relation);

		System.out.println("VersionedMapStoreDeltaImpl created.");
		return new DataRepresentationVersionMapStorePair<>(relation, mapStore);
	}

	public <T> void writeDeltaStore(Relation<T> relation, VersionedMapStoreDeltaImpl<Tuple, T> mapStore, DataOutputStream data, SerializerStrategy<T> serializerStrategy) throws IOException {
		//TODO If the map store has at least one state, serializes the state(s)
		//TODO ilyenkor ki kell írni egy üres tranzakciót
		MapTransaction<Tuple, T> lastNotEmptyTransaction = null;
		if(mapStore.getState(0) != null){
			for(int i = 0; i < mapStore.getStates().size(); i++){
				MapTransaction<Tuple, T> mapTransaction = mapStore.getState(i);
				MapDelta<Tuple, T>[] deltasOfTransaction = mapTransaction.deltas();

				//Writes out the version and the parent id of the mapTransaction
				data.writeLong(i);
				System.out.println("\tWriting version of transaction: " + i);

				int deltasLength = 0;

				if(mapTransaction.version() == i){
					lastNotEmptyTransaction = mapTransaction;
					if(mapTransaction.parent() == null) {
						data.writeLong(-1);
						System.out.println("\tWriting parent of transaction: -1");
					}
					else{
						data.writeLong(mapTransaction.parent().version());
						System.out.println("\tWriting parent of transaction: " + mapTransaction.parent().version());
					}
					deltasLength = mapTransaction.deltas().length;
				}
				else{
					if(lastNotEmptyTransaction == null){
						data.writeLong(-1);
						System.out.println("\tWriting parent of transaction: " + -1);
					}
					else{
						data.writeLong(lastNotEmptyTransaction.version());
						System.out.println("\tWriting parent of transaction: " + lastNotEmptyTransaction.version());
						System.out.println("\tDebuggolashoz: Ures transaction parentje: " + mapTransaction.parent());
					}
				}

				//Writes out the number of deltas
				data.writeInt(deltasLength);
				System.out.println("\tWriting number of deltas: " + deltasLength);

				for (int j = 0; j < deltasLength; j++) {
					MapDelta<Tuple, T> mapDelta = deltasOfTransaction[j];
					//Writes out key
					Tuple tuple = mapDelta.key();
					System.out.println("\t\tWriting key: " + mapDelta.key());

					for (int k = 0; k < relation.getArity(); k++) {
						data.writeInt(tuple.get(k));
					}

					//Writes out new and old value
					if (mapDelta.oldValue() == null) serializerStrategy.writeValue(data, relation.getDefaultValue());
					else serializerStrategy.writeValue(data, mapDelta.oldValue());
					System.out.println("\t\tWriting oldValue:  " + mapDelta.oldValue());

					serializerStrategy.writeValue(data, mapDelta.newValue());
					System.out.println("\t\tWriting newValue:  " + mapDelta.newValue());
				}
				System.out.println("");
			}
		}
		else{
			//Writes out the version and the parent id of the mapTransaction
			data.writeLong(0);
			System.out.println("\tWriting version of transaction: " + 0);

			data.writeLong(-1);
			System.out.println("\tWriting parent of transaction: -1");

			//Writes out the number of deltas
			data.writeInt(0);
			System.out.println("\tWriting number of deltas: " + 0);
		}
	}

	public <T> VersionedMapStoreDeltaImpl<Tuple,T> readDeltaStore(Relation<T> relation, DataInputStream data, SerializerStrategy<T> serializerStrategy, ModelStoreWithError modelStoreWithError) throws IOException {
		LongObjectHashMap<MapTransaction<Tuple, T>> mapTransactionArray = new LongObjectHashMap<>();
		long version = 0;
		try{
			while(data.available()!=0){
				version = data.readLong();
				System.out.println("\tReading version of transaction: " + version);
				long parent = data.readLong();
				System.out.println("\tReading parent of transaction: " + parent);
				int deltasLength = data.readInt();
				System.out.println("\tReading number of deltas: " + deltasLength);

				if(deltasLength == 0){
					MapTransaction<Tuple, T> parentTransaction = mapTransactionArray.get(parent);
					mapTransactionArray.put(version, parentTransaction);

				}
				else{
					var deltas = new MapDelta[deltasLength];
					for(int j = 0; j < deltasLength; j++){
						//Reads the elements of the tuple
						int[] tupleArray = new int[relation.getArity()];
						for(int k = 0; k < relation.getArity(); k++){
							tupleArray[k] = data.readInt();
						}
						Tuple tuple = Tuple.of(tupleArray);
						System.out.println("\t\tReading tuple: " + tuple);

						//Reads the old and new value
						var oldValue = serializerStrategy.readValue(data);
						System.out.println("\t\tReading oldValue: " + oldValue);

						var newValue = serializerStrategy.readValue(data);
						System.out.println("\t\tReading newValue: " + newValue);
						deltas[j] = new MapDelta<>(tuple, oldValue, newValue);
					}
					if(parent == -1){
						//noinspection unchecked
						mapTransactionArray.put(version, new MapTransaction<Tuple, T>(deltas, version, null));
					}
					else{
						MapTransaction<Tuple, T> parentTransaction = mapTransactionArray.get(parent);
						//noinspection unchecked
						mapTransactionArray.put(version, new MapTransaction<Tuple, T>(deltas, version, parentTransaction));
					}
				}
				System.out.println("");
			}
		}
		catch(IOException e){
			modelStoreWithError.setException(new IOException("Incomplete MapStore in file"));
			modelStoreWithError.setLastSuccessfulTransactionVersion(version);
			return new VersionedMapStoreDeltaImpl<>(relation.getDefaultValue(), mapTransactionArray);
		}

		return new VersionedMapStoreDeltaImpl<>(relation.getDefaultValue(), mapTransactionArray);
	}
}
