package tools.refinery.store.model;


import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.representation.Symbol;

public record SymbolVersionMapStorePair<K,V>(Symbol<V> symbol,
											 VersionedMapStore<K, V> mapStore) {

}
