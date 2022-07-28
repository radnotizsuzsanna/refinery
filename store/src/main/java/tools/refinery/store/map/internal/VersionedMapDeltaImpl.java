package tools.refinery.store.map.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStatistics;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;

public class VersionedMapDeltaImpl<K, V> implements VersionedMap<K, V> {
	protected final VersionedMapStoreDeltaImpl<K, V> store;

	final Map<K, V> current;

	final UncommitedDeltaStore<K, V> uncommitedStore;
	MapTransaction<K, V> previous;

	protected final V defalutValue;

	public VersionedMapDeltaImpl(VersionedMapStoreDeltaImpl<K, V> store, V defalutValue) {
		this.store = store;
		this.defalutValue = defalutValue;

		current = new HashMap<>();
		uncommitedStore = new UncommitedDeltaArrayStore<>();
	}

	@Override
	public long commit() {
		MapDelta<K, V>[] deltas = uncommitedStore.extractAndDeleteDeltas();
		long[] versionContainer = new long[1];
		this.previous = this.store.appendTransaction(deltas, previous, versionContainer);
		return versionContainer[0];
	}

	@Override
	public void restore(long state) {
		// 1. restore uncommited states
		MapDelta<K, V>[] uncommited = this.uncommitedStore.extractAndDeleteDeltas();
		backward(uncommited);
		// 2. get common ancestor
		List<MapDelta<K, V>[]> forward = new ArrayList<>();
		if (this.previous == null) {
			this.store.getPath(state, forward);
			this.forward(forward);
		} else {
			List<MapDelta<K, V>[]> backward = new ArrayList<>();
			this.store.getPath(this.previous.version(), state, backward, forward);
			this.backward(backward);
			this.forward(forward);
		}
	}

	protected void forward(List<MapDelta<K, V>[]> changes) {
		for (int i = changes.size() - 1; i >= 0; i--) {
			forward(changes.get(i));
		}
	}

	protected void backward(List<MapDelta<K, V>[]> changes) {
		for (int i = 0; i < changes.size(); i++) {
			backward(changes.get(i));
		}
	}

	protected void forward(MapDelta<K, V>[] changes) {
		for (int i = 0; i < changes.length; i++) {
			final MapDelta<K, V> change = changes[i];
			current.put(change.getKey(), change.getNewValue());
		}
	}

	protected void backward(MapDelta<K, V>[] changes) {
		for (int i = changes.length - 1; i >= 0; i--) {
			final MapDelta<K, V> change = changes[i];
			current.put(change.getKey(), change.getOldValue());
		}
	}

	@Override
	public V get(K key) {
		return current.getOrDefault(key, defalutValue);
	}

	@Override
	public Cursor<K, V> getAll() {
		return new IteratorAsCursor<>(this, current);
	}

	@Override
	public V put(K key, V value) {
		if (value == defalutValue) {
			V res = current.remove(key);
			if (res == null) {
				// no changes
				return defalutValue;
			} else {
				uncommitedStore.processChange(key, res, value);
				return res;
			}
		} else {
			V oldValue = current.put(key, value);
			uncommitedStore.processChange(key, oldValue, value);
			return oldValue;
		}
	}

	@Override
	public void putAll(Cursor<K, V> cursor) {
		throw new UnsupportedOperationException();

	}

	@Override
	public long getSize() {
		return current.size();
	}

	@Override
	public DiffCursor<K, V> getDiffCursor(long state) {
		MapDelta<K, V>[] backward = this.uncommitedStore.extractDeltas();
		List<MapDelta<K, V>[]> backwardTransactions = new ArrayList<>();
		List<MapDelta<K, V>[]> forwardTransactions = new ArrayList<>();

		if (backward != null) {
			backwardTransactions.add(backward);
		}

		if (this.previous != null) {
			store.getPath(this.previous.version(), state, backwardTransactions, forwardTransactions);
		} else {
			store.getPath(state, forwardTransactions);
		}

		return new DeltaDiffCursor<>(backwardTransactions, forwardTransactions);
	}

	public VersionedMapStatistics getStatistics() {
		throw new UnsupportedOperationException();
	}

	@Override
	public VersionedMapStatistics getStatistics(Map<Object, Object> cache) {
		throw new UnsupportedOperationException();
	}

}
