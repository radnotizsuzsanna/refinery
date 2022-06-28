package tools.refinery.store.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import tools.refinery.store.map.VersionedMapStatistics;

public class ModelStatistics {
	Map<String, VersionedMapStatistics> mapStatistics = new TreeMap<>();
	
	public Map<String, VersionedMapStatistics> getMapStatistics() {
		return mapStatistics;
	}
	
	public void addMapStatistic(String name, VersionedMapStatistics statistic) {
		this.mapStatistics.put(name, statistic);
	}
	
	public String print() {
		String result="";
		for (Entry<String, VersionedMapStatistics> entry : mapStatistics.entrySet()) {
			result += entry.getKey()+"\n"+entry.getValue().print();
		}
		return result;
	}
}
