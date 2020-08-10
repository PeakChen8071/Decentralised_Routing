package UserExamples;

import COMSETsystem.*;
import CustomDataParsing.RoadClusterParser;


import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Random destination search algorithm:
 * After dropping off a resource, the agent chooses a random intersection on the map as the destination,
 * and follows the shortest travel time path to go to the destination. When the destination is reached,
 * the agent chooses another random intersection to go to. This procedure is repeated until the agent
 * is assigned to a resource.
 */
public class AgentRandomDestination extends BaseAgent {

	// search route stored as a list of intersections.
	LinkedList<Intersection> route = new LinkedList<Intersection>();

	// random number generator
	Random rnd;

	boolean assigned = false;
	int cluster_id;
	long roadId;
	// static info
	static DummyDataModel dataModel = null;
	static CityMap map;
	static ZoneId zoneId;
	static RoadClusterParser rcp;
	static Map<Integer, Cluster> clusters;
	static ChoiceModel epsTool;
	static ClusterTool ct;
	static int timeslice;
	static int prevTime;
	static List<Integer> sharedTime;
	static Map<Long, AgentRandomDestination> agents;
	static Simulator simulator;
	public static HashMap<Long, Integer> roadClusterLookup;
	public static HashMap<Long, Integer> intersectionClusterLookup;
	public static boolean output = true;

	public AgentRandomDestination(long id, CityMap map) {
		super(id, map);
		this.simulator = map.simulator;
		rnd = new Random(id);
		if (epsTool == null) epsTool = new ChoiceModel(0.4); //dummy, won't use.

		if (rcp == null) {
			this.timeslice = 5;
			rcp = new RoadClusterParser(map);
			clusters = rcp.parseRoadClusterFile();

			this.roadClusterLookup = new HashMap<>();
			this.intersectionClusterLookup = new HashMap<>();
			for (Cluster c : clusters.values()) {
				for (Road r : c.roads) {
					roadClusterLookup.put(r.id, c.id);
				}
				for (Intersection i : c.intersections) {
					intersectionClusterLookup.put(i.id, c.id);

				}
			}

		}
		if (agents== null){
			agents = new HashMap<>();
		}
		agents.put(id, this);
		if (dataModel == null) {
			this.map = map;
			zoneId = this.map.computeZoneId();
			dataModel = new DummyDataModel(map);
			sharedTime = new ArrayList<>();
			int start = 60 * 7;
			int end = 60 * 10;
			for (int i = start; i < end; i += 5) {
				sharedTime.add(i);
			}

		}
	}
	/**
	 * Choose a random intersection of the map as the destination and set the
	 * shortest travel time path as the search route.
	 *
	 * IMPORTANT: The first intersection on the resulted search route must not be the
	 * end intersection of the current road, i.e., it must not be that
	 * route.get(0) == currentLocation.road.to.
	 */

	@Override
	public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {
		assigned = false;
		route.clear();
		Intersection sourceIntersection = currentLocation.road.to;
		int destinationIndex = rnd.nextInt(map.intersections().size());
		Intersection[] intersectionArray = map.intersections().values().toArray(new Intersection[map.intersections().size()]);
		Intersection destinationIntersection = intersectionArray[destinationIndex];
		if (destinationIntersection == sourceIntersection) {
			Road[] roadsFrom = sourceIntersection.roadsMapFrom.values().toArray(new Road[sourceIntersection.roadsMapFrom.values().size()]);
			destinationIntersection = roadsFrom[0].to;
		}

		route = map.bestCrusiseTimePath(sourceIntersection, destinationIntersection);
//		route = map.shortestTravelTimePath(sourceIntersection, destinationIntersection);
		route.poll(); // Ensure that route.get(0) != currentLocation.road.to.
	}

	/**
	 * This method polls the first intersection in the current route and returns this intersection.
	 *
	 * This method is a callback method which is called when the agent reaches an intersection. The Simulator
	 * will move the agent to the returned intersection and then call this method again, and so on.
	 * This is how a planned route (in this case randomly planned) is executed by the Simulator.
	 *
	 * @return Intersection that the Agent is going to move to.
	 */

