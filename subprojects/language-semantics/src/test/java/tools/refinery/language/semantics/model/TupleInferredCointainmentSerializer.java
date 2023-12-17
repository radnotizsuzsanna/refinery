package tools.refinery.language.semantics.model;

import tools.refinery.store.model.SerializerStrategy;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.containment.InferredContainment;
import tools.refinery.store.reasoning.translator.typehierarchy.InferredType;
import tools.refinery.store.representation.TruthValue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static tools.refinery.store.reasoning.translator.containment.InferredContainment.UNKNOWN;
import static tools.refinery.store.representation.TruthValue.*;

/**
 * Serializes and deserializes the boolean value
 */
public class TupleInferredCointainmentSerializer implements SerializerStrategy<InferredContainment> {
	final List<PartialRelation> sortedTypes;

	public TupleInferredCointainmentSerializer(List<PartialRelation> sortedTypes) {
		this.sortedTypes = sortedTypes;
	}

	/**
	 * Writes out the value
	 *
	 * @param stream The output stream for serializing the value
	 * @param value  The value to serialize
	 * @throws IOException Exception can occur when writing out the data
	 */
	@Override
	public void writeValue(DataOutputStream stream, InferredContainment value) throws IOException {
		TruthValue contains = value.contains();
		int ordinal;
		switch (contains){
			case TRUE -> ordinal = 0;
			case FALSE -> ordinal = 1;
			case UNKNOWN -> ordinal = 2;
			case ERROR -> ordinal = 3;
			default -> throw new IllegalStateException("Unexpected value: " +value);
		}
		stream.writeInt(ordinal);


		boolean[] array1 = new boolean[this.sortedTypes.size()];
		for (var mustLink : value.mustLinks()) {
			array1[this.sortedTypes.indexOf(mustLink)] = true;
		}

		for(int i = 0; i < this.sortedTypes.size();i++){
			stream.writeBoolean(array1[i]);
		}

		boolean[] array2 = new boolean[this.sortedTypes.size()];
		for (var forbiddenLink : value.forbiddenLinks()) {
			array2[this.sortedTypes.indexOf(forbiddenLink)] = true;
		}

		for(int i = 0; i < this.sortedTypes.size();i++){
			stream.writeBoolean(array2[i]);
		}
	}

	/**
	 * Reads the value from the stream
	 *
	 * @param stream The stream to read the value from
	 * @return The deserialized value
	 * @throws IOException Exception can occur when reading data from the stream
	 */
	@Override
	public InferredContainment readValue(DataInputStream stream) throws IOException {
		TruthValue contains;
		switch (stream.readInt()) {
			case 0 -> contains = TRUE;
			case 1 -> contains = FALSE;
			case 2 -> contains = TruthValue.UNKNOWN;
			case 3 -> contains = ERROR;
			default -> throw new IllegalStateException("Unexpected value: " + stream.readInt());
		}
		boolean[] array1 = new boolean[this.sortedTypes.size()];
		for (int i = 0; i < array1.length; i++) {
			array1[i] = stream.readBoolean();
		}

		Set<PartialRelation> mustLinks = new HashSet<>();

		for (int i = 0; i < array1.length; i++) {
			if(array1[i]) mustLinks.add(this.sortedTypes.get(i));
		}

		boolean[] array2 = new boolean[this.sortedTypes.size()];
		for (int i = 0; i < array2.length; i++) {
			array2[i] = stream.readBoolean();
		}

		Set<PartialRelation> forbiddenLinks = new HashSet<>();

		for (int i = 0; i < array2.length; i++) {
			if(array2[i]) forbiddenLinks.add(this.sortedTypes.get(i));
		}

		return new InferredContainment(contains,mustLinks,forbiddenLinks);
	}
}
