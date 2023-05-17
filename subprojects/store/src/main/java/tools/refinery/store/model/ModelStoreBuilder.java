package tools.refinery.store.model;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.adapter.ModelAdapterBuilderFactory;
import tools.refinery.store.adapter.ModelAdapterType;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public interface ModelStoreBuilder {
	default ModelStoreBuilder symbols(AnySymbol... symbols) {
		return symbols(List.of(symbols));
	}

	default ModelStoreBuilder symbols(Collection<? extends AnySymbol> symbols) {
		symbols.forEach(this::symbol);
		return this;
	}

	default ModelStoreBuilder symbol(AnySymbol symbol) {
		return symbol((Symbol<?>) symbol);
	}

	<T> ModelStoreBuilder symbol(Symbol<T> symbol);

	<T extends ModelAdapterBuilder> T with(ModelAdapterBuilderFactory<?, ?, T> adapterBuilderFactory);

	<T extends ModelAdapterBuilder> Optional<T> tryGetAdapter(ModelAdapterType<?, ?, ? extends T> adapterType);

	<T extends ModelAdapterBuilder> T getAdapter(ModelAdapterType<?, ?, T> adapterType);

	ModelStore build();

	ModelStore buildFromStores(HashMap<AnySymbol, VersionedMapStore<Tuple, ?>> stores);
}
