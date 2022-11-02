package tools.refinery.store.model;

import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.map.internal.MapDelta;
import tools.refinery.store.map.internal.MapTransaction;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

import java.io.*;
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
	public void write(ModelStore store, DataOutputStream data) throws IOException {
		if (store instanceof ModelStoreImpl impl) {
						for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : impl.stores.entrySet()) {
				DataRepresentation<?, ?> dataRepresentation = entry.getKey();
				if (dataRepresentation instanceof Relation<?> relation) {
					VersionedMapStore<?, ?> mapStore = entry.getValue();
					if (mapStore instanceof VersionedMapStoreDeltaImpl<?, ?> deltaStore) {
						//TODO Hash providert ki kell írni?
						//Relation name kiírása
						String name = relation.getName();
						byte[] nameByte = name.getBytes("UTF-8");
						data.writeInt(nameByte.length);
						data.write(nameByte);
						System.out.println("\tWriting Relation name: " + relation.getName());

						//Realtion arity kiírása
						data.writeInt((int) relation.getArity());
						System.out.println("\tWriting Relation arity: " + relation.getArity());

						//Relation defaultValue kiírása
						data.writeInt((int) relation.getDefaultValue());
						System.out.println("\tWriting Relation defaultValue: " + relation.getDefaultValue());

						writeDeltaStore(relation, deltaStore, data);
					} else {
						throw new UnsupportedOperationException("Only delta stores are supported!");
					}
				} else {
					throw new UnsupportedOperationException(
							"Only Relation representations are supported during serialization.");
				}
			}
			data.flush();
			data.close();
		}
	}

	//Ez a függvény jelenleg egy DatainputStreamet tud beolvasni, szóval egy ModelStore-t
	public ModelStore read(DataInputStream data) throws IOException {
		//Relation name bolvasása
		int length = data.readInt();
		byte[] nameByte = new byte[length];
		data.readFully(nameByte);
		String name = new String(nameByte,"UTF-8");
		System.out.println("\tReading name: " + name);

		//Relation aritás beolvasása
		int arity = data.readInt();
		System.out.println("\tReading Relation arity: " + arity);

		//Relation defaultValue beolvasása
		int defaultValue = data.readInt();
		System.out.println("\tReading Relation defaultValue: " + defaultValue);

		//Relation létrehozása
		Relation relation = new Relation(name, arity, defaultValue);
		System.out.println("\tRelation created: " + relation.getName());

		//VersionedMapStoreDeltaImpl létrehozása
		VersionedMapStoreDeltaImpl<?,?> mapStore = readDeltaStore(relation, data);
		System.out.println("\tVersionedMapStoreDeltaImpl created.");

		//ModelStore létrehozása Relationből és VersionedMapStoreDeltaImpl-ből
		Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> stores = new HashMap<>();
		stores.put(relation, mapStore);
		ModelStore store = new ModelStoreImpl(stores);

		data.close();
		return store;
	}

	protected void writeDeltaStore(Relation<?> relation, VersionedMapStoreDeltaImpl<?,?> mapStore, DataOutputStream data) throws IOException {
		System.out.println("\nSaving store for " + relation.getName());

		//Fájlba írja a defaultValue-ját a mapStore-nak
		data.writeBoolean((boolean) mapStore.getDefaultValue());
		System.out.println("\tWriting defaultValue: " + mapStore.getDefaultValue());

		//Fájlba irja Tuple-l hosszát
		int tupleLength = relation.getArity();
		data.writeInt(tupleLength);
		System.out.println("\tWriting tupleLength: " + tupleLength);

		//Vegigmegy a mapTransaction-okon TODO olyan teszt eset ahol több van mint egy
		for(int i = 0; i < mapStore.getStates().size(); i++){
			MapTransaction<?, ?>  mapTransaction = mapStore.getState(i);
			MapDelta<?, ?>[] deltasOfTransaction = mapTransaction.deltas();

			//Fájlba írja a mapTransaction versionjet, parent id-jét és db számát
			data.writeLong(mapTransaction.version());
			System.out.println("\t\tWriting version of transaction: " + mapTransaction.version());

			if(mapTransaction.parent() == null) {
				data.writeLong(-1);
				System.out.println("\t\tWriting parent of transaction: -1");
			}
			else{
				data.writeLong(mapTransaction.parent().version());
				System.out.println("\t\tWriting parent of transaction: " + mapTransaction.parent().version());
			}

			data.writeInt(mapTransaction.deltas().length);
			System.out.println("\t\tWriting number of deltas: " + mapTransaction.deltas().length);

			//Vegigmegy a deltakon
			for(int j = 0; j < deltasOfTransaction.length; j++){
				//Fájlba írja a key-t
				Tuple tuple = (Tuple) deltasOfTransaction[j].key();
				System.out.println("\t\t\tWriting key: " + deltasOfTransaction[j].key());

				for(int k = 0; k < tupleLength; k++){
					data.writeInt(tuple.get(k));
				}

				//Régi és új értékek fájlba írása TODO miért lehet null?
				if(deltasOfTransaction[j].oldValue() == null) data.writeBoolean(false);
				else data.writeBoolean((boolean) deltasOfTransaction[j].oldValue());
				System.out.println("\t\t\tWriting oldaValue:  " + deltasOfTransaction[j].oldValue());

				data.writeBoolean((boolean) deltasOfTransaction[j].newValue());
				System.out.println("\t\t\tWriting newValue:  " +  deltasOfTransaction[j].newValue());

			}
		}
	}

	protected VersionedMapStoreDeltaImpl<?,?> readDeltaStore(Relation<?> relation, DataInputStream data) throws IOException {
		System.out.println("\nLoading store for " + relation.getName());
		boolean defaultValue = data.readBoolean();
		System.out.println("\tReading defaultValue: " + defaultValue);

		int tupleLength = data.readInt();
		System.out.println("\tReading tupleLength: " + tupleLength);

		//TODO honnan tudom meddig kell menni?
		for(int i = 0; i < 1; i++){
			long version = data.readLong();
			System.out.println("\t\tReading version of transaction: " + version);
			long parent = data.readLong();
			//TODO -1 -> null?
			System.out.println("\t\tReading parent of transaction: " + parent);
			int num = data.readInt();
			System.out.println("\t\tReading number of deltas: " + num);

			MapDelta<Boolean, Boolean>[] deltas = new MapDelta[num];
			for(int j = 0; j < num; j++){

				//Reads the elements of the tuple
				int[] tupleArray = new int[tupleLength];
				for(int k = 0; k < tupleLength; k++){
					tupleArray[i] = data.readInt();
				}
				Tuple tuple = Tuple.of(tupleArray);
				System.out.println("\t\t\tReading tuple: " + tuple);

				//Reads the old and new value
				boolean oldValue = data.readBoolean();
				System.out.println("\t\t\tReading oldValue: " + oldValue);

				boolean newValue = data.readBoolean();
				System.out.println("\t\t\tReading newValue: " + newValue);
				deltas[i] = new MapDelta<Boolean, Boolean>(tuple, oldValue, newValue);
			}
			MapTransaction<Boolean, Boolean>  transaction = new MapTransaction<>(deltas, version, parent);
		}

		//TODO ezt nem tudom hogy kell aaa
		VersionedMapStoreDeltaImpl<Tuple, Boolean> mapStore= new VersionedMapStoreDeltaImpl<>(defaultValue);

		return mapStore;
	}
}
