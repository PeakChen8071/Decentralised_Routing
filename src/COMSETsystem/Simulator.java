package COMSETsystem;

import MapCreation.*;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.*;
import me.tongfei.progressbar.*;


import DataParsing.*;

/**
 * The Simulator class defines the major steps of the simulation. It is
 * responsible for loading the map, creating the necessary number of agents,
 * creating a respective AgentEvent for each of them such that they are added
 * to the events PriorityQueue. Furthermore it is also responsible for dealing
 * with the arrival of resources, map matching them to the map, and assigning
 * them to agents. This produces the score according to the scoring rules.
 * <p>
 * The time is simulated by the events having a variable time, this time
 * corresponds to when something will be empty and thus needs some
 * interaction (triggering). There's an event corresponding to every existent
 * Agent and for every resource that hasn't arrived yet. All of this events are
 * in a PriorityQueue called events which is ordered by their time in an
 * increasing way.
 */
public class Simulator {

	// The map that everything will happen on.
	protected CityMap map;

	// A deep copy of map to be passed to agents.
	// This is a way to make map unmodifiable.
	protected CityMap mapForAgents;

	// The event queue.
	protected PriorityQueue<Event> events = new PriorityQueue<>();

	// The set of empty agents.
	protected TreeSet<AgentEvent> emptyAgents = new TreeSet<>(new AgentEventComparator());

	// The set of resources that with no agent assigned to it yet.
	public TreeSet<ResourceEvent> waitingResources = new TreeSet<>(new ResourceEventComparator());

	// The maximum life time of a resource in seconds. This is a parameter of the simulator.
	public long ResourceMaximumLifeTime;

	// Full path to an OSM JSON map file
	protected String mapJSONFile;


	// Full path to a TLC New York Yellow trip record file
	protected String resourceFile = null;

	// Full path to a KML defining the bounding polygon to crop the map
	protected String boundingPolygonKMLFile;

	// The simulation end time is the expiration time of the last resource.
	protected long simulationEndTime;

	// Total trip time of all resources to which agents have been assigned.
	protected long totalResourceTripTime = 0;

	// Total wait time of all resources. The wait time of a resource is the amount of time
	// since the resource is introduced to the system until it is picked up by an agent.
	protected long totalResourceWaitTime = 0;

	// Total search time of all agents. The search time of an agent for a research is the amount of time
	// since the agent is labeled as empty, i.e., added to emptyAgents, until it picks up a resource.
	protected long totalAgentSearchTime = 0;

	// Total cruise time of all agents. The cruise time of an agent for a research is the amount of time
	// since the agent is labeled as empty until it is assigned to a resource.
	protected long totalAgentCruiseTime = 0;

	// Total approach time of all agents. The approach time of an agent for a research is the amount of time
	// since the agent is assigned to a resource until agent reaches the resource.
	protected long totalAgentApproachTime = 0;

	// The number of expired resources.
	protected long expiredResources = 0;

	// The number of resources that have been introduced to the system.
	protected long totalResources = 0;

	// The number of agents that are deployed (at the beginning of the simulation).
	protected long totalAgents;

	// The number of assignments that have been made.
	protected long totalAssignments = 0;

	// The output file names to record the time and location of resource introduction/expiration
	public String resourceLogName = "";
	public String expirationLogName = "";

	// A list of all the agents in the system. Not really used in COMSET, but maintained for
	// a user's debugging purposes.
	ArrayList<BaseAgent> agents;

	// A class that extends BaseAgent and implements a search routing strategy
	protected final Class<? extends BaseAgent> agentClass;

	/**
	 * Constructor of the class Main. This is made such that the type of
	 * agent/resourceAnalyzer used is not hardcoded and the users can choose
	 * whichever they wants.
	 *
	 * @param agentClass the agent class that is going to be used in this
	 * simulation.
	 */
	public Simulator(Class<? extends BaseAgent> agentClass) {
		this.agentClass = agentClass;
	}

