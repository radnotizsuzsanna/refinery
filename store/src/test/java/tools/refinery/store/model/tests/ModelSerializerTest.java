package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.*;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

class ModelSerializerTest {
	@Test
	void modelBuildingTest() throws IOException {
		Relation<Boolean> person = new Relation<>("person", 1, Boolean.class,false);
		Relation<Boolean> friend = new Relation<>("friend", 2, Boolean.class,false);

		ModelStore store = new ModelStoreImpl(Set.of(person, friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(friend, Tuple.of(0, 1), true);
		model.put(friend, Tuple.of(1, 0), true);

		long firstVersion = model.commit();

		model.put(person, Tuple.of(0), false);
		model.put(person, Tuple.of(1), false);

		long secondVersion = model.commit();

		ModelSerializer serializer = new ModelSerializer();

		//The HasMaps contain the DataStreams for serializing the MapStores (MapStores will be stored in separate files)
		HashMap<Relation<?>, DataOutputStream> streamMapOut = new HashMap<>();
		HashMap<Relation<?>, DataInputStream> streamMapIn = new HashMap<>();

		int numberOfRelations = store.getDataRepresentations().size();
		List< DataRepresentation<?,?>> list = store.getDataRepresentations().stream().toList();
		for(int i = 0; i < numberOfRelations; i++){
			PipedInputStream pipedInput = new PipedInputStream();
			PipedOutputStream pipedOutput = new PipedOutputStream();
			pipedInput.connect(pipedOutput);

			DataOutputStream dataOutputStream = new DataOutputStream(pipedOutput);
			DataInputStream dataInputStream = new DataInputStream(pipedInput);

			streamMapOut.put((Relation<?>) list.get(i), dataOutputStream);
			streamMapIn.put((Relation<?>) list.get(i), dataInputStream);
		}

		//DataStreams for serializing the Relations of the ModelStore
		PipedInputStream pipedInput = new PipedInputStream();
		PipedOutputStream pipedOutput = new PipedOutputStream();
		pipedInput.connect(pipedOutput);
		DataOutputStream relationsOutputStream = new DataOutputStream(pipedOutput);
		DataInputStream relationsInputStream = new DataInputStream(pipedInput);


		//Serializes the ModelStore
		try {
			serializer.write(store, relationsOutputStream, streamMapOut);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		//Deserializes the ModelStore
		try {
			ModelStore store2 = serializer.read(relationsInputStream, streamMapIn);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
