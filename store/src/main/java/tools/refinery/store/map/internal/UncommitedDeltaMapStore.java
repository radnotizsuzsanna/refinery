package tools.refinery.store.map.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.store.map.VersionedMap;

public class UncommitedDeltaMapStore<K, V> implements UncommitedDeltaStore<K, V> {
	final VersionedMap<K, V> source;
	final Map<K, V> uncommitedOldValues = new HashMap<>();

	public UncommitedDeltaMapStore(VersionedMap<K, V> source) {
		this.source = source;
	}

	@Override
	public void processChange(K key, V oldValue, V newValue) {
		this.uncommitedOldValues.putIfAbsent(key, oldValue);
	}

	@Override
	public MapDelta<K, V>[] extractDeltas() {
		if (uncommitedOldValues.isEmpty()) {
			return null;
		} else {
			@SuppressWarnings("unchecked")
			MapDelta<K, V>[] deltas = new MapDelta[uncommitedOldValues.size()];
			int i = 0;
			for (Entry<K, V> entry : uncommitedOldValues.entrySet()) {
				final K key = entry.getKey();
				final V oldValue = entry.getValue();
				final V newValue = source.get(key);
				deltas[i] = new MapDelta<>(key, oldValue, newValue);
			}

			return deltas;
		}
	}

	@Override
	public MapDelta<K, V>[] extractAndDeleteDeltas() {
		MapDelta<K, V>[] res = extractDeltas();
		this.uncommitedOldValues.clear();
		return res;
	}
}
