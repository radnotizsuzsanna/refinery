package tools.refinery.store.map;

import java.util.ArrayList;
import java.util.List;

public class VersionedMapStatistics {
	ArrayList<Integer> numberOfNodesAtLevel = new ArrayList<>();
	ArrayList<Integer> numberOfEntriesAtLevel = new ArrayList<>();
	ArrayList<Integer> numberOfImmutableNodeChildAtLevel = new ArrayList<>();
	ArrayList<Integer> numberOfMutableNodeChildAtLevel = new ArrayList<>();
	ArrayList<Integer> numberOfEmptySpacesAtLevel = new ArrayList<>();
	ArrayList<Integer> numberOfAllocatedAndUnusedSpaceAtLevel = new ArrayList<>();

	private void increment(ArrayList<Integer> list, int level, int value) {
		while(list.size()<=level) {
			list.add(0);
		}
		list.set(level, list.get(level) + value);
	}

	public List<Integer> getNumberOfNodesAtLevel() {
		return numberOfNodesAtLevel;
	}

	public void addNodeAtLevel(int level) {
		increment(numberOfNodesAtLevel, level, 1);
	}

	public List<Integer> getNumberOfEntriesAtLevel() {
		return numberOfEntriesAtLevel;
	}

	public void addEntryAtLevel(int level, int entries) {
		increment(numberOfEntriesAtLevel, level, entries);
	}

	public List<Integer> getNumberOfImmutableNodeChildAtLevel() {
		return numberOfImmutableNodeChildAtLevel;
	}

	public void addNumberOfImmutableNodeChildAtLevel(int level, int childs) {
		increment(numberOfImmutableNodeChildAtLevel, level, childs);
	}

	public List<Integer> getNumberOfMutableNodeChildAtLevel() {
		return numberOfMutableNodeChildAtLevel;
	}

	public void addNumberOfMutableNodeChildAtLevel(int level, int child) {
		increment(numberOfMutableNodeChildAtLevel, level, child);
	}

	public List<Integer> getNumberOfEmptySpacesAtLevel() {
		return numberOfEmptySpacesAtLevel;
	}

	public void addNumberOfEmptySpacesAtLevel(int level, int child) {
		increment(numberOfEmptySpacesAtLevel, level, child);
	}

	public ArrayList<Integer> getNumberOfAllocatedAndUnusedSpace() {
		return numberOfAllocatedAndUnusedSpaceAtLevel;
	}

	public void addNumberOfUnusedSpaceAtLevel(int level, int child) {
		increment(numberOfAllocatedAndUnusedSpaceAtLevel, level, child);
	}
	
	public static String delim = "\t";
	
	public String print() {
		String result = concatLine("Level","#Node","#Entry","#IChild","#MChild","#Empty","#Unused");
		for(int i= 0; i<numberOfNodesAtLevel.size(); i++) {
			result += concatLine(Integer.toString(i+1),
				numberOfNodesAtLevel.get(i).toString(),
				numberOfEntriesAtLevel.get(i).toString(),
				numberOfImmutableNodeChildAtLevel.get(i).toString(),
				numberOfMutableNodeChildAtLevel.get(i).toString(),
				numberOfEmptySpacesAtLevel.get(i).toString(),
				numberOfAllocatedAndUnusedSpaceAtLevel.get(i).toString());
		}
		return result;
		
	}
	
	private String concatLine(String...strings) {
		return String.join(delim, strings)+"\n";
	}
}
