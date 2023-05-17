package tools.refinery.store.map;

import tools.refinery.store.map.internal.DeltaDiffCursor;
import tools.refinery.store.map.internal.MapDelta;
import tools.refinery.store.map.internal.MapTransaction;
import tools.refinery.store.map.internal.VersionedMapDeltaImpl;

import java.util.*;

public class VersionedMapStoreDeltaImpl<K, V> implements VersionedMapStore<K, V> {
	// Configuration
	protected final boolean summarizeChanges;

	// Static data
	protected final V defaultValue;

	// Dynamic data
	//TODO kiszedtem a finalt
	protected /*final*/ Map<Long, MapTransaction<K, V>> states = new HashMap<>();
	protected long nextID = 0;

	public VersionedMapStoreDeltaImpl(boolean summarizeChanges, V defaultValue) {
		this.summarizeChanges = summarizeChanges;
		this.defaultValue = defaultValue;
	}


	@Override
	public VersionedMap<K, V> createMap() {
		return new VersionedMapDeltaImpl<>(this, this.summarizeChanges, this.defaultValue);
	}

	@Override
	public VersionedMap<K, V> createMap(long state) {
		VersionedMapDeltaImpl<K, V> result = new VersionedMapDeltaImpl<>(this, this.summarizeChanges, this.defaultValue);
		result.restore(state);
		return result;
	}

	public synchronized MapTransaction<K, V> appendTransaction(MapDelta<K, V>[] deltas, MapTransaction<K, V> previous, long[] versionContainer) {
		long version = nextID++;
		versionContainer[0] = version;
		if (deltas == null) {
			states.put(version, previous);
			return previous;
		} else {
			MapTransaction<K, V> transaction = new MapTransaction<>(deltas, version, previous);
			states.put(version, transaction);
			return transaction;
		}
	}

	//TODO publikussá tettem
	public synchronized MapTransaction<K, V> getState(long state) {
		return states.get(state);
	}

	//set states helyett
	public Map<Long, MapTransaction<K, V>> internalExposeStates() {
		return states;
	}

	/*public void setStates(Map<Long, MapTransaction<K, V>> s){
		states = s;
	}*/

	public MapTransaction<K, V> getPath(long to, List<MapDelta<K, V>[]> forwardTransactions) {
		final MapTransaction<K, V> target = getState(to);
		MapTransaction<K, V> toTransaction = target;
		while (toTransaction != null) {
			forwardTransactions.add(toTransaction.deltas());
			toTransaction = toTransaction.parent();
		}
		return target;
	}

	public MapTransaction<K, V> getPath(long from, long to,
						List<MapDelta<K, V>[]> backwardTransactions,
						List<MapDelta<K, V>[]> forwardTransactions) {
		MapTransaction<K, V> fromTransaction = getState(from);
		final MapTransaction<K, V> target = getState(to);
		MapTransaction<K, V> toTransaction = target;

		while (fromTransaction != toTransaction) {
			if (fromTransaction == null || (toTransaction != null && fromTransaction.version() < toTransaction.version())) {
				forwardTransactions.add(toTransaction.deltas());
				toTransaction = toTransaction.parent();
			} else {
				backwardTransactions.add(fromTransaction.deltas());
				fromTransaction = fromTransaction.parent();
			}
		}
		return target;
	}


	@Override
	public synchronized Set<Long> getStates() {
		return new HashSet<>(states.keySet());
	}

	@Override
	public DiffCursor<K, V> getDiffCursor(long fromState, long toState) {
		List<MapDelta<K, V>[]> backwardTransactions = new ArrayList<>();
		List<MapDelta<K, V>[]> forwardTransactions = new ArrayList<>();
		getPath(fromState, toState, backwardTransactions, forwardTransactions);
		return new DeltaDiffCursor<>(backwardTransactions, forwardTransactions);
	}
}
