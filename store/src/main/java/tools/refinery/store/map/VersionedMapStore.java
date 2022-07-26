package tools.refinery.store.map;

import java.util.Set;

import org.eclipse.collections.api.set.primitive.MutableLongSet;

public interface VersionedMapStore<K, V> {
	
	public VersionedMap<K, V> createMap();

	public VersionedMap<K, V> createMap(long state);
	
	public MutableLongSet getStates();

	public DiffCursor<K,V> getDiffCursor(long fromState, long toState);
	
	public VersionedMapStoreStatistics getStatistics(Set<VersionedMapStoreStatistics> existingStatistics);
}