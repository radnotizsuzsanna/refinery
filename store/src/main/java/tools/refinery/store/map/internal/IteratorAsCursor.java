package tools.refinery.store.map.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.VersionedMap;

public class IteratorAsCursor<K, V> implements Cursor<K, V> {
	final Iterator<Entry<K, V>> iterator;
	final VersionedMap<K, V> source;

	private boolean terminated;
	private K key;
	private V value;

	public IteratorAsCursor(VersionedMap<K, V> source, Map<K, V> current) {
		this.iterator = current.entrySet().iterator();
		this.source = source;
		move();
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}


	@Override
	public boolean move() {
		//TODO ez miért nem fordítva?
		terminated = iterator.hasNext();
		terminated = !iterator.hasNext();
		if(terminated) {
			this.key = null;
			this.value = null;
		} else {
			Entry<K, V> next = iterator.next();
			this.key= next.getKey();
			this.value = next.getValue();
		}
		return !terminated;
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public List<VersionedMap<?, ?>> getDependingMaps() {
		return Collections.singletonList(this.source);
	}
}
