package refinery.benchmark.viatracomparison.runner

import com.google.inject.Injector
import java.io.ByteArrayInputStream
import java.util.ArrayList
import java.util.List
import java.util.Random
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.resource.SynchronizedXtextResourceSet
import tools.refinery.language.ProblemStandaloneSetup
import tools.refinery.language.mapping.QueryableModelMapper
import tools.refinery.language.model.problem.Problem
import tools.refinery.store.model.Tuple
import tools.refinery.store.model.Tuple.Tuple1
import tools.refinery.store.model.representation.Relation
import tools.refinery.store.model.representation.TruthValue
import tools.refinery.store.query.QueriableModel
import tools.refinery.store.query.building.DNFPredicate

class RefineryRunner extends Runner {

	override getName() {
		return "Refinery"
	}

	val Problem problem;
	val boolean abstraction;
	new(boolean abstraction) {
		val Injector i = (new ProblemStandaloneSetup).createInjectorAndDoEMFRegistration
		val rs = new SynchronizedXtextResourceSet
		val resource = rs.createResource(URI.createURI("dummy:/example.problem"));
		val code = if(abstraction) {v4code} else {v2code}
		this.abstraction = abstraction
		val in = new ByteArrayInputStream(code.getBytes()); 
			
		resource.load(in, rs.getLoadOptions());
		problem = resource.getContents().get(0) as Problem;
		EcoreUtil::resolveAll(problem)
		resource.errors.forEach[println(it)]
	}

	static val v2code = '''
		class Lane {
		    Lane[0..*] following
		    Lane[0..1] left opposite right
		    Lane[0..1] right opposite left
		}
		class Car {
		    Lane[0..1] on
		}
		direct pred closeCars(car1, car2) <->
		    on(car1,lane)=true|unknown,
		    on(car2,lane)=true|unknown,
		    equals(car1,car2)=false
		 ;	following(lane1,lane2)=true|unknown,
		    on(car1,lane1)=true|unknown,
		    on(car2,lane2)=true|unknown
		 ;	left(lane1,lane2)=true|unknown,
		    on(car1,lane1)=true|unknown,
		    on(car2,lane2)=true|unknown.
		
		direct pred hasFollowing(from) <->
		    following(from,_to) = true|unknown.
		
		direct pred hasPrevious(to) <->
		    following(_from,to) = true|unknown.
		
		direct pred carOnLane(car,lane) <->
		    on(car,lane) = true|unknown.
		
		direct pred spawnCar(lane) <->
		    Lane(lane) = true|unknown,
		    !hasPrevious(lane),
		    !carOnLane(_,lane).
		
		direct pred despawnCar(lane,car) <->
		    !hasFollowing(lane),
		    carOnLane(car,lane).
		
		direct pred moveCar(from,to,car) <->
		    following(from,to) = true|unknown,
		    on(car,from) = true|unknown
		 ;	left(from,to) = true|unknown,
		    on(car,from) = true|unknown
		 ;	right(from,to) = true|unknown,
		    on(car,from) = true|unknown.
	'''
	static val v4code = '''
		class Lane {
		    Lane[0..*] following
		    Lane[0..1] left opposite right
		    Lane[0..1] right opposite left
		}
		class Car {
		    Lane[0..1] on
		}
		direct pred closeCars(car1, car2) <->
		    on(car1,lane)=true,
		    on(car2,lane)=true,
		    equals(car1,car2)=false
		 ;	following(lane1,lane2)=true,
		    on(car1,lane1)=true,
		    on(car2,lane2)=true
		 ;	left(lane1,lane2)=true,
		    on(car1,lane1)=true,
		    on(car2,lane2)=true.
		
		direct pred hasFollowing(from) <->
		    following(from,_to) = true.
		
		direct pred hasPrevious(to) <->
		    following(_from,to) = true.
		
		direct pred carOnLane(car,lane) <->
		    on(car,lane) = true.
		
		direct pred spawnCar(lane) <->
		    Lane(lane) = true,
		    !hasPrevious(lane),
		    !carOnLane(_,lane).
		
		direct pred despawnCar(lane,car) <->
		    !hasFollowing(lane),
		    carOnLane(car,lane).
		
		direct pred moveCar(from,to,car) <->
		    following(from,to) = true,
		    on(car,from) = true
		 ;	left(from,to) = true,
		    on(car,from) = true
		 ;	right(from,to) = true,
		    on(car,from) = true.
	'''

	var QueriableModel model

	var Relation<TruthValue> exists
	var Relation<TruthValue> equals
	var Relation<TruthValue> car
	var Relation<TruthValue> lane
	var Relation<TruthValue> following
	var Relation<TruthValue> on
	var Relation<TruthValue> left
	var Relation<TruthValue> right

	var DNFPredicate despawnCar
	var DNFPredicate moveCar
	var DNFPredicate spawnCar
	var DNFPredicate carOnLane

	var int nextId

