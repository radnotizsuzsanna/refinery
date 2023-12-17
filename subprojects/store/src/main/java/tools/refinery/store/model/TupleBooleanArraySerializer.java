package tools.refinery.store.model;

import tools.refinery.interpreter.matchers.psystem.IValueProvider;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Serializes and deserializes the TruthValue value
 */
public class TupleBooleanArraySerializer implements SerializerStrategy<Boolean[]> {
	/**
	 * Writes out the value
	 * @param stream The output stream for serializing the value
	 * @param value The value to serialize
	 * @throws IOException Exception can occur when writing out the data
	 */
	@Override
	public void writeValue(DataOutputStream stream, Boolean[] value) throws IOException {
		stream.writeInt(value.length);
		for(int i = 0; i < value.length;i++){
			stream.writeBoolean(value[i]);
		}
	}

	/**
	 * Reads the value from the stream
	 * @param stream The stream to read the value from
	 * @return The deserialized value
	 * @throws IOException Exception can occur when reading data from the stream
	 */
	@Override
	public Boolean[] readValue(DataInputStream stream) throws IOException {
		int length = stream.readInt();
		Boolean[] value = new Boolean[length];
		for(int i = 0; i < length; i++){
			value[i] = stream.readBoolean();
		}
		return value;
	}
}
