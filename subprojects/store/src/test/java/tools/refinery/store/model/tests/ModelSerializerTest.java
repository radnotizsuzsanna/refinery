package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.Version;
import tools.refinery.store.map.internal.delta.MapDelta;
import tools.refinery.store.map.internal.delta.MapTransaction;
import tools.refinery.store.model.*;
import tools.refinery.store.model.internal.ModelVersion;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ModelSerializerTest {
	static Symbol friend = new Symbol("friend", 2, Boolean.class, false);
	static Symbol age = new Symbol("age", 1, Integer.class, null);

	@Test
	void simpleModelSerializerTest() {
		ModelSerializer modelSerializer = new ModelSerializer();
		modelSerializer.addSerializeStrategy(Boolean.class, new TupleBooleanSerializer());
		modelSerializer.addSerializeStrategy(Integer.class, new TupleIntegerSerializer());

		var store = ModelStore.builder()
				.symbols(friend, age)
				.build();

		var model = store.createEmptyModel();
		model.getInterpretation(friend).put(Tuple.of(1, 2), true);
		model.getInterpretation(friend).put(Tuple.of(2, 1), true);

		model.getInterpretation(age).put(Tuple.of(1), 22);
		model.getInterpretation(age).put(Tuple.of(2), 23);

		var version1 = model.commit();

		model.getInterpretation(age).put(Tuple.of(2), 24);

		var version2 = model.commit();

		if(version1 instanceof ModelVersion && version2 instanceof ModelVersion) {
			List<ModelVersion> modelVersions = List.of((ModelVersion) version1, (ModelVersion) version2);

			try {
				File dataFile = initializeAndGetFile("data");
				File leafNodesFIle = initializeAndGetFile("leafNodes");
				modelSerializer.write(modelVersions, model.getStore(), dataFile, leafNodesFIle);
				List<ModelVersion>  modelVersions2 = modelSerializer.read(model.getStore(), dataFile, leafNodesFIle);
				compareModels(modelVersions, modelVersions2, model.getStore());
			} catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}



	//TODO szöveggel térjen vissza
	boolean compareModels(List<ModelVersion>  modelVersions1, List<ModelVersion>  modelVersions2,
						  ModelStore modelStore){
		var symbols = modelStore.getSymbols();
		if (modelVersions1.size() != modelVersions2.size()) return false;
		else{
			for(int i = 0; i < modelVersions1.size(); i++){
				ModelVersion modelVersion1 = modelVersions1.get(i);
				ModelVersion modelVersion2 = modelVersions2.get(i);
				for(int j = 0; j < symbols.size(); j++){
					MapTransaction version1 = (MapTransaction) ModelVersion.getInternalVersion(modelVersion1,j);
					MapTransaction version2 = (MapTransaction) ModelVersion.getInternalVersion(modelVersion2,j);
					//TODO csak az egyik null
					while (version1 != null || version2 != null) {
						if(version1 == null || version2 == null) return false;
						MapDelta[] deltas1 = version1.deltas();
						MapDelta[] deltas2 = version2.deltas();
						if (deltas1.length != deltas2.length) return false;
						else {
							for (int k = 0; k < deltas1.length; k++) {
								if (!Objects.equals(deltas1[k].key(), deltas2[k].key())) return false;
								else if (!Objects.equals(deltas1[k].oldValue(),deltas2[k].oldValue())) return false;
								else if (!Objects.equals(deltas1[k].newValue(), deltas2[k].newValue())) return false;
							}
						}
						version1 = version1.parent();
						version2 = version2.parent();
					}
				}
			}
		}
		return true;
	}

	File initializeAndGetFile(String fileName) throws FileNotFoundException {
		//TODO ehelyett valami univerzalis
		File file = new File("D:\\0Egyetem\\Refinery\\szakdoga\\" + fileName + ".txt");
		PrintWriter writer = new PrintWriter(file);
		writer.print("");
		writer.close();
		return file;
	}
}
