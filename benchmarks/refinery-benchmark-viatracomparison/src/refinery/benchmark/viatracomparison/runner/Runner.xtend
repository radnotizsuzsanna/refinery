package refinery.benchmark.viatracomparison.runner

import java.util.ArrayList
import java.util.List
import java.util.Random
import java.util.Collections

abstract class Runner {
	protected val Random r = new Random
	
	def String getName();
	
	def void initEmptyModel();
	def void buildModel(int xLanes, int yLanes);
	def void iterate();
	
	def selectRandomly(int from, int n) {
		val List<Boolean> selected = new ArrayList
		for(var i = 0; i<from; i++) {
			if(i<n) {
				selected+=true;
			} else {
				selected+=false;
			}
		}
		Collections.shuffle(selected,r);
		//println('''«from» - «n» = «selected»''')
		return selected
	}
}