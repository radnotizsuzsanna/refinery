package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.*;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.refinery.store.model.representation.TruthValue.TRUE;
import static tools.refinery.store.model.representation.TruthValue.UNKNOWN;

class ModelSerializerTest {
	HashMap<Relation<?>, DataInputStream> streamMapIn;
	HashMap<Relation<?>, DataOutputStream> streamMapOut;
	DataOutputStream relationsOutputStream;
	DataInputStream relationsInputStream;

	/**
	 * Tests if the ModelSerializer can serialize a model store with bool, int and TruthValue value types and Tuple key type.
	 * @throws IOException When the connection of the piped streams fails.
	 */
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
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);
		SerializerStrategy<TruthValue> strategyTruthValue = new TupleTruthValueSerializer();
		serializer.addStrategy(TruthValue.class, strategyTruthValue);

		List< DataRepresentation<?,?>> dataRepresentationList = store.getDataRepresentations().stream().toList();
		initializeStreamMapsWithPipedStreams(dataRepresentationList);
		initializeRelationStreamsWithPipedStream();

		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
			//Deserializes the ModelStore
			ModelStore store2 = serializer.read(relationsInputStream, streamMapIn);
			//Test if the ModelStore is the same after the serialization
			compareStores(store,store2);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tests if the ModelSerializer can serialize a model store with an empty map store
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializeModelWithEmptyMapStore() throws IOException{
		Relation<Boolean> person = new Relation<>("person", 1, Boolean.class,false);
		Relation<Integer> age = new Relation<>("age", 1, Integer.class,0);

		ModelStore store = new ModelStoreImpl(Set.of(person, age));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);

		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List< DataRepresentation<?,?>> dataRepresentationList = store.getDataRepresentations().stream().toList();
		initializeStreamMapsWithPipedStreams(dataRepresentationList);
		initializeRelationStreamsWithPipedStream();

		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
			//Deserializes the ModelStore
			ModelStore store2 = serializer.read(relationsInputStream, streamMapIn);
			//Test if the ModelStore is the same after the serialization
			compareStores(store,store2);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tests if the ModelSerializer can serialize a model store with multiple commit
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializerWithMultipleCommitTest() throws IOException{
		Relation<Boolean> person = new Relation<>("person", 1, Boolean.class,false);
		Relation<Integer> age = new Relation<>("age", 1, Integer.class,0);

		ModelStore store = new ModelStoreImpl(Set.of(person, age));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(age, Tuple.of(0), 21);
		model.put(age, Tuple.of(1), 34);

		model.commit();

		model.put(person, Tuple.of(0), false);
		model.put(person, Tuple.of(1), false);

		model.commit();
		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List< DataRepresentation<?,?>> dataRepresentationList = store.getDataRepresentations().stream().toList();
		initializeStreamMapsWithPipedStreams(dataRepresentationList);
		initializeRelationStreamsWithPipedStream();
		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
			//Deserializes the ModelStore
			ModelStore store2 = serializer.read(relationsInputStream, streamMapIn);
			//Test if the ModelStore is the same after the serialization
			compareStores(store,store2);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tests if the serializer can handle interrupted map store data while deserializing
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializationWithInterruptedMapStoreTest() throws IOException {
		Relation<Boolean> person = new Relation<>("person", 1, Boolean.class,false);
		Relation<Integer> age = new Relation<>("age", 1, Integer.class,0);

		ModelStore store = new ModelStoreImpl(Set.of(person, age));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(age, Tuple.of(0), 21);
		model.put(age, Tuple.of(1), 34);

		model.commit();

		model.put(person, Tuple.of(0), false);
		model.put(person, Tuple.of(1), false);

		model.commit();
		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List< DataRepresentation<?,?>> dataRepresentationList = store.getDataRepresentations().stream().toList();

		streamMapIn = new HashMap<>();
		streamMapOut = new HashMap<>();

		HashMap<Relation<?>, ByteArrayOutputStream> byteArrayOutputMap = new HashMap<>();
		for (DataRepresentation<?, ?> dataRepresentation : dataRepresentationList) {
			ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
			byteArrayOutputMap.put((Relation<?>) dataRepresentation, byteArrayOutput);
			DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutput);
			streamMapOut.put((Relation<?>) dataRepresentation, dataOutputStream);
		}

		initializeRelationStreamsWithPipedStream();

		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		//HashMap<Relation<?>, ByteArrayInputStream> byteArrayInputMap = new HashMap<>();
		for (DataRepresentation<?, ?> dataRepresentation : dataRepresentationList) {
			var byteArrayOutput = byteArrayOutputMap.get(dataRepresentation);
			byte[] byteArray = byteArrayOutput.toByteArray();
			//Creates the  ByteArrayInputStream with only 10 bytes of the byteArray so the mapStore's data will be interrupted
			ByteArrayInputStream byteArrayInput = new ByteArrayInputStream(byteArray, 0, 10);
			DataInputStream dataInputStream = new DataInputStream(byteArrayInput);
			streamMapIn.put((Relation<?>) dataRepresentation, dataInputStream);
		}

		try {
			//Deserializes the ModelStore
			ModelStore store2 = serializer.read(relationsInputStream, streamMapIn);
			//Test if the ModelStore is the same after the serialization
			compareStores(store,store2);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tests if the serializer can handle interrupted relation data while deserializing
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializationWithInterruptedRelationTest() throws IOException {
		Relation<Boolean> person = new Relation<>("person", 1, Boolean.class,false);
		Relation<Integer> age = new Relation<>("age", 1, Integer.class,0);

		ModelStore store = new ModelStoreImpl(Set.of(person, age));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(age, Tuple.of(0), 21);
		model.put(age, Tuple.of(1), 34);

		model.commit();

		model.put(person, Tuple.of(0), false);
		model.put(person, Tuple.of(1), false);

		model.commit();
		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List< DataRepresentation<?,?>> dataRepresentationList = store.getDataRepresentations().stream().toList();
		initializeStreamMapsWithPipedStreams(dataRepresentationList);

		ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
		relationsOutputStream = new DataOutputStream(byteArrayOutput);

		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		byte[] byteArray = byteArrayOutput.toByteArray();
		//Creates the  ByteArrayInputStream with only 2 bytes of the byteArray so the relation's data will be interrupted
		ByteArrayInputStream byteArrayInput = new ByteArrayInputStream(byteArray, 0, 2);
		DataInputStream relationsInputStream = new DataInputStream(byteArrayInput);

		try {
			//Deserializes the ModelStore
			ModelStore store2 = serializer.read(relationsInputStream, streamMapIn);
			//Test if the ModelStore is the same after the serialization
			compareStores(store,store2);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initializes the streamMapIn and streamMapOut maps with piped streams for serializing the map stores.
	 * @param dataRepresentationList The list of the data representations of the model store.
	 * @throws IOException When the connecting of the piped streams fails.
	 */
	void initializeStreamMapsWithPipedStreams(List< DataRepresentation<?,?>> dataRepresentationList) throws IOException {
		//The HasMaps contain the DataStreams for serializing the MapStores (MapStores will be stored in separate files)
		streamMapIn = new HashMap<>();
		streamMapOut = new HashMap<>();

		PipedOutputStream pipedOutput;
		PipedInputStream pipedInput;
		for (DataRepresentation<?, ?> dataRepresentation : dataRepresentationList) {
			pipedInput = new PipedInputStream();
			pipedOutput = new PipedOutputStream();
			pipedInput.connect(pipedOutput);

			DataOutputStream dataOutputStream = new DataOutputStream(pipedOutput);
			DataInputStream dataInputStream = new DataInputStream(pipedInput);

			streamMapOut.put((Relation<?>) dataRepresentation, dataOutputStream);
			streamMapIn.put((Relation<?>) dataRepresentation, dataInputStream);
		}
	}

	/**
	 * Initializes the streams for serializing the relations with piped streams.
	 * @throws IOException  When the connecting of the piped streams fails.
	 */
	void initializeRelationStreamsWithPipedStream() throws IOException {
		PipedInputStream pipedInput = new PipedInputStream();
		PipedOutputStream pipedOutput = new PipedOutputStream();
		pipedInput.connect(pipedOutput);

		relationsOutputStream = new DataOutputStream(pipedOutput);
		relationsInputStream = new DataInputStream(pipedInput);
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
		assertEquals(dataRepresentationHashMap.size(), dataRepresentationHashMap2.size());
		assertEquals(dataRepresentationHashMap, dataRepresentationHashMap2);

		//The two stores have the same amount of data reprezentations, and they contain the same name-valuetype pairs
		assertEquals(store.getStates(), store2.getStates());

		//The two stores have the same states
		store.getStates().forEach((item) -> {
			compareIfStatesHaveTheSameModel(store, store2, item);
		});
	}
	private void compareIfStatesHaveTheSameModel(ModelStore store, ModelStore store2, Long state){
		//System.out.println("state: " + state);
		//gets the cursors with getall, the puts them in HashMaps, then compare
		var dataRepresentations = store.getDataRepresentations();
		Model model = store.createModel(state);
		Model model2 = store2.createModel(state);
		HashMap<Object, Object> cursorMap1 = new HashMap<>();
		HashMap<Object, Object> cursorMap2 = new HashMap<>();
		for (DataRepresentation<?, ?> item : dataRepresentations) {
			//System.out.println(item.getName());
			Cursor<?, ?> cursor1 = model.getAll(item);
			Cursor<?, ?> cursor2 = model2.getAll(item);
			do {
				var key1 = cursor1.getKey();
				var value1 = cursor1.getValue();
				cursorMap1.put(key1, value1);
				var key2 = cursor2.getKey();
				var value2 = cursor2.getValue();
				//System.out.println("key1: " + key1 + " key2: " + key2 + " value1: " + value1 + " value2: " + value2);
				cursorMap2.put(key2, value2);
			} while (cursor1.move() && cursor2.move());
		}
		assertEquals(cursorMap1, cursorMap2);
	}
}
