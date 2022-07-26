package hu.bme.mit.trainbenchmark.generator.refinery;

import java.io.IOException;
import java.util.Map;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.scalable.ScalableModelGenerator;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.Tuple.Tuple1;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

public class RefinerySerializerFunctional extends RefinerySerializer{
	protected boolean isFunctional(String label) {
		return	
				label == ModelConstants.EXIT ||
				label == ModelConstants.ENTRY ||
				label == ModelConstants.TARGET ||
				(label == ModelConstants.CONNECTS_TO && ScalableModelGenerator.inverseConnectsTo) ||
				//label == ModelConstants.MONITORED_BY || // almost
				label == ModelConstants.ELEMENTS ||
				label == ModelConstants.SENSORS ||
				label == ModelConstants.FOLLOWS ||
				label == ModelConstants.SEMAPHORES;
	}
	protected boolean inverting(String label) {
		return label == ModelConstants.ELEMENTS ||
				label == ModelConstants.SENSORS ||
				label == ModelConstants.FOLLOWS ||
				label == ModelConstants.SEMAPHORES;
	}
	
	
	
	@Override
	protected void initReference(Map<String, DataRepresentation<Tuple, Object>> dataRepresentations, String name) {
		if(isFunctional(name)) {
			initFuncReference(dataRepresentations, name);
		} else {
			initRelReference(dataRepresentations, name);
		}
	}
	
	private void initRelReference(Map<String, DataRepresentation<Tuple, Object>> dataRepresentations, String name) {
		dataRepresentations.put(name, new Relation<Object>(name, 2, false));
	}
	private void initFuncReference(Map<String, DataRepresentation<Tuple, Object>> dataRepresentations, String name) {
		dataRepresentations.put(name, new Relation<Object>(name, 1, null));
	}
	
	protected AddTupleToCollection adder = new AddTupleToCollection();
	
	@Override
	public void createEdge(String label, Object from, Object to) throws IOException {
		if(isFunctional(label)) {
			if(inverting(label)) {
				model.put(dataRepresentations.get(label), (Tuple1) to, (Tuple1) from);
			} else {
				model.put(dataRepresentations.get(label), (Tuple1) from, (Tuple1) to);
			}
		} else {
			model.put(dataRepresentations.get(label), Tuple.of(((Tuple1) from).get(0), ((Tuple1) to).get(0)), true);
		}
	}
	
	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		if(isFunctional(label)) {
			if(inverting(label)) {
				model.put(dataRepresentations.get(label), (Tuple1) to, null);
			} else {
				model.put(dataRepresentations.get(label), (Tuple1) from, null);
			}
		} else {
			model.put(dataRepresentations.get(label), Tuple.of(((Tuple1) from).get(0), ((Tuple1) to).get(0)), false);
		}
	}
}
