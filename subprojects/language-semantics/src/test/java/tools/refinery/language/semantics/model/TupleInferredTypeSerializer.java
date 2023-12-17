package tools.refinery.language.semantics.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tools.refinery.store.model.SerializerStrategy;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.typehierarchy.InferredType;

/**
 * Serializes and deserializes the boolean value
 */
public class TupleInferredTypeSerializer implements SerializerStrategy<InferredType> {
	final List<PartialRelation> sortedTypes;

	public TupleInferredTypeSerializer(List<PartialRelation> sortedTypes) {
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
	public void writeValue(DataOutputStream stream, InferredType value) throws IOException {
		PartialRelation candidateType = value.candidateType();
		int candidateTypeInt = this.sortedTypes.indexOf(candidateType);
		stream.writeInt(candidateTypeInt);

		boolean[] array1 = new boolean[this.sortedTypes.size()];
		for (var mustType : value.mustTypes()) {
			//Ehelyett hashmap ha lassu
			array1[this.sortedTypes.indexOf(mustType)] = true;
		}

		for(int i = 0; i < this.sortedTypes.size();i++){
			stream.writeBoolean(array1[i]);
		}

		boolean[] array2 = new boolean[this.sortedTypes.size()];
		for (var mustType : value.mayConcreteTypes()) {
			array2[this.sortedTypes.indexOf(mustType)] = true;
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
	public InferredType readValue(DataInputStream stream) throws IOException {
		int candidateTypeInt = stream.readInt();
		PartialRelation candidateType;
		if(candidateTypeInt == -1) candidateType = null;
		else candidateType =  this.sortedTypes.get(candidateTypeInt);

		boolean[] array1 = new boolean[this.sortedTypes.size()];
		for (int i = 0; i < array1.length; i++) {
			array1[i] = stream.readBoolean();
		}

		Set<PartialRelation> mustTypes = new HashSet<>();

		for (int i = 0; i < array1.length; i++) {
			if(array1[i]) mustTypes.add(this.sortedTypes.get(i));
		}

		boolean[] array2 = new boolean[this.sortedTypes.size()];
		for (int i = 0; i < array2.length; i++) {
			array2[i] = stream.readBoolean();
		}

		Set<PartialRelation> mayConcreteTypes = new HashSet<>();

		for (int i = 0; i < array2.length; i++) {
			if(array2[i]) mayConcreteTypes.add(this.sortedTypes.get(i));
		}

		return new InferredType(mustTypes,mayConcreteTypes,candidateType);
	}
}
