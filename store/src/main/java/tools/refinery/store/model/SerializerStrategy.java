package tools.refinery.store.model;

import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.model.representation.Relation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

public interface SerializerStrategy<T> {
	void  writeRelation(Relation relation, DataOutputStream relations, HashMap<Relation<?>, DataOutputStream> streams, VersionedMapStore<?, ?> deltaStore) throws IOException;
	Relation readRelation(DataInputStream relations, HashMap<Relation<?>, DataInputStream> streams) throws IOException;
	public void writeDeltaStore(Relation relation, VersionedMapStoreDeltaImpl mapStore, DataOutputStream data) throws IOException;
	VersionedMapStoreDeltaImpl<?,?> readDeltaStore(Relation<?> relation, DataInputStream data) throws IOException;
}
