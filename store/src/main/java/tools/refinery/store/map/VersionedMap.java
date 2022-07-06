package tools.refinery.store.map;

import java.util.Map;

public interface VersionedMap<K,V> extends Versioned{
	public V get(K key);
	public Cursor<K,V> getAll();
	
	public V put(K key, V value);
	public void putAll(Cursor<K,V> cursor);
	
	public long getSize();
	
	public DiffCursor<K,V> getDiffCursor(long state);
	
	public VersionedMapStatistics getStatistics();
	
	public VersionedMapStatistics getStatistics(Map<Object,Object> cache);
}
