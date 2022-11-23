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
						//Writes out Relation ValueType
						Class<?> valueTypeClass = relation.getValueType();
						String valueTypeString = valueTypeClass.toString();
						//TODO ez így kicsit csúnya, de a toString odatette a class -t az osztály elé, ami miatt a Class.forName() nem tudta visszaalakítani
						valueTypeString = valueTypeString.replace("class ", "");
						byte[] valueTypeByte = valueTypeString.getBytes(StandardCharsets.UTF_8);
						relations.writeInt(valueTypeByte.length);
						relations.write(valueTypeByte);
						System.out.println("\nWriting Relation valueType: " + valueTypeString);

						//Set the strategy
						SerializerStrategy serializerStrategy = serializerStrategyMap.get(valueTypeClass);

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

						//Writes out tuple length
						int tupleLength = relation.getArity();
						relations.writeInt(tupleLength);
						System.out.println("Writing tupleLength: " + tupleLength);

						writeDeltaStore(relation,deltaStore, streams.get(relation), serializerStrategy);

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

		try{
			while(relations.available()!=0){
				//Reads Relation valueType
				int length = relations.readInt();
				byte[] valueTypeByte = new byte[length];
				relations.readFully(valueTypeByte);
				String valueTypeString = new String(valueTypeByte, StandardCharsets.UTF_8);
				Class valueTypeClass = Class.forName(valueTypeString);
				System.out.println("\nReading Relation valueType: " + valueTypeString);

				SerializerStrategy<?> serializerStrategy = serializerStrategyMap.get(valueTypeClass);

				//Reads Relation name
				length = relations.readInt();
				byte[] nameByte = new byte[length];
				relations.readFully(nameByte);
				String name = new String(nameByte, StandardCharsets.UTF_8);
				System.out.println("\nReading Relation name: " + name);

				//Reads Relation arity
				int arity = relations.readInt();
				System.out.println("Reading Relation arity: " + arity);

				//Reads defaultValue
				var defaultValue = serializerStrategy.readValue(relations);
				System.out.println("Reading Relation defaultValue: " + defaultValue);

				//Reads Tuple length
				int tupleLength = relations.readInt();
				System.out.println("Reading tupleLength: " + tupleLength);

				//Creates Relation
				var relation = new Relation(name, arity, valueTypeClass , defaultValue);
				System.out.println("Relation created: " + relation.getName());


				//Creates VersionedMapStoreDeltaImp
				DataInputStream data = streams.get(relation);
				VersionedMapStoreDeltaImpl<?,?> mapStore = readDeltaStore(relation, data, serializerStrategy);
				System.out.println("VersionedMapStoreDeltaImpl created.");

				//Creates ModelStore from Relation and VersionedMapStoreDeltaImpl
				stores.put(relation, mapStore);
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

	public void writeDeltaStore(Relation<?> relation, VersionedMapStoreDeltaImpl<?,?> mapStore, DataOutputStream data, SerializerStrategy serializerStrategy) throws IOException {
		for(int i = 0; i < mapStore.getStates().size(); i++){
			MapTransaction<?, ?> mapTransaction = mapStore.getState(i);
			MapDelta<?,?>[] deltasOfTransaction = mapTransaction.deltas();

			//Writes out the version and the parent id of the mapTransaction
			data.writeLong(mapTransaction.version());
			System.out.println("\tWriting version of transaction: " + mapTransaction.version());

			if(mapTransaction.parent() == null) {
				data.writeLong(-1);
				System.out.println("\tWriting parent of transaction: -1");
			}
			else{
				data.writeLong(mapTransaction.parent().version());
				System.out.println("\tWriting parent of transaction: " + mapTransaction.parent().version());
			}

			//Writes out the number of deltas
			data.writeInt(mapTransaction.deltas().length);
			System.out.println("\tWriting number of deltas: " + mapTransaction.deltas().length);

			for (MapDelta<?, ?> mapDelta : deltasOfTransaction) {
				//Writes out key
				Tuple tuple = (Tuple) mapDelta.key();
				System.out.println("\t\tWriting key: " + mapDelta.key());

				for (int k = 0; k < relation.getArity(); k++) {
					data.writeInt(tuple.get(k));
				}

				//Writes out new and old value
				if (mapDelta.oldValue() == null) serializerStrategy.writeValue(data, relation.getDefaultValue());
				else serializerStrategy.writeValue(data, mapDelta.oldValue());
				System.out.println("\t\tWriting oldaValue:  " + mapDelta.oldValue());

				serializerStrategy.writeValue(data, mapDelta.newValue());
				System.out.println("\t\tWriting newValue:  " + mapDelta.newValue());
			}
		}
	}

	public VersionedMapStoreDeltaImpl<?,?> readDeltaStore(Relation<?> relation, DataInputStream data, SerializerStrategy serializerStrategy) throws IOException {
		LongObjectHashMap mapTransactionArray = new LongObjectHashMap<>();
		try{
			while(data.available()!=0){
				long version = data.readLong();
				System.out.println("\tReading version of transaction: " + version);
				long parent = data.readLong();
				System.out.println("\tReading parent of transaction: " + parent);
				int num = data.readInt();
				System.out.println("\tReading number of deltas: " + num);

				var deltas = new MapDelta[num];
				for(int j = 0; j < num; j++){
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
					mapTransactionArray.put(parent, new MapTransaction<>(deltas, version, null));
				}
				else{
					MapTransaction parentTransaction = (MapTransaction) mapTransactionArray.get(parent);
					mapTransactionArray.put(parent, new MapTransaction<>(deltas, version, parentTransaction));
				}
			}
		}
		catch(IOException e){
			throw new IOException("Incomplete MapStore in file");
		}

		var defaultValue = relation.getDefaultValue();
		return new VersionedMapStoreDeltaImpl(defaultValue, mapTransactionArray);
	}
}
