package tools.refinery.store.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TupleDoubleSerializer implements SerializerStrategy<Double>{
	@Override
	public void writeValue(DataOutputStream stream, Double value) throws IOException {
		stream.writeDouble(value);
	}

	@Override
	public Double readValue(DataInputStream stream) throws IOException {
		return stream.readDouble();
	}
}
