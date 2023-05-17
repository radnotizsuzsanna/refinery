package tools.refinery.store.model;

import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreBuilder;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.map.internal.MapDelta;
import tools.refinery.store.map.internal.MapTransaction;
import tools.refinery.store.model.internal.ModelStoreImpl;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ModelSerializer {
	HashMap<Class<?>, SerializerStrategy<?>> serializerStrategyMap;

	public ModelSerializer(){
		this.serializerStrategyMap = new HashMap<>();
	}

	public <T> void addStrategy(Class<T> valueType, SerializerStrategy<T> strategy){
		serializerStrategyMap.put(valueType, strategy);

	}

	public void write(ModelStore store, DataOutputStream relations, HashMap<Symbol<?>, DataOutputStream> streams) throws IOException {
		if (store instanceof ModelStoreImpl impl) {
			for (Entry<? extends AnySymbol, ? extends VersionedMapStore<Tuple, ?>> entry : impl.stores.entrySet()) {
				AnySymbol dataRepresentation = entry.getKey();
				if (dataRepresentation instanceof Symbol<?> relation) {
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
	private <T> void writeRelation(Symbol<T> relation,
								   VersionedMapStoreDeltaImpl<?, ?> rawVersionedMapStore,
								   DataOutputStream relations,
								   HashMap<Symbol<?>, DataOutputStream> streams) throws IOException {
		// Guaranteed to succeed by the precondition of this method.
		@SuppressWarnings("unchecked")
		VersionedMapStoreDeltaImpl<Tuple, T> versionedMapStore = (VersionedMapStoreDeltaImpl<Tuple, T>) rawVersionedMapStore;
		//Writes out Relation ValueType
		Class<T> valueTypeClass = relation.valueType();
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
		String name = relation.name();
		byte[] nameByte = name.getBytes(StandardCharsets.UTF_8);
		relations.writeInt(nameByte.length);
		relations.write(nameByte);
		System.out.println("\nWriting Relation name: " + relation.name());

		//Writes out Relation arity
		relations.writeInt(relation.arity());
		System.out.println("Writing Relation arity: " + relation.arity());

		//Writes out defaultValue
		serializerStrategy.writeValue(relations, relation.defaultValue());
		System.out.println("Writing Relation defaultValue: " + relation.defaultValue());

		writeDeltaStore(relation, versionedMapStore, streams.get(relation), serializerStrategy);
	}

	public void read(ModelStoreWithError modelStoreWithError, DataInputStream relations, HashMap<String, DataInputStream> streams) throws IOException,
			ClassNotFoundException {
		long lastVersion = 2;
		Map<AnySymbol, VersionedMapStore<?, ?>> stores = new HashMap<>();

		var mapStores = (Map<AnySymbol, VersionedMapStore<Tuple, ?>>)((ModelStoreImpl )modelStoreWithError.modelStore).stores;

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
				SymbolNameVersionMapStorePair<Tuple, ?> pair = readRelation(relations, streams, valueTypeClass, modelStoreWithError);
				if (pair == null) {
					modelStoreWithError = new ModelStoreWithError(null, new Exception("Could not read a relation"),-1, null);
					//TODO ha a relációt sem sikerült beolvasni,akkor mi legyen
				}
				//Creates ModelStore from Relation and VersionedMapStoreDeltaImpl
				//TODO ehelyett majd más kell (while)
				for (Entry<AnySymbol, VersionedMapStore<Tuple, ?>> entry : mapStores.entrySet()){
					if(entry.getKey().name().equals(pair.symbolName())){
						stores.put(entry.getKey(), pair.mapStore());
					}
				}
			}
		}
		catch (IOException e){
			if(e.getMessage() == null) throw new IOException("Incomplete Relation in file");
			else throw  e;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		for (Entry<AnySymbol, VersionedMapStore<Tuple, ?>> entry : mapStores.entrySet()){
			VersionedMapStore<Tuple, ?> mapStore = mapStores.get(entry.getKey());


			var store = stores.get(entry.getKey());
			var states = store.getStates();

			Map<Long, MapTransaction> statesHasMap = ((VersionedMapStoreDeltaImpl) mapStore).internalExposeStates();
			for (Long value : states) {
				if (value <= lastVersion) {
					statesHasMap.put(value, ((VersionedMapStoreDeltaImpl) store).getState(value));
				}
			}

			mapStores.put(entry.getKey(), mapStore);
		}

		relations.close();
	}

	/*private <T> VersionedMapStore<Tuple, T>createMapStore(Symbol<T> symbol){
		return VersionedMapStoreBuilder.<Tuple, T>builder().setDefaultValue(symbol.defaultValue()).buildOne();
	}*/

	/**
	 * Deserializes a relation with transactions stored in a delta map store.
	 *
	 * @param relations The stream to read relation metadata from.
	 * @param streams   The streams to read relation contents from.
	 * @param <T>       The type of values to deserialize.
	 * @return The model store with the deserialized data.
	 * @throws IOException When the deserialization fails.
	 */
	private <T> SymbolNameVersionMapStorePair readRelation(DataInputStream relations,
																   HashMap<String, DataInputStream> streams,
																   Class<T> valueTypeClass, ModelStoreWithError modelStoreWithError) throws IOException, ClassNotFoundException {
		@SuppressWarnings("unchecked")
		SerializerStrategy<T> serializerStrategy = (SerializerStrategy<T>) serializerStrategyMap.get(valueTypeClass);

		String name;
		int arity;
		T defaultValue;

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
		//TODO ez nem kell
		var relation = new Symbol<>(name, arity, valueTypeClass, defaultValue);
		System.out.println("Relation created: " + relation.name());

		//Creates VersionedMapStoreDeltaImp
		DataInputStream data = streams.get(relation.name());

		var mapStore = readDeltaStore(relation, data, serializerStrategy, modelStoreWithError);
		if (modelStoreWithError.exception == null) modelStoreWithError.setGuiltyRelation(relation);

		System.out.println("VersionedMapStoreDeltaImpl created.");
		return new SymbolNameVersionMapStorePair<>(relation.name(), mapStore);
	}

	public <T> void writeDeltaStore(Symbol<T> relation, VersionedMapStoreDeltaImpl<Tuple, T> mapStore, DataOutputStream data, SerializerStrategy<T> serializerStrategy) throws IOException {
		//TODO If the map store has at least one state, serializes the state(s)
		//TODO ilyenkor ki kell írni egy üres tranzakciót
		if(mapStore.getState(0) != null){
			for(int i = 0; i < mapStore.getStates().size(); i++){
				MapTransaction<Tuple, T> mapTransaction = mapStore.getState(i);
				MapDelta<Tuple, T>[] deltasOfTransaction = mapTransaction.deltas();

				//Writes out the version and the parent id of the mapTransaction
				data.writeLong(i);
				System.out.println("\tWriting version of transaction: " + i);

				int deltasLength = 0;

				if(mapTransaction.version() == i){
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
					data.writeLong(mapTransaction.version());
					System.out.println("\tWriting parent of transaction: " + mapTransaction.version());
				}

				//Writes out the number of deltas
				data.writeInt(deltasLength);
				System.out.println("\tWriting number of deltas: " + deltasLength);

				for (int j = 0; j < deltasLength; j++) {
					MapDelta<Tuple, T> mapDelta = deltasOfTransaction[j];
					//Writes out key
					Tuple tuple = mapDelta.key();
					System.out.println("\t\tWriting key: " + mapDelta.key());

					for (int k = 0; k < relation.arity(); k++) {
						data.writeInt(tuple.get(k));
					}

					//Writes out new and old value
					if (mapDelta.oldValue() == null) serializerStrategy.writeValue(data, relation.defaultValue());
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

	public <T> VersionedMapStoreDeltaImpl<Tuple,T> readDeltaStore(Symbol<T> relation, DataInputStream data, SerializerStrategy<T> serializerStrategy, ModelStoreWithError modelStoreWithError) throws ClassNotFoundException {
		HashMap<Long, MapTransaction<Tuple, T>> mapTransactionArray = new HashMap<>();

		var mapStore =
				(VersionedMapStoreDeltaImpl<Tuple,T>)	VersionedMapStoreBuilder.<Tuple, T>builder().setDefaultValue(relation.defaultValue()).buildOne();

		Map statesHasMap = ((VersionedMapStoreDeltaImpl) mapStore).internalExposeStates();

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
					MapTransaction<Tuple, T> parentTransaction = (MapTransaction<Tuple, T>) statesHasMap.get(parent);
					statesHasMap.put(version, parentTransaction);

				}
				else{
					var deltas = new MapDelta[deltasLength];
					for(int j = 0; j < deltasLength; j++){
						//Reads the elements of the tuple
						int[] tupleArray = new int[relation.arity()];
						for(int k = 0; k < relation.arity(); k++){
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
						statesHasMap.put(version, new MapTransaction<Tuple, T>(deltas, version, null));
					}
					else{
						MapTransaction<Tuple, T> parentTransaction = (MapTransaction<Tuple, T>) statesHasMap.get(parent);
						//noinspection unchecked
						statesHasMap.put(version, new MapTransaction<Tuple, T>(deltas, version, parentTransaction));
					}
				}
				System.out.println("");
			}
		}
		catch(IOException e){
			modelStoreWithError.setException(new IOException("Incomplete MapStore in file"));
			modelStoreWithError.setLastSuccessfulTransactionVersion(version);

			//Todo a Tuple jó?
			//var mapStore = VersionedMapStoreBuilder.<Tuple, T>builder().setDefaultValue(relation.defaultValue()).buildOne();
		//	mapStore.setStates(mapTransactionArray);
			return mapStore;

			//return new VersionedMapStoreDeltaImpl<>(relation.defaultValue(), mapTransactionArray);
		}

		//return new VersionedMapStoreDeltaImpl<>(relation.defaultValue(), mapTransactionArray);
		//mapStore.setStates(mapTransactionArray);
		return mapStore;
	}
}
