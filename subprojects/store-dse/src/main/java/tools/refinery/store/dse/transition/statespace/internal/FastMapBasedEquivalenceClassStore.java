/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.statecoding.StateCoderResult;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;

public abstract class FastMapBasedEquivalenceClassStore extends AbstractEquivalenceClassStore implements EquivalenceClassStore {
	private final MutableIntSet codes = IntSets.mutable.empty();
	private final MutableObjectIntMap<VersionWithObjectiveValue> version2CodeMap = ObjectIntMaps.mutable.empty();

	protected FastMapBasedEquivalenceClassStore(StateCoderStoreAdapter stateCoderStoreAdapter) {
		super(stateCoderStoreAdapter);
	}

	@Override
	protected synchronized boolean tryToAdd(StateCoderResult stateCoderResult, VersionWithObjectiveValue newVersion,
											int[] emptyActivations, boolean accept) {
		int code = stateCoderResult.modelCode();
		boolean isNew = this.codes.add(code);
		this.version2CodeMap.put(newVersion, code);
		return isNew;
	}

	@Override
	public synchronized boolean tryToAdd(StateCoderResult stateCoderResult) {
		return this.codes.add(stateCoderResult.modelCode());
	}

	@Override
	public void resolveOneSymmetry() {
		throw new IllegalArgumentException("This equivalence storage is not prepared to resolve symmetries!");
	}

	@Override
	public int getCode(VersionWithObjectiveValue version) {
		return version2CodeMap.get(version);
	}
}
