package tools.refinery.store.model;

import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.map.internal.MapDelta;
import tools.refinery.store.map.internal.MapTransaction;
import tools.refinery.store.model.representation.Relation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TupleIntegerSerializer<T> implements SerializerStrategy<T>{

	@Override
	public void writeRelation(Relation<?> relation, DataOutputStream relations, HashMap<Relation<?>, DataOutputStream> streams, VersionedMapStore<?, ?> deltaStore) throws IOException {
		//Writes out Relation ValueType
		Class<?> valueTypeClass = relation.getValueType();
		String valueTypeString = valueTypeClass.toString();
		//TODO ez így kicsit csúnya, de a toString odatette a class -t az osztály elé, ami miatt a Class.forName() nem tudta visszaalakítani
		valueTypeString = valueTypeString.replace("class ", "");
		byte[] valueTypeByte = valueTypeString.getBytes(StandardCharsets.UTF_8);
		relations.writeInt(valueTypeByte.length);
		relations.write(valueTypeByte);
		System.out.println("\nWriting Relation valueType: " + valueTypeString);

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
		relations.writeInt((int) relation.getDefaultValue());
		System.out.println("Writing Relation defaultValue: " + relation.getDefaultValue());

		//Writes out tuple length
		int tupleLength = relation.getArity();
		relations.writeInt(tupleLength);
		System.out.println("Writing tupleLength: " + tupleLength);
	}

	@Override
	public Relation<?> readRelation(DataInputStream relations, HashMap<Relation<?>, DataInputStream> streams) throws IOException {
		//Reads Relation name
		int length = relations.readInt();
		byte[] nameByte = new byte[length];
		relations.readFully(nameByte);
		String name = new String(nameByte, StandardCharsets.UTF_8);
		System.out.println("\nReading Relation name: " + name);

		//Reads Relation arity
		int arity = relations.readInt();
		System.out.println("Reading Relation arity: " + arity);

		//Reads defaultValue
		int defaultValue = relations.readInt();
		System.out.println("Reading Relation defaultValue: " + defaultValue);

		//Reads Tuple length
		int tupleLength = relations.readInt();
		System.out.println("Reading tupleLength: " + tupleLength);

		//Creates Relation
		var relation = new Relation<>(name, arity, Integer.class, defaultValue);
		System.out.println("Relation created: " + relation.getName());

		//Creates VersionedMapStoreDeltaImpl
		DataInputStream data = (DataInputStream) streams.get(relation);

		return relation;
	}

	@Override
	public void writeDeltaStore(Relation<?> relation, VersionedMapStoreDeltaImpl<?, ?> mapStore, DataOutputStream data) throws IOException {
		for(int i = 0; i < mapStore.getStates().size(); i++){
			MapTransaction<?, ?> mapTransaction = mapStore.getState(i);
			MapDelta<?, ?>[] deltasOfTransaction = mapTransaction.deltas();

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
				if (mapDelta.oldValue() == null) data.writeInt(0);
				else data.writeInt((int) mapDelta.oldValue());
				System.out.println("\t\tWriting oldaValue:  " + mapDelta.oldValue());

				data.writeInt((int) mapDelta.newValue());
				System.out.println("\t\tWriting newValue:  " + mapDelta.newValue());

			}
		}
	}

	@Override
	public VersionedMapStoreDeltaImpl<?, ?> readDeltaStore(Relation<?> relation, DataInputStream data) throws IOException {
		LongObjectHashMap<MapTransaction<Tuple, Integer>> mapTransactionArray = new LongObjectHashMap<>();
		try{
			while(data.available()!=0){
				long version = data.readLong();
				System.out.println("\tReading version of transaction: " + version);
				long parent = data.readLong();
				System.out.println("\tReading parent of transaction: " + parent);
				int num = data.readInt();
				System.out.println("\tReading number of deltas: " + num);

				@SuppressWarnings({"unchecked"})
				var deltas = (MapDelta<Tuple, Integer>[]) new MapDelta[num];
				for(int j = 0; j < num; j++){
					//Reads the elements of the tuple
					int[] tupleArray = new int[relation.getArity()];
					for(int k = 0; k < relation.getArity(); k++){
						tupleArray[k] = data.readInt();
					}
					Tuple tuple = Tuple.of(tupleArray);
					System.out.println("\t\tReading tuple: " + tuple);

					//Reads the old and new value
					int oldValue = data.readInt();
					System.out.println("\t\tReading oldValue: " + oldValue);

					int newValue = data.readInt();
					System.out.println("\t\tReading newValue: " + newValue);
					deltas[j] = new MapDelta<>(tuple, oldValue, newValue);
				}
				if(parent == -1){
					mapTransactionArray.put(parent, new MapTransaction<>(deltas, version, null));
				}
				else{
					MapTransaction<Tuple, Integer> parentTransaction = mapTransactionArray.get(parent);
					mapTransactionArray.put(parent, new MapTransaction<>(deltas, version, parentTransaction));
				}
			}
		}
		catch(IOException e){
			throw new IOException("Incomplete MapStore in file");
		}

		int defaultValue = (int) relation.getDefaultValue();
		return new VersionedMapStoreDeltaImpl<>(defaultValue, mapTransactionArray);
	}
}
