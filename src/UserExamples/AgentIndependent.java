package UserExamples;

import COMSETsystem.*;
import CustomDataParsing.RoadClusterParser;

import java.io.*;
import java.util.*;

public class AgentIndependent extends BaseAgent {
    static double[] attract;

    static ChoiceModel choiceModel;
    static RoadClusterParser rcp;
    static Map<Integer, Cluster> clusters;
    static ClusterTool ct;

    boolean failed = false; // flag value indicating if the last agent search failed
//    boolean start = true;
//    static String searchLog;
    int timer; // create a timer for agents to record their search time, clears upon entering a new cluster
    int prev_cluster;
    long prevTime;

//    int planned_destination_cluster;

//    static int [] replan_count;
//    static int [] design_to_go;
//    static int [] pickup_as_designed;
//    static int [] pickuped_by_others;

    LinkedList<Intersection> route = new LinkedList<>();

//    public double[] generateEpsTracker(){
//        String configFile = "etc/config.properties";
//        int num = 0;
//        try {
//            Properties prop = new Properties();
//            prop.load(new FileInputStream(configFile));
//
//            String filename = prop.getProperty("comset.cluster_file").trim();
//
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//        return new double[num];
//    }

    public AgentIndependent(long id, CityMap map) {
        super(id, map);
        timer = 0;
        prev_cluster = -1;
        prevTime = -1;
        int totalClusterNumber = 0;
        if (choiceModel == null) choiceModel = new ChoiceModel(0.4); //dummy, won't use.

        if (rcp == null){
            rcp = new RoadClusterParser(map);
            clusters = rcp.parseRoadClusterFile();
            totalClusterNumber = clusters.size();
        }
        if (ct == null){
            ct = new ClusterTool(map, clusters, choiceModel);

        }
        if (attract == null){
            attract = new double[totalClusterNumber];
            for (int i = 0; i < totalClusterNumber; i++){
                attract[i] = clusters.get(i).attr;
            }
//            String properties = "";
//            StringBuilder sb = new StringBuilder();
//            try {
//                Properties prop = new Properties();
//                prop.load(new FileInputStream("etc/config.properties"));
//
//                String fleetSize = prop.getProperty("comset.number_of_agents").trim();
//                sb.append(fleetSize);
//                String method = "RandomDest";
//                if (method.equals("UserExamples.AgentRandomDestination")){
//                    method = "RandomDest";
//                }else{
//                    method = "Independent";
//                }
//
//                sb.append("_");
//                sb.append(method);
//                String road_cluster_file = prop.getProperty("cluster.road_cluster_file").trim();
//                String substr = road_cluster_file.substring(12, road_cluster_file.length() - 4);
//                sb.append("_");
//                sb.append(substr);
//            }catch (IOException ioe){
//                ioe.printStackTrace();
//            }
//            properties = properties + sb.toString();
//            searchLog = "Search_" + properties + "_original_dijkstra.csv";
//            replan_count = new int[clusters.size()];
//            design_to_go = new int[clusters.size()];
//            pickup_as_designed = new int[clusters.size()];
//            pickuped_by_others = new int[clusters.size()];
        }


    }
    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {
//        if(id % 10000 == 0){
//            System.out.println("Replan");
//            System.out.println(Arrays.toString(replan_count));
//            System.out.println("Design to go");
//            System.out.println(Arrays.toString(design_to_go));
//            System.out.println("pickup as designed");
//            System.out.println(Arrays.toString(pickup_as_designed));
//            System.out.println("Pickuped by others");
//            System.out.println(Arrays.toString(pickuped_by_others));
//        }
        Cluster c = ct.getClusterFromRoad(currentLocation.road);

//      this part activates another planning when the last search fails
//        if (failed){
//            if(start){
//                start = false;
//                try{
//                    PrintWriter pw = new PrintWriter(searchLog);
//                    pw.close();
//                }catch (IOException ioe){
//                    ioe.printStackTrace();
//                }
//            } else {
                //log expriation events - my modification
//                try{
//                    FileWriter fw = new FileWriter(searchLog, true); //Set true for append mode
//                    PrintWriter pw = new PrintWriter(fw);
//                    pw.write(Long.toString(currentLocation.road.id));
//                    pw.write(",");
//                    pw.close();
//                } catch (FileNotFoundException fnfe){
//                    fnfe.printStackTrace();
//                } catch (IOException ioe){
//                    ioe.printStackTrace();
//                }
//            }
//        }

        HashMap<Integer, Double> options = new HashMap<>();
        for (int i : c.nbs){
            options.put(i, Math.exp(attract[i]));
        }

        if (!failed){
            options.put(c.id, Math.exp(attract[c.id]));
        }
//        for (Cluster i : clusters.values()) {
//            options.put(i.id, attract[i.id]);
//        }

        int target = choiceModel.choiceByProbability(options);
        Cluster dest = clusters.get(target);

//        // can manually overwrite the destination cluster here.
//        dest = clusters.get(3);

        // randomly select a road in the destination cluster as the destination road
        HashSet<Road> roads = dest.roads;
        if(roads == null || roads.size() == 0){
            //safety concern only
            roads = clusters.get(0).roads;
        }
        Road destinationRoad = choiceModel.getRandomFromSet(roads);

//        // apply another Logit choice model at the road level within the destination cluster
//        HashMap<Integer, Double> road_options = new HashMap<>();
//        for (Road r : dest.roads) {
//            road_options.put((int) r.id, Math.exp(1.0 / r.rating));
//        }
//        Road destinationRoad = rcp.roadIdLookup.get((long) choiceModel.choiceByProbability(road_options));

        // the agent is dispatched to the end (intersection) of the destination road
        Intersection sourceIntersection = currentLocation.road.to;
        Intersection destinationIntersection = destinationRoad.to;

//        int destination_cluster_id = ct.getClusterFromRoad(destinationRoad).id;
        // design_to_go[destination_cluster_id] += 1;
//        planned_destination_cluster = destination_cluster_id;

        if (sourceIntersection == destinationIntersection) {
            // destination cannot be the source
            // if destination is the source, choose a neighbor to be the destination
            Road[] roadsFrom = sourceIntersection.roadsMapFrom.values().toArray(new Road[0]);
            destinationIntersection = roadsFrom[0].to;
        }

        route = map.bestCrusiseTimePath(sourceIntersection, destinationIntersection);
//        route = map.shortestTravelTimePath(sourceIntersection, destinationIntersection);

        timer = 0;
        prevTime = currentTime;
        failed = true;
//        canpickup = false; // modify "canpickup" here and in the "nextIntersection" method to limit pick-up locations
        route.poll();
    }

