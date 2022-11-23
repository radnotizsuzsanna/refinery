package tools.refinery.store.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TupleIntegerSerializer implements SerializerStrategy<Integer>{
	@Override
	public void writeValue(DataOutputStream stream, Integer value) throws IOException {
		stream.writeInt(value);
	}

	@Override
	public Integer readValue(DataInputStream stream) throws IOException {
		return stream.readInt();
	}
}
