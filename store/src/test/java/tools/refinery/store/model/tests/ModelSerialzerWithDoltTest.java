package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.*;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static tools.refinery.store.model.representation.TruthValue.TRUE;
import static tools.refinery.store.model.representation.TruthValue.UNKNOWN;

public class ModelSerialzerWithDoltTest {
	@Test
	void serializeModelWithDifferentTypesTest() throws IOException {
		Relation<Boolean> person = new Relation<>("person", 1, Boolean.class,false);
		Relation<Integer> age = new Relation<>("age", 1, Integer.class,0);
		Relation<Boolean> friend = new Relation<>("friend", 2, Boolean.class,false);
		Relation<TruthValue> girl = new Relation<>("girl", 1, TruthValue.class, UNKNOWN);

		ModelStore store = new ModelStoreImpl(Set.of(person, age, friend, girl));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(age, Tuple.of(0), 21);
		model.put(age, Tuple.of(1), 34);
		model.put(friend, Tuple.of(0, 1), true);
		model.put(girl, Tuple.of(0), TRUE);
		model.put(girl, Tuple.of(1), UNKNOWN);
		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializerWithDolt serializer = new ModelSerializerWithDolt();

		try {
			serializer.write(store);
		} catch (SQLException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