	/**
	 * Configure the simulation system including:
	 *
	 * 1. Create a map from the map file and the bounding polygon KML file.
	 * 2. Load the resource data set and map match.
	 * 3. Create the event queue.
	 *
	 * See Main.java for detailed description of the parameters.
	 *
	 * @param mapJSONFile The map file
	 * @param resourceFile The dataset file
	 * @param totalAgents The total number of agents to deploy
	 * @param boundingPolygonKMLFile The KML file defining a bounding polygon of the simulated area
	 * @param maximumLifeTime The maximum life time of a resource
	 * @param agentPlacementSeed The see for the random number of generator when placing the agents
	 * @param speedRudction The speed reduction to accommodate traffic jams and turn delays
	 */
	public void configure(String mapJSONFile, String resourceFile, Long totalAgents, String boundingPolygonKMLFile, Long maximumLifeTime, long agentPlacementRandomSeed, double speedReduction) {

		this.mapJSONFile = mapJSONFile;

		this.totalAgents = totalAgents;

		this.boundingPolygonKMLFile = boundingPolygonKMLFile;

		this.ResourceMaximumLifeTime = maximumLifeTime;

		this.resourceFile = resourceFile;

		MapCreator creator = new MapCreator(this.mapJSONFile, this.boundingPolygonKMLFile, speedReduction);
		System.out.println("Creating the map...");

		creator.createMap();

		// Output the map
		map = creator.outputCityMap();

		// Pre-compute shortest travel times between all pairs of intersections.
		System.out.println("Pre-computing all pair travel times...");
		map.calcTravelTimes();

		// Pre-compute best weighted search route
		System.out.println("Pre-computing best search route...");
		map.calcSearchTimes();

		// Make a map copy for agents to use so that an agent cannot modify the map used by
		// the simulator
		mapForAgents = map.makeCopy();

		MapWithData mapWD = new MapWithData(map, this.resourceFile, agentPlacementRandomSeed);

		// map match resources
		System.out.println("Loading and map-matching resources...");
		long latestResourceTime = mapWD.createMapWithData(this);
		System.out.println("latest resource time: " + latestResourceTime);
		// The simulation end time is the expiration time of the last resource.
		this.simulationEndTime = latestResourceTime;

		// Deploy agents at random locations of the map.
		System.out.println("Randomly placing " + this.totalAgents + " agents on the map...");


		// Manipulate the starting position of agents
		agents = mapWD.placeAgentsRandomly(this);
		//agents = mapWD.placeAgentFromHighest(this);

		// Initialize the event queue.
		events = mapWD.getEvents();

//		try {
//			PrintWriter writer = new PrintWriter("roadConnectivityWithTravelTimeSpeed.txt", "UTF-8");
//			List<Road> rs = map.roads();
//			writer.println("roadId,from_node,to_node,from_node_lat,from_node_lon,to_node_lat,to_node_lon,length,travel_time");
//			for (Road r : rs) {
//				writer.print(r.id);
//				writer.print(",");
//				writer.print(r.from.id);
//				writer.print(",");
//				writer.print(r.to.id);
//				writer.print(",");
//				writer.print(r.from.latitude);
//				writer.print(",");
//				writer.print(r.from.longitude);
//				writer.print(",");
//				writer.print(r.to.latitude);
//				writer.print(",");
//				writer.print(r.to.longitude);
//				writer.print(",");
//				writer.print(r.length);
//				writer.print(",");
//				writer.print(r.travelTime);
//				writer.println();
//			}
//			writer.close();
//		}catch(IOException ioe){
//			ioe.printStackTrace();
//		}
//			writer.close();
//			writer.println("roadId,toto,fromfrom,tofrom,fromto");
//			for(Road r:rs){
//				Intersection to = r.to;
//				Intersection from = r.from;
//
//				Set<Road> ToTo = to.getRoadsTo();
//				Set<Road> FromFrom = from.getRoadsFrom();
//				Set<Road> ToFrom = to.getRoadsFrom();
//				Set<Road> FromTo = from.getRoadsTo();
//
//				writer.print(r.id+",");
//				for (Road n:ToTo) {
//					writer.print(n.id + " ");
//				}
//				writer.print(",");
//				for (Road n:FromFrom) {
//					writer.print(n.id + " ");
//				}
//				writer.print(",");
//				for (Road n:ToFrom) {
//					writer.print(n.id + " ");
//				}
//				writer.print(",");
//				for (Road n:FromTo) {
//					writer.print(n.id + " ");
//				}
//				writer.println();
//			}
//			writer.close();
//			PrintWriter writer2 = new PrintWriter("intersection_list.txt", "UTF-8");
//			Map<Long, Intersection> ints = map.intersections();
//			writer2.println("origin,from,to,lon,lat");
//			for(Long l:ints.keySet()){
//
//				writer2.print(l+",");
//				Intersection L = ints.get(l);
//
//				for (Intersection af :L.getAdjacentFrom()) {
//					writer2.print(af.id + "-");
//					writer2.print(L.roadTo(af).id);
//					writer2.print(" ");
//
//				}
//				writer2.print(",");
//				for (Intersection at :L.getAdjacentTo()) {
//					writer2.print(at.id + "-");
//					writer2.print(at.roadTo(L).id);
//					writer2.print(" ");
//				}
//				writer2.print(",");
//				writer2.print(L.longitude);
//				writer2.print(",");
//				writer2.print(L.latitude);
//				writer2.print(",");
//				writer2.print(L.latitude);
//				writer2.println();
//			}
//			writer2.close();
//		}catch(IOException ioe){
//			ioe.printStackTrace();
//		}
//


	}

