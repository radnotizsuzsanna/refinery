package tools.refinery.store.map;

import static tools.refinery.store.util.StatisticsUtil.addLine;
import static tools.refinery.store.util.StatisticsUtil.sum;

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
		while (list.size() <= level) {
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

	public List<Integer> getNumberOfAllocatedAndUnusedSpace() {
		return numberOfAllocatedAndUnusedSpaceAtLevel;
	}

	public void addNumberOfUnusedSpaceAtLevel(int level, int child) {
		increment(numberOfAllocatedAndUnusedSpaceAtLevel, level, child);
	}

	public String print() {
		return format(numberOfNodesAtLevel, numberOfEntriesAtLevel, numberOfImmutableNodeChildAtLevel,
				numberOfMutableNodeChildAtLevel, numberOfEmptySpacesAtLevel, numberOfAllocatedAndUnusedSpaceAtLevel);
	}

	private static String format(ArrayList<Integer> numberOfNodesAtLevel, ArrayList<Integer> numberOfEntriesAtLevel,
			ArrayList<Integer> numberOfImmutableNodeChildAtLevel, ArrayList<Integer> numberOfMutableNodeChildAtLevel,
			ArrayList<Integer> numberOfEmptySpacesAtLevel, ArrayList<Integer> numberOfAllocatedAndUnusedSpaceAtLevel) {
		StringBuilder result = new StringBuilder();
		addLine(result, "Level", "#Node", "#Entry", "#IChild", "#MChild", "#Empty", "#Unused");
		for (int i = 0; i < numberOfNodesAtLevel.size(); i++) {
			addLine(result, Integer.toString(i + 1), numberOfNodesAtLevel.get(i).toString(),
					numberOfEntriesAtLevel.get(i).toString(), numberOfImmutableNodeChildAtLevel.get(i).toString(),
					numberOfMutableNodeChildAtLevel.get(i).toString(), numberOfEmptySpacesAtLevel.get(i).toString(),
					numberOfAllocatedAndUnusedSpaceAtLevel.get(i).toString());
		}
		addLine(result, "sum", Integer.toString(sum(numberOfNodesAtLevel)),
				Integer.toString(sum(numberOfEntriesAtLevel)), Integer.toString(sum(numberOfImmutableNodeChildAtLevel)),
				Integer.toString(sum(numberOfMutableNodeChildAtLevel)),
				Integer.toString(sum(numberOfEmptySpacesAtLevel)),
				Integer.toString(sum(numberOfAllocatedAndUnusedSpaceAtLevel)));
		return result.toString();
	}

	public static String printSum(List<VersionedMapStatistics> statistics) {
		ArrayList<Integer> numberOfNodesAtLevel = new ArrayList<>();
		ArrayList<Integer> numberOfEntriesAtLevel = new ArrayList<>();
		ArrayList<Integer> numberOfImmutableNodeChildAtLevel = new ArrayList<>();
		ArrayList<Integer> numberOfMutableNodeChildAtLevel = new ArrayList<>();
		ArrayList<Integer> numberOfEmptySpacesAtLevel = new ArrayList<>();
		ArrayList<Integer> numberOfAllocatedAndUnusedSpaceAtLevel = new ArrayList<>();

		for (VersionedMapStatistics versionedMapStatistics : statistics) {
			add(numberOfNodesAtLevel, versionedMapStatistics.numberOfNodesAtLevel);
			add(numberOfEntriesAtLevel, versionedMapStatistics.numberOfEntriesAtLevel);
			add(numberOfImmutableNodeChildAtLevel, versionedMapStatistics.numberOfImmutableNodeChildAtLevel);
			add(numberOfMutableNodeChildAtLevel, versionedMapStatistics.numberOfMutableNodeChildAtLevel);
			add(numberOfEmptySpacesAtLevel, versionedMapStatistics.numberOfEmptySpacesAtLevel);
			add(numberOfAllocatedAndUnusedSpaceAtLevel, versionedMapStatistics.numberOfAllocatedAndUnusedSpaceAtLevel);
		}
		return format(numberOfNodesAtLevel, numberOfEntriesAtLevel, numberOfImmutableNodeChildAtLevel,
				numberOfMutableNodeChildAtLevel, numberOfEmptySpacesAtLevel, numberOfAllocatedAndUnusedSpaceAtLevel);
	}

	private static void add(ArrayList<Integer> to, ArrayList<Integer> from) {
		int commonSize = Math.max(to.size(), from.size());
		while (to.size() < commonSize) {
			to.add(0);
		}
		for (int i = 0; i < from.size(); i++) {
			to.set(i, to.get(i) + from.get(i));
		}
	}
}
