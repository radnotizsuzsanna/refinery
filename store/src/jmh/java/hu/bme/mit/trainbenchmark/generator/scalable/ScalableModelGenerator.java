package hu.bme.mit.trainbenchmark.generator.scalable;

import static hu.bme.mit.trainbenchmark.constants.ModelConstants.ACTIVE;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.CONNECTS_TO;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.CURRENTPOSITION;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.ELEMENTS;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.ENTRY;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.EXIT;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.FOLLOWS;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.LENGTH;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.MONITORED_BY;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.POSITION;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.REGION;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.REQUIRES;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.ROUTE;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SEGMENT;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SEMAPHORE;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SEMAPHORES;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SENSOR;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SENSORS;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SIGNAL;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SWITCH;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SWITCHPOSITION;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.TARGET;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import hu.bme.mit.trainbenchmark.constants.Position;
import hu.bme.mit.trainbenchmark.constants.Signal;
import hu.bme.mit.trainbenchmark.constants.TrainBenchmarkConstants;
import hu.bme.mit.trainbenchmark.generator.ModelGenerator;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;
import hu.bme.mit.trainbenchmark.generator.Scenario;

public class ScalableModelGenerator extends ModelGenerator {

	protected static final int MAX_SEGMENT_LENGTH = 1000;
	protected static final int PERCENTAGE_BASE = 100;
	protected static final int THOUSANDTH_BASE = 1000;

	protected final int maxSegments;
	protected final int maxRoutes;
	protected final int maxSwitchPositions;
	protected final int maxSensors;

	protected int connectedSegmentsErrorPercentage = 0;
	protected int posLengthErrorPercentage         = 0;
	protected int routeSensorErrorPercentage       = 0;
	protected int semaphoreNeighborErrorPercentage = 0;
	protected int switchMonitoredErrorPercentage   = 0;
	protected int switchSetErrorPercentage         = 0;
	protected int routeLengthErrorPercentage       = 0;
	protected int activeRouteErrorPercentage       = 0;
	protected int routeReachabilityErrorThousandth = 0;

	protected final Random connectedSegmentRandom  = new Random(TrainBenchmarkConstants.RANDOM_SEED + 0);
	protected final Random posLengthRandom         = new Random(TrainBenchmarkConstants.RANDOM_SEED + 1);
	protected final Random routeSensorRandom       = new Random(TrainBenchmarkConstants.RANDOM_SEED + 2);
	protected final Random semaphoreNeighborRandom = new Random(TrainBenchmarkConstants.RANDOM_SEED + 3);
	protected final Random switchMonitoredRandom   = new Random(TrainBenchmarkConstants.RANDOM_SEED + 4);
	protected final Random switchSetRandom         = new Random(TrainBenchmarkConstants.RANDOM_SEED + 5);
	protected final Random activeRouteRandom       = new Random(TrainBenchmarkConstants.RANDOM_SEED + 7);
	protected final Random routeLengthRandom       = new Random(TrainBenchmarkConstants.RANDOM_SEED + 6);
	protected final Random routeReachabilityRandom = new Random(TrainBenchmarkConstants.RANDOM_SEED + 8);

	protected final Random lengthRandom    = new Random(TrainBenchmarkConstants.RANDOM_SEED);
	protected final Random positionsRandom = new Random(TrainBenchmarkConstants.RANDOM_SEED);
	protected final Random sensorsRandom   = new Random(TrainBenchmarkConstants.RANDOM_SEED);
	protected final Random swpsRandom      = new Random(TrainBenchmarkConstants.RANDOM_SEED);
	
	LinkedList<GenerationError> errors = new LinkedList<>();
	
	public static boolean inverseConnectsTo = true;
	private final int commitAfterBuildActions;
	private int actions;

	public ScalableModelGenerator(final ModelSerializer serializer, int size, int width, Scenario scenario, int commitAfterBuildActions) {
		super(serializer);
		
		maxSegments = 5 * width;
		maxRoutes = 5 * size * width;
		maxSwitchPositions = 20 * width;
		maxSensors = 10 * width;
		
		this.commitAfterBuildActions = commitAfterBuildActions;
		switch (scenario) {
		case BATCH:
			// set all error percents to 0
			break;
		case INJECT:
			connectedSegmentsErrorPercentage =  5;
			posLengthErrorPercentage         =  2;
			routeSensorErrorPercentage       =  4;
			semaphoreNeighborErrorPercentage =  7;
			switchMonitoredErrorPercentage   =  2;
			switchSetErrorPercentage         =  8;
			activeRouteErrorPercentage       = 10;
			routeLengthErrorPercentage       = 10;
			routeReachabilityErrorThousandth =  2;
			break;
		case INJECTX:
			connectedSegmentsErrorPercentage =  80;
			posLengthErrorPercentage         =  0;
			routeSensorErrorPercentage       =  0;
			semaphoreNeighborErrorPercentage =  0;
			switchMonitoredErrorPercentage   =  0;
			switchSetErrorPercentage         =  0;
			activeRouteErrorPercentage       = 0;
			routeLengthErrorPercentage       = 0;
			routeReachabilityErrorThousandth =  0;
			break;
		case REPAIR:
			connectedSegmentsErrorPercentage =  5;
			posLengthErrorPercentage         = 10;
			routeSensorErrorPercentage       = 10;
			semaphoreNeighborErrorPercentage = 25;
			switchMonitoredErrorPercentage   = 18;
			switchSetErrorPercentage         = 15;
			activeRouteErrorPercentage       = 20;
			routeLengthErrorPercentage       = 20;
			routeReachabilityErrorThousandth =  1;
			break;
		default:
			throw new UnsupportedOperationException("Scenario not supported.");
		}
	}
	
