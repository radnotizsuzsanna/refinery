package tools.refinery.store.model;

import tools.refinery.store.map.Version;
import tools.refinery.store.map.internal.delta.MapDelta;
import tools.refinery.store.map.internal.delta.MapTransaction;
import tools.refinery.store.tuple.Tuple;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Serializes and deserializes a list of versions
 */
public class  VersionListSerializer{
	//The serializer strategy for writing and reading values
	SerializerStrategy<?> serializerStrategy;

	//TODO erre lehet valami szebbet?
	//Maps of serialized versions and their id-s (ids are given by the serializer and used only locally)
	HashMap<Long, Version> serializedVersions;
	HashMap<Version, Long> serializedIDs;

	//Map of deserialized versions and their id-s (ids are given by the serializer and used only locally)
	HashMap<Long, Version> deSerializedVersions;

	//The next version ID to hand out
	Long nextVersionID;

	/**
	 * Set the serializer strategy for the serializer
	 * @param strategy The serializer strategy to use
	 * @param <T> The value type
	 */
	public <T> void setStrategy(SerializerStrategy<T> strategy){
		serializerStrategy = strategy;
	}

	/**
	 * Constructor, initializes the maps and variables
	 */
	public VersionListSerializer(){
		this.serializedVersions = new HashMap<>();
		this.serializedIDs = new HashMap<>();
		this.deSerializedVersions = new HashMap<>();
		this.nextVersionID = 0L;
	}

