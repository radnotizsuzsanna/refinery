package tools.refinery.store.model;

import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
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
						//TODO: hol érdemes létrehozni a strategyt?
						SerializerStrategy<?> serializerStrategy = serializerStrategyMap.get(dataRepresentation.getValueType());
						//Writes out the Relation
						serializerStrategy.writeRelation(relation, relations, streams, deltaStore);
						//Writes out the MapStore
						serializerStrategy.writeDeltaStore(relation,  deltaStore, streams.get(relation));
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

	public ModelStore read(DataInputStream relations, HashMap<Relation<?>, DataInputStream> streams) throws IOException {
		Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> stores = new HashMap<>();

		int i = 0;
		try{
			while(relations.available()!=0){
				//Reads Relation valueType
				int length = relations.readInt();
				byte[] valueTypeByte = new byte[length];
				relations.readFully(valueTypeByte);
				String valueTypeString = new String(valueTypeByte, StandardCharsets.UTF_8);
				Class<?> valueTypeClass = Class.forName(valueTypeString);

				SerializerStrategy<?> serializerStrategy = serializerStrategyMap.get(valueTypeClass);

				//Creates Relation
				var relation = serializerStrategy.readRelation(relations, streams);
				System.out.println("Relation created: " + relation.getName());

				//Creates VersionedMapStoreDeltaImp
				DataInputStream data = streams.get(relation);
				VersionedMapStoreDeltaImpl<?,?> mapStore = serializerStrategy.readDeltaStore(relation, data);
				System.out.println("VersionedMapStoreDeltaImpl created.");

				//Creates ModelStore from Relation and VersionedMapStoreDeltaImpl
				stores.put(relation, mapStore);
				i++;
			}
		}
		//TODO ezt hogyan lehet szépen
		catch (IOException e){
			if(e.getMessage().compareTo("Incomplete MapStore in file") == 0) throw e;
			else throw new IOException("Incomplete Relation in file");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		ModelStore store = new ModelStoreImpl(stores);
		relations.close();
		return store;
	}
}
