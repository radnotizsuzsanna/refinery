package tools.refinery.store.model;


import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.representation.Symbol;

public record SymbolVersionMapStorePair<Tuple,V>(Symbol<V> symbol,
											 VersionedMapStore<Tuple, V> mapStore) {

}
