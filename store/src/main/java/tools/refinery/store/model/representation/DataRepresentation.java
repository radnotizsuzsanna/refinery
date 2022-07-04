package tools.refinery.store.model.representation;

import java.util.Objects;

import tools.refinery.store.map.ContinousHashProvider;

public abstract class DataRepresentation<K, V> implements Comparable<DataRepresentation<K, V>>{
	protected final ContinousHashProvider<K> hashProvider;
	protected final V defaultValue;

	protected DataRepresentation(ContinousHashProvider<K> hashProvider,	V defaultValue) {
		this.hashProvider = hashProvider;
		this.defaultValue = defaultValue;
	}
	
	public abstract String getName();
	
	public ContinousHashProvider<K> getHashProvider() {
		return hashProvider;
	}
	public abstract boolean isValidKey(K key);

	public V getDefaultValue() {
		return defaultValue;
	}
	
	@Override
	public int compareTo(DataRepresentation<K, V> o) {
		return this.getName().compareTo(o.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(defaultValue, hashProvider);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataRepresentation<?, ?> other = (DataRepresentation<?, ?>) obj;
		return Objects.equals(defaultValue, other.defaultValue) && Objects.equals(hashProvider, other.hashProvider);
	}
}
