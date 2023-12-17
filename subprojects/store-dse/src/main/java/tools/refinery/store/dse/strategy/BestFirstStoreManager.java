/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.dse.transition.statespace.ObjectivePriorityQueue;
import tools.refinery.store.dse.transition.statespace.SolutionStore;
import tools.refinery.store.dse.transition.statespace.internal.*;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.*;
import tools.refinery.store.model.internal.ModelVersion;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;
import tools.refinery.visualization.statespace.VisualizationStore;
import tools.refinery.visualization.statespace.internal.VisualizationStoreImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BestFirstStoreManager {

	ModelStore modelStore;
	ObjectivePriorityQueue objectiveStore;
	ActivationStore activationStore;
	SolutionStore solutionStore;
	EquivalenceClassStore equivalenceClassStore;
	VisualizationStore visualizationStore;

	public BestFirstStoreManager(ModelStore modelStore, int maxNumberOfSolutions) {
		this.modelStore = modelStore;
		DesignSpaceExplorationStoreAdapter storeAdapter =
				modelStore.getAdapter(DesignSpaceExplorationStoreAdapter.class);

		objectiveStore = new ObjectivePriorityQueueImpl(storeAdapter.getObjectives());
		Consumer<VersionWithObjectiveValue> whenAllActivationsVisited = x -> objectiveStore.remove(x);
		activationStore = new ActivationStoreImpl(storeAdapter.getTransformations().size(), whenAllActivationsVisited);
		solutionStore = new SolutionStoreImpl(maxNumberOfSolutions);
		equivalenceClassStore = new FastMapBasedEquivalenceClassStore(modelStore.getAdapter(StateCoderStoreAdapter.class)) {
			@Override
			protected void delegate(VersionWithObjectiveValue version, int[] emptyActivations, boolean accept) {
				throw new UnsupportedOperationException("This equivalence storage is not prepared to resolve " +
						"symmetries!");
			}
		};
		visualizationStore = new VisualizationStoreImpl();
	}

	public ModelStore getModelStore() {
		return modelStore;
	}

	ObjectivePriorityQueue getObjectiveStore() {
		return objectiveStore;
	}

	ActivationStore getActivationStore() {
		return activationStore;
	}

	public SolutionStore getSolutionStore() {
		return solutionStore;
	}

	EquivalenceClassStore getEquivalenceClassStore() {
		return equivalenceClassStore;
	}

	public VisualizationStore getVisualizationStore() {
		return visualizationStore;
	}

	public void startExploration(Version initial) {
		startExploration(initial, 1);
	}


	public void startExploration(Version initial, int randomSeed) {
		BestFirstExplorer bestFirstExplorer = new BestFirstExplorer(this, modelStore.createModelForState(initial),
				randomSeed);
		bestFirstExplorer.explore();
	}

	public void startExploration(Version initial, int randomSeed, ModelSerializer modelSerializer) {
		BestFirstExplorer bestFirstExplorer = new BestFirstExplorer(this, modelStore.createModelForState(initial),
				randomSeed);
		bestFirstExplorer.explore(modelSerializer);
	}

	public void startExploration(Version initial, ModelSerializer modelSerializer) {
		startExploration(initial, 1, modelSerializer);
	}

	//TODO Ezt valami szebbre
	public static int saveAmount = 10;

	synchronized public void saveStates(ModelSerializer modelSerializer) {
		List<Version> versionsToSave = new ArrayList<>(saveAmount);
		List<double[]> objectiveValuesToSave = new ArrayList<>(saveAmount);
		int[] stateCodes = new int[saveAmount];

		var bestIterator = this.objectiveStore.getAll().iterator();
		int i = 0;
		while (i < saveAmount && bestIterator.hasNext()) {
			var entry = bestIterator.next();
			Version version = entry.version();
			ObjectiveValue objectiveValue = entry.objectiveValue();

			versionsToSave.add(version);

			double[] objective = new double[objectiveValue.getSize()];
			for (int j = 0; j < objective.length; j++) {
				objective[j] = objectiveValue.get(j);
			}
			objectiveValuesToSave.add(objective);

			stateCodes[i] = this.equivalenceClassStore.getCode(entry);
			i++;
		}



		HashMap<String, File> relationFiles = new HashMap<>();
		/*try {

			//TODO ez valahogy máshogy
			var symbols = modelStore.getSymbols().toArray();
			for (Object symbol : symbols) {
				if(Objects.equals(((Symbol<?>) symbol).name(), "Vertex::incomingTransition")){
					File dataFile = initializeAndGetFile("incomingTransition");
					//relationFiles.put(((Symbol<?>) symbol).name(), dataFile);
				}else{
					File dataFile = initializeAndGetFile(((Symbol<?>) symbol).name());
					//relationFiles.put(((Symbol<?>) symbol).name(), dataFile);
				}
			}

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}*/

		try {
			//TODO ezt nem ide kell menteni
			File file = new File("D:\\0Egyetem\\Refinery\\szakdoga\\data.txt");
			PrintWriter writer = new PrintWriter(file);
			writer.print("");
			writer.close();
			modelSerializer.write(versionsToSave, modelStore, null, relationFiles, objectiveValuesToSave,stateCodes);
		} catch (IOException e) {
			System.out.println("Error while saving states. ");
		}
	}

	File initializeAndGetFile(String fileName) throws FileNotFoundException {
		//TODO ehelyett valami univerzalis
		File file = new File("D:\\0Egyetem\\Refinery\\szakdoga\\" + fileName + ".txt");
		PrintWriter writer = new PrintWriter(file);
		writer.print("");
		writer.close();
		return file;
	}
}
