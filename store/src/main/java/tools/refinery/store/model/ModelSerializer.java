package tools.refinery.store.model;

import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.map.internal.MapDelta;
import tools.refinery.store.map.internal.MapTransaction;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Sushi, Inci
 *
 */
public class ModelSerializer {
	public void write(ModelStore store, DataOutputStream relations, HashMap<Relation<?>, DataOutputStream> streames) throws IOException {
		if (store instanceof ModelStoreImpl impl) {
			for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : impl.stores.entrySet()) {

				DataRepresentation<?, ?> dataRepresentation = entry.getKey();
				if (dataRepresentation instanceof Relation<?> relation) {
					VersionedMapStore<?, ?> mapStore = entry.getValue();
					if (mapStore instanceof VersionedMapStoreDeltaImpl<?, ?> deltaStore) {
						//Writes out Relation name
						String name = relation.getName();
						byte[] nameByte = name.getBytes("UTF-8");
						relations.writeInt(nameByte.length);
						relations.write(nameByte);
						System.out.println("\nWriting Relation name: " + relation.getName());

						//Writes out Realtion arity
						relations.writeInt((int) relation.getArity());
						System.out.println("Writing Relation arity: " + relation.getArity());

						//Writes out defaultValue
						relations.writeBoolean((boolean) relation.getDefaultValue());
						System.out.println("Writing Relation defaultValue: " + relation.getDefaultValue());

						//Writes out tuple length
						int tupleLength = relation.getArity();
						relations.writeInt(tupleLength);
						System.out.println("Writing tupleLength: " + tupleLength);

						writeDeltaStore(relation, deltaStore, streames.get(relation));
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

	public ModelStore read(DataInputStream relations, HashMap<Relation<?>, DataInputStream> streames) throws IOException {
		Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> stores = new HashMap<>();

		int i = 0;
		try{
			while(relations.available()!=0){
				//Relation name bolvasása
				int length = relations.readInt();
				byte[] nameByte = new byte[length];
				relations.readFully(nameByte);
				String name = new String(nameByte,"UTF-8");
				System.out.println("\nReading Relation name: " + name);

				//Relation aritás beolvasása
				int arity = relations.readInt();
				System.out.println("Reading Relation arity: " + arity);

				//Relation defaultValue beolvasása
				boolean defaultValue = relations.readBoolean();
				System.out.println("Reading Relation defaultValue: " + defaultValue);

				//Tuple length beolvasasa
				int tupleLength = relations.readInt();
				System.out.println("Reading tupleLength: " + tupleLength);

				//Relation létrehozása
				var relation = new Relation<>(name, arity, Boolean.class, defaultValue);
				System.out.println("Relation created: " + relation.getName());

				//VersionedMapStoreDeltaImpl létrehozása
				DataInputStream data = streames.get(relation);
				VersionedMapStoreDeltaImpl<?,?> mapStore = readDeltaStore(relation, data);
				System.out.println("VersionedMapStoreDeltaImpl created.");

				//ModelStore létrehozása Relationből és VersionedMapStoreDeltaImpl-ből
				stores.put(relation, mapStore);
				i++;
			}
		}
		//TODO ezt hogyan lehet szépen
		catch (IOException e){
			if(e.getMessage().compareTo("Incomplete MapStore in file") == 0) throw e;
			else throw new IOException("Incomplete Relation in file");
		}


		ModelStore store = new ModelStoreImpl(stores);

		relations.close();
		return store;
	}

	protected void writeDeltaStore(Relation<?> relation, VersionedMapStoreDeltaImpl<?,?> mapStore, DataOutputStream data) throws IOException {
		for(int i = 0; i < mapStore.getStates().size(); i++){
			MapTransaction<?, ?>  mapTransaction = mapStore.getState(i);
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

			for(int j = 0; j < deltasOfTransaction.length; j++){
				//Writes out key
				Tuple tuple = (Tuple) deltasOfTransaction[j].key();
				System.out.println("\t\tWriting key: " + deltasOfTransaction[j].key());

				for(int k = 0; k < relation.getArity(); k++){
					data.writeInt(tuple.get(k));
				}

				//Writes out new and old value
				if(deltasOfTransaction[j].oldValue() == null) data.writeBoolean(false);
				else data.writeBoolean((boolean) deltasOfTransaction[j].oldValue());
				System.out.println("\t\tWriting oldaValue:  " + deltasOfTransaction[j].oldValue());

				data.writeBoolean((boolean) deltasOfTransaction[j].newValue());
				System.out.println("\t\tWriting newValue:  " +  deltasOfTransaction[j].newValue());

			}
		}
	}

	protected VersionedMapStoreDeltaImpl<?,?> readDeltaStore(Relation<?> relation, DataInputStream data) throws IOException {
		LongObjectHashMap<MapTransaction<Tuple, Boolean>> mapTransactionArray = new LongObjectHashMap<>();
		try{
			while(data.available()!=0){
				long version = data.readLong();
				System.out.println("\tReading version of transaction: " + version);
				long parent = data.readLong();
				System.out.println("\tReading parent of transaction: " + parent);
				int num = data.readInt();
				System.out.println("\tReading number of deltas: " + num);

				@SuppressWarnings({"unchecked"})
				var deltas = (MapDelta<Tuple, Boolean>[]) new MapDelta[num];
				for(int j = 0; j < num; j++){
					//Reads the elements of the tuple
					int[] tupleArray = new int[relation.getArity()];
					for(int k = 0; k < relation.getArity(); k++){
						tupleArray[k] = data.readInt();
					}
					Tuple tuple = Tuple.of(tupleArray);
					System.out.println("\t\tReading tuple: " + tuple);

					//Reads the old and new value
					boolean oldValue = data.readBoolean();
					System.out.println("\t\tReading oldValue: " + oldValue);

					boolean newValue = data.readBoolean();
					System.out.println("\t\tReading newValue: " + newValue);
					deltas[j] = new MapDelta<>(tuple, oldValue, newValue);
				}
				if(parent == -1){
					mapTransactionArray.put(parent, new MapTransaction<>(deltas, version, null));
				}
				else{
					MapTransaction<Tuple, Boolean> parentTransaction = mapTransactionArray.get(parent);
					mapTransactionArray.put(parent, new MapTransaction<>(deltas, version, parentTransaction));
				}
			}
		}
		catch(IOException e){
			throw new IOException("Incomplete MapStore in file");
		}

		boolean defaultValue = (boolean) relation.getDefaultValue();
		return new VersionedMapStoreDeltaImpl<>(defaultValue, mapTransactionArray);
	}
}
