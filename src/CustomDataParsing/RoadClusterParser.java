package CustomDataParsing;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import COMSETsystem.Road;
import UserExamples.Cluster;

public class RoadClusterParser{

    private static CityMap map;
    public Map<Integer, Cluster> knownClusters;
    public HashMap<Long, Road> roadIdLookup;
    public HashMap<Road, Cluster> roadClusterLookup;
    public HashMap<Intersection, Cluster> intersectionClusterLookup;
    private HashSet<Integer> clusterIdsThatHasAtLeastOneIntersection;
    public RoadClusterParser(CityMap cmap) {
        map = cmap;
        roadIdLookup = new HashMap<>();
        knownClusters = new TreeMap<>();
        roadClusterLookup = new HashMap<>();
        intersectionClusterLookup = new HashMap<>();
        clusterIdsThatHasAtLeastOneIntersection = new HashSet<>();
        for(Road r: map.roads()){
            roadIdLookup.put(r.id, r);
        }
    }
    public Map<Integer, Cluster> parseRoadClusterFile(){
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("etc/config.properties"));

            String clusterAttrFile = prop.getProperty("cluster.attr").trim();
            Scanner sc = new Scanner(new File(clusterAttrFile));
            sc.useDelimiter("[,\n]");    //scanner will skip over "," and "\n" found in file
            sc.nextLine(); // skip the header
            while (sc.hasNext()) {
                String str = sc.nextLine();

                String[] s = str.split(",");
                int cluster_id = Integer.parseInt(s[0]);
                double total_cluster_PR = Double.parseDouble(s[1]);
                double total_cluster_tt = Double.parseDouble(s[2]);
                int total_road_count = Integer.parseInt(s[3]);
                double cluster_attractiveness = Double.parseDouble(s[4]);
                double normalised_attractiveness = Double.parseDouble(s[5]);
                knownClusters.put(cluster_id, new Cluster(cluster_id, normalised_attractiveness, total_cluster_tt, 5 / cluster_attractiveness));
            }

            String clusterNbFile = prop.getProperty("cluster.cluster_nb_file").trim();
            sc = new Scanner(new File(clusterNbFile));
            sc.useDelimiter("[,\n]");    //scanner will skip over "," and "\n" found in file
            sc.nextLine(); // skip the header
            while (sc.hasNext()) {
                String str = sc.nextLine();

                String[] s = str.split(",");
                Integer cluster_id = Integer.parseInt(s[0]);
                String[] nbs = s[1].split(" ");
//                System.out.println(nbs);
//                System.out.println(cluster_id);
                for (String nb:nbs){
                    knownClusters.get(cluster_id).nbs.add(Integer.parseInt(nb));
                }
            }

            String roadClusterFile = prop.getProperty("cluster.road_cluster_file").trim();
            sc = new Scanner(new File(roadClusterFile));
            sc.useDelimiter("[,\n]");    //scanner will skip over "," and "\n" found in file
            sc.nextLine(); // skip the header
            while (sc.hasNext()) {
                String str = sc.nextLine();

                String[] s = str.split(",");
                int roadId = Integer.parseInt(s[0]);
                int clusterId = Integer.parseInt(s[1]);
                addRoadEntry(roadId, clusterId);
            }

            sc.close();
            computeIntersectionClusterInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int totalItx = 0;
        int totalRoad = 0;
        for (Cluster c : knownClusters.values()) {
            totalItx += c.intersections.size();
            totalRoad += c.roads.size();
            System.out.println("c" + c.id + " has " + c.intersections.size() + " intersections, "
                    + c.roads.size() + " roads, and attractiveness: " + c.getClusterAttractiveness());
        }
//        System.out.println("total intersection sizes " + totalItx);
//        System.out.println("total intersection sizes confirm " + intersectionClusterLookup.keySet().size());
//        System.out.println("total road sizes " + totalRoad);
//        System.out.println("total road sizes confirm " + roadClusterLookup.keySet().size());

