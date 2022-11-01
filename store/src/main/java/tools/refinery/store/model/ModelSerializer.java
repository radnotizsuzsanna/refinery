package tools.refinery.store.model;

import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.map.internal.MapDelta;
import tools.refinery.store.map.internal.MapTransaction;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

import java.io.*;
import java.util.Map.Entry;

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

	public ModelStore read(ModelStore store, DataInputStream data) throws IOException {
		if (store instanceof ModelStoreImpl impl) {
			for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : impl.stores.entrySet()) {
				DataRepresentation<?, ?> dataRepresentation = entry.getKey();
				if (dataRepresentation instanceof Relation<?> relation) {
					//TODO ezzel mit csináljunk?
					VersionedMapStoreDeltaImpl<?,?> mapStore = readDeltaStore(relation, data);
				}
			}
			data.close();
		}
		return null;
	}

	protected void writeDeltaStore(Relation<?> relation, VersionedMapStoreDeltaImpl<?,?> mapStore, DataOutputStream data) throws IOException {
		System.out.println("\nSaving store for " + relation.getName());

		//Fájlba írja a defaultValue-ját a mapStore-nak
		data.writeBoolean((boolean) mapStore.getDefaultValue());
		System.out.println("\tWriting defaultValue: " + mapStore.getDefaultValue());

		//Kiszámolja a tuple hosszát
		MapTransaction<?, ?> mapTransaction = mapStore.getState(0);
		MapDelta<?, ?>[] deltasOfTransaction = mapTransaction.deltas();
		int tupleLength = relation.getArity();

		//Fájlba Tuple-l hosszát
		data.writeInt(tupleLength);
		System.out.println("\tWriting tupleLength: " + tupleLength);

		//Vegigmegy a mapTransaction-okon TODO olyan teszt eset ahol több van mint egy
		for(int i = 0; i < mapStore.getStates().size(); i++){
			mapTransaction = mapStore.getState(i);
			deltasOfTransaction = mapTransaction.deltas();

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

		//TODO ezt nem tudom hogy kel aaa
		VersionedMapStoreDeltaImpl<Tuple, Boolean> mapStore= new VersionedMapStoreDeltaImpl<>(defaultValue);
		VersionedMap<Tuple, Boolean> versionedMap = mapStore.createMap();

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
			System.out.println("\t\tWriting number of deltas: " + num);

			for(int j = 0; j < num; j++){
				//TODO ezt a többi esetre hogyan? vagy nem kell?
				if(tupleLength == 1){
					int in = data.readInt();
					Tuple tuple = Tuple.of(in);
					System.out.println("\t\t\tWriting key: (" + tuple + ")");
				}
				else if(tupleLength == 2){
					int in1 = data.readInt();
					int in2 = data.readInt();
					Tuple tuple = Tuple.of(in1, in2);
					System.out.println("\t\t\tWriting key: (" + tuple.get(0) + " " + tuple.get(0) + ")");
				}
				else throw new UnsupportedOperationException(
						"Csak egy vagy ket elemű tuple-ek támogatottak");

				boolean oldValue = data.readBoolean();
				System.out.println("\t\t\tWriting oldValue: " + oldValue);

				boolean newValue = data.readBoolean();
				System.out.println("\t\t\tWriting newValue: " + newValue);
			}
		}

		return null;
	}


}
