package tools.refinery.store.util;

import java.util.List;

import org.eclipse.collections.api.list.primitive.IntList;

public class StatisticsUtil {
	private static String delim = "\t";
	
	public static int sum(List<Integer> numbers) {
		int result = 0;
		for (int integer : numbers) {
			result+=integer;
		}
		return result;
	}
	public static int sum(IntList numbers) {
		int result = 0;
		for(int i=0; i<numbers.size(); i++) {
			result += numbers.get(i);
		}
		return result;
	}
	
	public static void addLine(StringBuilder builder, Object... objects) {
		for(int i=0; i<objects.length; i++) {
			if(i != 0) {
				builder.append(delim);
			}
			builder.append(objects[i]);
		}
		builder.append("\n");
	}
}