        return knownClusters;
    }

    public HashMap<Cluster, HashMap<Integer, Integer>> parseRoadPickupFile() {

        HashMap<Cluster, HashMap<Integer, Integer>> outerMap = new HashMap<>();
        try {

            Properties prop = new Properties();
            prop.load(new FileInputStream("etc/config.properties"));
            String roadPickupFile = prop.getProperty("cluster.road_pickup_file").trim();


            ArrayList<Integer> innerMapTimeKeys = new ArrayList<>();
            Scanner sc = new Scanner(new File(roadPickupFile));
            sc.useDelimiter(",|\n");    //scanner will skip over "," and "\n" found in file

            String headerLine = sc.nextLine();

            //process header
            for(String header: headerLine.split(",")){
                if (header.matches("[0-9][0-9]:[0-9][0-9]")){
                    Integer timeKey = Integer.parseInt(header.substring(3))+Integer.parseInt(header.substring(0,2))*60;
                    innerMapTimeKeys.add(timeKey);
                }
            }

            //process content
            int invalidCounter = 0;
            while (sc.hasNext()) {
                String str = sc.nextLine();
                String[] s = str.split(",");
                Long roadId = Long.parseLong(s[0]);

                if(roadIdLookup.get(roadId)==null){
                    invalidCounter++;
                    continue;
                }
                Road r = roadIdLookup.get(roadId);
                Double length = r.length; //potential use to normalize pickup data.
                Cluster c = roadClusterLookup.get(r);

                if(!outerMap.containsKey(c)){
                    HashMap<Integer, Integer> innerMap = new HashMap<>();
                    for(int i = 1;i<s.length;i++) {
                        innerMap.put(innerMapTimeKeys.get(i - 1), Integer.parseInt(s[i]));

                    }
                    outerMap.put(c, innerMap);
                }else{
                    HashMap<Integer, Integer> innerMap = outerMap.get(c);
                    for(int i = 1;i<s.length;i++) {
                        innerMap.put(innerMapTimeKeys.get(i - 1), innerMap.get(innerMapTimeKeys.get(i - 1))+Integer.parseInt(s[i]));

                    }
                }
            }
            System.out.println("Number of invalid: "+invalidCounter);
            sc.close();


        } catch (Exception e) {

            e.printStackTrace();
        }
        //attach information to c pickup - source of reward.
//        for(Cluster c:knownClusters.values()){
//            c.pickupTimeMap = outerMap.get(c);
//        }
        return outerMap;
    }


    public void addRoadEntry(long roadId, int clusterID){
        Cluster c;
        if (knownClusters.containsKey(clusterID)) {
            c = knownClusters.get(clusterID);
        } else {
            c = new Cluster(clusterID, 0, 0, 1000);
            knownClusters.put(clusterID, c);
        }
        Road r = roadIdLookup.get(roadId);
        if (r == null) return;
        c.addRoad(r);

        c.distance += r.length;
        c.totalClusterSpeed += r.length / r.travelTime;
        roadClusterLookup.put(roadIdLookup.get(roadId), c);
    }

    public void computeIntersectionClusterInfo() {
        for(Intersection itx: map.intersections().values()){
            calculateIntersectionCluster(itx);
        }
        for(Cluster c : knownClusters.values()){
            if (!clusterIdsThatHasAtLeastOneIntersection.contains(c.id)){
                fillEmptyClusters(c);
                clusterIdsThatHasAtLeastOneIntersection.add(c.id);
            }
        }
    }

    public void fillEmptyClusters(Cluster c){
        int i = 0;

        for (Road r: c.roads){

            Intersection start = r.to;
            Intersection end = r.from;
            Cluster sc = intersectionClusterLookup.get(start);
            Cluster ec = intersectionClusterLookup.get(end);
            if (sc.intersections.size()>1){
                sc.intersections.remove(start);
                c.intersections.add(start);
                intersectionClusterLookup.put(start, c);
                i++;
            }
            if (ec.intersections.size()>1){
                ec.intersections.remove(end);
                c.intersections.add(end);
                intersectionClusterLookup.put(end, c);
                i++;
            }
            if (i>=2) break;
        }

    }


    public void calculateIntersectionCluster(Intersection itx){
        if (intersectionClusterLookup.containsKey(itx)) return;

        int clusterId = 0;
        HashSet<Road> neighbourRoads = new HashSet<>(itx.getRoadsFrom());
        neighbourRoads.addAll(itx.getRoadsTo());
        HashMap<Integer, Integer> majorityCount = new HashMap<>();
        int maxVal = 0;

        for(Road r: neighbourRoads){
            Integer roadClusterId = 0;
            Cluster c= roadClusterLookup.get(r);
            if(c!=null) roadClusterId = c.id;
            if(roadClusterId==0) continue;
            majorityCount.merge(roadClusterId, 1, (a, b) -> a + b);
            //if the majority road is greatest so far, we follow the majority road.
            if (majorityCount.get(roadClusterId) > maxVal ){
                maxVal = majorityCount.get(roadClusterId);
                clusterId = roadClusterId;
            }
            //to maintain minority cluster numbers - this step makes sure each cluster has at least one interection, if possible.
            if (majorityCount.get(roadClusterId)==maxVal && !clusterIdsThatHasAtLeastOneIntersection.contains(roadClusterId)){
                maxVal = majorityCount.get(roadClusterId);
                clusterId = roadClusterId;
            }

        }


        clusterIdsThatHasAtLeastOneIntersection.add(clusterId);
        Cluster c = knownClusters.get(clusterId);
        c.addIntersction(itx); //attach intersection to this cluster
        intersectionClusterLookup.put(itx, c); //attach above information for intersection-cluster lookup.

    }
}
