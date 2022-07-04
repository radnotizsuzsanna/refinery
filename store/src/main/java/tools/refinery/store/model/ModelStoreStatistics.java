package tools.refinery.store.model;

import static tools.refinery.store.util.StatisticsUtil.addLine;
import static tools.refinery.store.util.StatisticsUtil.sum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import tools.refinery.store.map.VersionedMapStatistics;
import tools.refinery.store.map.VersionedMapStoreStatistics;

public class ModelStoreStatistics {
	private static final String THE_STATISTICS_ALREADY_FINALIZED = "The statistics already finalized!";

	boolean finalized = false;
	
	long states;
	
	int allNodeInStore = 0;
	int allEntryInStore = 0;
	int allIChildInStore = 0;
	int allMChildInStore = 0;
	int allEmptyInStore = 0;
	int allUnusedInStore = 0;
	
	long allNodeInStates = 0;
	long allEntryInStates = 0;
	long allIChildInStates = 0;
	long allMChildInStates = 0;
	long allEmptyInStates = 0;
	long allUnusedInStates = 0;
	
	double nodeSharing;
	double entrySharing;
	
	List<String> symbolCollection = new ArrayList<>();
	List<VersionedMapStoreStatistics> storedNodes = new ArrayList<>();
	SortedMap<Long,ModelStatistics> stateStatistics = new TreeMap<>();

	void addStoreStatistics(String symbolNames, VersionedMapStoreStatistics mapStatistics) {
		if(finalized) {
			throw new IllegalStateException(THE_STATISTICS_ALREADY_FINALIZED);
		}
		symbolCollection.add(symbolNames);
		storedNodes.add(mapStatistics);
	}
	
	public void setStates(long states) {
		if(finalized) {
			throw new IllegalStateException(THE_STATISTICS_ALREADY_FINALIZED);
		}
		this.states = states;
	}
	public void addStateStatistics(long state, ModelStatistics statistics) {
		if(finalized) {
			throw new IllegalStateException(THE_STATISTICS_ALREADY_FINALIZED);
		}
		stateStatistics.put(state,statistics);
	}
	
	public void finish() {
		for (int i = 0; i < symbolCollection.size(); i++) {
			VersionedMapStatistics s = storedNodes.get(i).getNodeCacheStatistics();
			int node = s.getNumberOfNodesAtLevel().get(0);
			int entry = s.getNumberOfEntriesAtLevel().get(0);
			int iChild = s.getNumberOfImmutableNodeChildAtLevel().get(0);
			int mChild = s.getNumberOfMutableNodeChildAtLevel().get(0);
			int empty = s.getNumberOfEmptySpacesAtLevel().get(0);
			int unused = s.getNumberOfAllocatedAndUnusedSpace().get(0);
			allNodeInStore += node;
			allEntryInStore += entry;
			allIChildInStore += iChild;
			allMChildInStore += mChild;
			allEmptyInStore += empty;
			allUnusedInStore += unused;
		}

		for(Entry<Long, ModelStatistics> entry1 : this.stateStatistics.entrySet()) {
			Map<String, VersionedMapStatistics> mapStatistics = entry1.getValue().mapStatistics;
			for(Entry<String, VersionedMapStatistics> entry2 : mapStatistics.entrySet()) {
				VersionedMapStatistics symbolStatistics = entry2.getValue();
				allNodeInStates += sum(symbolStatistics.getNumberOfNodesAtLevel());
				allEntryInStates += sum(symbolStatistics.getNumberOfEntriesAtLevel());
				allIChildInStates += sum(symbolStatistics.getNumberOfImmutableNodeChildAtLevel());
				allMChildInStates += sum(symbolStatistics.getNumberOfMutableNodeChildAtLevel());
				allEmptyInStates += sum(symbolStatistics.getNumberOfEmptySpacesAtLevel());
				allUnusedInStates += sum(symbolStatistics.getNumberOfAllocatedAndUnusedSpace());
			}
		}
		
		nodeSharing = (allNodeInStore+0.0)/allNodeInStates;
		entrySharing = (allEntryInStore+0.0)/allEntryInStates;
		
		this.finalized = true;
	}
	
	public int getAllNodeInStore() {
		return allNodeInStore;
	}

	public int getAllEntryInStore() {
		return allEntryInStore;
	}

	public long getAllNodeInStates() {
		return allNodeInStates;
	}

	public long getAllEntryInStates() {
		return allEntryInStates;
	}

	public long getStates() {
		return states;
	}

	public String print() {
		if(!finalized) {
			throw new IllegalStateException("The statistics are not finalized!");
		}
		
		StringBuilder result = new StringBuilder();
		addLine(result, "- storage information");
		addLine(result, "group", "#Node", "#Entry", "#IChild", "#MChild", "#Empty", "#Unused", "Symbols");
		
		for (int i = 0; i < symbolCollection.size(); i++) {
			VersionedMapStatistics s = storedNodes.get(i).getNodeCacheStatistics();
			
			int node = s.getNumberOfNodesAtLevel().get(0);
			int entry = s.getNumberOfEntriesAtLevel().get(0);
			int iChild = s.getNumberOfImmutableNodeChildAtLevel().get(0);
			int mChild = s.getNumberOfMutableNodeChildAtLevel().get(0);
			int empty = s.getNumberOfEmptySpacesAtLevel().get(0);
			int unused = s.getNumberOfAllocatedAndUnusedSpace().get(0);
			
			addLine(result,
				i,
				node,
				entry,
				iChild,
				mChild,
				empty,
				unused,
				symbolCollection.get(i));
		}
		addLine(result,
				"sum",
				allNodeInStore,
				allEntryInStore,
				allIChildInStore,
				allMChildInStore,
				allEmptyInStore,
				allUnusedInStore,
				"*all*");
		
		addLine(result, "- state information");
		addLine(result, "state", "#Node", "#Entry", "#IChild", "#MChild", "#Empty", "#Unused");
		for(Entry<Long, ModelStatistics> entry1 : this.stateStatistics.entrySet()) {
						
			long state = entry1.getKey();
			Map<String, VersionedMapStatistics> mapStatistics = entry1.getValue().mapStatistics;
			int node = 0;
			int entry = 0;
			int iChild = 0;
			int mChild = 0;
			int empty = 0;
			int unused = 0;
			for(Entry<String, VersionedMapStatistics> entry2 : mapStatistics.entrySet()) {
				VersionedMapStatistics symbolStatistics = entry2.getValue();
				node += sum(symbolStatistics.getNumberOfNodesAtLevel());
				entry += sum(symbolStatistics.getNumberOfEntriesAtLevel());
				iChild += sum(symbolStatistics.getNumberOfImmutableNodeChildAtLevel());
				mChild += sum(symbolStatistics.getNumberOfMutableNodeChildAtLevel());
				empty += sum(symbolStatistics.getNumberOfEmptySpacesAtLevel());
				unused += sum(symbolStatistics.getNumberOfAllocatedAndUnusedSpace());
			}
			addLine(result,
					state,
					node,
					entry,
					iChild,
					mChild,
					empty,
					unused);
		}
		addLine(result,
				"sum",
				allNodeInStates,
				allEntryInStates,
				allIChildInStates,
				allMChildInStates,
				allEmptyInStates,
				allUnusedInStates);
		
		addLine(result, "node share efficiency = " + nodeSharing);
		addLine(result, "entry share efficiency = " + entrySharing);
		
		return result.toString();
	}
}