    @Override
    public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {

        timer += currentTime - prevTime;
//        if(this.id % 10 == 0){
//            System.out.print(timer + " ");
//        }
        prevTime = currentTime;

        Cluster c = ct.getClusterFromRoad(currentLocation.road);

//        // activate "canpickup" when the agent enters the destination cluster
//        if (c.id == planned_destination_cluster) canpickup = true;

        if (c.id != prev_cluster){
            prev_cluster = c.id;
            timer = 0; // "timer" clears when agent moves to a new cluster
        }

//        // choose another destination if the search time limit is exceeded
//        if (route.size() >= 1) { // && timer > c.getSearchTime()){
//            //System.out.println("Triggered reset in cluster " + c.id);
//            replan_count[c.id] += 1;
//            Intersection sourceIntersection = route.get(0);
//            route.clear();
//
//            HashMap<Integer,Double> options = new HashMap<>();
//
//            for (int i = 0; i < attract.length; i++){
//                if (i != c.id && !c.nbs.contains(i)) {
//                    options.put(i, Math.exp(attract[i]));
//                }
//            }
//
//            Integer target = epsTool.destinationChoice(options);
//            Cluster dest = clusters.get(target);
//
//            HashSet<Road> roads = dest.roads;
//            if(roads == null || roads.size() == 0){
//                roads = clusters.get(0).roads;
//                dest = clusters.get(0);
//
//            }
//            design_to_go[dest.id] += 1;
//            planned_destination_cluster = dest.id;
//            Road destinationRoad = epsTool.getRandomFromSet(roads);
//            Intersection destinationIntersection = destinationRoad.to;
//
//            if (sourceIntersection == destinationIntersection) {
//                Road[] roadsFrom = sourceIntersection.roadsMapFrom.values().toArray(new Road[sourceIntersection.roadsMapFrom.values().size()]);
//                destinationIntersection = roadsFrom[0].to;
//            }
//            route = map.shortestTravelTimePath(
//                    sourceIntersection, destinationIntersection);
//            timer = -600;
//            //System.out.println("Finished");
//            failed = true;
//            return route.poll();
//        }


        // Original nextIntersection
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
        timer = 0;
        prevTime = -1;
//        int actual_destination_cluster_id = ct.getClusterFromRoad(resourcePickupLocation.road).id;
//        if (actual_destination_cluster_id == planned_destination_cluster){
//            pickup_as_designed[actual_destination_cluster_id] += 1;
//        }else{
//            pickuped_by_others[actual_destination_cluster_id] += 1;
//        }

        route.clear();
        failed = false;
    }
}
