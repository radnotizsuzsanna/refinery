package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.*;
import tools.refinery.store.model.representation.Relation;

import java.io.*;
import java.util.Set;

class ModelSerializerTest {
	@Test
	void modelBuildingTest() throws IOException {
		Relation<Boolean> person = new Relation<>("Person", 1, false);
		//Relation<Integer> age = new Relation<Integer>("age", 1, null);
		Relation<Boolean> friend = new Relation<>("friend", 2, false);

		ModelStore store = new ModelStoreImpl(Set.of(person, /*age,*/ friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		//model.put(age, Tuple.of(0), 3);
		//model.put(age, Tuple.of(1), 1);
		model.put(friend, Tuple.of(0, 1), true);
		model.put(friend, Tuple.of(1, 0), true);

		long firstVersion = model.commit();

		//Model model = store.createModel(3);

		//model.put(person, Tuple.of(0), false);
		//model.put(person, Tuple.of(1), false);

		//long secondVersion = model.commit();

		ModelSerializer serializer = new ModelSerializer();

		//Temporary file for serializing the ModelStore
		File file = File.createTempFile("test", ".txt");

		//Serializes the ModelStore
		try {
			FileOutputStream fileStream = new FileOutputStream(file);
			DataOutputStream data = new DataOutputStream(fileStream);
			serializer.write(store, data);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		//Deserializes the ModelStore
		try {
			InputStream input = new FileInputStream(file);
			DataInputStream data = new DataInputStream(input);
			ModelStore store2 = serializer.read(data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