	protected void actionPerformed() {
		if (commitAfterBuildActions > 0) {
			actions++;
			if (actions > commitAfterBuildActions) {
				this.serializer.commit();
				actions = 0;
			}
		}
	}
	
	@Override
	protected void constructModel() throws IOException {
		Object prevSemaphore = null;
		Object firstSemaphore = null;
		List<Object> firstTracks = null;
		List<Object> prevTracks = null;
		for (int i = 0; i < maxRoutes; i++) {
			boolean firstSegment = true;

			if (prevSemaphore == null) {
				final Map<String, Object> semaphoreAttributes = Map.of(SIGNAL, Signal.GO);

				prevSemaphore = serializer.createVertex(SEMAPHORE, semaphoreAttributes);
				actionPerformed();
				firstSemaphore = prevSemaphore;
			}

			Object semaphore;
			if (i != maxRoutes - 1) {
				final Map<String, Object> semaphoreAttributes = Map.of(SIGNAL, Signal.GO);
				semaphore = serializer.createVertex(SEMAPHORE, semaphoreAttributes);
				actionPerformed();
			} else {
				semaphore = firstSemaphore;
			}

			// the semaphoreNeighborErrorPercentage
			final boolean semaphoreNeighborError1 = semaphoreNeighborRandom.nextInt(PERCENTAGE_BASE) < semaphoreNeighborErrorPercentage;
			final Object entry = prevSemaphore;
			final Object exit = semaphore;

			// the entry might be null, therefore we avoid using an ImmutableMap here
			final Map<String, Object> routeOutgoingEdges = new HashMap<>();
			routeOutgoingEdges.put(ENTRY, entry);
			routeOutgoingEdges.put(EXIT, exit);

			final Map<String, Object> routeAttributes = new HashMap<>();
			routeAttributes.put(ACTIVE, true);

			final Object route = serializer.createVertex(ROUTE, routeAttributes, routeOutgoingEdges);
			actionPerformed();
			
			if(semaphoreNeighborError1) {
				this.errors.add(new SemaphoreNeighborError(route,entry));
			}
			
			final Object region = serializer.createVertex(REGION);
			actionPerformed();

			final int swPs = swpsRandom.nextInt(maxSwitchPositions - 1) + 1;
			final List<Object> currentTrack = new ArrayList<>();
			final Set<Object> switches = new HashSet<>();
			for (int j = 0; j < swPs; j++) {
				final int numberOfPositions = Position.values().length;
				final int positionOrdinal = positionsRandom.nextInt(numberOfPositions);
				final Position position = Position.values()[positionOrdinal];
				final Map<String, ? extends Object> swAttributes = Map.of(CURRENTPOSITION, position);
				final Object sw = serializer.createVertex(SWITCH, swAttributes);
				actionPerformed();
				currentTrack.add(sw);
				switches.add(sw);

				// (region)-[:elements]->(sw)
				serializer.createEdge(ELEMENTS, region, sw);
				actionPerformed();

				final int sensors = sensorsRandom.nextInt(maxSensors - 1) + 1;

				for (int k = 0; k < sensors; k++) {
					final Object sensor = serializer.createVertex(SENSOR);
					serializer.createEdge(SENSORS, region, sensor);
					actionPerformed();

					// add "monitored by" edge from switch to sensor
					final boolean switchMonitoredError = switchMonitoredRandom.nextInt(PERCENTAGE_BASE) < switchMonitoredErrorPercentage;
					serializer.createEdge(MONITORED_BY, sw, sensor);
					actionPerformed();
					
					if(switchMonitoredError) {
						this.errors.add(new SwitchMonitorError(sw, sensor));
					}
					serializer.createEdge(REQUIRES, route, sensor);
					actionPerformed();
					
					if (!switchMonitoredError) {
						// add "requires" edge from route to sensor
						final boolean routeSensorError = routeSensorRandom.nextInt(PERCENTAGE_BASE) < routeSensorErrorPercentage;
						if (routeSensorError) {
							this.errors.add(new RouteSensorError(route, sensor));
						}
					}

					// generate segments
					for (int m = 0; m < maxSegments; m++) {
						final Object segment = createSegment(currentTrack, sensor, region);

						if (firstSegment) {
							serializer.createEdge(SEMAPHORES, segment, semaphore);
							actionPerformed();
							firstSegment = false;
						}
					}

					// create another extra segment
					if (connectedSegmentRandom.nextInt(PERCENTAGE_BASE) < connectedSegmentsErrorPercentage) {
						final int segmentLength = lengthRandom.nextInt(MAX_SEGMENT_LENGTH) + 1;
						this.errors.add(new ConnectedSegmentsError(segmentLength,region,sensor));
					}
				}

				// the errorInjectedState may contain a bad value
				final boolean switchSetError = switchSetRandom.nextInt(PERCENTAGE_BASE) < switchSetErrorPercentage;
				final Position invalidPosition = Position.values()[positionOrdinal];
				

				final Map<String, Object> swPAttributes = Map.of(POSITION, invalidPosition);
				final Map<String, Object> swPOutgoingEdges = Map.of(TARGET, sw);
				final Object swP = serializer.createVertex(SWITCHPOSITION, swPAttributes, swPOutgoingEdges);
				actionPerformed();
				
				if(switchSetError) {
					int invalidPositionOrdinal = (numberOfPositions - 1) - positionOrdinal;
					final Position y = Position.values()[invalidPositionOrdinal];
					errors.add(new SwitchSetError(swP, y));
				}

				// (route)-[:follows]->(swP)
				serializer.createEdge(FOLLOWS, route, swP);
				actionPerformed();
			}

//			final Set<Integer> usedTracks = new HashSet<>();
			// (trackElement)-[:connectsTo]-(trackElement)
			for (int j = 1; j < currentTrack.size(); ++j) {
				final Object current = currentTrack.get(j);
//				if (usedTracks.contains(current))
//					continue;

				final boolean routeReachabilityError = routeReachabilityRandom.nextInt(THOUSANDTH_BASE) < routeReachabilityErrorThousandth;
				Object from = currentTrack.get(j - 1);
				Object to = current;
				serializer.createEdge(CONNECTS_TO, from, to);
				actionPerformed();
				
				if(inverseConnectsTo) {
					serializer.createEdge(CONNECTS_TO, to, from);
					actionPerformed();
				}
				if (routeReachabilityError) {
					this.errors.add(new RouteReachabilityError(from,to));
				}

//				if (switches.contains(current)) {
//					final int segmentID = j + random.nextInt(currentTrack.size() - j);
//					if (!usedTracks.contains(segmentID)) {
//						// TODO check why this causes double edges
//						// serializer.createEdge(CONNECTS_TO, current, currentTrack.get(segmentID));
//						usedTracks.add(segmentID);
//					}
//				}
			}

			if (prevTracks != null && prevTracks.size() > 0 && currentTrack.size() > 0) {
				serializer.createEdge(CONNECTS_TO, prevTracks.get(prevTracks.size() - 1), currentTrack.get(0));
				actionPerformed();
				
				if(inverseConnectsTo) {
					serializer.createEdge(CONNECTS_TO, currentTrack.get(0), prevTracks.get(prevTracks.size() - 1));
					actionPerformed();
				}
			}

			// loop the last track element of the last route to the first track element of the first route
			if (i == maxRoutes - 1) {
				if (currentTrack != null && currentTrack.size() > 0 && firstTracks.size() > 0) {
					serializer.createEdge(CONNECTS_TO, currentTrack.get(currentTrack.size() - 1), firstTracks.get(0));
					actionPerformed();
					if(inverseConnectsTo) {
						serializer.createEdge(CONNECTS_TO, firstTracks.get(0), currentTrack.get(currentTrack.size() - 1));
						actionPerformed();
					}
				}
			}

			if (prevTracks == null) {
				firstTracks = currentTrack;
			}

			prevTracks = currentTrack;
			prevSemaphore = semaphore;
		}
	}
	
