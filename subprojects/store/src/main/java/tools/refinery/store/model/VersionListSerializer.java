package tools.refinery.store.model;

import tools.refinery.store.map.Version;
import tools.refinery.store.map.internal.delta.MapDelta;
import tools.refinery.store.map.internal.delta.MapTransaction;
import tools.refinery.store.tuple.Tuple;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VersionListSerializer {
	HashMap<Class<?>, SerializerStrategy<?>> serializerStrategyMap;
	SerializerStrategy serializerStrategy;
	HashMap<Long, Version> serializedVersions;
	HashMap<Version, Long> serializedIDs;
	HashMap<Long, Version> deSerializedVersions;
	Long lastVersion;

	public <T> void setStrategy(SerializerStrategy<T> strategy){
		serializerStrategy = strategy;
	}

	public VersionListSerializer(){
		this.serializerStrategyMap = new HashMap<>();
		this.serializedVersions = new HashMap<>();
		this.serializedIDs = new HashMap<>();
		this.deSerializedVersions = new HashMap<>();
		this.lastVersion = 0L;
	}

	public void write(List<Version> versions, DataOutputStream fileDataStream) throws IOException {
		if (versions.get(0) instanceof MapTransaction<?, ?>) {
			if (((MapTransaction<?, ?>) versions.get(0)).deltas()[0].getKey() instanceof Tuple){
				int arity = ((Tuple) ((MapTransaction<?, ?>) versions.get(0)).deltas()[0].getKey()).getSize();
				fileDataStream.writeInt(arity);

				for (Version value : versions) {
					MapTransaction<?, ?> version = (MapTransaction<?, ?>) value;
					while (version != null) {
						//next is a transaction
						if (!serializedVersions.containsValue(version)) {
							fileDataStream.writeBoolean(true);
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
				fileDataStream.writeBoolean(false);
				fileDataStream.writeInt(versions.size());
				for (Version version : versions) {
					fileDataStream.writeLong(serializedIDs.get(version));
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
	public ArrayList<Version> read(DataInputStream fileDataInStream) throws IOException, ClassNotFoundException {
		int arity = fileDataInStream.readInt();

		HashMap<Long, Version> deSerializedVersionsTemp = new HashMap<>();
		ArrayList<Long> rootNodes = new ArrayList<>();
		ArrayList<Edge> edges = new ArrayList<>();

		while (fileDataInStream.readBoolean()) {
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

			Version v = new MapTransaction<>(deltas,null, depth);
			deSerializedVersionsTemp.put(id,v);
			if(parentID == -1){
				rootNodes.add(id);
			}else{
				edges.add(new Edge(id, parentID));
			}
		}
		ArrayList<Long> leafNodesArray = new ArrayList<>();

		int numberOfLeafNodes = fileDataInStream.readInt();

		for(int i = 0; i < numberOfLeafNodes; i++){
			Long id = fileDataInStream.readLong();
			leafNodesArray.add(id);
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
		for (Long leaf : leafNodesArray) {
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
