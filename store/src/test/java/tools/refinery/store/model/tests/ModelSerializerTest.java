package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.*;
import tools.refinery.store.model.representation.Relation;

import java.util.Set;

class ModelSerializerTest {
	@Test
	void modelBuildingTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, false);
		Relation<Integer> age = new Relation<Integer>("age", 1, null);
		Relation<Boolean> friend = new Relation<>("friend", 2, false);

		ModelStore store = new ModelStoreImpl(Set.of(person, age, friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(age, Tuple.of(0), 3);
		model.put(age, Tuple.of(1), 1);
		model.put(friend, Tuple.of(0, 1), true);
		model.put(friend, Tuple.of(1, 0), true);

		model.commit();

		ModelSerializer serializer = new ModelSerializer();

		serializer.write(store, "nemtudomLOL");
	}
}
