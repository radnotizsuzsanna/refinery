package tools.refinery.store.map.internal;

import java.util.ArrayList;
import java.util.List;

public class UncommitedDeltaArrayStore<K,V> implements UncommitedDeltaStore<K, V> {
	final List<MapDelta<K, V>> uncommitedOldValues = new ArrayList<>();

	@Override
	public void processChange(K key, V oldValue, V newValue) {
		//System.out.println("itt jartam, key: " + key + "oldavalue: " + oldValue + "newValue: " +newValue);
		if(oldValue != null){
			System.out.println("itt allj meg");
		}
		uncommitedOldValues.add(new MapDelta<>(key, oldValue, newValue));
	}

	@Override
	public MapDelta<K, V>[] extractDeltas() {
		if(uncommitedOldValues.isEmpty()) {
			//TODO miert empty?
			return null;
		} else {
			@SuppressWarnings("unchecked")
			MapDelta<K, V>[] result = uncommitedOldValues.toArray(new MapDelta[0]);
			return result;
		}
	}

	@Override
	public MapDelta<K, V>[] extractAndDeleteDeltas() {
		//null-t ad vissza
		MapDelta<K, V>[] res = extractDeltas();
		this.uncommitedOldValues.clear();
		return res;
	}
}
