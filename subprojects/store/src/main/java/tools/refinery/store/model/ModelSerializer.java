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
import java.util.logging.Logger;

public class ModelSerializer {
	HashMap<Class<?>, SerializerStrategy<?>> serializerStrategyMap;

	public ModelSerializer(){
		this.serializerStrategyMap = new HashMap<>();
	}

	Logger logger = Logger.getLogger(ModelSerializer.class.getName());

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
		//logger.info("Writing Relation valueType: " + valueTypeString);

		// Get the strategy. This will always match the type of <code>valueTypeClass</code> because we store
		// the matching serializer for each value type in <code>serializerStrategyMap</code>.
		@SuppressWarnings("unchecked")
		SerializerStrategy<T> serializerStrategy = (SerializerStrategy<T>) serializerStrategyMap.get(valueTypeClass);

		//Writes out Relation name
		String name = relation.name();
		byte[] nameByte = name.getBytes(StandardCharsets.UTF_8);
		relations.writeInt(nameByte.length);
		relations.write(nameByte);
		//logger.info("Writing Relation name: " + relation.name());

		//Writes out Relation arity
		relations.writeInt(relation.arity());
		//logger.info("Writing Relation arity: " + relation.arity());

		//Writes out defaultValue
		serializerStrategy.writeValue(relations, relation.defaultValue());
		//logger.info("Writing Relation defaultValue: " + relation.defaultValue());

