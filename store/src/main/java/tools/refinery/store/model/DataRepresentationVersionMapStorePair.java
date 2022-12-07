package tools.refinery.store.model;

import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.model.representation.DataRepresentation;

public record DataRepresentationVersionMapStorePair<K,V>(DataRepresentation<K, V> dataRepresentation, VersionedMapStore<K, V> mapStore) {

}
