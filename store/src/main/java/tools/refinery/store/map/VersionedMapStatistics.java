package tools.refinery.store.map;

import static tools.refinery.store.util.StatisticsUtil.addLine;
import static tools.refinery.store.util.StatisticsUtil.sum;

import java.util.List;

import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public class VersionedMapStatistics {

	IntArrayList numberOfNodesAtLevel = new IntArrayList();
	IntArrayList numberOfEntriesAtLevel = new IntArrayList();
	IntArrayList numberOfImmutableNodeChildAtLevel = new IntArrayList();
	IntArrayList numberOfMutableNodeChildAtLevel = new IntArrayList();
	IntArrayList numberOfEmptySpacesAtLevel = new IntArrayList();
	IntArrayList numberOfAllocatedAndUnusedSpaceAtLevel = new IntArrayList();

	private void increment(IntArrayList list, int level, int value) {
		while (list.size() <= level) {
			list.add(0);
		}
		list.set(level, list.get(level) + value);
	}

	public IntList getNumberOfNodesAtLevel() {
		return numberOfNodesAtLevel;
	}

	public void addNodeAtLevel(int level) {
		increment(numberOfNodesAtLevel, level, 1);
	}

	public IntList getNumberOfEntriesAtLevel() {
		return numberOfEntriesAtLevel;
	}

	public void addEntryAtLevel(int level, int entries) {
		increment(numberOfEntriesAtLevel, level, entries);
	}

	public IntList getNumberOfImmutableNodeChildAtLevel() {
		return numberOfImmutableNodeChildAtLevel;
	}

	public void addNumberOfImmutableNodeChildAtLevel(int level, int childs) {
		increment(numberOfImmutableNodeChildAtLevel, level, childs);
	}

	public IntList getNumberOfMutableNodeChildAtLevel() {
		return numberOfMutableNodeChildAtLevel;
	}

	public void addNumberOfMutableNodeChildAtLevel(int level, int child) {
		increment(numberOfMutableNodeChildAtLevel, level, child);
	}

	public IntList getNumberOfEmptySpacesAtLevel() {
		return numberOfEmptySpacesAtLevel;
	}

	public void addNumberOfEmptySpacesAtLevel(int level, int child) {
		increment(numberOfEmptySpacesAtLevel, level, child);
	}

	public IntList getNumberOfAllocatedAndUnusedSpace() {
		return numberOfAllocatedAndUnusedSpaceAtLevel;
	}

	public void addNumberOfUnusedSpaceAtLevel(int level, int child) {
		increment(numberOfAllocatedAndUnusedSpaceAtLevel, level, child);
	}
	
	public void merge(VersionedMapStatistics other) {
		add(this.numberOfNodesAtLevel, other.numberOfNodesAtLevel);
		add(this.numberOfEntriesAtLevel, other.numberOfEntriesAtLevel);
		add(this.numberOfImmutableNodeChildAtLevel, other.numberOfImmutableNodeChildAtLevel);
		add(this.numberOfMutableNodeChildAtLevel, other.numberOfMutableNodeChildAtLevel);
		add(this.numberOfEmptySpacesAtLevel, other.numberOfEmptySpacesAtLevel);
		add(this.numberOfAllocatedAndUnusedSpaceAtLevel, other.numberOfAllocatedAndUnusedSpaceAtLevel);
		
	}

	public String print() {
		return format(numberOfNodesAtLevel, numberOfEntriesAtLevel, numberOfImmutableNodeChildAtLevel,
				numberOfMutableNodeChildAtLevel, numberOfEmptySpacesAtLevel, numberOfAllocatedAndUnusedSpaceAtLevel);
	}

	private static String format(IntArrayList numberOfNodesAtLevel, IntArrayList numberOfEntriesAtLevel,
			IntArrayList numberOfImmutableNodeChildAtLevel, IntArrayList numberOfMutableNodeChildAtLevel,
			IntArrayList numberOfEmptySpacesAtLevel, IntArrayList numberOfAllocatedAndUnusedSpaceAtLevel) {
		StringBuilder result = new StringBuilder();
		addLine(result, "Level", "#Node", "#Entry", "#IChild", "#MChild", "#Empty", "#Unused");
		for (int i = 0; i < numberOfNodesAtLevel.size(); i++) {
			addLine(result, Integer.toString(i + 1), numberOfNodesAtLevel.get(i), numberOfEntriesAtLevel.get(i),
					numberOfImmutableNodeChildAtLevel.get(i), numberOfMutableNodeChildAtLevel.get(i),
					numberOfEmptySpacesAtLevel.get(i), numberOfAllocatedAndUnusedSpaceAtLevel.get(i));
		}
		addLine(result, "sum", Integer.toString(sum(numberOfNodesAtLevel)),
				Integer.toString(sum(numberOfEntriesAtLevel)), Integer.toString(sum(numberOfImmutableNodeChildAtLevel)),
				Integer.toString(sum(numberOfMutableNodeChildAtLevel)),
				Integer.toString(sum(numberOfEmptySpacesAtLevel)),
				Integer.toString(sum(numberOfAllocatedAndUnusedSpaceAtLevel)));
		return result.toString();
	}

	public static String printSum(List<VersionedMapStatistics> statistics) {
		IntArrayList numberOfNodesAtLevel = new IntArrayList();
		IntArrayList numberOfEntriesAtLevel = new IntArrayList();
		IntArrayList numberOfImmutableNodeChildAtLevel = new IntArrayList();
		IntArrayList numberOfMutableNodeChildAtLevel = new IntArrayList();
		IntArrayList numberOfEmptySpacesAtLevel = new IntArrayList();
		IntArrayList numberOfAllocatedAndUnusedSpaceAtLevel = new IntArrayList();

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

	private static void add(IntArrayList to, IntArrayList from) {
		int commonSize = Math.max(to.size(), from.size());
		while (to.size() < commonSize) {
			to.add(0);
		}
		for (int i = 0; i < from.size(); i++) {
			to.set(i, to.get(i) + from.get(i));
		}
	}
}
