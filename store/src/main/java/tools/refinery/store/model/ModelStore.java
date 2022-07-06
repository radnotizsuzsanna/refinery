package tools.refinery.store.model;

import java.util.Map;
import java.util.Set;

import tools.refinery.store.map.VersionedMapStatistics;
import tools.refinery.store.map.internal.Node;
import tools.refinery.store.model.representation.DataRepresentation;

public interface ModelStore {
	@SuppressWarnings("squid:S1452")
	Set<DataRepresentation<?, ?>> getDataRepresentations();
	
	Model createModel();
	Model createModel(long state);
	
	Set<Long> getStates();
	ModelDiffCursor getDiffCursor(long from, long to);
	
	ModelStoreStatistics getStatistics();
}