	/**
	 * Serializes the list of versions
	 * @param versions The list of versions to serialize
	 * @param fileDataStream The data stream to use for serializing
	 * @throws IOException Exception that can occur during writing data into the file
	 */
	public void write(List<Version> versions, DataOutputStream fileDataStream) throws IOException {
		if (versions.get(0) instanceof MapTransaction<?, ?>) {
			if (((MapTransaction<?, ?>) versions.get(0)).deltas()[0].getKey() instanceof Tuple){
				//Writing out the arity of the keys in the versions
				int arity = ((Tuple) ((MapTransaction<?, ?>) versions.get(0)).deltas()[0].getKey()).getSize();
				fileDataStream.writeInt(arity);

				//Iterating through the version list and writing out the data of the versions
				for (Version value : versions) {
					MapTransaction<?, ?> version = (MapTransaction<?, ?>) value;

					//Iterating through the version ancestors
					while (version != null) {
						//Serializing the version if it is not serialized yet
						if (!serializedVersions.containsValue(version)) {
							//Writing out the information, that the next data will be a transaction
							fileDataStream.writeBoolean(true);

							//Adding the version to the serialized version map and writing out the ID of the version
							serializedVersions.put(nextVersionID, version);
							serializedIDs.put(version, nextVersionID);
							fileDataStream.writeLong(nextVersionID);

							//Writing out parent ID
							Version parent = version.parent();
							if (parent == null) {
								//Writes out -1 as parent id if the version doesn't have a parent
								fileDataStream.writeLong(-1);
							} else if (!serializedIDs.containsKey(version.parent())) {
								//If the parent of the version is not serialized yet, then the ID of the parent will be
								// the next id given by the serializer (actual id incremented by 1)
								fileDataStream.writeLong(nextVersionID + 1);
							} else {
								//If the parent of the version is serialized,then it already has an id
								Long parentID = serializedIDs.get(parent);
								fileDataStream.writeLong(parentID);
							}

							//Writing out depth
							fileDataStream.writeInt(version.depth());

							//Writing out number of deltas
							MapDelta<?, ?>[] deltas = version.deltas();
							fileDataStream.writeInt(deltas.length);

							//Iterating through the deltas of the version and writing them out
							for (MapDelta delta : deltas) {
								//Writing out the key
								Tuple tuple = (Tuple) delta.key();
								for (int k = 0; k < arity; k++) {
									fileDataStream.writeInt(tuple.get(k));
								}
								//writing out the old and new value with the appropriate strategy
								writeValue(serializerStrategy, fileDataStream, delta);
							}
						}
						//Continuing with the parent of the version
						version = version.parent();

						//Incrementing the version number to hand out for the next versions
						nextVersionID++;
					}
				}
				//Writing out the information, that the next data will not be a transaction
				fileDataStream.writeBoolean(false);

				//Writing out the IDs of the leaf nodes (versions in the given version list)
				fileDataStream.writeInt(versions.size());
				for (Version leaf : versions) {
					fileDataStream.writeLong(serializedIDs.get(leaf));
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

	/**
	 * Write out the old and new value of the delta and handle null values
	 * @param strategy The serializer strategy fot the value type.
	 * @param fileDataStream The file stream for writing out the values.
	 * @param delta The delta to serialize.
	 * @param <T> The value type of the delta
	 * @throws IOException Exception that can occur during writing data into the file
	 */
	private <T> void writeValue(SerializerStrategy<T> strategy, DataOutputStream fileDataStream,
								MapDelta<Tuple, T> delta) throws IOException {
		//If the old value is null then writing out that information instead of the value
		if (delta.oldValue() == null){
			fileDataStream.writeBoolean(true);
		}
		//If the old value not a null then writing out that information and the value itself
		else {
			fileDataStream.writeBoolean(false);
			strategy.writeValue(fileDataStream, delta.getOldValue());
		}

		//If the old value is null then writing out that information instead of the value
		if (delta.newValue() == null){
			fileDataStream.writeBoolean(true);
		}
		//If the new value not a null then writing out that information and the value itself
		else {
			fileDataStream.writeBoolean(false);
			strategy.writeValue(fileDataStream, delta.getNewValue());
		}
	}

	/**
	 * Deserializes the ist of versions
	 * @param fileDataInStream The file stream to read the data from
	 * @return The list of deserialized versions
	 * @throws IOException Exception that can occur during reading data from the file
	 */
	public ArrayList<Version> read(DataInputStream fileDataInStream) throws IOException {
		//Reading the arity of the keys in the versions
		int arity = fileDataInStream.readInt();

		//Creating temporary maps and lists for reading the versions and building the version graph
		HashMap<Long, Version> deSerializedVersionsTemp = new HashMap<>();
		ArrayList<Long> rootNodes = new ArrayList<>();
		ArrayList<Edge> edges = new ArrayList<>();

		//Reading data from the file while the nex data is a transaction
		while (fileDataInStream.readBoolean()) {
			//Reading id of version
			Long id = fileDataInStream.readLong();

			//Reading parent id of version
			long parentID = fileDataInStream.readLong();

			//Reading depth of version
			int depth = fileDataInStream.readInt();

			//Reading the number of deltas in the version and creating an array to store them
			int numberOfDeltas = fileDataInStream.readInt();
			MapDelta[] deltas = new MapDelta[numberOfDeltas];

			//Reading the deltas of the version
			for (int i = 0; i < numberOfDeltas; i++) {
				//Reading and creating the key of the delta
				int[] tupleArray = new int[arity];
				for (int j = 0; j < arity; j++) {
					tupleArray[j] = fileDataInStream.readInt();
				}
				Tuple tuple = Tuple.of(tupleArray);

				//Reading the old and new value of the version with the appropriate strategy and storing in the array
				deltas[i] = readValue(serializerStrategy, fileDataInStream, tuple);
			}

			//Creating the temporary version with the data already collected (parent is missing)
			Version v = new MapTransaction(deltas,null, depth);
			deSerializedVersionsTemp.put(id,v);

			//Adding edges to the edge list
			if(parentID == -1){
				//If the parent id is -1 then the version is a root node version
				rootNodes.add(id);
			}else{
				//Else adding the edge with the version as the start and parent version as the end
				edges.add(new Edge(id, parentID));
			}
		}

		ArrayList<Long> leafNodesArray = new ArrayList<>();
		//Reading the number of leaf nodes
		int numberOfLeafNodes = fileDataInStream.readInt();
		//Reading the ID-s of the leaf nodes from the file
		for(int i = 0; i < numberOfLeafNodes; i++){
			Long id = fileDataInStream.readLong();
			leafNodesArray.add(id);
		}

		//Adding the root node version to the deserialized version map
		for (Long rootID : rootNodes) {
			Version rootVersion = deSerializedVersionsTemp.get(rootID);
			deSerializedVersions.put(rootID, rootVersion);
		}

		//Iterating through the edges and creating the complete versions
		for (Edge edge : edges) {
			//Getting the incomplete deserialized version as the start of the edge
			Version incompleteVersion = deSerializedVersionsTemp.get(edge.id());

			//Creating the parent version from the data of the version on the end of the edge
			MapTransaction<?, ?> parent = (MapTransaction<?, ?>) deSerializedVersions.get(edge.parentID());

			//Creating the complete version with the data of the incomplete version and the parent
			Version completeVersion = new MapTransaction<>(((MapTransaction) incompleteVersion).deltas(), parent,
					((MapTransaction<?, ?>) incompleteVersion).depth());
			deSerializedVersions.put(edge.id(), completeVersion);
		}

		//Creating the list with the leaf node version to return
		ArrayList<Version> versions = new ArrayList<>();
		for (Long leaf : leafNodesArray) {
			versions.add(deSerializedVersions.get(leaf));
		}
		return versions;
	}

	/**
	 * Read the old and new value of the delta and create the delta
	 * @param strategy The serializer strategy fot the value type.
	 * @param fileDataInStream The file stream for reading the values.
	 * @param tuple The key of the delta
	 * @return The deserialized delta
	 * @param <T> The value type of the delta
	 * @throws IOException Exception that can occur during reading data from the file
	 */
	private <T> MapDelta<Tuple, T> readValue(SerializerStrategy<T> strategy, DataInputStream fileDataInStream,
										 Tuple tuple) throws IOException {
		//Reading the information if the old value is null or not
		T oldValue = null;
		boolean nullValue = fileDataInStream.readBoolean();

		//If the old value not null then reading the value
		if(!nullValue){
			oldValue = strategy.readValue(fileDataInStream);
		}

		//Reading the information if the new value is null or not
		T newValue = null;
		nullValue = fileDataInStream.readBoolean();

		//If the new value not null then reading the value
		if(!nullValue){
			newValue = strategy.readValue(fileDataInStream);
		}

		//Creating the delta with the deserialized data and returning it
		return new MapDelta<>(tuple, oldValue, newValue);
	}
}
