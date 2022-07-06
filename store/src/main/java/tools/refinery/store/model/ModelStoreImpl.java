package tools.refinery.store.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStatistics;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreImpl;
import tools.refinery.store.map.VersionedMapStoreStatistics;
import tools.refinery.store.map.internal.Node;
import tools.refinery.store.model.internal.ModelImpl;
import tools.refinery.store.model.internal.SimilarRelationEquivalenceClass;
import tools.refinery.store.model.representation.AuxilaryData;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

public class ModelStoreImpl implements ModelStore {

	private final Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> stores;

	public ModelStoreImpl(Set<DataRepresentation<?, ?>> dataRepresentations) {
		stores = new HashMap<>();
		initStores(stores, dataRepresentations);
	}

	public ModelStoreImpl(List<Set<DataRepresentation<?, ?>>> dataRepresentationGroup) {
		stores = new HashMap<>();
		for (Set<DataRepresentation<?, ?>> set : dataRepresentationGroup) {
			initStores(stores, set);
		}
	}

	private void initStores(Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> result,
			Set<DataRepresentation<?, ?>> dataRepresentations) {
		Map<SimilarRelationEquivalenceClass, List<Relation<?>>> symbolRepresentationsPerHashPerArity = new HashMap<>();

		for (DataRepresentation<?, ?> dataRepresentation : dataRepresentations) {
			if (dataRepresentation instanceof Relation<?> symbolRepresentation) {
				addOrCreate(symbolRepresentationsPerHashPerArity,
						new SimilarRelationEquivalenceClass(symbolRepresentation), symbolRepresentation);
			} else if (dataRepresentation instanceof AuxilaryData<?, ?>) {
				VersionedMapStoreImpl<?, ?> store = new VersionedMapStoreImpl<>(dataRepresentation.getHashProvider(),
						dataRepresentation.getDefaultValue());
				result.put(dataRepresentation, store);
			} else {
				throw new UnsupportedOperationException(
						"Model store does not have strategy to use " + dataRepresentation.getClass() + "!");
			}
		}
		for (List<Relation<?>> symbolGroup : symbolRepresentationsPerHashPerArity.values()) {
			initRepresentationGroup(result, symbolGroup);
		}
	}

	private void initRepresentationGroup(Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> result,
			List<Relation<?>> symbolGroup) {
		final ContinousHashProvider<Tuple> hashProvider = symbolGroup.get(0).getHashProvider();
		final Object defaultValue = symbolGroup.get(0).getDefaultValue();

		List<VersionedMapStore<Tuple, Object>> maps = VersionedMapStoreImpl
				.createSharedVersionedMapStores(symbolGroup.size(), hashProvider, defaultValue);

		for (int i = 0; i < symbolGroup.size(); i++) {
			result.put(symbolGroup.get(i), maps.get(i));
		}
	}

	private static <K, V> void addOrCreate(Map<K, List<V>> map, K key, V value) {
		List<V> list;
		if (map.containsKey(key)) {
			list = map.get(key);
		} else {
			list = new LinkedList<>();
			map.put(key, list);
		}
		list.add(value);
	}

	@Override
	public Set<DataRepresentation<?, ?>> getDataRepresentations() {
		return this.stores.keySet();
	}

	@Override
	public ModelImpl createModel() {
		Map<DataRepresentation<?, ?>, VersionedMap<?, ?>> maps = new HashMap<>();
		for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : this.stores.entrySet()) {
			maps.put(entry.getKey(), entry.getValue().createMap());
		}
		return new ModelImpl(this, maps);
	}

	@Override
	public synchronized ModelImpl createModel(long state) {
		Map<DataRepresentation<?, ?>, VersionedMap<?, ?>> maps = new HashMap<>();
		for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : this.stores.entrySet()) {
			maps.put(entry.getKey(), entry.getValue().createMap(state));
		}
		return new ModelImpl(this, maps);
	}

	@Override
	public synchronized Set<Long> getStates() {
		var iterator = stores.values().iterator();
		if (iterator.hasNext()) {
			return Set.copyOf(iterator.next().getStates());
		}
		return Set.of();
	}

	@Override
	public synchronized ModelDiffCursor getDiffCursor(long from, long to) {
		Map<DataRepresentation<?, ?>, DiffCursor<?, ?>> diffcursors = new HashMap<>();
		for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : stores.entrySet()) {
			DataRepresentation<?, ?> representation = entry.getKey();
			DiffCursor<?, ?> diffCursor = entry.getValue().getDiffCursor(from, to);
			diffcursors.put(representation, diffCursor);
		}
		return new ModelDiffCursor(diffcursors);
	}

	@Override
	public ModelStoreStatistics getStatistics() {

		// 1. Collect symbols grouped by common statistics.

		Map<VersionedMapStoreStatistics, String> statistics2Name = new HashMap<>();
		for (Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : this.stores.entrySet()) {
			VersionedMapStoreStatistics newstatistics = entry.getValue().getStatistics(statistics2Name.keySet());
			String symbolCollectionName = statistics2Name.get(newstatistics);
			if (symbolCollectionName != null) {
				statistics2Name.put(newstatistics, symbolCollectionName + "," + entry.getKey().getName());
			} else {
				statistics2Name.put(newstatistics, entry.getKey().getName());
			}
		}

		// 2. For each group, collect state statistic calculated from the statistics of
		// the restored state.

		ModelStoreStatistics statistics = new ModelStoreStatistics();
		for (Entry<VersionedMapStoreStatistics, String> statisticsGroup : statistics2Name.entrySet()) {
			statistics.addStoreStatistics(statisticsGroup.getValue(), statisticsGroup.getKey());
		}
		
		// 3. get the number of states

		Set<Long> states = this.getStates();
		statistics.setStates(states.size());
		
		// 4. collect state statistics
		
		Map<Object, Object> cache = new HashMap<>();
		for (Long state : states) {
			statistics.addStateStatistics(state, this.createModel(state).getStatistics(cache));
		}
		
		// 5. finalize the statistics
		
		statistics.finish();
		
		return statistics;
	}
}
