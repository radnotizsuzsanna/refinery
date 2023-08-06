/*******************************************************************************
 * Copyright (c) 2010-2014, Miklos Foldenyi, Andras Szabolcs Nagy, Abel Hegedus, Akos Horvath, Zoltan Ujhelyi and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.dse.internal;

import tools.refinery.store.map.Version;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.Strategy;
import tools.refinery.store.dse.objectives.Fitness;
import tools.refinery.store.dse.objectives.Objective;
import tools.refinery.store.dse.objectives.ObjectiveComparatorHelper;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;
import tools.refinery.visualization.ModelVisualizerAdapter;

import java.util.*;

public class DesignSpaceExplorationAdapterImpl implements DesignSpaceExplorationAdapter {
	static final Symbol<Integer> NODE_COUNT_SYMBOL = Symbol.of("MODEL_SIZE", 0, Integer.class, 0);
	private final Model model;
	private final ModelQueryAdapter queryEngine;
	private final DesignSpaceExplorationStoreAdapterImpl storeAdapter;
	private final LinkedHashSet<TransformationRule> transformationRules;
	private final LinkedHashSet<RelationalQuery> globalConstraints;
	private final List<Objective> objectives;
	private final LinkedHashSet<ResultSet<Boolean>> globalConstraintResultSets = new LinkedHashSet<>();
	private final Interpretation<Integer> sizeInterpretation;
	private final Strategy strategy;

	private ObjectiveComparatorHelper objectiveComparatorHelper;
	private List<Version> trajectory = new LinkedList<>();
	private Fitness lastFitness;
	private final LinkedHashSet<Version> solutions = new LinkedHashSet<>();
	private Map<Version, LinkedHashSet<Activation>> statesAndUntraversedActivations;
	private Map<Version, LinkedHashSet<Activation>> statesAndTraversedActivations;
	private Random random = new Random();
	private boolean isNewState = false;
	private final boolean isVisualizationEnabled;
	private final ModelVisualizerAdapter modelVisualizerAdapter;

	public List<Version> getTrajectory() {
		return new LinkedList<>(trajectory);
	}

	public DesignSpaceExplorationAdapterImpl(Model model, DesignSpaceExplorationStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		this.sizeInterpretation = model.getInterpretation(NODE_COUNT_SYMBOL);
		queryEngine = model.getAdapter(ModelQueryAdapter.class);

		globalConstraints = storeAdapter.getGlobalConstraints();
		for (var constraint : globalConstraints) {
			globalConstraintResultSets.add(queryEngine.getResultSet(constraint));
		}

		transformationRules = storeAdapter.getTransformationSpecifications();
		for (var rule : transformationRules) {
			rule.prepare(model, queryEngine);
		}

		objectives = storeAdapter.getObjectives();
		statesAndUntraversedActivations = new HashMap<>();
		statesAndTraversedActivations = new HashMap<>();
		strategy = storeAdapter.getStrategy();
		modelVisualizerAdapter = model.tryGetAdapter(ModelVisualizerAdapter.class).orElse(null);
		isVisualizationEnabled = modelVisualizerAdapter != null;

	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public DesignSpaceExplorationStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}

	@Override
	public LinkedHashSet<Version> explore() {
		var state = model.commit();
		trajectory.add(state);
		statesAndUntraversedActivations.put(state, getAllActivations());
		statesAndTraversedActivations.put(state, new LinkedHashSet<>());
		strategy.initStrategy(this);
		strategy.explore();
		return solutions;
	}

	@Override
	public int getModelSize() {
		return sizeInterpretation.get(Tuple.of());
	}

	@Override
	public Tuple1 createObject() {
		var newNodeId =  getModelSize();
		sizeInterpretation.put(Tuple.of(), newNodeId + 1);
		return Tuple.of(newNodeId);
	}

	@Override
	public Tuple deleteObject(Tuple tuple) {
		if (tuple.getSize() != 1) {
			throw new IllegalArgumentException("Tuple size must be 1");
		}
//		TODO: implement more efficient deletion
//		if (tuple.get(0) == getModelSize() - 1) {
//			sizeInterpretation.put(Tuple.of(), getModelSize() - 1);
//		}
		return tuple;
	}

	@Override
	public boolean checkGlobalConstraints() {
		for (var resultSet : globalConstraintResultSets) {
			if (resultSet.size() > 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean backtrack() {
		if (trajectory.size() < 2) {
			return false;
		}
		if (isVisualizationEnabled) {
			modelVisualizerAdapter.addTransition(trajectory.get(trajectory.size() - 1),
					trajectory.get(trajectory.size() - 2), "backtrack");
		}
		model.restore(trajectory.get(trajectory.size() - 2));
		trajectory.remove(trajectory.size() - 1);
		return true;
	}

	@Override
	public void restoreTrajectory(List<Version> trajectory) {
		model.restore(trajectory.get(trajectory.size() - 1));
//		if (isVisualizationEnabled) {
//			modelVisualizerAdapter.addTransition(this.trajectory.get(trajectory.size() - 1),
//					trajectory.get(trajectory.size() - 1), "restore");
//		}
		this.trajectory = trajectory;

	}

	@Override
	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	public void setRandom(long seed) {
		this.random = new Random(seed);
	}

	@Override
	public Fitness calculateFitness() {
		Fitness result = new Fitness();
		boolean satisfiesHardObjectives = true;
		for (Objective objective : objectives) {
			var fitness = objective.getFitness(this);
			result.put(objective.getName(), fitness);
			if (objective.isHardObjective() && !objective.satisfiesHardObjective(fitness)) {
				satisfiesHardObjectives = false;
			}
		}
		result.setSatisfiesHardObjectives(satisfiesHardObjectives);

		lastFitness = result;

		return result;
	}

	@Override
	public void newSolution() {
		var state = model.getState();
		solutions.add(state);
		if (isVisualizationEnabled) {
			modelVisualizerAdapter.addSolution(state);
		}
	}

	@Override
	public int getDepth() {
		return trajectory.size() - 1;
	}

	public LinkedHashSet<Activation> getUntraversedActivations() {
//		return statesAndUntraversedActivations.get(model.getState());
		LinkedHashSet<Activation> untraversedActivations = new LinkedHashSet<>();
		for (Activation activation : getAllActivations()) {
			if (!statesAndTraversedActivations.get(model.getState()).contains(activation)) {
				untraversedActivations.add(activation);
			}
		}

		return untraversedActivations;
	}

	@Override
	public boolean fireActivation(Activation activation) {
		if (activation == null) {
			return false;
		}
		var previousState = model.getState();
		if (!statesAndUntraversedActivations.get(previousState).contains(activation)) {
//			TODO: throw exception?
			return false;
		}
		if (!activation.fire()) {
			return false;
		}
		statesAndUntraversedActivations.get(previousState).remove(activation);
		statesAndTraversedActivations.get(previousState).add(activation);
		var newState = model.commit();
		trajectory.add(newState);
		isNewState = !statesAndUntraversedActivations.containsKey(newState);
		statesAndUntraversedActivations.put(newState, getAllActivations());
		statesAndTraversedActivations.put(newState, new LinkedHashSet<>());
		if (isVisualizationEnabled) {
			if (isNewState) {
				modelVisualizerAdapter.addState(newState);
			}
			modelVisualizerAdapter.addTransition(trajectory.get(trajectory.size() - 2),
					trajectory.get(trajectory.size() - 1), activation.transformationRule().getName(),
					activation.activation());
		}
		return true;
	}

	@Override
	public void fireRandomActivation() {
		var activations = getUntraversedActivations();
		if (activations.isEmpty()) {
//			TODO: throw exception
			return;
		}
		int index = random.nextInt(activations.size());
		var iterator = activations.iterator();
		while (index-- > 0) {
			iterator.next();
		}
		var activationId = iterator.next();
		fireActivation(activationId);
	}

	@Override
	public boolean isCurrentInTrajectory() {
		return trajectory.contains(model.getState());
	}

	public LinkedHashSet<Activation> getAllActivations() {
		LinkedHashSet<Activation> result = new LinkedHashSet<>();
		for (var rule : transformationRules) {
			result.addAll(rule.getAllActivations());
		}
		return result;
	}

	public boolean isCurrentStateAlreadyTraversed() {
//		TODO: check isomorphism?
		return !isNewState;
	}

	public Fitness getLastFitness() {
		return lastFitness;
	}

	public ObjectiveComparatorHelper getObjectiveComparatorHelper() {
		if (objectiveComparatorHelper == null) {
			objectiveComparatorHelper = new ObjectiveComparatorHelper(objectives);
		}
		return objectiveComparatorHelper;
	}
}