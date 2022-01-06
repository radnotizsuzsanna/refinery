package refinery.benchmark.viatracomparison.runner

import java.util.ArrayList
import java.util.List
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine
import org.eclipse.viatra.query.runtime.emf.EMFScope
import refinery.benchmark.viatracomparison.vql.CarOnLane
import refinery.benchmark.viatracomparison.vql.CloseCars
import refinery.benchmark.viatracomparison.vql.DespawnCar
import refinery.benchmark.viatracomparison.vql.MoveCar
import refinery.benchmark.viatracomparison.vql.Queries
import refinery.benchmark.viatracomparison.vql.SpawnCar
import traffic.Lane
import traffic.TrafficFactory
import traffic.TrafficPackage
import traffic.TrafficSituation
import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine
import java.util.concurrent.Callable

class ViatraRunner extends Runner {
	var Resource resource;
	var AdvancedViatraQueryEngine engine;
	Queries queries;

	//CloseCars.Matcher closeCarMatcher
	//FollowingLane.Matcher followingLaneMatcher
	CarOnLane.Matcher carOnLaneMatcher
	SpawnCar.Matcher spawnCarMatcher
	DespawnCar.Matcher despawnCarMatcher
	MoveCar.Matcher moveCarMatcher

	new(boolean coalescing) {
		this.coalescing = coalescing
		EPackage.Registry.INSTANCE.put("refinery-benchmark-viatracomparison.traffic", TrafficPackage.eINSTANCE)
	}
	
	override getName() {
		"VQL"
	}

	override initEmptyModel() {
		val rs = new ResourceSetImpl;
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		resource = rs.createResource(URI.createFileURI("dummy.xmi"));

		engine = AdvancedViatraQueryEngine.createUnmanagedEngine(new EMFScope(resource))
		queries = Queries.instance
		Queries.instance.prepare(engine);

		val closeCarMatcher = queries.closeCars.getMatcher(engine);
		carOnLaneMatcher = queries.carOnLane.getMatcher(engine);
		spawnCarMatcher = queries.spawnCar.getMatcher(engine)
		despawnCarMatcher = queries.despawnCar.getMatcher(engine)
		moveCarMatcher = queries.moveCar.getMatcher(engine);
	}

	var TrafficSituation trafficSituation
	var int idealCars

	override buildModel(int xLanes, int yLanes) {
		idealCars = yLanes

		trafficSituation = TrafficFactory.eINSTANCE.createTrafficSituation
		resource.contents.add(trafficSituation);

		val List<List<Lane>> laneList = new ArrayList();
		for (var x = 0; x < xLanes; x++) {
			laneList.add(new ArrayList)
		}
		// create lanes
		for (var x = 0; x < xLanes; x++) {
			for (var y = 0; y < yLanes; y++) {
				val lane = TrafficFactory.eINSTANCE.createLane
				trafficSituation.lanes.add(lane)
				laneList.get(x).add(lane)
			}
		}
		// connect lanes
		for (var x = 0; x < xLanes - 1; x++) {
			for (var y = 0; y < yLanes; y++) {
				laneList.get(x).get(y).following += laneList.get(x + 1).get(y)
			}
		}

		for (var x = 0; x < xLanes; x++) {
			for (var y = 0; y < yLanes - 1; y++) {
				laneList.get(x).get(y).right = laneList.get(x).get(y + 1)
			}
		}
		// place cars
		for (var y = 0; y < yLanes; y++) {
			val car = TrafficFactory.eINSTANCE.createCar
			trafficSituation.cars.add(car)
			car.on = laneList.get(0).get(y)
		}
	}
	
	val boolean coalescing
	
	override iterate() {
		engine.delayUpdatePropagation([
		// if there is car to despawn, remove it
		for (m : despawnCarMatcher.allMatches) {
			m.car.on = null
			trafficSituation.cars -= m.car
		}

		// for other cars, move to random place if can
		for (c : trafficSituation.cars) {
			val m = moveCarMatcher.getAllMatches(null, null, c);
			if(m.size > 0) {
				val selected = this.r.nextInt(m.size)
				val selecetedMatch = m.get(selected)
	
				selecetedMatch.car.on = selecetedMatch.to
			}
		}

		// count the number of cars
		val cars = carOnLaneMatcher.countMatches
		val newCars = this.idealCars - cars
		val spawnCarMatches = spawnCarMatcher.allMatches
		if (newCars > 0) {
			val randomlySelected = selectRandomly(spawnCarMatches.size, newCars)
			for (var i = 0; i < spawnCarMatches.size; i++) {
				if (randomlySelected.get(i)) {
					val car = TrafficFactory.eINSTANCE.createCar
					trafficSituation.cars.add(car)
					car.on = spawnCarMatches.get(i).lane
				}
			}
		}
		return true
	])
	}

}