	override initEmptyModel() {
		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(problem)
		model = partialModelMapperDTO.getModel as QueriableModel
		nextId = mapper.prepareNodes(partialModelMapperDTO).values.max + 1

		exists = findRelation("exists")
		equals = findRelation("equals")
		car = findRelation("Car")
		lane = findRelation("Lane")
		following = findRelation("following")
		on = findRelation("on")
		left = findRelation("left")
		right = findRelation("right")
		// partialModelMapperDTO.
		despawnCar = findQuery("despawnCar")
		moveCar = findQuery("moveCar")
		spawnCar = findQuery("spawnCar")
		carOnLane = findQuery("carOnLane")
	}

	private def findRelation(String name) {
		model.dataRepresentations.findFirst[it.name == name] as Relation<TruthValue>
	}

	private def findQuery(String name) {
		model.predicates.findFirst[it.name == name]
	}

	var int idealCars

	override buildModel(int xLanes, int yLanes) {
		idealCars = yLanes

		val List<List<Tuple>> laneList = new ArrayList();
		for (var x = 0; x < xLanes; x++) {
			laneList.add(new ArrayList)
		}

		// create lanes
		for (var x = 0; x < xLanes; x++) {
			for (var y = 0; y < yLanes; y++) {
				val id = nextId++
				val l = Tuple.of1(id)
				model.put(lane, l, TruthValue.TRUE)
				model.put(exists, l, TruthValue.TRUE)
				model.put(equals, Tuple.of(l.get(0), l.get(0)), TruthValue.TRUE)
				laneList.get(x).add(l)
			}
		}
		// connect lanes
		for (var x = 0; x < xLanes - 1; x++) {
			for (var y = 0; y < yLanes; y++) {
				model.put(
					following,
					Tuple.of(
						laneList.get(x).get(y).get(0),
						laneList.get(x + 1).get(y).get(0)
					),
					TruthValue.TRUE
				)
			}
		}

		for (var x = 0; x < xLanes; x++) {
			for (var y = 0; y < yLanes - 1; y++) {
				model.put(
					left,
					Tuple.of(
						laneList.get(x).get(y).get(0),
						laneList.get(x).get(y + 1).get(0)
					),
					TruthValue.TRUE
				)
				model.put(
					right,
					Tuple.of(
						laneList.get(x).get(y + 1).get(0),
						laneList.get(x).get(y).get(0)
					),
					TruthValue.TRUE
				)
			}
		}
		// place cars
		for (var y = 0; y < yLanes; y++) {
			val id = nextId++
			val c = Tuple.of1(id);
			model.put(car, c, TruthValue.TRUE)
			model.put(exists, c, TruthValue.TRUE)
			model.put(equals, Tuple.of(id, id), TruthValue.TRUE)
			model.put(on, Tuple.of(id, laneList.get(0).get(y).get(0)), onTV)
		}
		model.flushChanges

	}
	def onTV() {
		/*if(this.abstraction) return TruthValue.UNKNOWN
		else return*/ TruthValue.TRUE
	}

	val Random r = new Random

	override iterate() {
		// if there is car to despawn, remove it
		val despawnIterator = model.allResults(despawnCar).iterator
		while (despawnIterator.hasNext) {
			val match = despawnIterator.next
			val c = match.get(0) as Tuple1
			val l = match.get(1) as Tuple1
			model.put(car, c, TruthValue.FALSE)
			model.put(exists, c, TruthValue.FALSE)
			model.put(equals, Tuple.of(c.get(0), c.get(0)), TruthValue.FALSE)
			model.put(on, Tuple.of(c.get(0), l.get(1)), TruthValue.FALSE)
		}

		// for other cars, move to random place if can
		val cursor = model.getAll(car)
		while (cursor.move) {
			val c = cursor.key as Tuple1
			val moveOptions = model.countResults(moveCar, #[null, null, c])
			if (moveOptions > 0) {
				val moveIterator = model.allResults(moveCar, #[null, null, c]).iterator
				var selectedIndex = r.nextInt(moveOptions)
				while (selectedIndex > 0) {
					moveIterator.next()
					selectedIndex--
				}
				val selectedMove = moveIterator.next
				val from = selectedMove.get(0) as Tuple1
				val to = selectedMove.get(0) as Tuple1
				model.put(on, Tuple.of(c.get(0), from.get(0)), TruthValue.FALSE)
				model.put(on, Tuple.of(c.get(0), to.get(0)), onTV)
			}

		}

		// count the number of cars
		val cars = model.countResults(carOnLane)
		val newCars = this.idealCars - cars
		if (newCars > 0) {
			val spawnIterator = model.allResults(spawnCar).iterator
			val spawnOptions = model.countResults(spawnCar)
			val randomlySelected = selectRandomly(spawnOptions, newCars)
			for (selected : randomlySelected) {
				val match = spawnIterator.next
				if (selected) {
					val lane = match.get(0) as Tuple1
					val id = nextId++
					val c = Tuple.of1(id);
					model.put(car, c, TruthValue.TRUE)
					model.put(exists, c, TruthValue.TRUE)
					model.put(equals, Tuple.of(id, id), TruthValue.TRUE)
					model.put(on, Tuple.of(id, lane.get(0)), onTV)
				}
			}
		}
		model.flushChanges
	}

}
