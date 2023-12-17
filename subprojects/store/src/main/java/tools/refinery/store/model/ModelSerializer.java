package tools.refinery.store.model;

import tools.refinery.store.map.Version;
import tools.refinery.store.model.internal.ModelVersion;
import tools.refinery.store.representation.Symbol;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Serializes and deserializes model version and manages serializer strategies for different value types
 */
public class ModelSerializer {

	//Map of serializer strategies for different value types
	HashMap<Class<?>, SerializerStrategy<?>> serializerStrategyMap = new HashMap<>();

	File dataFileC;

	HashMap<String, File> relationFilesC = new HashMap<>();

	/**
	 * Add serializer strategy to the strategy map
	 * @param type Value type
	 * @param strategy The serializer strategy that can serialize the value type
	 */
	public void addSerializeStrategy(Class<?> type,  SerializerStrategy<?> strategy){
		serializerStrategyMap.put(type, strategy);
	}

	public void setDataFile(File f){
		dataFileC = f;
	}

	public void putInRelationFiles(String s, File f){
		relationFilesC.put(s, f);
	}

	/**
	 * Serializes the model versions
	 * @param modelVersions The list of model versions to serialize
	 * @param modelStore The modelStore of the versions
	 * @param dataFile The file where the serializer writes the data
	 * @param doubleArrayList List of double arrays containing additional information about model versions and have
	 *                           to be serialized
	 * @param intArray Hash value of the model version
	 * @throws IOException Exception that can occur during writing data into the file
	 */
	public void write(List<Version> modelVersions, ModelStore modelStore, File dataFile,
					  HashMap<String, File> relationFiles,
					  List<double[]> doubleArrayList, int[] intArray) throws IOException {

		//TODO choose
		//Creating the data stream
		FileOutputStream fileFileStream= new FileOutputStream(dataFileC, true);
		DataOutputStream fileDataStream = new DataOutputStream(fileFileStream);

		//OutputStream out = new ByteArrayOutputStream();
		//DataOutputStream fileDataStream = new DataOutputStream(out);

		//Initializing the arraylist for the versions
		var symbols = modelStore.getSymbols();
		ArrayList<Version>[] versionArrayList = new ArrayList[symbols.size()];
		for (int i = 0; i < symbols.size(); i++) {
			versionArrayList[i] = new ArrayList<>();
		}

		//Writing out the number of versions
		fileDataStream.writeInt(modelVersions.size());

		//Writing out the length of the double arrays
		fileDataStream.writeInt(doubleArrayList.get(0).length);

		//Iterating through the list of model versions
		for(int i = 0; i < modelVersions.size(); i++){
			ModelVersion modelVersion = (ModelVersion) modelVersions.get(i);

			//Writing out the hash of the model version
			fileDataStream.writeInt(intArray[i]);

			//Writing out the double array of the model version
			double[] doubleArray = doubleArrayList.get(i);
			for (double v : doubleArray) {
				fileDataStream.writeDouble(v);
			}

			//Adding the internal versions of the model version to the version arraylist
			for(int j = 0; j < symbols.size(); j++){
				Version version = ModelVersion.getInternalVersion(modelVersion,j);
				versionArrayList[j].add(version);
			}
		}

		//Symbols of the model versions
		var symbolList = symbols.stream().toList();

		//Iterating through the symbols and writing out the model versions
		for(int i = 0; i < symbolList.size(); i++){
			//Creating the serializer strategy for the value type of the symbol
			Symbol<?> symbol = (Symbol<?>) symbolList.get(i);
			Class<?> valueType = symbol.valueType();
			//System.out.println("Valuetype: " + valueType);
			SerializerStrategy<?> serializerStrategy = serializerStrategyMap.get(valueType);

			//Creating and initializing the version list serializer
			VersionListSerializer versionListSerializer = new VersionListSerializer();
			versionListSerializer.setStrategy(serializerStrategy);

			FileOutputStream fileFileStreamRelation = new FileOutputStream(relationFilesC.get(symbol.name()), true);
			DataOutputStream fileDataStreamRelation = new DataOutputStream(fileFileStreamRelation);

			//OutputStream outRelation = new ByteArrayOutputStream();
			//DataOutputStream fileDataStreamRelation = new DataOutputStream(outRelation);

			//Writing out the version list
			ArrayList<Version> versionList = versionArrayList[i];
			versionListSerializer.write(versionList, fileDataStreamRelation);
		}
	}

	/**
	 * Deserializes the list of model versions.
	 * @param modelStore The modelStore of the model versions
	 * @param dataFile The file to read the data from
	 * @return The list of model versions from the file
	 * @throws IOException Exception that can occur during reading data from the file
	 */
	public List<ModelVersion> read(ModelStore modelStore, File dataFile, HashMap<String, File> relationFiles) throws IOException {


		//Creating the data stream
		FileInputStream fileIn = new FileInputStream(dataFileC);
		DataInputStream fileDataInStream = new DataInputStream(fileIn);

		//Initializing the arraylist for the versions
		var symbols = modelStore.getSymbols();
		ArrayList<Version>[] versionArrayList = new ArrayList[symbols.size()];

		//Reading number of serialized model versions
		int numberOfModelVersion = fileDataInStream.readInt();

		//Reading length of serialized double arrays
		int doubleArraysLength = fileDataInStream.readInt();

		//Creating the list of arrays for the additional information about the model version
		List<double[]> doubleArrayList = new ArrayList<>();

		//Creating the array for the hashes of the model versions
		int[] longArray = new int[numberOfModelVersion];

		//Iterating through the serialized model versions
		for(int i = 0; i < numberOfModelVersion; i++){
			//Reading the hash of the model version
			longArray[i] = fileDataInStream.readInt();

			//Reading the double values of the model version
			double[] doubleArray = new double[doubleArraysLength];
			for(int j = 0; j < doubleArraysLength; j++){
				doubleArray[j] = fileDataInStream.readDouble();
			}
			doubleArrayList.add(doubleArray);
		}

		var symbolList = symbols.stream().toList();

		//Iterating through the symbols of the model versions
		for(int i = 0; i < symbolList.size(); i++){
			//Creating the serializer strategy for the value type of the symbol
			Symbol<?> symbol = (Symbol<?>) symbolList.get(i);
			var valueType = symbol.valueType();
			SerializerStrategy<?> serializerStrategy = serializerStrategyMap.get(valueType);

			//Creating and initializing the version list serializer
			VersionListSerializer serializer = new VersionListSerializer();
			serializer.setStrategy(serializerStrategy);

			//if(serializerStrategy == null) System.out.println("Nem tudom beolvasni ezt a tipust: " + valueType);
			//else System.out.println("Be tudom olvasni ezt a tipust: " + valueType);

			//Reading the version list
			FileInputStream fileFileStreamRelation = new FileInputStream(relationFilesC.get(symbol.name()));
			DataInputStream fileDataStreamRelation = new DataInputStream(fileFileStreamRelation);

			ArrayList<Version> versions = serializer.read(fileDataStreamRelation);
			versionArrayList[i] = versions;
		}

		//Creating the list for the model versions to return
		List<ModelVersion> modelVersions = new ArrayList<>();
		Version[] versionArray;

		//Iterating through the arraylist of versions and adding the versions to the model version list
		for(int i = 0; i < versionArrayList[0].size(); i++){
			versionArray = new Version[symbolList.size()];
			for(int j = 0; j < symbolList.size(); j++){
				versionArray[j] = versionArrayList[j].get(i);
			}
			ModelVersion modelVersion = new ModelVersion(versionArray);
			modelVersions.add(modelVersion);
		}
		return modelVersions;
	}
}