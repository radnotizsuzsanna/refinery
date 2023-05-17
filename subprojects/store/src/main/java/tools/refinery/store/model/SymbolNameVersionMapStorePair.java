package tools.refinery.store.model;


import tools.refinery.store.map.VersionedMapStore;

public record SymbolNameVersionMapStorePair<K,V>(String symbolName,
														 VersionedMapStore<K, V> mapStore) {

}
