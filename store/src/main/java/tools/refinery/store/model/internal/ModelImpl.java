package tools.refinery.store.model.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.internal.MapDiffCursor;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.ModelStatistics;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

public class ModelImpl implements Model {
	private final ModelStore store;
	private final Map<DataRepresentation<?, ?>, VersionedMap<?, ?>> maps;

	public ModelImpl(ModelStore store, Map<DataRepresentation<?, ?>, VersionedMap<?, ?>> maps) {
		this.store = store;
		this.maps = new HashMap<>(maps);
	}

	@Override
	public Set<DataRepresentation<?, ?>> getDataRepresentations() {
		return maps.keySet();
	}

	@SuppressWarnings("unchecked")
	private <K, V> VersionedMap<K, V> getMap(DataRepresentation<K, V> representation) {
		if (maps.containsKey(representation)) {
			return (VersionedMap<K, V>) maps.get(representation);
		} else {
			throw new IllegalArgumentException("Model does have representation " + representation);
		}
	}

	private <K, V> VersionedMap<K, V> getMapValidateKey(DataRepresentation<K, V> representation, K key) {
		if (representation.isValidKey(key)) {
			return getMap(representation);
		} else {
			throw new IllegalArgumentException(
					"Key is not valid for representation! (representation=" + representation + ", key=" + key + ");");
		}
	}

	@Override
	public <K, V> V get(DataRepresentation<K, V> representation, K key) {
		return getMapValidateKey(representation, key).get(key);
	}

	@Override
	public <K, V> Cursor<K, V> getAll(DataRepresentation<K, V> representation) {
		return getMap(representation).getAll();
	}

	@Override
	public <K, V> V put(DataRepresentation<K, V> representation, K key, V value) {
		return getMapValidateKey(representation, key).put(key, value);
	}

	@Override
	public <K, V> void putAll(DataRepresentation<K, V> representation, Cursor<K, V> cursor) {
		getMap(representation).putAll(cursor);
	}

	@Override
	public <K, V> long getSize(DataRepresentation<K, V> representation) {
		return getMap(representation).getSize();
	}

	@Override
	public ModelDiffCursor getDiffCursor(long to) {
		Model toModel = store.createModel(to);
		Map<DataRepresentation<?, ?>, DiffCursor<?, ?>> diffCursors = new HashMap<>();
		for (DataRepresentation<?, ?> representation : this.maps.keySet()) {
			MapDiffCursor<?, ?> diffCursor = constructDiffCursor(toModel, representation);
			diffCursors.put(representation, diffCursor);
		}
		return new ModelDiffCursor(diffCursors);
	}

	private <K, V> MapDiffCursor<K, V> constructDiffCursor(Model toModel, DataRepresentation<K, V> representation) {
		@SuppressWarnings("unchecked")
		Cursor<K, V> fromCursor = (Cursor<K, V>) this.maps.get(representation).getAll();
		Cursor<K, V> toCursor = toModel.getAll(representation);

		ContinousHashProvider<K> hashProvider = representation.getHashProvider();
		V defaultValue = representation.getDefaultValue();
		return new MapDiffCursor<>(hashProvider, defaultValue, fromCursor, toCursor);
	}

	@Override
	public long commit() {
		long version = 0;
		boolean versionSet = false;
		for (VersionedMap<?, ?> map : maps.values()) {
			long newVersion = map.commit();
			if (versionSet) {
				if (version != newVersion) {
					throw new IllegalStateException(
							"Maps in model have different versions! (" + version + " and " + newVersion + ")");
				}
			} else {
				version = newVersion;
				versionSet = true;

			}
		}

		return version;
	}

	@Override
	public void restore(long state) {
		if(store.getStates().contains(state)) {
			for (VersionedMap<?, ?> map : maps.values()) {
				map.restore(state);
			}
		} else {
			throw new IllegalArgumentException("Map does not contain state "+state+"!");
		}
	}

	@Override
	public int hashCode() {
		return this.maps.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this.maps.equals(obj);
	}

	@Override
	public ModelStatistics getStatistics() {
		ModelStatistics statistics = new ModelStatistics();
		for(Entry<DataRepresentation<?, ?>, VersionedMap<?, ?>> entry : maps.entrySet()) {
			DataRepresentation<?, ?> key = entry.getKey();
			String name = key.getName();
			if(key instanceof Relation<?> relation) {
				name += "/"+relation.getArity();
			}
			statistics.addMapStatistic(name, entry.getValue().getStatistics());
		}
		return statistics;
	}

	@Override
	public ModelStatistics getStatistics(Map<Object, Object> cache) {
		ModelStatistics statistics = new ModelStatistics();
		for(Entry<DataRepresentation<?, ?>, VersionedMap<?, ?>> entry : maps.entrySet()) {
			DataRepresentation<?, ?> key = entry.getKey();
			String name = key.getName();
			if(key instanceof Relation<?> relation) {
				name += "/"+relation.getArity();
			}
			statistics.addMapStatistic(name, entry.getValue().getStatistics(cache));
		}
		return statistics;
	}
}
