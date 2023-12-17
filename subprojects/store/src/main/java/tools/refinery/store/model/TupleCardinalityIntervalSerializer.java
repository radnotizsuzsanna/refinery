package tools.refinery.store.model;

import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static tools.refinery.store.representation.TruthValue.*;

/**
 * Serializes and deserializes the TruthValue value
 */
public class TupleCardinalityIntervalSerializer implements SerializerStrategy<CardinalityInterval> {
	/**
	 * Writes out the value
	 * @param stream The output stream for serializing the value
	 * @param value The value to serialize
	 * @throws IOException Exception can occur when writing out the data
	 */
	@Override
	public void writeValue(DataOutputStream stream, CardinalityInterval value) throws IOException {
		if(value instanceof EmptyCardinalityInterval){
			stream.writeInt(-2);
		}else{
			NonEmptyCardinalityInterval nonEmptyValue = (NonEmptyCardinalityInterval) value;
			int lowerBound = value.lowerBound();
			stream.writeInt(lowerBound);
			UpperCardinality upperBound = nonEmptyValue.upperBound();
			if(upperBound instanceof UnboundedUpperCardinality){
				stream.writeInt(-1);
			}else{
				FiniteUpperCardinality finiteUpperCardinality = (FiniteUpperCardinality) upperBound;
				int upperBoundValue = finiteUpperCardinality.finiteUpperBound();
				stream.writeInt(upperBoundValue);
			}
		}
	}

	/**
	 * Reads the value from the stream
	 * @param stream The stream to read the value from
	 * @return The deserialized value
	 * @throws IOException Exception can occur when reading data from the stream
	 */
	@Override
	public CardinalityInterval readValue(DataInputStream stream) throws IOException {
		int lowerBound = stream.readInt();
		if(lowerBound == -2){
			return CardinalityIntervals.ERROR;
		}else{
			int upperBound = stream.readInt();
			if(upperBound ==-1){
				return CardinalityIntervals.atLeast(lowerBound);
			}else{
				return new NonEmptyCardinalityInterval(lowerBound, new FiniteUpperCardinality(upperBound));
			}
		}
	}
}
