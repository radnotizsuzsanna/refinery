package tools.refinery.store.map.tests.fuzz;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.map.tests.fuzz.utils.FuzzTestUtils;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;
import tools.refinery.store.model.*;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static tools.refinery.store.map.tests.fuzz.utils.FuzzTestCollections.*;

public class SerializeModelWithDifferentTypesFuzzTest {


	HashMap<String, DataInputStream> streamMapIn;
	HashMap<Symbol<?>, DataOutputStream> streamMapOut;
	DataOutputStream relationsOutputStream;
	DataInputStream relationsInputStream;

	public void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue,
							 boolean nullDefault, int commitFrequency) {
		Integer[] intValues = MapTestEnvironment.prepareIntegerValues(maxValue, nullDefault);
		Boolean[] boolValues = MapTestEnvironment.prepareBooleanValues(maxValue, nullDefault);

		Symbol<Boolean> person = new Symbol<>("person", 1, Boolean.class,false);
		Symbol<Integer> age = new Symbol<>("age", 1, Integer.class,0);
		ModelStore store = ModelStore.builder().symbols(person, age).build();
		Model model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);

		ModelStore storeR =  ModelStore.builder().symbols(person, age).build();

		iterativeRandomPutsAndCommitsThenRestore(scenario, model,storeR, ageInterpretation,  personInterpretation ,

				steps,
				maxKey,	intValues, boolValues, seed, commitFrequency);
	}

	private void iterativeRandomPutsAndCommitsThenRestore(String scenario,
														  Model model, ModelStore storeR,
														  Interpretation<Integer> interpretation1,
														  Interpretation<Boolean> interpretation2, int steps,
														  int maxKey,
														  Integer[] values1, Boolean[] values2, int seed, int commitFrequency) {
		// 1. build a map with versions
		Random r = new Random(seed);
		Map<Integer, Long> index2Version = new HashMap<>();

		for (int i = 0; i < steps; i++) {
			int index = i + 1;
			Tuple nextKey1 = Tuple.of(r.nextInt(maxKey));
			Integer nextValue1 = values1[r.nextInt(values1.length)];
			Boolean nextValue2 = values2[r.nextInt(values1.length)];
			try {
				interpretation1.put(nextKey1, nextValue1);
				interpretation2.put(nextKey1, nextValue2);
			} catch (Exception exception) {
				exception.printStackTrace();
				fail(scenario + ":" + index + ": exception happened: " + exception);
			}
			if (index % commitFrequency == 0) {
				long version = model.commit();
				index2Version.put(i, version);
			}
			//MapTestEnvironment.printStatus(scenario, index, steps, "building");
		}

		var store = model.getStore();


		ModelStoreWithError modelStoreWithError = new ModelStoreWithError(null);
		modelStoreWithError.setModelStore(storeR);

		ModelSerializer serializer = new ModelSerializer();
		SerializerStrategy<Boolean> strategyBoolean = new TupleBooleanSerializer();
		serializer.addStrategy(Boolean.class,strategyBoolean);
		SerializerStrategy<Integer> strategyInteger = new TupleIntegerSerializer();
		serializer.addStrategy(Integer.class,strategyInteger);

		List<AnySymbol> dataRepresentationList = store.getSymbols().stream().toList();
		try {
			initializeStreamMapsWithPipedStreams(dataRepresentationList);
			initializeRelationStreamsWithPipedStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			//Serializes the ModelStore
			serializer.write(store, relationsOutputStream, streamMapOut);
			//Deserializes the ModelStore
			serializer.read(modelStoreWithError, relationsInputStream, streamMapIn);
			//nézze meg az összes lehetséges
			compareStores(store,storeR);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}



		//Test if the ModelStore is the same after the serialization


		// 2. create a non-versioned and
	/*	VersionedMap<Integer, String> reference = store.createMap();
		r = new Random(seed);

		for (int i = 0; i < steps; i++) {
			int index = i + 1;
			int nextKey = r.nextInt(maxKey);
			String nextValue = values[r.nextInt(values.length)];
			try {
				reference.put(nextKey, nextValue);
			} catch (Exception exception) {
				exception.printStackTrace();
				fail(scenario + ":" + index + ": exception happened: " + exception);
			}
			if (index % commitFrequency == 0) {
				versioned.restore(index2Version.get(i));
				MapTestEnvironment.compareTwoMaps(scenario + ":" + index, reference, versioned);
			}
			MapTestEnvironment.printStatus(scenario, index, steps, "comparison");
		}*/

	}

	void initializeStreamMapsWithPipedStreams(List< AnySymbol> dataRepresentationList) throws IOException {
		//The HasMaps contain the DataStreams for serializing the MapStores (MapStores will be stored in separate files)
		streamMapIn = new HashMap<>();
		streamMapOut = new HashMap<>();

		//PipedOutputStream pipedOutput;
		//PipedInputStream pipedInput;
		for (AnySymbol dataRepresentation : dataRepresentationList) {
			/*pipedInput = new PipedInputStream();
			pipedOutput = new PipedOutputStream();
			pipedInput.connect(pipedOutput);
			DataOutputStream dataOutputStream = new DataOutputStream(pipedOutput);
			DataInputStream dataInputStream = new DataInputStream(pipedInput);*/

			FileOutputStream fileOut =
					new FileOutputStream("D:\\0Egyetem\\Refinery\\deltas\\data"+dataRepresentation.name()+".txt");
			FileInputStream fileIn = new FileInputStream("D:\\0Egyetem\\Refinery\\deltas\\data"+dataRepresentation.name()+".txt");
			DataOutputStream dataOutputStream  = new DataOutputStream(fileOut);
			DataInputStream dataInputStream = new DataInputStream(fileIn);

			streamMapOut.put((Symbol<?>) dataRepresentation, dataOutputStream);
			streamMapIn.put(dataRepresentation.name(), dataInputStream);
		}
	}

	/**
	 * Initializes the streams for serializing the relations with piped streams.
	 * @throws IOException  When the connecting of the piped streams fails.
	 */
	void initializeRelationStreamsWithPipedStream() throws IOException {
		/*PipedInputStream pipedInput = new PipedInputStream();
		PipedOutputStream pipedOutput = new PipedOutputStream();
		pipedInput.connect(pipedOutput);
		relationsOutputStream = new DataOutputStream(pipedOutput);
		relationsInputStream = new DataInputStream(pipedInput);*/

		FileOutputStream fileOut = new FileOutputStream("D:\\0Egyetem\\Refinery\\deltas\\relation.txt");
		FileInputStream fileIn = new FileInputStream("D:\\0Egyetem\\Refinery\\deltas\\relation.txt");
		relationsOutputStream = new DataOutputStream(fileOut);
		relationsInputStream = new DataInputStream(fileIn);
	}
	public static final String title = "Commit {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} commit frequency={5} " +
			"seed={6}";

	@ParameterizedTest(name = title)
	@MethodSource
	@Timeout(value = 10)
	@Tag("smoke")
	void parametrizedFastFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed) {
		runFuzzTest("RestoreS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency);
	}

	static Stream<Arguments> parametrizedFastFuzz() {
		return FuzzTestUtils.permutationWithSize(new Object[]{10}, keyCounts, valueCounts, nullDefaultOptions,
				commitFrequencyOptions, randomSeedOptions);
	}
	private static Symbol<Boolean> person = new Symbol<>("person", 1, Boolean.class,false);
	private static Symbol<Integer> age = new Symbol<>("age", 1, Integer.class,0);

	public static final Object[] modelStoreConfigs = {
			ModelStore.builder().symbols(person,age).build()
	};

	@ParameterizedTest(name = title)
	@MethodSource
	@Tag("smoke")
	@Tag("slow")
	void parametrizedSlowFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed) {
		runFuzzTest("RestoreS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency);
	}

	//unit teszt, runFuzzTest
	@Test
	void unitTest(){
		System.out.println("Start");
		try {
			System.in.read();
			System.in.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		runFuzzTest("Teljesitmenymeres", 0, 1, 10, 10,
				false, 10);
		System.out.println("Finish");
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(SerializeModelWithDifferentTypesFuzzTest.parametrizedFastFuzz(), 1);
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

		//The two stores have the same amount of data representations, and they contain the same name-value type pairs
		assertEquals(store.getStates(), store2.getStates());

		//The two stores have the same states
		store.getStates().forEach((item) -> {
			compareIfStatesHaveTheSameModel(store, store2, item);
		});
	}
	private void compareIfStatesHaveTheSameModel(ModelStore store, ModelStore store2, Long state){
		//gets the cursors with get all, the puts them in HashMaps, then compare
		var dataRepresentations = store.getSymbols();
		Model model = store.createModelForState(state);
		Model model2 = store2.createModelForState(state);
		HashMap<Object, Object> cursorMap1 = new HashMap<>();
		HashMap<Object, Object> cursorMap2 = new HashMap<>();
		for (AnySymbol item : dataRepresentations) {
			var interpretation = model.getInterpretation((Symbol<?>) item);
			var cursor1 = interpretation.getAll();
			var interpretation2 = model2.getInterpretation((Symbol<?>) item);
			var cursor2 = interpretation2.getAll();
			do {
				var key1 = cursor1.getKey();
				var value1 = cursor1.getValue();
				cursorMap1.put(key1, value1);
				var key2 = cursor2.getKey();
				var value2 = cursor2.getValue();
				cursorMap2.put(key2, value2);
			} while (cursor1.move() && cursor2.move());
		}
		assertEquals(cursorMap1, cursorMap2);
	}
}
