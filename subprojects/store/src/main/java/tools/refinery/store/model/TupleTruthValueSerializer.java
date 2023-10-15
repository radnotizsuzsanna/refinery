package tools.refinery.store.model;

import tools.refinery.store.representation.TruthValue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static tools.refinery.store.representation.TruthValue.*;

/**
 * Serializes and deserializes the TruthValue value
 */
public class TupleTruthValueSerializer implements SerializerStrategy<TruthValue> {
	/**
	 * Writes out the value
	 * @param stream The output stream for serializing the value
	 * @param value The value to serialize
	 * @throws IOException Exception can occur when writing out the data
	 */
	@Override
	public void writeValue(DataOutputStream stream, TruthValue value) throws IOException {
		int ordinal;
		//Converting the TruthValues to integers
		switch (value){
			case TRUE -> ordinal = 0;
			case FALSE -> ordinal = 1;
			case UNKNOWN -> ordinal = 2;
			case ERROR -> ordinal = 3;
			default -> throw new IllegalStateException("Unexpected value: " +value);
		}
		stream.writeInt(ordinal);
	}

	/**
	 * Reads the value from the stream
	 * @param stream The stream to read the value from
	 * @return The deserialized value
	 * @throws IOException Exception can occur when reading data from the stream
	 */
	@Override
	public TruthValue readValue(DataInputStream stream) throws IOException {
		//Converting integers to TruthValue
		return switch (stream.readInt()) {
			case 0 -> TRUE;
			case 1 -> FALSE;
			case 2 -> UNKNOWN;
			case 3 -> ERROR;
			default -> throw new IllegalStateException("Unexpected value: " + stream.readInt());
		};
	}
}
