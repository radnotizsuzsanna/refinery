package tools.refinery.store.model;

import static tools.refinery.store.util.StatisticsUtil.addLine;

import java.util.ArrayList;
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
		StringBuilder result = new StringBuilder();
		for (Entry<String, VersionedMapStatistics> entry : mapStatistics.entrySet()) {
			addLine(result, "- " + entry.getKey() + "\n" + entry.getValue().print());
		}
		ArrayList<VersionedMapStatistics> all = new ArrayList<>(mapStatistics.values());
		addLine(result, "- *all*\n" + VersionedMapStatistics.printSum(all));
		return result.toString();
	}
}