	public List<GenerationError> getErrors() {
		return errors;
	}
	
	public void shuffleErrors(int randomSeed) {
		Collections.shuffle(errors, new Random(randomSeed));
	}
	
	public void applyErrors(int from, int to) throws IOException {
		for(int i = from; i<to; i++) {
			this.errors.get(i).apply(serializer);
		}
	}

	Object createSegment(final List<Object> currTracks, final Object sensor, final Object region) throws IOException {
		final boolean posLengthError = posLengthRandom.nextInt(PERCENTAGE_BASE) < posLengthErrorPercentage;
		final int segmentLength = lengthRandom.nextInt(MAX_SEGMENT_LENGTH) + 1;

		final Map<String, Object> segmentAttributes = Map.of(LENGTH, segmentLength);
		final Object segment = serializer.createVertex(SEGMENT, segmentAttributes);
		actionPerformed();
		
		if(posLengthError) {
			this.errors.add(new PosLengthError(segment, -segmentLength));
		}

		// (region)-[:elements]->(segment)
		serializer.createEdge(ELEMENTS, region, segment);
		actionPerformed();

		// (segment)-[:monitoredBy]->(sensor) monitoredBy n:m edge
		serializer.createEdge(MONITORED_BY, segment, sensor);
		actionPerformed();
		currTracks.add(segment);
		return segment;
	}

}