package tools.refinery.store.map.internal;

public interface UncommitedDeltaStore<K, V> {
	void processChange(K key, V oldValue, V newValue);

	MapDelta<K, V>[] extractDeltas();

	MapDelta<K, V>[] extractAndDeleteDeltas();

}
