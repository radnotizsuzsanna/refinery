package tools.refinery.store.model;

import tools.refinery.store.map.Version;
import tools.refinery.store.map.internal.delta.MapDelta;
import tools.refinery.store.map.internal.delta.MapTransaction;
import tools.refinery.store.tuple.Tuple;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class Serializer {
	HashMap<Class<?>, SerializerStrategy<?>> serializerStrategyMap;
	HashMap<Long, Version> serializedVersions;
	HashMap<Version, Long> serializedIDs;
	HashMap<Long, Version> deSerializedVersions;
	Long lastVersion;

	public <T> void addStrategy(Class<T> valueType, SerializerStrategy<T> strategy){
		serializerStrategyMap.put(valueType, strategy);
	}

	public Serializer(){
		this.serializerStrategyMap = new HashMap<>();
		this.serializedVersions = new HashMap<>();
		this.serializedIDs = new HashMap<>();
		this.deSerializedVersions = new HashMap<>();
		this.lastVersion = 0L;
	}

	public void write(List<Version> versions, File file) throws IOException {
		FileOutputStream fileFileStream= new FileOutputStream(file, true);
		DataOutputStream fileDataStream = new DataOutputStream(fileFileStream);

		if (versions.get(0) instanceof MapTransaction<?, ?>) {
			Class<?> valueTypeClass =((MapTransaction<?, ?>) versions.get(0)).deltas()[0].getNewValue().getClass();
			SerializerStrategy<?> serializerStrategy = serializerStrategyMap.get(valueTypeClass);
			String valueTypeString = valueTypeClass.toString();
			valueTypeString = valueTypeString.replace("class ", "");
			byte[] valueTypeByte = valueTypeString.getBytes(StandardCharsets.UTF_8);
			fileDataStream.writeInt(valueTypeByte.length);
			fileDataStream.write(valueTypeByte);

			if (((MapTransaction<?, ?>) versions.get(0)).deltas()[0].getKey() instanceof Tuple){
				int arity = ((Tuple) ((MapTransaction<?, ?>) versions.get(0)).deltas()[0].getKey()).getSize();
				fileDataStream.writeInt(arity);
				for (Version value : versions) {
					MapTransaction<?, ?> version = (MapTransaction<?, ?>) value;

					while (version != null) {
						if (!serializedVersions.containsValue(version)) {
							//Writing out ID
							serializedVersions.put(lastVersion, version);
							serializedIDs.put(version, lastVersion);
							fileDataStream.writeLong(lastVersion);

							//Writing out parent ID
							Version parent = version.parent();
							if (parent == null) {
								fileDataStream.writeLong(-1);
							} else if (!serializedIDs.containsKey(version.parent())) {
								fileDataStream.writeLong(lastVersion + 1);
							} else {
								Long parentID = serializedIDs.get(parent);
								fileDataStream.writeLong(parentID);
							}

							//Writing out depth
							fileDataStream.writeInt(version.depth());

							//Writing out number of deltas
							MapDelta<?, ?>[] deltas = version.deltas();
							fileDataStream.writeInt(deltas.length);
							for (MapDelta delta : deltas) {
								Tuple tuple = (Tuple) delta.key();
								for (int k = 0; k < arity; k++) {
									fileDataStream.writeInt(tuple.get(k));
								}
								writeValue(serializerStrategy, fileDataStream, delta);
							}
						}
						version = version.parent();
						lastVersion++;
					}
				}
			}else {
				throw new UnsupportedOperationException(
						"Only Tuple keys are supported during serialization.");
			}

		}else {
			throw new UnsupportedOperationException(
					"Only MapTransaction versions are supported during serialization.");
		}
	}
	private <T> void writeValue(SerializerStrategy<T> strategy, DataOutputStream fileDataStream,
								MapDelta<Tuple, T> delta) throws IOException {
		if (delta.oldValue() == null){
			fileDataStream.writeBoolean(true);
		}
		else {
			fileDataStream.writeBoolean(false);
			strategy.writeValue(fileDataStream, delta.getOldValue());
		}
		if (delta.newValue() == null){
			fileDataStream.writeBoolean(true);
		}
		else {
			fileDataStream.writeBoolean(false);
			strategy.writeValue(fileDataStream, delta.getNewValue());
		}
	}
	public ArrayList<Version> read(File file) throws IOException, ClassNotFoundException {

		FileInputStream fileIn = new FileInputStream(file);
		DataInputStream fileDataInStream = new DataInputStream(fileIn);

		int length = fileDataInStream.readInt();
		byte[] valueTypeByte = new byte[length];
		fileDataInStream.readFully(valueTypeByte);
		String valueTypeString = new String(valueTypeByte, StandardCharsets.UTF_8);
		Class<?> valueTypeClass = Class.forName(valueTypeString);
		SerializerStrategy<?> serializerStrategy = serializerStrategyMap.get(valueTypeClass);

		int arity = fileDataInStream.readInt();

		HashMap<Long, Version> deSerializedVersionsTemp = new HashMap<>();

		ArrayList<Long> rootNodes = new ArrayList<>();

		ArrayList<Edge> edges = new ArrayList<>();
		while (fileDataInStream.available() != 0) {
			Long id = fileDataInStream.readLong();
			long parentID = fileDataInStream.readLong();
			int depth = fileDataInStream.readInt();
			int numberOfDeltas = fileDataInStream.readInt();
			var deltas = new MapDelta[numberOfDeltas];
			for (int i = 0; i < numberOfDeltas; i++) {
				int[] tupleArray = new int[arity];
				for (int j = 0; j < arity; j++) {
					tupleArray[j] = fileDataInStream.readInt();
				}
				Tuple tuple = Tuple.of(tupleArray);
				deltas[i] = readValue(serializerStrategy, fileDataInStream, tuple);
			}

			//TODO
			Version v = new MapTransaction<>(deltas,null, depth);
			deSerializedVersionsTemp.put(id,v);
			if(parentID == -1){
				rootNodes.add(id);
			}else{
				edges.add(new Edge(id, parentID));
			}

		}


		ArrayList<Long> leafNodes = new ArrayList<>();
		TreeSet<Long> parents = new TreeSet<>();

		for (Edge value : edges) {
			parents.add(value.parentID());
		}

		for(long i = 0; i < deSerializedVersionsTemp.size(); i++){
			if(!parents.contains(i)) leafNodes.add(i);
		}

		for (Long rootID : rootNodes) {
			Version rootVersion = deSerializedVersionsTemp.get(rootID);
			deSerializedVersions.put(rootID, rootVersion);
		}

		for (Edge edge : edges) {
			Version halfVersion = deSerializedVersionsTemp.get(edge.id());
			MapTransaction<?, ?> parent = (MapTransaction<?, ?>) deSerializedVersions.get(edge.parentID());
			Version completeVersion = new MapTransaction<>(((MapTransaction) halfVersion).deltas(), parent,
					((MapTransaction<?, ?>) halfVersion).depth());
			deSerializedVersions.put(edge.id(), completeVersion);
		}
		ArrayList<Version> versions = new ArrayList<>();
		for (Long leaf : leafNodes) {
			versions.add(deSerializedVersions.get(leaf));
		}
		return versions;
	}


	private <T> MapDelta<Tuple, T> readValue(SerializerStrategy<T> strategy, DataInputStream fileDataInStream,
										 Tuple tuple) throws IOException {
		T oldValue = null;
		boolean nullValue = fileDataInStream.readBoolean();
		if(!nullValue){
			oldValue = strategy.readValue(fileDataInStream);
		}

		T newValue = null;
		nullValue = fileDataInStream.readBoolean();

		if(!nullValue){
			newValue = strategy.readValue(fileDataInStream);
		}

		return new MapDelta<>(tuple, oldValue, newValue);
	}
}
