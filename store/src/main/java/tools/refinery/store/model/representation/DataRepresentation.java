package tools.refinery.store.model.representation;
import java.util.Objects;

import tools.refinery.store.map.ContinousHashProvider;

public abstract class DataRepresentation<K, V> implements Comparable<DataRepresentation<K, V>>{
	protected final ContinousHashProvider<K> hashProvider;
	private final String name;
	protected final V defaultValue;

	private final Class<K> keyType;
	private final Class<V> valueType;

	protected DataRepresentation(String name, ContinousHashProvider<K> hashProvider, Class<K> keyType, Class<V> valueType,	V defaultValue) {
		this.name = name;
		this.hashProvider = hashProvider;
		this.defaultValue = defaultValue;
		this.keyType = keyType;
		this.valueType = valueType;
	}

	public abstract String getName();

	public ContinousHashProvider<K> getHashProvider() {
		return hashProvider;
	}
	public abstract boolean isValidKey(K key);

	public V getDefaultValue() {
		return defaultValue;
	}

	public Class<K> getKeyType() {
		return keyType;
	}

	public Class<V> getValueType() {
		return valueType;
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
