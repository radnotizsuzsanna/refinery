package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.Version;
import tools.refinery.store.map.internal.delta.MapDelta;
import tools.refinery.store.map.internal.delta.MapTransaction;
import tools.refinery.store.model.Serializer;
import tools.refinery.store.model.SerializerStrategy;
import tools.refinery.store.model.TupleBooleanSerializer;
import tools.refinery.store.tuple.Tuple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class SerializerTest {
	@Test
	void serializeTest() {
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
			ArrayList<File> files = initializeAndGetFiles(2);
			serializer.write(versions, files);
			ArrayList<Version> versions2 = serializer.read(files);
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

	ArrayList<File> initializeAndGetFiles(int n) throws FileNotFoundException {
		ArrayList<File> fileList = new ArrayList<>();
		for(int i = 0; i < n; i++){
			File file =  new File("D:\\0Egyetem\\Refinery\\szakdoga\\data"+i+".txt");
			PrintWriter writer = new PrintWriter(file);
			writer.print("");
			writer.close();
			fileList.add(file);
		}
		return fileList;
	}
}
