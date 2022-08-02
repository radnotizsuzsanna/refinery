package tools.refinery.store.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreConfiguration;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.map.VersionedMapStoreImpl;
import tools.refinery.store.map.VersionedMapStoreStatistics;
import tools.refinery.store.model.internal.ModelImpl;
import tools.refinery.store.model.internal.SimilarRelationEquivalenceClass;
import tools.refinery.store.model.representation.AuxilaryData;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

public class ModelStoreImpl implements ModelStore {

	private final Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> stores;

	public ModelStoreImpl(Set<DataRepresentation<?, ?>> dataRepresentations) {
		this(dataRepresentations, ModelStoreConfig.defaultConfig);
	}

	public ModelStoreImpl(Set<DataRepresentation<?, ?>> dataRepresentations, ModelStoreConfig config) {
		stores = new HashMap<>();
		initStores(stores, dataRepresentations, config);
	}

	private void initStores(Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> result,
			Set<DataRepresentation<?, ?>> dataRepresentations, ModelStoreConfig config) {
		if (config.stateBasedConfig == null) {
			initDeltaStores(result, dataRepresentations);
		}
		else {
			initStateBasedStores(result, dataRepresentations, config);
		}
	}
	
	private void initDeltaStores(Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> result,
			Set<DataRepresentation<?, ?>> dataRepresentations) {
		for (DataRepresentation<?, ?> dataRepresentation : dataRepresentations) {
			final VersionedMapStore<?, ?> store = new VersionedMapStoreDeltaImpl<>(
					dataRepresentation.getDefaultValue());
			result.put(dataRepresentation, store);
		}
	}

	private void initStateBasedStores(Map<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> result,
			Set<DataRepresentation<?, ?>> dataRepresentations, ModelStoreConfig config) {
		VersionedMapStoreConfiguration stateBasedConfig = config.stateBasedConfig;
		if(stateBasedConfig.isSharedNodeCacheInStoreGroups()) {
			// initialize groups
			Map<SimilarRelationEquivalenceClass, List<Relation<?>>> symbolRepresentationsPerHashPerArity = new HashMap<>();
			
			// fill groups with similar data representations
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
			
			// construct groups
			for (List<Relation<?>> symbolGroup : symbolRepresentationsPerHashPerArity.values()) {
				initRepresentationGroup(result, symbolGroup);
			}
		} else {
			// create individual groups
			for (DataRepresentation<?, ?> dataRepresentation : dataRepresentations) {
				final VersionedMapStore<?, ?> store = new VersionedMapStoreImpl<>(dataRepresentation.getHashProvider(),
							dataRepresentation.getDefaultValue(), config.stateBasedConfig);
				result.put(dataRepresentation, store);
			}
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
	public synchronized MutableLongSet getStates() {
		var iterator = stores.values().iterator();
		if (iterator.hasNext()) {
			return iterator.next().getStates();
		}
		return new LongHashSet();
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

		statistics.setStates(this.getStates().size());

		// 4. collect state statistics

		Map<Object, Object> cache = new HashMap<>();
		for (Long state : getStates().toArray()) {
			statistics.addStateStatistics(state, this.createModel(state).getStatistics(cache));
		}

		// 5. finalize the statistics

		statistics.finish();

		return statistics;
	}
}
