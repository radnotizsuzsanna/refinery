package tools.refinery.store.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class TupleBooleanSerializer implements SerializerStrategy<Boolean>{
	@Override
	public void writeValue(DataOutputStream stream, Boolean value) throws IOException {
		stream.writeBoolean(value);
	}

	@Override
	public Boolean readValue(DataInputStream stream) throws IOException {
		return stream.readBoolean();
	}


}
