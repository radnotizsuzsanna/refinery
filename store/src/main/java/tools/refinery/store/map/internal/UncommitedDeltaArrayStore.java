package tools.refinery.store.map.internal;

import java.util.ArrayList;
import java.util.List;

public class UncommitedDeltaArrayStore<K,V> implements UncommitedDeltaStore<K, V> {
	final List<MapDelta<K, V>> uncommitedOldValues = new ArrayList<>();

	@Override
	public void processChange(K key, V oldValue, V newValue) {
		uncommitedOldValues.add(new MapDelta<>(key, oldValue, newValue));
	}

	@Override
	public MapDelta<K, V>[] extractDeltas() {
		if(uncommitedOldValues.isEmpty()) {
			//TODO miert empty azert volt az, mert csak a masik store-t modositottuk
			return null;
		} else {
			@SuppressWarnings("unchecked")
			MapDelta<K, V>[] result = uncommitedOldValues.toArray(new MapDelta[0]);
			return result;
		}
	}

	@Override
	public MapDelta<K, V>[] extractAndDeleteDeltas() {
		//TODO null-t ad vissza
		MapDelta<K, V>[] res = extractDeltas();
		this.uncommitedOldValues.clear();
		return res;
	}
}
