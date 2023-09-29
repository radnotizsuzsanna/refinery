package tools.refinery.store.model;

import tools.refinery.store.map.Version;
import tools.refinery.store.map.internal.delta.MapDelta;
import tools.refinery.store.map.internal.delta.MapTransaction;
import tools.refinery.store.tuple.Tuple;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class Serializer {
	HashMap<Class<?>, SerializerStrategy<?>> serializerStrategyMap;

	public <T> void addStrategy(Class<T> valueType, SerializerStrategy<T> strategy){
		serializerStrategyMap.put(valueType, strategy);

	}

	public Serializer(){
		this.serializerStrategyMap = new HashMap<>();
	}

	public void write(ArrayList<Version> versions, ArrayList<File> files) throws IOException {
		for (int i = 0; i < versions.size(); i++) {
			FileOutputStream fileFileStream= new FileOutputStream(files.get(i), true);
			DataOutputStream fileDataStream = new DataOutputStream(fileFileStream);

			Class valueTypeClass =  ((MapTransaction) versions.get(i)).deltas()[0].getNewValue().getClass();
			String valueTypeString = valueTypeClass.toString();
			valueTypeString = valueTypeString.replace("class ", "");
			byte[] valueTypeByte = valueTypeString.getBytes(StandardCharsets.UTF_8);
			fileDataStream.writeInt(valueTypeByte.length);
			fileDataStream.write(valueTypeByte);

			int arity =((Tuple) ((MapTransaction) versions.get(0)).deltas()[0].getKey()).getSize();
			fileDataStream.writeInt(arity);

			SerializerStrategy<?> serializerStrategy = serializerStrategyMap.get(valueTypeClass);

			MapTransaction version = (MapTransaction) versions.get(i);
			// depth?
			//fileDataStream.writeInt(i);
			while(version != null){
				MapDelta[] deltas = version.deltas();
				fileDataStream.writeInt(deltas.length);
				for(int j = 0; j < deltas.length; j ++){
					MapDelta delta = deltas[j];
					Tuple tuple = (Tuple) delta.key();
					for(int k = 0; k < arity; k++) {
						fileDataStream.writeInt(tuple.get(k));
					}
					writeValue(serializerStrategy, fileDataStream, delta);
				}
				version = version.parent();
			}
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
	public ArrayList<Version> read(ArrayList<File> files) throws IOException, ClassNotFoundException {
		ArrayList<Version> versions = new ArrayList<>();
		for(int n = 0; n < files.size(); n++){
			FileInputStream fileIn = new FileInputStream(files.get(n));
			DataInputStream fileDataInStream = new DataInputStream(fileIn);

			int length = fileDataInStream.readInt();
			byte[] valueTypeByte = new byte[length];
			fileDataInStream.readFully(valueTypeByte);
			String valueTypeString = new String(valueTypeByte, StandardCharsets.UTF_8);
			Class<?> valueTypeClass = Class.forName(valueTypeString);
			SerializerStrategy serializerStrategy = serializerStrategyMap.get(valueTypeClass);

			int arity = fileDataInStream.readInt();

			ArrayList<MapDelta[]> deltaArrayList = new ArrayList<>();

			int depth = 0;
			while(fileDataInStream.available()!=0){
				//Todo ez kell egyáltalán?
			//	int id = fileDataInStream.readInt();
				int numberOfDeltas = fileDataInStream.readInt();
				var deltas = new MapDelta[numberOfDeltas];
				for(int i = 0; i < numberOfDeltas; i++){
					int[] tupleArray = new int[arity];
					for(int j = 0; j < arity; j++){
						tupleArray[j] = fileDataInStream.readInt();
					}
					Tuple tuple = Tuple.of(tupleArray);
					deltas[i] =  readValue(serializerStrategy,fileDataInStream, tuple);
				}
				deltaArrayList.add(deltas);
				depth++;
			}


			MapTransaction version = new MapTransaction(deltaArrayList.get(deltaArrayList.size()-1), null, 0);
			MapTransaction parentVersion;
			for(int i = 1; i < deltaArrayList.size(); i++){
				parentVersion = version;
				version = new MapTransaction( deltaArrayList.get(deltaArrayList.size()-i-1), parentVersion, i);
			}
			versions.add(version);
		}
		return versions;
	}

	private <T> MapDelta<Tuple, T> readValue(SerializerStrategy<T> strategy, DataInputStream fileDataInStream,
										 Tuple tuple) throws IOException {
		T oldValue = null;
		Boolean nullValue = fileDataInStream.readBoolean();
		if(!nullValue){
			oldValue = strategy.readValue(fileDataInStream);
		}

		//logger.info("\t\tReading oldValue: " + oldValue);

		T newValue = null;
		nullValue = fileDataInStream.readBoolean();

		if(!nullValue){
			newValue = strategy.readValue(fileDataInStream);
		}

		//logger.info("\t\tReading newValue: " + newValue);
		return new MapDelta<>(tuple, oldValue, newValue);
	}
}