	/**
	 * This method corresponds to running the simulation. An object of ScoreInfo
	 * is created in order to keep track of performance in the current
	 * simulation. Go through every event until the simulation is over.
	 *
	 * @throws Exception since triggering events may create an Exception
	 */
	public void run() throws Exception {
		System.out.println("Running the simulation...");

		ScoreInfo score = new ScoreInfo();
		if (map == null) {
			System.out.println("map is null at beginning of run");
		}

		String properties = "";
		StringBuilder sb = new StringBuilder();
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream("etc/config.properties"));
			String fleetSize = prop.getProperty("comset.number_of_agents").trim();
			sb.append(fleetSize);
			String test_file = prop.getProperty("comset.dataset_file").trim().replaceAll(
					"Raw_Yellow_Taxi_Data/", "_").replaceAll(".csv", "");
			sb.append(test_file);
			String method = prop.getProperty("comset.agent_class").trim();
			if (method.equals("UserExamples.AgentRandomDestination")){
				method = "RandomDest";
			}else{
				method = "Independent";
			}
			sb.append("_");
			sb.append(method);
			String road_cluster_file = prop.getProperty("cluster.road_cluster_file").trim();
			String substr = road_cluster_file.substring(12,road_cluster_file.length()-4);
			sb.append("_");
			sb.append(substr);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		properties = properties + sb.toString();
		String agentLogName = "Resource and Expiration Results/02_01_Agents_" + properties + ".csv";
		FileWriter fw = new FileWriter(agentLogName);
		PrintWriter pw = new PrintWriter(fw);
		pw.write("time,empty_agents,waiting_resources\n");
		pw.close();

		resourceLogName = "Resource and Expiration Results/02_01_Resources_" + properties + ".csv";
		FileWriter fw1 = new FileWriter(resourceLogName);
		PrintWriter pw1 = new PrintWriter(fw1);
		pw1.write("time,Road ID\n");
		pw1.close();

		expirationLogName = "Resource and Expiration Results/02_01_Expiration_"+properties+".csv";
		FileWriter fw2 = new FileWriter(expirationLogName);
		PrintWriter pw2 = new PrintWriter(fw2);
		pw2.write("time,expiration\n");
		pw2.close();

