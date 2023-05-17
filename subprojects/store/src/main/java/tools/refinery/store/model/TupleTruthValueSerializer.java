package tools.refinery.store.model;

import tools.refinery.store.representation.TruthValue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static tools.refinery.store.representation.TruthValue.*;

public class TupleTruthValueSerializer implements SerializerStrategy<TruthValue> {
	@Override
	public void writeValue(DataOutputStream stream, TruthValue value) throws IOException {
		//TODO itt is switch legyen
		int ordinal;
		switch (value){
		case TRUE -> ordinal = 0;
		case FALSE -> ordinal = 1;
		case UNKNOWN -> ordinal = 2;
		case ERROR -> ordinal = 3;
		default -> throw new IllegalStateException("Unexpected value: " +value);
		}
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
