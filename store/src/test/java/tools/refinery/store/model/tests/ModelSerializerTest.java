package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.*;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.refinery.store.model.representation.TruthValue.TRUE;
import static tools.refinery.store.model.representation.TruthValue.UNKNOWN;

class ModelSerializerTest {
	@Test
	void modelBuildingTest() throws IOException {
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
		model.put(friend, Tuple.of(1, 0), true);
		model.put(girl, Tuple.of(0), TRUE);
		model.put(girl, Tuple.of(1), UNKNOWN);

		long firstVersion = model.commit();

		model.put(person, Tuple.of(0), false);
		model.put(person, Tuple.of(1), false);

		long secondVersion = model.commit();
		long thirdVersion = model.commit();


		ModelSerializer serializer = new ModelSerializer();

		//TODO
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);
		SerializerStrategy<TruthValue> strategyTruthValue = new TupleTruthValueSerializer();
		serializer.addStrategy(TruthValue.class, strategyTruthValue);


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
			//compareStores(store,store2);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void compareStores(ModelStore store, ModelStore store2){
		var dataRepresentationSet =  store.getDataRepresentations();
		HashMap<String, Class<?>> dataRepresentationHashMap = new HashMap<>();
		for (DataRepresentation<?, ?> item : dataRepresentationSet){
			dataRepresentationHashMap.put(item.getName(), item.getValueType());
		}
		var dataRepresentationSet2 =  store2.getDataRepresentations();
		HashMap<String, Class<?>> dataRepresentationHashMap2 = new HashMap<>();
		for (DataRepresentation<?, ?> item : dataRepresentationSet2){
			dataRepresentationHashMap2.put(item.getName(), item.getValueType());
		}
		assertTrue(dataRepresentationHashMap.size() == dataRepresentationHashMap2.size());
		assertTrue(dataRepresentationHashMap.equals(dataRepresentationHashMap2));
		//The two stores have the same amount of data reprezentations, and they contain the same name-valuetype pairs


		//var sotreStates= store.getStates();
		//var sotre2States= store2.getStates();


		assertTrue(store.getStates().equals(store2.getStates()));
		//The two stores have the same states

		//TODO Ezt hogy?
		HashSet <Long> states = new HashSet<>((Collection) store.getStates());

		//checks if the two modelStores' states' models are the same -> for every dataRepresentation, they have the same keys and values
		for (Long item : states){
			compareIfStatesHaveTheSameModel(store, store2, item);
		}
	}
	private void compareIfStatesHaveTheSameModel(ModelStore store, ModelStore store2, Long state){
		//gets the cursors with getall, the puts them in HashMaps, then compare
		var dataRepresentations = store.getDataRepresentations();
		Model model = store.createModel(state);
		Model model2 = store.createModel(state);
		HashMap<Object, Object> cursorMap = new HashMap();
		HashMap<Object, Object> cursorMap2 = new HashMap();
		for (DataRepresentation<?, ?> item : dataRepresentations){
			var cursor = model.getAll(item);
			var cursor2 = model2.getAll(item);
			var key = cursor.getKey();
			var value = cursor.getValue();
			cursorMap.put(key,value);
			var key2 = cursor2.getKey();
			var value2 = cursor2.getValue();
			cursorMap2.put(key2, value2);
		}
		assertTrue(cursorMap.equals(cursorMap2));
	}
}
