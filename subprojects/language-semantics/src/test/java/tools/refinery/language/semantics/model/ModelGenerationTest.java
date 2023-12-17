/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.model;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.refinery.language.ProblemStandaloneSetup;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.strategy.BestFirstExplorer;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.map.Version;
import tools.refinery.store.map.internal.delta.MapTransaction;
import tools.refinery.store.model.*;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.containment.InferredContainment;
import tools.refinery.store.reasoning.translator.typehierarchy.InferredType;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
//@Disabled("For debugging purposes only")
class ModelGenerationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private ModelInitializer modelInitializer;

	void socialNetworkTest() {
		var parsedProblem = parseHelper.parse("""
				% Metamodel
				class Person {
				    contains Post posts opposite author
				    Person friend opposite friend
				}

				class Post {
				    container Person[0..1] author opposite posts
				    Post replyTo
				}

				% Constraints
				error replyToNotFriend(Post x, Post y) <->
				    replyTo(x, y),
				    author(x, xAuthor),
				    author(y, yAuthor),
				    xAuthor != yAuthor,
				    !friend(xAuthor, yAuthor).

				error replyToCycle(Post x) <-> replyTo+(x, x).

				% Instance model
				!friend(a, b).
				author(p1, a).
				author(p2, b).

				!author(Post::new, a).

				% Scope
				scope Post = 50, Person = 50.
				""");
		assertThat(parsedProblem.errors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ModelVisualizerAdapter.builder()
						.withOutputPath("test_output")
						.withFormat(FileFormat.DOT)
						.withFormat(FileFormat.SVG)
//						.saveStates()
						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion);
		var resultStore = bestFirst.getSolutionStore();
		System.out.println("states size: " + resultStore.getSolutions().size());
		//initialModel.getAdapter(ModelVisualizerAdapter.class).visualize(bestFirst.getVisualizationStore());
	}


	@Test
	void socialNetworkTestForMeasurement() {
		var parsedProblem = parseHelper.parse("""
				% Metamodel
				class Person {
				    contains Post posts opposite author
				    Person friend opposite friend
				}

				class Post {
				    container Person[0..1] author opposite posts
				    Post replyTo
				}

				% Constraints
				error replyToNotFriend(Post x, Post y) <->
				    replyTo(x, y),
				    author(x, xAuthor),
				    author(y, yAuthor),
				    xAuthor != yAuthor,
				    !friend(xAuthor, yAuthor).

				error replyToCycle(Post x) <-> replyTo+(x, x).

				% Instance model
				!friend(a, b).
				author(p1, a).
				author(p2, b).

				!author(Post::new, a).

				% Scope
				scope Post = 55, Person = 50.
				""");
		assertThat(parsedProblem.errors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ModelVisualizerAdapter.builder()
						.withOutputPath("test_output")
						.withFormat(FileFormat.DOT)
						.withFormat(FileFormat.SVG)
						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());


		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var initialVersion = initialModel.commit();

		int[] stepLimits = {100, 200, 300, 400, 500};
		//int[] stepLimits = {600};
		//int[] saveNumbers = {10, 20, 30, 40, 50};
		int[] saveNumbers = {10};

		ModelSerializer modelSerializer = new ModelSerializer();

		modelSerializer.addSerializeStrategy(Boolean.class, new TupleBooleanSerializer());
		modelSerializer.addSerializeStrategy(Integer.class, new TupleIntegerSerializer());
		modelSerializer.addSerializeStrategy(TruthValue.class, new TupleTruthValueSerializer());
		modelSerializer.addSerializeStrategy(CardinalityInterval.class, new TupleCardinalityIntervalSerializer());
		modelSerializer.addSerializeStrategy(Boolean[].class, new TupleBooleanArraySerializer());


		List<PartialRelation> sortedTypes =
				new ArrayList<>(modelInitializer.getMetamodel().typeHierarchy().getAllTypes());
		sortedTypes.sort((o1, o2) -> o1.name().compareTo(o2.name()));

		List<PartialRelation> sortedContainments =
				new ArrayList<>(modelInitializer.getMetamodel().containmentHierarchy().keySet());
		sortedContainments.sort((o1, o2) -> o1.name().compareTo(o2.name()));

		modelSerializer.addSerializeStrategy(InferredType.class, new TupleInferredTypeSerializer(sortedTypes));
		modelSerializer.addSerializeStrategy(InferredContainment.class, new TupleInferredCointainmentSerializer(sortedContainments));

		File file = new File("D:\\0Egyetem\\Refinery\\szakdoga\\data.txt");
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		writer.print("");
		writer.close();
		modelSerializer.setDataFile(file);

		//TODO mokolos
		var symbols = store.getSymbols().toArray();
		for (Object symbol : symbols) {
			String name = ((Symbol<?>) symbol).name();
			String[] nameArray = name.split("[#%&{}\\<>*?/ $!'\":@+`|=]");
			name = String.join("", nameArray);
			File dataFile;
			try {
				dataFile = initializeAndGetFile(name);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			modelSerializer.putInRelationFiles(((Symbol<?>) symbol).name(), dataFile);
		}

		for (int stepLimit : stepLimits) {
			for (int saveNumber : saveNumbers) {
				BestFirstExplorer.stepLimit = stepLimit;
				BestFirstStoreManager.saveAmount = saveNumber;

				for(int i = 0; i < 20; i++){
					System.out.print("Social network;" + stepLimit + ";" + saveNumber + ";" + i + ";");

					var bestFirst = new BestFirstStoreManager(store, 1);
					bestFirst.startExploration(initialVersion, modelSerializer);

					System.gc();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}

					try {
						long start = System.nanoTime();
						modelSerializer.read(store,null, null);
						System.out.print(System.nanoTime()-start + ";");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					System.gc();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
					System.out.println();
				}
			}
		}
	}

	void statechartTest() {
		var parsedProblem = parseHelper.parse("""
				// Metamodel
				abstract class CompositeElement {
				    contains Region[] regions
				}

				class Region {
				    contains Vertex[] vertices opposite region
				}

				abstract class Vertex {
				    container Region[0..1] region opposite vertices
				    contains Transition[] outgoingTransition opposite source
				    Transition[] incomingTransition opposite target
				}

				class Transition {
				    container Vertex[0..1] source opposite outgoingTransition
				    Vertex target opposite incomingTransition
				}

				abstract class Pseudostate extends Vertex.

				abstract class RegularState extends Vertex.

				class Entry extends Pseudostate.

				class Exit extends Pseudostate.

				class Choice extends Pseudostate.

				class FinalState extends RegularState.

				class State extends RegularState, CompositeElement.

				class Statechart extends CompositeElement.

				// Constraints

				/////////
				// Entry
				/////////

				pred entryInRegion(Region r, Entry e) <->
					vertices(r, e).

				error noEntryInRegion(Region r) <->
				    !entryInRegion(r, _).

				error multipleEntryInRegion(Region r) <->
				    entryInRegion(r, e1),
				    entryInRegion(r, e2),
				    e1 != e2.

				error incomingToEntry(Transition t, Entry e) <->
				    target(t, e).

				error noOutgoingTransitionFromEntry(Entry e) <->
				    !source(_, e).

				error multipleTransitionFromEntry(Entry e, Transition t1, Transition t2) <->
				    outgoingTransition(e, t1),
				    outgoingTransition(e, t2),
				    t1 != t2.

				/////////
				// Exit
				/////////

				error outgoingFromExit(Transition t, Exit e) <->
				    source(t, e).

				/////////
				// Final
				/////////

				error outgoingFromFinal(Transition t, FinalState e) <->
				    source(t, e).

				/////////
				// State vs Region
				/////////

				pred stateInRegion(Region r, State s) <->
				    vertices(r, s).

				error noStateInRegion(Region r) <->
				    !stateInRegion(r, _).

				/////////
				// Choice
				/////////

				error choiceHasNoOutgoing(Choice c) <->
				    !source(_, c).

				error choiceHasNoIncoming(Choice c) <->
				    !target(_, c).

				scope node = 40..50, Region = 2..*, Choice = 1..*, Statechart = 1.
				""");
		assertThat(parsedProblem.errors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
//				.with(ModelVisualizerAdapter.builder()
//						.withOutputPath("test_output")
//						.withFormat(FileFormat.DOT)
//						.withFormat(FileFormat.SVG)
//						.saveStates()
//						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion);
		var resultStore = bestFirst.getSolutionStore();
		System.out.println("states size: " + resultStore.getSolutions().size());

		var model = store.createModelForState(resultStore.getSolutions().get(0).version());
		var interpretation = model.getAdapter(ReasoningAdapter.class)
				.getPartialInterpretation(Concreteness.CANDIDATE, ReasoningAdapter.EXISTS_SYMBOL);
		var cursor = interpretation.getAll();
		int max = -1;
		var types = new LinkedHashMap<PartialRelation, Integer>();
		var typeInterpretation = model.getInterpretation(TypeHierarchyTranslator.TYPE_SYMBOL);
		while (cursor.move()) {
			max = Math.max(max, cursor.getKey().get(0));
			var type = typeInterpretation.get(cursor.getKey());
			if (type != null) {
				types.compute(type.candidateType(), (ignoredKey, oldValue) -> oldValue == null ? 1 : oldValue + 1);
			}
		}
		System.out.println("Model size: " + (max + 1));
		//System.out.println(types);
//		initialModel.getAdapter(ModelVisualizerAdapter.class).visualize(bestFirst.getVisualizationStore());
	}

	@Test
	void statechartTestForMeasurement(){
		var parsedProblem = parseHelper.parse("""
				// Metamodel
				abstract class CompositeElement {
				    contains Region[] regions
				}

				class Region {
				    contains Vertex[] vertices opposite region
				}

				abstract class Vertex {
				    container Region[0..1] region opposite vertices
				    contains Transition[] outgoingTransition opposite source
				    Transition[] incomingTransition opposite target
				}

				class Transition {
				    container Vertex[0..1] source opposite outgoingTransition
				    Vertex target opposite incomingTransition
				}

				abstract class Pseudostate extends Vertex.

				abstract class RegularState extends Vertex.

				class Entry extends Pseudostate.

				class Exit extends Pseudostate.

				class Choice extends Pseudostate.

				class FinalState extends RegularState.

				class State extends RegularState, CompositeElement.

				class Statechart extends CompositeElement.

				// Constraints

				/////////
				// Entry
				/////////

				pred entryInRegion(Region r, Entry e) <->
					vertices(r, e).

				error noEntryInRegion(Region r) <->
				    !entryInRegion(r, _).

				error multipleEntryInRegion(Region r) <->
				    entryInRegion(r, e1),
				    entryInRegion(r, e2),
				    e1 != e2.

				error incomingToEntry(Transition t, Entry e) <->
				    target(t, e).

				error noOutgoingTransitionFromEntry(Entry e) <->
				    !source(_, e).

				error multipleTransitionFromEntry(Entry e, Transition t1, Transition t2) <->
				    outgoingTransition(e, t1),
				    outgoingTransition(e, t2),
				    t1 != t2.

				/////////
				// Exit
				/////////

				error outgoingFromExit(Transition t, Exit e) <->
				    source(t, e).

				/////////
				// Final
				/////////

				error outgoingFromFinal(Transition t, FinalState e) <->
				    source(t, e).

				/////////
				// State vs Region
				/////////

				pred stateInRegion(Region r, State s) <->
				    vertices(r, s).

				error noStateInRegion(Region r) <->
				    !stateInRegion(r, _).

				/////////
				// Choice
				/////////

				error choiceHasNoOutgoing(Choice c) <->
				    !source(_, c).

				error choiceHasNoIncoming(Choice c) <->
				    !target(_, c).

				scope node = 60, Region = 4, Choice = 4, Statechart = 1.
				""");
		assertThat(parsedProblem.errors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var initialVersion = initialModel.commit();

		//int[] stepLimits = {100, 150, 200, 250, 300};
		int[] stepLimits = {300};
		//int[] saveNumbers = {1, 10, 20, 30, 40};
		int[] saveNumbers = {10};

		ModelSerializer modelSerializer = new ModelSerializer();

		modelSerializer.addSerializeStrategy(Boolean.class, new TupleBooleanSerializer());
		modelSerializer.addSerializeStrategy(Integer.class, new TupleIntegerSerializer());
		modelSerializer.addSerializeStrategy(TruthValue.class, new TupleTruthValueSerializer());
		modelSerializer.addSerializeStrategy(CardinalityInterval.class, new TupleCardinalityIntervalSerializer());
		modelSerializer.addSerializeStrategy(Boolean[].class, new TupleBooleanArraySerializer());

		List<PartialRelation> sortedTypes =
				new ArrayList<>(modelInitializer.getMetamodel().typeHierarchy().getAllTypes());
		sortedTypes.sort((o1, o2) -> o1.name().compareTo(o2.name()));

		List<PartialRelation> sortedContainments =
				new ArrayList<>(modelInitializer.getMetamodel().containmentHierarchy().keySet());
		sortedContainments.sort((o1, o2) -> o1.name().compareTo(o2.name()));

		modelSerializer.addSerializeStrategy(InferredType.class, new TupleInferredTypeSerializer(sortedTypes));
		modelSerializer.addSerializeStrategy(InferredContainment.class, new TupleInferredCointainmentSerializer(sortedContainments));

		File file = new File("D:\\0Egyetem\\Refinery\\szakdoga\\data.txt");
		PrintWriter writer;
		try {
			writer = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		writer.print("");
		writer.close();
		modelSerializer.setDataFile(file);

		//TODO mokolos
		var symbols = store.getSymbols().toArray();
		for (Object symbol : symbols) {
			String name = ((Symbol<?>) symbol).name();
			String[] nameArray = name.split("[#%&{}\\<>*?/ $!'\":@+`|=]");
			name = String.join("", nameArray);
			File dataFile;
			try {
				dataFile = initializeAndGetFile(name);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			modelSerializer.putInRelationFiles(((Symbol<?>) symbol).name(), dataFile);
		}


		for (int stepLimit : stepLimits) {
			for (int saveNumber : saveNumbers) {
				BestFirstExplorer.stepLimit = stepLimit;
				BestFirstStoreManager.saveAmount = saveNumber;

				for(int i = 0; i < 1; i++){
					System.out.print("Statechart;" + stepLimit + ";" + saveNumber + ";" + i + ";");

					var bestFirst = new BestFirstStoreManager(store, 1);
					bestFirst.startExploration(initialVersion, modelSerializer);

					System.gc();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}

					try {
						long start = System.nanoTime();
						modelSerializer.read(store,null, null);
						System.out.print(System.nanoTime()-start + ";");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					System.gc();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
					System.out.println();
				}
			}
		}
	}

	void filesystemTest() {
		var parsedProblem = parseHelper.parse("""
				class Filesystem {
					contains Entry root
				}

				abstract class Entry.

				class Directory extends Entry {
					contains Entry[] entries
				}

				class File extends Entry.

				Filesystem(fs).

				scope Filesystem += 0, Entry = 100.
				""");
		assertThat(parsedProblem.errors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
//				.with(ModelVisualizerAdapter.builder()
//						.withOutputPath("test_output")
//						.withFormat(FileFormat.DOT)
//						.withFormat(FileFormat.SVG)
//						.saveStates()
//						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion);
		var resultStore = bestFirst.getSolutionStore();
		System.out.println("states size: " + resultStore.getSolutions().size());

		var model = store.createModelForState(resultStore.getSolutions().get(0).version());
		var interpretation = model.getAdapter(ReasoningAdapter.class)
				.getPartialInterpretation(Concreteness.CANDIDATE, ReasoningAdapter.EXISTS_SYMBOL);
		var cursor = interpretation.getAll();
		int max = -1;
		var types = new LinkedHashMap<PartialRelation, Integer>();
		var typeInterpretation = model.getInterpretation(TypeHierarchyTranslator.TYPE_SYMBOL);
		while (cursor.move()) {
			max = Math.max(max, cursor.getKey().get(0));
			var type = typeInterpretation.get(cursor.getKey());
			if (type != null) {
				types.compute(type.candidateType(), (ignoredKey, oldValue) -> oldValue == null ? 1 : oldValue + 1);
			}
		}
		System.out.println("Model size: " + (max + 1));
		//System.out.println(types);
//		initialModel.getAdapter(ModelVisualizerAdapter.class).visualize(bestFirst.getVisualizationStore());
	}

	@Test
	void filesystemTestForMeasurement(){
		var parsedProblem = parseHelper.parse("""
						class Filesystem {
						contains Entry root
					}

					abstract class Entry.

					class Directory extends Entry {
						contains Entry[] entries
					}

					class File extends Entry.

					Filesystem(fs).

					scope Filesystem += 1, Entry = 10000.
					""");
		assertThat(parsedProblem.errors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();


		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var initialVersion = initialModel.commit();

		//int[] stepLimits = {500, 1000, 1500, 2000, 2500};
		int[] stepLimits = {2500};
		//int[] saveNumbers = {10, 20, 30, 40, 50};
		int[] saveNumbers = {10};

		ModelSerializer modelSerializer = new ModelSerializer();
		//TODO ezt kijebb kell majd mozgatni
		modelSerializer.addSerializeStrategy(Boolean.class, new TupleBooleanSerializer());
		modelSerializer.addSerializeStrategy(Integer.class, new TupleIntegerSerializer());
		modelSerializer.addSerializeStrategy(TruthValue.class, new TupleTruthValueSerializer());
		modelSerializer.addSerializeStrategy(CardinalityInterval.class, new TupleCardinalityIntervalSerializer());
		modelSerializer.addSerializeStrategy(InferredType.class, new TupleBooleanArraySerializer());

		List<PartialRelation> sortedTypes =
				new ArrayList<>(modelInitializer.getMetamodel().typeHierarchy().getAllTypes());
		sortedTypes.sort((o1, o2) -> o1.name().compareTo(o2.name()));

		List<PartialRelation> sortedContainments =
				new ArrayList<>(modelInitializer.getMetamodel().containmentHierarchy().keySet());
		sortedContainments.sort((o1, o2) -> o1.name().compareTo(o2.name()));

		// modelSerializer.a
		modelSerializer.addSerializeStrategy(InferredType.class, new TupleInferredTypeSerializer(sortedTypes));
		modelSerializer.addSerializeStrategy(InferredContainment.class, new TupleInferredCointainmentSerializer(sortedContainments));

		File file = new File("D:\\0Egyetem\\Refinery\\szakdoga\\data.txt");
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		writer.print("");
		writer.close();
		modelSerializer.setDataFile(file);

		//TODO mokolos
		var symbols = store.getSymbols().toArray();
		for (Object symbol : symbols) {
			String name = ((Symbol<?>) symbol).name();
			String[] nameArray = name.split("[#%&{}\\<>*?/ $!'\":@+`|=]");
			name = String.join("", nameArray);
			File dataFile;
			try {
				dataFile = initializeAndGetFile(name);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			modelSerializer.putInRelationFiles(((Symbol<?>) symbol).name(), dataFile);
		}

		for (int stepLimit : stepLimits) {
			for (int saveNumber : saveNumbers) {
				BestFirstExplorer.stepLimit = stepLimit;
				BestFirstStoreManager.saveAmount = saveNumber;

				for(int i = 0; i < 1; i++){
					System.out.print("Filesystem;" + stepLimit + ";" + saveNumber + ";" + i + ";");

					var bestFirst = new BestFirstStoreManager(store, 1);
					bestFirst.startExploration(initialVersion, modelSerializer);

					System.gc();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}

					try {
						long start = System.nanoTime();
						modelSerializer.read(store,null, null);
						System.out.print(System.nanoTime()-start + ";");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					System.gc();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
					System.out.println();
				}
			}
		}
	}

	File initializeAndGetFile(String fileName) throws FileNotFoundException {
		//TODO ehelyett valami univerzalis
		File file = new File("D:\\0Egyetem\\Refinery\\szakdoga\\" + fileName + ".txt");
		//System.out.println(fileName);
		PrintWriter writer = new PrintWriter(file);
		writer.print("");
		writer.close();
		return file;
	}
	public static void main(String[] args) {
		ProblemStandaloneSetup.doSetup();
		var injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
		var test = injector.getInstance(ModelGenerationTest.class);
		try {
			test.statechartTest();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
