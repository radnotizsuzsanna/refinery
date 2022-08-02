package tools.refinery.store.model;

import tools.refinery.store.map.VersionedMapStoreConfiguration;

public class ModelStoreConfig {
	public static final ModelStoreConfig delta = new ModelStoreConfig(null);
	public static final ModelStoreConfig state = new ModelStoreConfig(new VersionedMapStoreConfiguration());
	public static final ModelStoreConfig defaultConfig = delta;
	
	protected ModelStoreConfig(VersionedMapStoreConfiguration stateBasedConfig) {
		this.stateBasedConfig = stateBasedConfig;
		
	}
	
	VersionedMapStoreConfiguration stateBasedConfig;
}
