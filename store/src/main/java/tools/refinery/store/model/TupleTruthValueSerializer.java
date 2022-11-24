package tools.refinery.store.model;

import tools.refinery.store.model.SerializerStrategy;
import tools.refinery.store.model.representation.TruthValue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static tools.refinery.store.model.representation.TruthValue.*;

public class TupleTruthValueSerializer implements SerializerStrategy<TruthValue> {
	@Override
	public void writeValue(DataOutputStream stream, TruthValue value) throws IOException {
		//TODO itt is switch legyen
		int ordinal = value.ordinal();
		stream.writeInt(ordinal);
	}

	@Override
	public TruthValue readValue(DataInputStream stream) throws IOException {
		return switch (stream.readInt()) {
			case 0 -> TRUE;
			case 1 -> FALSE;
			case 2 -> UNKNOWN;
			case 3 -> ERROR;
			default -> throw new IllegalStateException("Unexpected value: " + stream.readInt());
		};
	}
}
