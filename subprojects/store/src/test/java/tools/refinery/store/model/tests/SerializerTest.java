package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.Version;
import tools.refinery.store.map.internal.delta.MapDelta;
import tools.refinery.store.map.internal.delta.MapTransaction;
import tools.refinery.store.model.Serializer;
import tools.refinery.store.model.SerializerStrategy;
import tools.refinery.store.model.TupleBooleanSerializer;
import tools.refinery.store.model.TupleIntegerSerializer;
import tools.refinery.store.tuple.Tuple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class SerializerTest {
	@Test
	void serializeWithOneTypeTest() {
		MapTransaction<Tuple,Boolean> version;
		MapDelta<Tuple,Boolean > mapdelta2;
		MapDelta<Tuple,Boolean > mapdelta1;

		ArrayList<Version> versions = new ArrayList<>();

		mapdelta1 = new MapDelta<>(Tuple.of(1), null,true);
		mapdelta2 = new MapDelta<>(Tuple.of(2), null,true);
		MapDelta<Tuple,Boolean>[] mapDeltas = new MapDelta[] {mapdelta1, mapdelta2};
	 	version = new MapTransaction<>(mapDeltas, null, 0);

		MapTransaction<Tuple,Boolean> parent = version;
		mapdelta1 = new MapDelta<>(Tuple.of(3), null,true);
		mapDeltas = new MapDelta[] {mapdelta1};
		version = new MapTransaction<>(mapDeltas, parent, 1);
		versions.add(version);

		mapdelta1 = new MapDelta<>(Tuple.of(4), null,true);
		mapDeltas = new MapDelta[] {mapdelta1};
		version = new MapTransaction<>(mapDeltas, parent, 1);
		versions.add(version);

		Serializer serializer = new Serializer();
		SerializerStrategy<Boolean> strategy = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class, strategy);


		try {
			File dataFile = initializeAndGetFile("data");
			serializer.write(versions, dataFile);
			ArrayList<Version> versions2 = serializer.read(dataFile);
			assertTrue(compare(versions,versions2));
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void serializeWithDifferentTypesTest() {
		MapTransaction<Tuple,Boolean> version;
		MapDelta<Tuple,Boolean > mapdelta2;
		MapDelta<Tuple,Boolean > mapdelta1;

		ArrayList<Version> versions = new ArrayList<>();

		mapdelta1 = new MapDelta<>(Tuple.of(1), null,true);
		mapdelta2 = new MapDelta<>(Tuple.of(2), null,true);
		MapDelta<Tuple,Boolean>[] mapDeltas = new MapDelta[] {mapdelta1, mapdelta2};
		version = new MapTransaction<>(mapDeltas, null, 0);

		MapTransaction<Tuple,Boolean> parent = version;
		mapdelta1 = new MapDelta<>(Tuple.of(3), null,true);
		mapDeltas = new MapDelta[] {mapdelta1};
		version = new MapTransaction<>(mapDeltas, parent, 1);
		versions.add(version);

		MapTransaction<Tuple,Integer> version2;

		MapDelta<Tuple,Integer > mapdelta3;
		MapDelta<Tuple,Integer > mapdelta4;
		mapdelta3 = new MapDelta<>(Tuple.of(1), null,32);
		mapdelta4 = new MapDelta<>(Tuple.of(2), null,22);
		MapDelta[] mapDeltas2 = new MapDelta[] {mapdelta3, mapdelta4};
		version2 = new MapTransaction<>(mapDeltas2, null, 1);
		MapTransaction<Tuple,Integer> parent2 = version2;

		mapdelta3 = new MapDelta<>(Tuple.of(3), null,28);
		mapDeltas2 = new MapDelta[] {mapdelta3};
		version2 = new MapTransaction<>(mapDeltas2, parent2, 1);
		versions.add(version2);

		Serializer serializer = new Serializer();
		SerializerStrategy<Boolean> strategy = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class, strategy);
		SerializerStrategy<Integer> strategy2 = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class, strategy2);

		try {
			File file = initializeAndGetFile("data");
			serializer.write(versions, file);
			ArrayList<Version> versions2 = serializer.read(file);
			assertTrue(compare(versions,versions2));
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	Boolean compare(ArrayList<Version> versions1, ArrayList<Version> versions2){
		if(versions1.size() != versions2.size()) return false;
		else{
			for (int i = 0; i < versions1.size(); i++) {
				MapTransaction version1 = (MapTransaction) versions1.get(i);
				MapTransaction version2 = (MapTransaction) versions2.get(i);
				while(version1 != null && version2 != null){
					MapDelta[] deltas1 = version1.deltas();
					MapDelta[] deltas2 = version2.deltas();
					if(deltas1.length != deltas2.length) return false;
					else {
						for(int j = 0; j < deltas1.length; j ++){
							if(deltas1[j].key() != deltas2[j].key()) return false;
							else if(deltas1[j].oldValue() != deltas2[j].oldValue()) return false;
							else if(deltas1[j].newValue() != deltas2[j].newValue()) return false;
						}
					}
					version1 = version1.parent();
					version2 = version2.parent();
				}
			}
		}
		return true;
	}

	File initializeAndGetFile(String fileName) throws FileNotFoundException {
		File file =  new File("D:\\0Egyetem\\Refinery\\szakdoga\\"+fileName+".txt");
		PrintWriter writer = new PrintWriter(file);
		writer.print("");
		writer.close();
		return file;
	}
}