	@Override
	public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {
		this.roadId = currentLocation.road.id;
		this.cluster_id = roadClusterLookup.get(this.roadId);
		int curTime = mapTime(currentTime);
		if (sharedTime.size() == 1 && curTime > sharedTime.get(0) && output){
//			try {
//				PrintWriter pw = new PrintWriter("MeetingFunction.csv");
//				pw.println("cluster,time,pickup,emptyAgent,waitingResource");
//				for(int i = 0; i < clusters.size(); i++) {
//					int start = 60 * 7;
//					int end = 60 * 10;
//					for (int j = start; j < end; j += 5) {
//						pw.print(i);
//						pw.print(",");
//						pw.print(j);
//						pw.print(",");
//						pw.print(clusters.get(i).timePickup.get(j));
//						pw.print(",");
//						pw.print(clusters.get(i).timeEmptyAgent.get(j));
//						pw.print(",");
//						pw.print(clusters.get(i).timeResource.get(j));
//						pw.print("\n");
//					}
//
//				}
//				pw.close();
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}

			output= false;
		} else if (sharedTime.size() > 0 && curTime > sharedTime.get(0)) {
//			System.out.println("Time: "+sharedTime.get(0));
			Integer timelog = sharedTime.remove(0);
			int occupied = 0;
			for (AgentRandomDestination a:agents.values()){
				if (a.assigned) {
					occupied ++;
					continue;
				}

				Cluster c = clusters.get(roadClusterLookup.get(a.roadId));
				c.timeEmptyAgent.put(timelog, c.timeEmptyAgent.get(timelog) + 1);
			}


			for (ResourceEvent re: simulator.waitingResources){
				LocationOnRoad lor = re.pickupLoc;

				Cluster c = clusters.get(0);
				try{
					c = clusters.get(roadClusterLookup.get(lor.road.id));
				}catch(Exception e){
//					e.printStackTrace();
					System.out.println("Error cant find " + lor.road.id);

				}

				c.timeResource.put(timelog, c.timeResource.get(timelog) + 1);
			}
		}
		if (route.size() != 0) {
			// Route is not empty, take the next intersection.
			Intersection nextIntersection = route.poll();
			return nextIntersection;
		} else {
			// Finished the planned route. Plan a new route.
			planSearchRoute(currentLocation, currentTime);
			return route.poll();
		}
	}

	/**
	 * A dummy implementation of the assignedTo callback function which does nothing but clearing the current route.
	 * assignedTo is called when the agent is assigned to a resource.
	 */

	@Override
	public void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePikcupLocation, LocationOnRoad resourceDropoffLocation) {
		// Clear the current route.
		assigned = true;
//		Cluster c = ct.getClusterFromRoad(resourcePikcupLocation.road);
		Cluster c = clusters.get(roadClusterLookup.get(resourcePikcupLocation.road.id));
		Integer timelog = mapTime(currentTime);

		c.timePickup.put(timelog, c.timePickup.getOrDefault(timelog, 0)+1);

		route.clear();

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Agent " + this.id + " assigned to resource " + resourceId);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "currentLocation = " + currentLocation);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "currentTime = " + currentTime);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "resourcePickupLocation = " + resourcePikcupLocation);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "resourceDropoffLocation = " + resourceDropoffLocation);
	}






    public static int mapTime(long time){
        Instant i = Instant.ofEpochSecond(time);

        ZonedDateTime z = ZonedDateTime.ofInstant(i, zoneId);

        int hour = z.getHour();
        int minute = z.getMinute();
        int timeKey = hour * 60 + mapMinute(minute);

        return timeKey;
    }
    public static int mapTimeAccurate(long time){
        Instant i = Instant.ofEpochSecond(time);
        ZonedDateTime z = ZonedDateTime.ofInstant(i, zoneId);

        int hour = z.getHour();
        int minute = z.getMinute();
        int timeKey = hour*60 + minute;

        return timeKey;
    }
	private static int mapMinute(int minute){
		if (minute > 60 || minute < 0){
			System.out.println("Error in minute conversion");
			return 0;
		}
		return minute - (minute%timeslice);
	}

}