		writeDeltaStore(relation, versionedMapStore, streams.get(relation), serializerStrategy);
	}

	public void read(ModelStoreWithError modelStoreWithError, DataInputStream relations, HashMap<String, DataInputStream> streams) throws IOException,
			ClassNotFoundException {

		Map<AnySymbol, VersionedMapStore<?, ?>> stores = new HashMap<>();

		@SuppressWarnings("unchecked")
		var mapStores = (Map<AnySymbol, VersionedMapStore<Tuple, ?>>)((ModelStoreImpl )modelStoreWithError.modelStore).stores;

		try{
			while(relations.available()!=0){
				//Reads Relation valueType
				int length = relations.readInt();
				byte[] valueTypeByte = new byte[length];
				relations.readFully(valueTypeByte);
				String valueTypeString = new String(valueTypeByte, StandardCharsets.UTF_8);
				Class<?> valueTypeClass = Class.forName(valueTypeString);
				//logger.info("Reading Relation valueType: " + valueTypeString);

				SymbolVersionMapStorePair<Tuple, ?> pair = readRelation(relations, streams, valueTypeClass, modelStoreWithError);
				if (pair == null) {
					modelStoreWithError.setException(new Exception("Incomplete Relation in file"));
				}

				for (Entry<AnySymbol, VersionedMapStore<Tuple, ?>> entry : mapStores.entrySet()){
					assert pair != null;
					if(entry.getKey().name().equals(pair.symbol().name())){
						if(pair.symbol().arity() != entry.getKey().arity()){
							modelStoreWithError.setException(new Exception("The arity read from the file and the " +
									"arity of the store's symbol are not the same. "));
							throw new RuntimeException();
						}
						if(pair.symbol().defaultValue() != ((Symbol<?>) entry.getKey()).defaultValue()){
							modelStoreWithError.setException(new Exception("The default value read from the file and " +
									"the default value of the store's symbol are not the same. "));
							throw new RuntimeException();
						}
						stores.put(entry.getKey(), pair.mapStore());
					}
				}
			}
		}
		catch (IOException e){
			modelStoreWithError.setException(new Exception("Incomplete Relation in file"));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		//If there was no problem with reading the relation, puts the states into the mapStore
		if(modelStoreWithError.getException()==null){
			long lastVersion = modelStoreWithError.lastSuccessfulTransactionVersion;
			for (Entry<AnySymbol, VersionedMapStore<Tuple, ?>> entry : mapStores.entrySet()){
				VersionedMapStore<Tuple, ?> mapStore = mapStores.get(entry.getKey());

				var store = stores.get(entry.getKey());
				var states = store.getStates();

				var statesHasMap = ((VersionedMapStoreDeltaImpl) mapStore).internalExposeStates();
				for (Long value : states) {
					if (value <= lastVersion) {
						statesHasMap.put(value, ((VersionedMapStoreDeltaImpl) store).getState(value));
					}
				}

				mapStores.put(entry.getKey(), mapStore);
			}
		}
		relations.close();
	}

	private <T> SymbolVersionMapStorePair<Tuple, T> readRelation(DataInputStream relations,
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
			//logger.info("Reading Relation name: " + name);

			//Reads Relation arity
			arity = relations.readInt();
			//logger.info("Reading Relation arity: " + arity);

			//Reads defaultValue
			defaultValue = serializerStrategy.readValue(relations);
			//logger.info("Reading Relation defaultValue: " + defaultValue);
		}catch (IOException exception){
			return null;
		}

		//Creates Relation
		var relation = new Symbol<>(name, arity, valueTypeClass, defaultValue);
		//logger.info("Relation created: " + relation.name());

		//Creates VersionedMapStoreDeltaImp
		DataInputStream data = streams.get(relation.name());

		var mapStore = readDeltaStore(relation, data, serializerStrategy, modelStoreWithError);
		if (modelStoreWithError.exception == null) modelStoreWithError.setGuiltyRelation(relation);

		//logger.info("VersionedMapStoreDeltaImpl created.");
		return new SymbolVersionMapStorePair<>(relation, mapStore);
	}

	public <T> void writeDeltaStore(Symbol<T> relation, VersionedMapStoreDeltaImpl<Tuple, T> mapStore, DataOutputStream data, SerializerStrategy<T> serializerStrategy) throws IOException {
		if(mapStore.getState(0) != null){
			for(int i = 0; i < mapStore.getStates().size(); i++){
				MapTransaction<Tuple, T> mapTransaction = mapStore.getState(i);
				MapDelta<Tuple, T>[] deltasOfTransaction = mapTransaction.deltas();

				//Writes out the version and the parent id of the mapTransaction
				data.writeLong(i);
				//logger.info("\tWriting version of transaction: " + i);

				int deltasLength = 0;

				if(mapTransaction.version() == i){
					if(mapTransaction.parent() == null) {
						data.writeLong(-1);
						//logger.info("\tWriting parent of transaction: -1");
					}
					else{
						data.writeLong(mapTransaction.parent().version());
						//logger.info("\tWriting parent of transaction: " + mapTransaction.parent().version());
					}
					deltasLength = mapTransaction.deltas().length;
				}
				else{
					data.writeLong(mapTransaction.version());
					//logger.info("\tWriting parent of transaction: " + mapTransaction.version());
				}

				//Writes out the number of deltas
				data.writeInt(deltasLength);
				//logger.info("\tWriting number of deltas: " + deltasLength);

				for (int j = 0; j < deltasLength; j++) {
					MapDelta<Tuple, T> mapDelta = deltasOfTransaction[j];
					//Writes out key
					Tuple tuple = mapDelta.key();
					//logger.info("\t\tWriting key: " + mapDelta.key());

					for (int k = 0; k < relation.arity(); k++) {
						data.writeInt(tuple.get(k));
					}

					//Writes out new and old value
					if (mapDelta.oldValue() == null) serializerStrategy.writeValue(data, relation.defaultValue());
					else serializerStrategy.writeValue(data, mapDelta.oldValue());
					//logger.info("\t\tWriting oldValue:  " + mapDelta.oldValue());

					serializerStrategy.writeValue(data, mapDelta.newValue());
					//logger.info("\t\tWriting newValue:  " + mapDelta.newValue());
				}
			}
		}
		else{
			//Writes out the version and the parent id of the mapTransaction
			data.writeLong(0);
			//logger.info("\tWriting version of transaction: " + 0);

			data.writeLong(-1);
			//logger.info("\tWriting parent of transaction: -1");

			//Writes out the number of deltas
			data.writeInt(0);
			//logger.info("\tWriting number of deltas: " + 0);
		}
	}

	public <T> VersionedMapStoreDeltaImpl<Tuple,T> readDeltaStore(Symbol<T> relation, DataInputStream data, SerializerStrategy<T> serializerStrategy, ModelStoreWithError modelStoreWithError) {
		//HashMap<Long, MapTransaction<Tuple, T>> mapTransactionArray = new HashMap<>();
		long lastSuccessful = 0;
		var mapStore =
				(VersionedMapStoreDeltaImpl<Tuple,T>)	VersionedMapStoreBuilder.<Tuple, T>builder().setDefaultValue(relation.defaultValue()).buildOne();

		Map<Long, MapTransaction<Tuple, T>> statesHasMap = mapStore.internalExposeStates();

		long version = 0;
		try{
			while(data.available()!=0){
				version = data.readLong();
				//logger.info("\tReading version of transaction: " + version);
				long parent = data.readLong();
				//logger.info("\tReading parent of transaction: " + parent);
				int deltasLength = data.readInt();
				//logger.info("\tReading number of deltas: " + deltasLength);

				if(deltasLength == 0){
					MapTransaction<Tuple, T> parentTransaction = statesHasMap.get(parent);
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
						//logger.info("\t\tReading tuple: " + tuple);

						//Reads the old and new value
						var oldValue = serializerStrategy.readValue(data);
						//logger.info("\t\tReading oldValue: " + oldValue);

						var newValue = serializerStrategy.readValue(data);
						//logger.info("\t\tReading newValue: " + newValue);
						deltas[j] = new MapDelta<>(tuple, oldValue, newValue);
					}
					if(parent == -1){
						//noinspection unchecked
						statesHasMap.put(version, new MapTransaction<Tuple, T>(deltas, version, null));
					}
					else{
						MapTransaction<Tuple, T> parentTransaction = statesHasMap.get(parent);
						//noinspection unchecked
						statesHasMap.put(version, new MapTransaction<Tuple, T>(deltas, version, parentTransaction));
					}
				}
				lastSuccessful = version;
				System.out.println();
			}
		}
		catch(IOException e){
			modelStoreWithError.setException(new IOException("Incomplete MapStore in file"));
			//If it's the first store that is interrupted, then sets the lastSuccessfulTransaction to the last
			// fully read version
			if(modelStoreWithError.getLastSuccessfulTransactionVersion() == -1){
				modelStoreWithError.setLastSuccessfulTransactionVersion(lastSuccessful);
			}
			//If it's not the first interrupted store, the lastSuccessfulTransaction is the minimum of the two last
			else if(modelStoreWithError.getLastSuccessfulTransactionVersion() > lastSuccessful){
				modelStoreWithError.setLastSuccessfulTransactionVersion(lastSuccessful);
			}

			return mapStore;
		}
		modelStoreWithError.setLastSuccessfulTransactionVersion(version);
		return mapStore;
	}
}
