package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.*;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.io.*;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static tools.refinery.store.representation.TruthValue.TRUE;
import static tools.refinery.store.representation.TruthValue.UNKNOWN;


class ModelSerializerTest {
	HashMap<String, DataInputStream> streamMapIn;
	HashMap<Symbol<?>, DataOutputStream> streamMapOut;
	DataOutputStream relationsOutputStream;
	DataInputStream relationsInputStream;

	/**
	 * Tests if the ModelSerializer can serialize a model store with bool, int and TruthValue value types and Tuple key type.
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializeModelWithDifferentTypesTest() throws IOException {
		Symbol<Boolean> person = new Symbol<>("person", 1, Boolean.class,false);
		Symbol<Integer> age = new Symbol<>("age", 1, Integer.class,0);
		Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class,false);
		Symbol<TruthValue> girl = new Symbol<>("girl", 1, TruthValue.class, UNKNOWN);

		ModelStore store = ModelStore.builder().symbols(person, age, friend, girl).build();
		Model model = store.createEmptyModel();

		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var friendInterpretation = model.getInterpretation(friend);
		var girlInterpretation = model.getInterpretation(girl);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		ageInterpretation.put(Tuple.of(0), 21);
		ageInterpretation.put(Tuple.of(1), 34);
		friendInterpretation.put(Tuple.of(0, 1), true);
		girlInterpretation.put(Tuple.of(0), TRUE);
		girlInterpretation.put(Tuple.of(1), UNKNOWN);
		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);
		SerializerStrategy<TruthValue> strategyTruthValue = new TupleTruthValueSerializer();
		serializer.addStrategy(TruthValue.class, strategyTruthValue);

	//	List<AnySymbol> dataRepresentationList = (List<AnySymbol>) store.getSymbols();
		List< AnySymbol> dataRepresentationList = store.getSymbols().stream().toList();
		initializeStreamMapsWithPipedStreams(dataRepresentationList);
		initializeRelationStreamsWithPipedStream();

		ModelStore store2 = ModelStore.builder().symbols(person, age, friend, girl).build();
		ModelStoreWithError modelStoreWithError = new ModelStoreWithError(null);
		modelStoreWithError.setModelStore(store2);

		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
			//Deserializes the ModelStore
			//ModelStore store2 = serializer.read(relationsInputStream, streamMapIn).getModelStore();
			serializer.read(modelStoreWithError, relationsInputStream, streamMapIn);
			//Test if the ModelStore is the same after the serialization
			compareStores(store,store2);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tests if the ModelSerializer can serialize a model store with an empty map store
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializeModelWithEmptyMapStore() throws IOException{
		Symbol<Boolean> person = new Symbol<>("person", 1, Boolean.class,false);
		Symbol<Integer> age = new Symbol<>("age", 1, Integer.class,0);

		ModelStore store = ModelStore.builder().symbols(person, age).build();
		Model model = store.createEmptyModel();

		var personInterpretation = model.getInterpretation(person);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List< AnySymbol> dataRepresentationList = store.getSymbols().stream().toList();
		initializeStreamMapsWithPipedStreams(dataRepresentationList);
		initializeRelationStreamsWithPipedStream();

		ModelStore store2 = ModelStore.builder().symbols(person, age).build();
		ModelStoreWithError modelStoreWithError = new ModelStoreWithError(null);
		modelStoreWithError.setModelStore(store2);

		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
			//Deserializes the ModelStore
			serializer.read(modelStoreWithError, relationsInputStream, streamMapIn);
			//Test if the ModelStore is the same after the serialization
			compareStores(store,store2);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tests if the ModelSerializer can serialize a model store with multiple commit
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializerWithMultipleCommitTest() throws IOException{
		Symbol<Boolean> person = new Symbol<>("person", 1, Boolean.class,false);
		Symbol<Integer> age = new Symbol<>("age", 1, Integer.class,0);

		ModelStore store = ModelStore.builder().symbols(person, age).build();
		Model model = store.createEmptyModel();

		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		ageInterpretation.put(Tuple.of(0), 21);
		ageInterpretation.put(Tuple.of(1), 34);

		model.commit();
		model.commit();
		model.commit();

		personInterpretation.put(Tuple.of(0), false);
		personInterpretation.put(Tuple.of(1), false);


		model.commit();
		model.commit();

		personInterpretation.put(Tuple.of(0), true);

		model.commit();
		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List< AnySymbol> dataRepresentationList = store.getSymbols().stream().toList();
		initializeStreamMapsWithPipedStreams(dataRepresentationList);
		initializeRelationStreamsWithPipedStream();

		ModelStore store2 = ModelStore.builder().symbols(person, age).build();
		ModelStoreWithError modelStoreWithError = new ModelStoreWithError(null);
		modelStoreWithError.setModelStore(store2);

		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
			//Deserializes the ModelStore
			serializer.read(modelStoreWithError, relationsInputStream, streamMapIn);
			//Test if the ModelStore is the same after the serialization
			compareStores(store,store2);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Tests if the ModelSerializer can serialize a model store with restore
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializerWithRestoreTest() throws IOException{
		Symbol<Boolean> person = new Symbol<>("person", 1, Boolean.class,false);
		Symbol<Integer> age = new Symbol<>("age", 1, Integer.class,0);


		ModelStore store = ModelStore.builder().symbols(person, age).build();
		Model model = store.createEmptyModel();

		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		ageInterpretation.put(Tuple.of(0), 21);
		ageInterpretation.put(Tuple.of(1), 34);

		model.commit();

		personInterpretation.put(Tuple.of(0), false);
		personInterpretation.put(Tuple.of(1), false);

		var state1 = model.commit();

		personInterpretation.put(Tuple.of(2), true);
		var state2 = model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List< AnySymbol> dataRepresentationList = store.getSymbols().stream().toList();
		initializeStreamMapsWithPipedStreams(dataRepresentationList);
		initializeRelationStreamsWithPipedStream();

		ModelStore store2 = ModelStore.builder().symbols(person, age).build();
		ModelStoreWithError modelStoreWithError = new ModelStoreWithError(null);
		modelStoreWithError.setModelStore(store2);

		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
			//Deserializes the ModelStore
			serializer.read(modelStoreWithError, relationsInputStream, streamMapIn);

			Model model2 = store2.createModelForState(state2);


			var get = personInterpretation.get(Tuple.of(2));
			assertTrue(get);

			var personInterpretation2 = model2.getInterpretation(person);

			var get2 = personInterpretation2.get(Tuple.of(2));
			assertTrue(get2);

			model.restore(state1);
			get = personInterpretation.get(Tuple.of(2));
			assertFalse(get);

			model2.restore(state1);
			get2 = personInterpretation2.get(Tuple.of(2));
			assertFalse(get2);

			//Test if the ModelStore is the same after the serialization
			compareStores(store,store2);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	//TODO ezt kiegészíteni

	/**
	 * Tests if the serializer can handle interrupted map store data while deserializing
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializationWithInterruptedMapStoreTest() throws IOException, ClassNotFoundException {
		Symbol<Boolean> person = new Symbol<>("person", 1, Boolean.class,false);
		Symbol<Integer> age = new Symbol<>("age", 1, Integer.class,0);

		ModelStore store = ModelStore.builder().symbols(person, age).build();
		Model model = store.createEmptyModel();

		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		ageInterpretation.put(Tuple.of(0), 21);
		ageInterpretation.put(Tuple.of(1), 34);

		model.commit();

		personInterpretation.put(Tuple.of(0), false);
		personInterpretation.put(Tuple.of(1), false);

		model.commit();
		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List< AnySymbol> dataRepresentationList = store.getSymbols().stream().toList();

		streamMapIn = new HashMap<>();
		streamMapOut = new HashMap<>();

		HashMap<Symbol<?>, ByteArrayOutputStream> byteArrayOutputMap = new HashMap<>();
		for (AnySymbol dataRepresentation : dataRepresentationList) {
			ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
			byteArrayOutputMap.put((Symbol<?>) dataRepresentation, byteArrayOutput);
			DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutput);
			streamMapOut.put((Symbol<?>) dataRepresentation, dataOutputStream);
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
		for (AnySymbol dataRepresentation : dataRepresentationList) {
			var byteArrayOutput = byteArrayOutputMap.get(dataRepresentation);
			byte[] byteArray = byteArrayOutput.toByteArray();
			//Creates the  ByteArrayInputStream with only 10 bytes of the byteArray so the mapStore's data will be interrupted
			ByteArrayInputStream byteArrayInput = new ByteArrayInputStream(byteArray, 0, 10);
			DataInputStream dataInputStream = new DataInputStream(byteArrayInput);
			streamMapIn.put(dataRepresentation.name(), dataInputStream);
		}


		ModelStore store2 = ModelStore.builder().symbols(person, age).build();
		ModelStoreWithError modelStoreWithError = new ModelStoreWithError(null);
		modelStoreWithError.setModelStore(store2);

		serializer.read(modelStoreWithError, relationsInputStream, streamMapIn);

		assertEquals(modelStoreWithError.getException().getMessage(), "Incomplete MapStore in file");
	}

	/**
	 * Tests if the serializer can handle interrupted relation data while deserializing
	 * @throws IOException When the connection of the piped streams fails.
	 */
	@Test
	void serializationWithInterruptedRelationTest() throws IOException {
		Symbol<Boolean> person = new Symbol<>("person", 1, Boolean.class,false);
		Symbol<Integer> age = new Symbol<>("age", 1, Integer.class,0);

		ModelStore store = ModelStore.builder().symbols(person, age).build();
		Model model = store.createEmptyModel();

		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		ageInterpretation.put(Tuple.of(0), 21);
		ageInterpretation.put(Tuple.of(1), 34);

		model.commit();

		personInterpretation.put(Tuple.of(0), false);
		personInterpretation.put(Tuple.of(1), false);

		model.commit();
		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List< AnySymbol> dataRepresentationList = store.getSymbols().stream().toList();
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

		ModelStore store2 = ModelStore.builder().symbols(person, age).build();
		ModelStoreWithError modelStoreWithError = new ModelStoreWithError(null);
		modelStoreWithError.setModelStore(store2);

		Exception exception = assertThrows(IOException.class, () -> {
			serializer.read(modelStoreWithError, relationsInputStream, streamMapIn);
		});

		assertEquals(exception.getMessage(), "Incomplete Relation in file");

	}

