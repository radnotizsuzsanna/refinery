package tools.refinery.store.map.tests.fuzz;

public class SerializeModelWithDifferentTypesFuzzTest {
	/*private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue,
							 boolean nullDefault, int commitFrequency,
							ModelStoreBuilder builder) {
		Integer[] intValues = MapTestEnvironment.prepareIntegerValues(maxValue, nullDefault);
		Boolean[] boolValues = MapTestEnvironment.prepareBooleanValues(maxValue, nullDefault);

		Symbol<Boolean> person = new Symbol<>("person", 1, Boolean.class,false);
		Symbol<Integer> age = new Symbol<>("age", 1, Integer.class,0);
		ModelStore store = builder.symbols(person, age).build();
		Model model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);

		iterativeRandomPutsAndCommitsThenRestore(scenario, model,  ageInterpretation, personInterpretation , steps,
				maxKey,	intValues, boolValues, seed, commitFrequency);
	}

	private void iterativeRandomPutsAndCommitsThenRestore(String scenario,
														  Model model,  Interpretation<Integer> interpretation1,
														  Interpretation<Boolean> interpretation2, int steps, int maxKey,
														  Integer[] values1, Boolean[] values2, int seed, int commitFrequency) {
		// 1. build a map with versions
		Random r = new Random(seed);
		Map<Integer, Long> index2Version = new HashMap<>();

		for (int i = 0; i < steps; i++) {
			int index = i + 1;
			Tuple nextKey = Tuple.of(r.nextInt(maxKey), r.nextInt(maxKey));
			Integer nextValue1 = values1[r.nextInt(values1.length)];
			Boolean nextValue2 = values2[r.nextInt(values1.length)];
			try {
				interpretation1.put(nextKey, nextValue1);
				interpretation2.put(nextKey, nextValue2);
			} catch (Exception exception) {
				exception.printStackTrace();
				fail(scenario + ":" + index + ": exception happened: " + exception);
			}
			if (index % commitFrequency == 0) {
				long version = model.commit();
				index2Version.put(i, version);
			}
			MapTestEnvironment.printStatus(scenario, index, steps, "building");
		}
		// 2. create a non-versioned and
		VersionedMap<Integer, String> reference = store.createMap();
		r = new Random(seed);

	/*	for (int i = 0; i < steps; i++) {
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
		}

	}

	public static final String title = "Commit {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} commit frequency={5} " +
			"seed={6} config={7}";

	@ParameterizedTest(name = title)
	@MethodSource
	@Timeout(value = 10)
	@Tag("smoke")
	void parametrizedFastFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed, VersionedMapStoreBuilder<Integer, String> builder) {
		runFuzzTest("RestoreS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, builder);
	}

	static Stream<Arguments> parametrizedFastFuzz() {
		return FuzzTestUtils.permutationWithSize(stepCounts, keyCounts, valueCounts, nullDefaultOptions,
				commitFrequencyOptions, randomSeedOptions, storeConfigs);
	}

	@ParameterizedTest(name = title)
	@MethodSource
	@Tag("smoke")
	@Tag("slow")
	void parametrizedSlowFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed, VersionedMapStoreBuilder<Integer, String> builder) {
		runFuzzTest("RestoreS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, builder);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(RestoreFuzzTest.parametrizedFastFuzz(), 1);
	}*/
}
