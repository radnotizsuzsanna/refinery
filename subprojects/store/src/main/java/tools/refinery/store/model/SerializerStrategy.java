package tools.refinery.store.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface SerializerStrategy<T> {
	void writeValue(DataOutputStream stream, T value) throws IOException;
	T readValue(DataInputStream stream) throws IOException;
}