	/**
	 * Initializes the streamMapIn and streamMapOut maps with piped streams for serializing the map stores.
	 * @param dataRepresentationList The list of the data representations of the model store.
	 * @throws IOException When the connecting of the piped streams fails.
	 */
	void initializeStreamMapsWithPipedStreams(List< AnySymbol> dataRepresentationList) throws IOException {
		//The HasMaps contain the DataStreams for serializing the MapStores (MapStores will be stored in separate files)
		streamMapIn = new HashMap<>();
		streamMapOut = new HashMap<>();

		PipedOutputStream pipedOutput;
		PipedInputStream pipedInput;
		for (AnySymbol dataRepresentation : dataRepresentationList) {
			pipedInput = new PipedInputStream();
			pipedOutput = new PipedOutputStream();
			pipedInput.connect(pipedOutput);

			DataOutputStream dataOutputStream = new DataOutputStream(pipedOutput);
			DataInputStream dataInputStream = new DataInputStream(pipedInput);

			streamMapOut.put((Symbol<?>) dataRepresentation, dataOutputStream);
			streamMapIn.put(dataRepresentation.name(), dataInputStream);
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
		var dataRepresentationSet =  store.getSymbols();
		HashMap<String, Class<?>> dataRepresentationHashMap = new HashMap<>();
		for (AnySymbol item : dataRepresentationSet){
			dataRepresentationHashMap.put(item.name(), item.valueType());
		}
		var dataRepresentationSet2 =  store2.getSymbols();
		HashMap<String, Class<?>> dataRepresentationHashMap2 = new HashMap<>();
		for (AnySymbol item : dataRepresentationSet2){
			dataRepresentationHashMap2.put(item.name(), item.valueType());
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
		var dataRepresentations = store.getSymbols();
		Model model = store.createModelForState(state);
		Model model2 = store2.createModelForState(state);
		HashMap<Object, Object> cursorMap1 = new HashMap<>();
		HashMap<Object, Object> cursorMap2 = new HashMap<>();
		for (AnySymbol item : dataRepresentations) {
			//System.out.println(item.getName());

			var interpretation = model.getInterpretation((Symbol<? extends Object>) item);
			var cursor1 = interpretation.getAll();
			var interpretation2 = model2.getInterpretation((Symbol<? extends Object>) item);
			var cursor2 = interpretation2.getAll();
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
