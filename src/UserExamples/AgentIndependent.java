package UserExamples;

import COMSETsystem.*;
import CustomDataParsing.RoadClusterParser;

import java.io.*;
import java.util.*;

public class AgentIndependent extends BaseAgent {
    static double[] attract;

    static EpsilonGreedyTools epsTool;
    static RoadClusterParser rcp;
    static Map<Integer, Cluster> clusters;
    static ClusterTool ct;

    boolean failed = true;
    boolean start = true;
    static String searchLog;
    int timer;
    int prev_cluster;
    long prev_time;

    int planned_destination_cluster;

    static int [] replan_count;

    static int [] design_to_go;
    static int [] pickup_as_designed;
    static int [] pickuped_by_others;

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
        prev_time = -1;
        int totalClusterNumber = 0;
        if ( epsTool==null) epsTool = new EpsilonGreedyTools(0.4); //dummy, won't use.


        if (rcp==null){
            rcp = new RoadClusterParser(map);
            clusters = rcp.parseRoadClusterFile();
            totalClusterNumber = clusters.size();
        }
        if (ct==null){
            ct = new ClusterTool(map, clusters, epsTool);

        }
        if (attract==null){
            attract = new double[totalClusterNumber];
            for (int i = 0;i<totalClusterNumber;i++){
                attract[i] = clusters.get(i).attr;
            }
            String properties = "";
            StringBuilder sb = new StringBuilder();
            try {

                Properties prop = new Properties();
                prop.load(new FileInputStream("etc/config.properties"));

                String fleetSize = prop.getProperty("comset.number_of_agents").trim();
                sb.append(fleetSize);
                String method = "Independent";
                sb.append("_");
                sb.append(method);
                String road_cluster_file = prop.getProperty("cluster.road_cluster_file").trim();
                String substr = road_cluster_file.substring(12,road_cluster_file.length()-4);
                sb.append("_");
                sb.append(substr);
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
            properties = properties + sb.toString();
            searchLog = "Search_"+properties+"_original_dijkstra.csv";
            replan_count = new int[clusters.size()];
            design_to_go = new int[clusters.size()];
            pickup_as_designed = new int[clusters.size()];
            pickuped_by_others = new int[clusters.size()];
        }


    }
    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {
        if(id%10000==0){
            System.out.println("Replan");
            System.out.println(Arrays.toString(replan_count));
            System.out.println("Design to go");
            System.out.println(Arrays.toString(design_to_go));
            System.out.println("pickup as designed");
            System.out.println(Arrays.toString(pickup_as_designed));
            System.out.println("Pickuped by others");
            System.out.println(Arrays.toString(pickuped_by_others));
        }
        Cluster c = ct.getClusterFromRoad(currentLocation.road);
        if (failed){
            if(start){
                start = false;
                try{
                    PrintWriter pw = new PrintWriter(searchLog);
                    pw.close();
                }catch (IOException ioe){
                    ioe.printStackTrace();

                }
            }else{
                //log expriation events - my modification
                try{
                    FileWriter fw = new FileWriter(searchLog, true); //Set true for append mode
                    PrintWriter pw = new PrintWriter(fw);
                    pw.write(Long.toString(currentLocation.road.id));
                    pw.write(",");
                    pw.close();
                }catch (FileNotFoundException fnfe){
                    fnfe.printStackTrace();
                }catch (IOException ioe){
                    ioe.printStackTrace();
                }
            }
        }

        HashMap<Integer,Double> options = new HashMap<>();
        for (int i:c.nbs){

            options.put(i,attract[i]);
        }
        if (!failed){
            options.put(c.id,attract[c.id]);
        }
        Integer target = epsTool.getRandomWithWeight(options);
        Cluster dest = clusters.get(target);

        HashSet<Road> roads= dest.roads;
        if(roads==null || roads.size()==0){
            //safety concern only
            roads = clusters.get(0).roads;

        }
        Intersection sourceIntersection = currentLocation.road.to;


        Road destinationRoad = epsTool.getRandomFromSet(roads);
        Intersection destinationIntersection = destinationRoad.to;

        int destination_cluster_id = ct.getClusterFromRoad(destinationRoad).id;
        design_to_go[destination_cluster_id] += 1;
        planned_destination_cluster = destination_cluster_id;

        if (sourceIntersection == destinationIntersection) {
            // destination cannot be the source
            // if destination is the source, choose a neighbor to be the destination
            Road[] roadsFrom = sourceIntersection.roadsMapFrom.values().toArray(new Road[sourceIntersection.roadsMapFrom.values().size()]);
            destinationIntersection = roadsFrom[0].to;
        }

        route = map.shortestTravelTimePath(
                sourceIntersection, destinationIntersection);
        timer = 0;
        prev_time = currentTime;
        failed = true;
        route.poll();
    }

    @Override
    public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {
        timer += currentTime - prev_time;
        prev_time = currentTime;

        Cluster c = ct.getClusterFromRoad(currentLocation.road);
        int cluster_id = c.id;
        if (cluster_id!=prev_cluster){
            prev_cluster = cluster_id;
            timer = 0;
        }



        if (timer > c.getSearchTime() && route.size()>=1){
            //System.out.println("Triggered reset in cluster "+c.id);
            replan_count[c.id] +=1;
            Intersection sourceIntersection = route.get(0);
            route.clear();

            HashMap<Integer,Double> options = new HashMap<>();
            for (int i = 0;i<attract.length;i++){
                if (!c.nbs.contains(i)) {
                    options.put(i, attract[i]);
                }
            }

            Integer target = epsTool.getRandomWithWeight(options);
            Cluster dest = clusters.get(target);

            HashSet<Road> roads= dest.roads;
            if(roads==null || roads.size()==0){
                roads = clusters.get(0).roads;
                dest = clusters.get(0);

            }
            design_to_go[dest.id]+=1;
            planned_destination_cluster = dest.id;
            Road destinationRoad = epsTool.getRandomFromSet(roads);
            Intersection destinationIntersection = destinationRoad.to;

            if (sourceIntersection == destinationIntersection) {
                Road[] roadsFrom = sourceIntersection.roadsMapFrom.values().toArray(new Road[sourceIntersection.roadsMapFrom.values().size()]);
                destinationIntersection = roadsFrom[0].to;
            }
            route = map.shortestTravelTimePath(
                    sourceIntersection, destinationIntersection);
            timer = 0;
            //System.out.println("Finished");
            failed = true;
            return route.poll();
        }


        //standard update nextIntersection
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
        prev_time = -1;
        int actural_destination_cluster_id = ct.getClusterFromRoad(resourcePickupLocation.road).id;
        if (actural_destination_cluster_id==planned_destination_cluster){
            pickup_as_designed[actural_destination_cluster_id]+=1;
        }else{
            pickuped_by_others[actural_destination_cluster_id]+=1;
        }

        route.clear();
        failed = false;

    }
}
