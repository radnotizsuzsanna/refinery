package tools.refinery.store.model;

import tools.refinery.store.map.Version;
import tools.refinery.store.model.internal.ModelVersion;
import tools.refinery.store.representation.Symbol;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ModelSerializer {

	HashMap<Class<?>, SerializerStrategy<?>> serializerStrategyMap = new HashMap<>();

	public void addSerializeStrategy(Class type,  SerializerStrategy strategy){
		serializerStrategyMap.put(type, strategy);
	}
	public void write(List<ModelVersion> modelVersions, ModelStore modelStore, File dataFile,
					  List<double[]> doubleArrayList, long[] longArray) throws IOException {
		FileOutputStream fileFileStream= new FileOutputStream(dataFile, true);
		DataOutputStream fileDataStream = new DataOutputStream(fileFileStream);

		var symbols = modelStore.getSymbols();
		ArrayList<Version>[] versionListArray = new ArrayList[symbols.size()];
		for (int i = 0; i < symbols.size(); i++) {
			versionListArray[i] = new ArrayList<>();
		}

		//hány double tömb lesz kiírva
		fileDataStream.writeInt(modelVersions.size());
		fileDataStream.writeInt(doubleArrayList.get(0).length);
		for(int i = 0; i < modelVersions.size(); i++){
			ModelVersion modelVersion = modelVersions.get(i);
			fileDataStream.writeLong(longArray[i]);
			//ide írom hogy mennyire jó a modell (mert modellverziónként van, nem symbolonként)
			double[] doubleArray = doubleArrayList.get(i);
			for(int j = 0; j < doubleArray.length; j++){
				fileDataStream.writeDouble(doubleArray[j]);
			}
			for(int j = 0; j < symbols.size(); j++){
				Version version = ModelVersion.getInternalVersion(modelVersion,j);
				versionListArray[j].add(version);
			}
		}

		var symbolList = symbols.stream().toList();
		for(int i = 0; i < symbolList.size(); i++){
			Symbol symbol = (Symbol) symbolList.get(i);
			var valueType = symbol.valueType();
			SerializerStrategy serializerStrategy = serializerStrategyMap.get(valueType);
			VersionListSerializer serializer = new VersionListSerializer();
			serializer.setStrategy(serializerStrategy);
			ArrayList<Version> versionList = versionListArray[i];
			serializer.write(versionList, fileDataStream);
		}
	}

	public List<ModelVersion> read(ModelStore modelStore, File dataFile) throws IOException,
			ClassNotFoundException {

		FileInputStream fileIn = new FileInputStream(dataFile);
		DataInputStream fileDataInStream = new DataInputStream(fileIn);

		List<ModelVersion> modelVersions = new ArrayList<>();
		var symbols = modelStore.getSymbols();
		ArrayList<Version>[] versionListArray = new ArrayList[symbols.size()];

		int numberOfModelVersion = fileDataInStream.readInt();
		int doubleArraysLength = fileDataInStream.readInt();

		List<double[]> doubleArrayList = new ArrayList<>();
		long[] longArray = new long[numberOfModelVersion];
		for(int i = 0; i < numberOfModelVersion; i++){
			longArray[i] = fileDataInStream.readLong();
			double[] doubleArray = new double[doubleArraysLength];
			for(int j = 0; j < doubleArraysLength; j++){
				doubleArray[j] = fileDataInStream.readDouble();
			}
			doubleArrayList.add(doubleArray);
		}

		var symbolList = symbols.stream().toList();
		for(int i = 0; i < symbolList.size(); i++){
			Symbol symbol = (Symbol) symbolList.get(i);
			var valueType = symbol.valueType();
			SerializerStrategy serializerStrategy = serializerStrategyMap.get(valueType);
			VersionListSerializer serializer = new VersionListSerializer();
			serializer.setStrategy(serializerStrategy);

			ArrayList<Version> versions = serializer.read(fileDataInStream);
			versionListArray[i] = versions;
		}

		Version[] versionArray;
		for(int i = 0; i < versionListArray[0].size(); i++){
			versionArray = new Version[symbolList.size()];
			for(int j = 0; j < symbolList.size(); j++){
				versionArray[j] = versionListArray[j].get(i);
			}
			ModelVersion modelVersion = new ModelVersion(versionArray);
			modelVersions.add(modelVersion);
		}
		return modelVersions;
	}
}
