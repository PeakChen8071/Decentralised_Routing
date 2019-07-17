package UserExamples;

import COMSETsystem.*;
import CustomDataParsing.Cluster;
import CustomDataParsing.RoadClusterParser;

import java.util.*;

public class AgentQTableDestination extends BaseAgent {

    LinkedList<Intersection> route = new LinkedList<Intersection>();

    // random number generator
    Random rnd;
    static int counter = 0;
    // a static singleton object of a data model, shared by all agents
    static RoadClusterParser rcp = null;
    static Cluster cluster;
    static Set<Integer> clusterIDs;
    HashMap<Integer, HashMap<Integer, Double>> qTable;
    static EpsilonGreedyTools epsTool;
    Integer currentClusterId = -1;

    //qtable:
    /*     c1  c2  c3  < inner key
     * c1 r11 r12 r13  < inner map value
     * c2 r21 r22 r23
     * c3 r31 r32 r33
     * ^
     * outer key
     */

    /**
     * BaseAgent constructor.
     *
     * @param id  An id that is unique among all agents and resources
     * @param map The map
     */
    public AgentQTableDestination(long id, CityMap map) {
        super(id, map);
        if (rcp==null){
            rcp = new RoadClusterParser("trial_data/data_20160601.csv", map);
            cluster = rcp.parse();
            cluster.calculateInitialReward();
        }
        if (epsTool==null){
            epsTool = new EpsilonGreedyTools(0.5);
        }
        qTable = new HashMap<>();
        clusterIDs = cluster.getClusterIds();
        for(Integer i: clusterIDs){
            HashMap<Integer, Double> qInnerTable = new HashMap<Integer, Double>();
            for(Integer j:clusterIDs){
                qInnerTable.put(j, cluster.getClusterInitialReward(j));
            }
            qTable.put(i, qInnerTable);
        }
        if (counter==0){
            printQtablePretty();
        }

        counter++;

    }
    private void printQtablePretty(){
        StringBuilder sb = new StringBuilder();
        for (int i:clusterIDs){
            sb.append("\t");
            sb.append("c"+i);
            sb.append("\t");
        }
        sb.append("\n");
        for (int i:clusterIDs){
            sb.append("c"+i);
            sb.append("\t");
            HashMap<Integer, Double> innerTable = qTable.get(i);
            for(int j:innerTable.keySet()){
                sb.append(String.format("%.3f", innerTable.get(j)));
                sb.append("\t");
            }
            sb.append("\n");
        }
        System.out.println(sb);

    }

    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {
        route.clear();
        Long c = currentLocation.road.id;
        currentClusterId = cluster.getClusterID(c);
        HashMap<Integer, Double> probTable = epsTool.getProbabilityTable(qTable.get(currentClusterId));
        Integer choosenClusterId = epsTool.getRandomWithWeight(probTable);

        HashSet<Intersection> itxs = cluster.getClusterIntersections(choosenClusterId);
        if(itxs==null){
            itxs = cluster.getClusterIntersections(-1);
            choosenClusterId = -1;
        }
        Intersection sourceIntersection = currentLocation.road.to;

        System.out.println("Currently Cluster: "+cluster.getIntersectionCluster(sourceIntersection)+", destination Cluster: "+choosenClusterId);
        Intersection destinationIntersection = epsTool.getRandomFromSet(itxs);


        if (sourceIntersection == destinationIntersection) {
            // destination cannot be the source
            // if destination is the source, choose a neighbor to be the destination
            Road[] roadsFrom = sourceIntersection.roadsMapFrom.values().toArray(new Road[sourceIntersection.roadsMapFrom.values().size()]);
            destinationIntersection = roadsFrom[0].to;
        }
        route = map.shortestTravelTimePath(sourceIntersection, destinationIntersection);

        route.poll(); // Ensure that route.get(0) != currentLocation.road.to.
    }


    @Override
    public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {
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

    @Override
    public void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePickupLocation, LocationOnRoad resourceDropoffLocation) {
        route.clear();
        Long r = resourcePickupLocation.road.id;
        Integer pickupClusterId = cluster.getClusterID(r);
        qTable.get(currentClusterId).put(pickupClusterId ,
                                         qTable.get(currentClusterId).get(pickupClusterId) + 0.01);


    }
}
