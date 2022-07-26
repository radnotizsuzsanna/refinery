package hu.bme.mit.trainbenchmark.generator.refinery;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreImpl;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.Tuple.Tuple1;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

public class RefinerySerializer extends ModelSerializer{

	Map<String, DataRepresentation<Tuple, Object>> dataRepresentations;
	protected ModelStore store = null;
	protected Model model = null;
	
	public RefinerySerializer() {
		dataRepresentations = new HashMap<>();
		String[] types = new String[] {
				ModelConstants.TRACKELEMENT,
				ModelConstants.REGION,
				ModelConstants.ROUTE,
				ModelConstants.SEGMENT,
				ModelConstants.SENSOR,
				ModelConstants.SEMAPHORE,
				ModelConstants.SWITCH,
				ModelConstants.SWITCHPOSITION
		};
		for(String type : types) {
			initType(dataRepresentations, type);
		}
		
		String[] references = new String[] {
				ModelConstants.CONNECTS_TO,
				ModelConstants.ELEMENTS,
				ModelConstants.EXIT,
				ModelConstants.ENTRY,
				ModelConstants.FOLLOWS,
				ModelConstants.REQUIRES,
				ModelConstants.MONITORED_BY,
				ModelConstants.SEMAPHORES,
				ModelConstants.SENSORS,
				ModelConstants.TARGET
		};
		for(String reference : references) {
			initReference(dataRepresentations, reference);
		}
		initAttriubte(dataRepresentations, ModelConstants.ACTIVE, false);
		initAttriubte(dataRepresentations, ModelConstants.LENGTH, 0);
		initAttriubte(dataRepresentations, ModelConstants.SIGNAL, null);
		initAttriubte(dataRepresentations, ModelConstants.CURRENTPOSITION, null);
		initAttriubte(dataRepresentations, ModelConstants.POSITION, 0);
		
	}
	
	@Override
	public String syntax() {
		return null;
	}
	
	@Override
	public void initModel() throws IOException {
		store = new ModelStoreImpl(new HashSet<>(dataRepresentations.values()));
		model = store.createModel();
	}


	protected void initType(Map<String, DataRepresentation<Tuple, Object>> dataRepresentations, String name) {
		dataRepresentations.put(name, new Relation<Object>(name, 1, false));
	}
	protected void initReference(Map<String, DataRepresentation<Tuple, Object>> dataRepresentations, String name) {
		dataRepresentations.put(name, new Relation<Object>(name, 2, false));
	}
	protected void initAttriubte(Map<String, DataRepresentation<Tuple, Object>> dataRepresentations, String name, Object defaultValue) {
		dataRepresentations.put(name, new Relation<>(name, 1, defaultValue));
	}

	public void print() {
		System.out.println(this.store.getStatistics().print());
	}
	
	public void getHash() {
		this.model.hashCode();
	}

	@Override
	public Object createVertex(int id, String type, Map<String, ? extends Object> attributes,
			Map<String, Object> outgoingEdges, Map<String, Object> incomingEdges) throws IOException {
		Tuple1 vertex = Tuple.of1(id);
		
		// 1. add to type map
		model.put(dataRepresentations.get(type), vertex, true);
//		if(type.equals(ModelConstants.SEGMENT) || type.equals(ModelConstants.SWITCH)) {
//			model.put(dataRepresentations.get(ModelConstants.TRACKELEMENT), Tuple.of1(id), true);
//		}
		// 2. set attributes
		for(Entry<String, ? extends Object> attribute : attributes.entrySet()) {
			model.put(dataRepresentations.get(attribute.getKey()), vertex, attribute.getValue());
		}
		// 3. outgoing edges
		for(Entry<String, ? extends Object> outgoing : outgoingEdges.entrySet()) {
			this.createEdge(outgoing.getKey(), vertex, outgoing.getValue());
		}
		// 4. incoming edges
		for(Entry<String, ? extends Object> incoming : incomingEdges.entrySet()) {
			this.createEdge(incoming.getKey(), incoming.getValue(), vertex);
		}
		// finish
		return vertex;
	}

	@Override
	public void createEdge(String label, Object from, Object to) throws IOException {
		model.put(dataRepresentations.get(label), Tuple.of(((Tuple1) from).get(0), ((Tuple1) to).get(0)), true);
	}
	
	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		model.put(dataRepresentations.get(label), Tuple.of(((Tuple1) from).get(0), ((Tuple1) to).get(0)), false);
	}
	
	@Override
	public void setAttribute(String label, Object object, Object value) throws IOException {
		model.put(dataRepresentations.get(label), (Tuple1)object, value);
	}
	
	@Override
	public long commit() {
		long res = this.model.commit();
		//print();
		return res;
		
	}
	@Override
	public void restore(long version) {
		this.model.restore(version);
	}
}