		try (ProgressBar pb = new ProgressBar("Progress:", 100, ProgressBarStyle.ASCII)) {
			long beginTime = events.peek().time;
			long recordTime = events.peek().time;

			while (events.peek().time <= simulationEndTime) {
				Event toTrigger = events.poll();
				pb.stepTo((long)(((float)(toTrigger.time - beginTime)) / (simulationEndTime - beginTime) * 100.0));

			//	create output file to record the number of available agent when an agent event is triggered
				if (recordTime < events.peek().time) {
					fw = new FileWriter(agentLogName, true);
					pw = new PrintWriter(fw);
					pw.write(events.peek().time + "," + emptyAgents.size() + "," + waitingResources.size() + "\n");
					pw.close();
					recordTime = events.peek().time;
				}

				Event e = toTrigger.trigger();
				if (e != null) {
					events.add(e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Simulation finished.");

		score.end();
	}

	/**
	 * This class is used to give a performance report and the score. It prints
	 * the total running time of the simulation, the used memory and the score.
	 * It uses Runtime which allows the application to interface with the
	 * environment in which the application is running.
	 */
	class ScoreInfo {

		Runtime runtime = Runtime.getRuntime();
		NumberFormat format = NumberFormat.getInstance();
		StringBuilder sb = new StringBuilder();

		long startTime;
		long allocatedMemory;

		/**
		 * Constructor for ScoreInfo class. Runs beginning, this method
		 * initializes all the necessary things.
		 */
		ScoreInfo() {
			startTime = System.nanoTime();
			// Suppress memory allocation information display
			// beginning();
		}

		/**
		 * Initializes and gets the max memory, allocated memory and free
		 * memory. All of these are added to the Performance Report which is
		 * saved in the StringBuilder. Furthermore also takes the time, such
		 * that later on we can compare to the time when the simulation is over.
		 * The allocated memory is also used to compare to the allocated memory
		 * by the end of the simulation.
		 */
		void beginning() {
			// Getting the memory used
			long maxMemory = runtime.maxMemory();
			allocatedMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();

			// probably unnecessary
			sb.append("Performance Report: " + "\n");
			sb.append("free memory: " + format.format(freeMemory / 1024) + "\n");
			sb.append("allocated memory: " + format.format(allocatedMemory / 1024)
					+ "\n");
			sb.append("max memory: " + format.format(maxMemory / 1024) + "\n");

			// still looking into this one "freeMemory + (maxMemory -
			// allocatedMemory)"
			sb.append("total free memory: "
					+ format.format(
					(freeMemory + (maxMemory - allocatedMemory)) / 1024)
					+ "\n");

			System.out.print(sb.toString());
		}

		/**
		 * Calculate the time the simulation took by taking the time right now
		 * and comparing to the time when the simulation started. Add the total
		 * time to the report and the score as well. Furthermore, calculate the
		 * allocated memory by the participant's implementation by comparing the
		 * previous allocated memory with the current allocated memory. Print
		 * the Performance Report.
		 */
		void end() {
			// Empty the string builder
			sb.setLength(0);

			long endTime = System.nanoTime();
			long totalTime = (endTime - startTime) / 1000000000;

			System.out.println("\nrunning time: " + totalTime);

			System.out.println("\n***Simulation environment***");
			System.out.println("JSON map file: " + mapJSONFile);
			System.out.println("Resource dataset file: " + resourceFile);
			System.out.println("Bounding polygon KML file: " + boundingPolygonKMLFile);
			System.out.println("Number of agents: " + totalAgents);
			System.out.println("Number of resources: " + totalResources);
			System.out.println("Resource Maximum Life Time: " + ResourceMaximumLifeTime + " seconds");
			System.out.println("Agent class: " + agentClass.getName());

			System.out.println("\n***Statistics***");

			if (totalResources != 0) {
				// Collect the "search" time for the agents that are empty at the end of the simulation.
				// These agents are in search status and therefore the amount of time they spend on
				// searching until the end of the simulation should be counted toward the total search time.
				long totalRemainTime = 0;
				for (AgentEvent ae: emptyAgents) {
					totalRemainTime += (simulationEndTime - ae.startSearchTime);
				}

				sb.append("average agent search time: " + Math.floorDiv(totalAgentSearchTime + totalRemainTime, (totalAssignments + emptyAgents.size())) + " seconds \n");
				sb.append("average resource wait time: " + Math.floorDiv(totalResourceWaitTime, totalResources) + " seconds \n");
				sb.append("resource expiration percentage: " + Math.floorDiv(expiredResources * 100, totalResources) + "%\n");
				sb.append("\n");
				sb.append("average agent cruise time: " + Math.floorDiv(totalAgentCruiseTime, totalAssignments) + " seconds \n");
				sb.append("average agent approach time: " + Math.floorDiv(totalAgentApproachTime, totalAssignments) + " seconds \n");
				sb.append("average resource trip time: " + Math.floorDiv(totalResourceTripTime, totalAssignments) + " seconds \n");
				sb.append("total number of assignments: " + totalAssignments + "\n");
			} else {
				sb.append("No resources.\n");
			}

			System.out.print(sb.toString());
		}
	}

	/**
	 * Compares agent events
	 */
	class AgentEventComparator implements Comparator<AgentEvent> {

		/**
		 * Checks if two agentEvents are the same by checking their ids.
		 *
		 * @param a1 The first agent event
		 * @param a2 The second agent event
		 * @return returns 0 if the two agent events are the same, 1 if the id of
		 * the first agent event is bigger than the id of the second agent event,
		 * -1 otherwise
		 */
		public int compare(AgentEvent a1, AgentEvent a2) {
			if (a1.id == a2.id)
				return 0;
			else if (a1.id > a2.id)
				return 1;
			else
				return -1;
		}
	}

	/**
	 * Compares resource events
	 */
	class ResourceEventComparator implements Comparator<ResourceEvent> {
		/**
		 * Checks if two resourceEvents are the same by checking their ids.
		 *
		 * @param a1 The first resource event
		 * @param a2 The second resource event
		 * @return returns 0 if the two resource events are the same, 1 if the id of
		 * the resource event is bigger than the id of the second resource event,
		 * -1 otherwise
		 */
		public int compare(ResourceEvent a1, ResourceEvent a2) {
			if (a1.id == a2.id)
				return 0;
			else if (a1.id > a2.id)
				return 1;
			else
				return -1;
		}
	}

	/**
	 * Retrieves the total number of agents
	 *
	 * @return {@code totalAgents }
	 */
	public long totalAgents() {
		return totalAgents;
	}

	/**
	 * Retrieves the CityMap instance of this simulation
	 *
	 * @return {@code map }
	 */
	public CityMap getMap() {
		return map;
	}

	/**
	 * Sets the events of the simulation.
	 *
	 * @param events The PriorityQueue of events
	 */
	public void setEvents(PriorityQueue<Event> events) {
		this.events = events;
	}

	/**
	 * Retrieves the queue of events of the simulation.
	 *
	 * @return {@code events }
	 */
	public PriorityQueue<Event> getEvents() {
		return events;
	}

	/**
	 * Gets the empty agents in the simulation
	 *
	 * @return {@code emptyAgents }
	 */
	public TreeSet<AgentEvent> getEmptyAgents() {
		return emptyAgents;
	}

	/**
	 * Sets the empty agents in the simulation
	 *
	 * @param emptyAgents The TreeSet of agent events to set.
	 */
	public void setEmptyAgents(TreeSet<AgentEvent> emptyAgents) {
		this.emptyAgents = emptyAgents;
	}

	/**
	 * Make an agent copy of locationOnRoad so that an agent cannot modify the attributes of the road.
	 *
	 * @param locationOnRoad the location to make a copy for
	 * @return an agent copy of the location
	 */
	public LocationOnRoad agentCopy(LocationOnRoad locationOnRoad) {
		Intersection from = mapForAgents.intersections().get(locationOnRoad.road.from.id);
		Intersection to = mapForAgents.intersections().get(locationOnRoad.road.to.id);
		Road roadAgentCopy = from.roadsMapFrom.get(to);
		LocationOnRoad locationOnRoadAgentCopy = new LocationOnRoad(roadAgentCopy, locationOnRoad.travelTimeFromStartIntersection);
		return locationOnRoadAgentCopy;
	}
}
