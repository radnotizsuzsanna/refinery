package tools.refinery.store.map;

public class VersionedMapStoreStatistics {
	int states;
	Object nodeCache;
	VersionedMapStatistics nodeCacheStatistics;
	public VersionedMapStoreStatistics(int states, Object nodeCache, VersionedMapStatistics nodeCacheStatistics) {
		super();
		this.states = states;
		this.nodeCache = nodeCache;
		this.nodeCacheStatistics = nodeCacheStatistics;
	}
	public int getStates() {
		return states;
	}
	public Object getNodeCache() {
		return nodeCache;
	}
	public VersionedMapStatistics getNodeCacheStatistics() {
		return nodeCacheStatistics;
	}
}
