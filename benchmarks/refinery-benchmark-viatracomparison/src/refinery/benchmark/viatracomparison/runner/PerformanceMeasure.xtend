package refinery.benchmark.viatracomparison.runner

import java.util.List
import java.util.ArrayList

class PerformanceMeasure {
	int run;
	Runner r;
	int x;
	int y;
	List<Integer> iterationSteps;
	
	new(int run,Runner r,int x,int y,List<Integer> iterationSteps) {
		this.run = run
		this.r=r
		this.x=x
		this.y=y
		this.iterationSteps=iterationSteps
	}
	
	def static void main(String[] args) {
		println('''Engine,Run,X,Y,InitTime,BuildTime,Step1000,Step2000,Step3000,Step4000,Step5000''')
		val List<PerformanceMeasure> items = new ArrayList()
		//items.addMeasurements(5,new ViatraRunner,10,10,#[1000,2000,3000,4000,5000])
//		items.addMeasurements(25,new ViatraRunner,50,50,#[1000,2000,3000,4000,5000])
//		items.addMeasurements(25,new ViatraRunner,100,100,#[1000,2000,3000,4000,5000])
//		items.addMeasurements(25,new ViatraRunner,150,150,#[1000,2000,3000,4000,5000])
//		items.addMeasurements(25,new ViatraRunner,200,200,#[1000,2000,3000,4000,5000])
//		items.addMeasurements(25,new ViatraRunner,250,250,#[1000,2000,3000,4000,5000])
		
		//items.addMeasurements(5,new ViatraRunner,500,550,#[1000,2000,3000,4000,5000])
		
//		items.addMeasurements(25,new RefineryRunner,50,50,#[1000,2000,3000,4000,5000])
//		items.addMeasurements(25,new RefineryRunner,100,100,#[1000,2000,3000,4000,5000])
//		items.addMeasurements(25,new RefineryRunner,150,150,#[1000,2000,3000,4000,5000])
//		items.addMeasurements(25,new RefineryRunner,200,200,#[1000,2000,3000,4000,5000])
//		items.addMeasurements(25,new RefineryRunner,250,250,#[1000,2000,3000,4000,5000])
		
		items.addMeasurements(5,new RefineryRunner(true),10,10,#[1000,2000,3000,4000,5000])
		items.addMeasurements(25,new RefineryRunner(true),50,50,#[1000,2000,3000,4000,5000])
		items.addMeasurements(25,new RefineryRunner(true),100,100,#[1000,2000,3000,4000,5000])
		items.addMeasurements(25,new RefineryRunner(true),150,150,#[1000,2000,3000,4000,5000])
		items.addMeasurements(25,new RefineryRunner(true),200,200,#[1000,2000,3000,4000,5000])
		items.addMeasurements(25,new RefineryRunner(true),250,250,#[1000,2000,3000,4000,5000])
		items.addMeasurements(25,new RefineryRunner(true),500,500,#[1000,2000,3000,4000,5000])
		items.addMeasurements(25,new RefineryRunner(true),750,750,#[1000,2000,3000,4000,5000])
		items.addMeasurements(25,new RefineryRunner(true),1000,1000,#[1000,2000,3000,4000,5000])
		items.addMeasurements(25,new RefineryRunner(true),1250,1250,#[1000,2000,3000,4000,5000])
		
//			new PerformanceMeasure(1,new ViatraRunner,20,20,#[1000,2000,3000,4000,5000]),
//			new PerformanceMeasure(2,new ViatraRunner,20,20,#[1000,2000,3000,4000,5000]),
//			new PerformanceMeasure(3,new ViatraRunner,20,20,#[1000,2000,3000,4000,5000]),
//			new PerformanceMeasure(4,new ViatraRunner,20,20,#[1000,2000,3000,4000,5000]),
//			new PerformanceMeasure(5,new ViatraRunner,20,20,#[1000,2000,3000,4000,5000]),
////			new PerformanceMeasure(1,new ViatraRunner,200,200,#[1000,2000,3000,4000,5000])
////			

//			new PerformanceMeasure(1, new RefineryRunner,20,20,#[1000,20000,3000,4000,5000]),
//			new PerformanceMeasure(1, new RefineryRunner,20,20,#[1000,20000,3000,4000,5000]),
//			new PerformanceMeasure(1, new RefineryRunner,20,20,#[1000,20000,3000,4000,5000]),
//			new PerformanceMeasure(1, new RefineryRunner,20,20,#[1000,20000,3000,4000,5000])
		for(item : items) {
			item.execute
			if(item != items.last) {
				System.gc System.gc System.gc
				Thread.sleep(2000)
			}
		}
	}
	def static addMeasurements(List<PerformanceMeasure> list, int run,Runner r,int x,int y,List<Integer> iterationSteps) {
		for(var i = 0; i<run; i++) {
			list+= new PerformanceMeasure(i+1,r,x,y,iterationSteps)
		}
	}
	
	def execute() {
		val start = System.nanoTime
		r.initEmptyModel
		val initFinishedTime = System.nanoTime
		r.buildModel(x,y)
		val buildFinished = System.nanoTime
		val iterationTimes = new ArrayList<Long>();
		for(var i = 0; i<iterationSteps.last; i++) {
			r.iterate
			if(iterationSteps.contains(i+1)) {
				iterationTimes.add(System.nanoTime);
			}
		}
		println('''«r.name»,«run»,«x»,«y»,«(initFinishedTime-start)/1000000»,«(buildFinished-initFinishedTime)/1000000»«
			FOR stepTime:iterationTimes»,«(stepTime-buildFinished)/1000000»«ENDFOR»''')
	}
}