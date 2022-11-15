package tools.refinery.store.model.representation;

import java.util.Objects;

import tools.refinery.store.map.ContinousHashProvider;

public class AuxilaryData<K,V> extends DataRepresentation<K, V> {
	private final String name;

	public AuxilaryData(String name, Class<K> keyType, ContinousHashProvider<K> hashProvider, Class<V> valueType, V defaultValue) {
		super(name, hashProvider, keyType, valueType, defaultValue);
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isValidKey(K key) {
		return true;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuxilaryData<?, ?> other = (AuxilaryData<?, ?>) obj;
		return Objects.equals(name, other.name);
	}
}
