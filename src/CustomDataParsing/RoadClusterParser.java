package CustomDataParsing;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import COMSETsystem.Link;
import COMSETsystem.Road;
import UserExamples.Cluster;

public class RoadClusterParser{

    private static CityMap map;
    public HashMap<Integer, Cluster> knownClusters;
    public HashMap<Long, Road> roadIdLookup;
    public HashMap<Road, Cluster> roadClusterLookup;

    public HashMap<Intersection, Cluster> intersectionClusterLookup;
    private HashSet<Integer> clusterIdsThatHasAtLeastOneIntersection;
    public RoadClusterParser(CityMap cmap) {
        map = cmap;
        roadIdLookup = new HashMap<>();
        knownClusters = new LinkedHashMap<>();
        roadClusterLookup = new HashMap<>();
        intersectionClusterLookup = new HashMap<>();
        clusterIdsThatHasAtLeastOneIntersection = new HashSet<>();
        for(Road r: map.roads()){
            roadIdLookup.put(r.id, r);
        }
    }
    public HashMap<Integer, Cluster> parseRoadClusterFile(String roadClusterFile){
        try {

            Scanner sc = new Scanner(new File(roadClusterFile));
            sc.useDelimiter(",|\n");    //scanner will skip over "," and "\n" found in file
            sc.nextLine(); // skip the header
            while (sc.hasNext()) {
                String str = sc.nextLine();

                String[] s = str.split(",");
                Long roadId = Long.parseLong(s[0]);
                Integer clusterId = Integer.parseInt(s[1])+1;
                addRoadEntry(roadId, clusterId);
            }
            sc.close();
            computeIntersectionClusterInfo();

        } catch (Exception e) {

            e.printStackTrace();
        }
        return knownClusters;
    }
    public HashMap<Cluster, HashMap<Integer, Integer>> parseRoadPickupFile(String roadPickupFile) {

        HashMap<Cluster, HashMap<Integer, Integer>> outerMap = new HashMap<>();
        try {


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
        for(Cluster c:knownClusters.values()){
            c.pickupTimeMap = outerMap.get(c);
        }
        return outerMap;
    }



    public void addRoadEntry(long roadId, int clusterID){
        Cluster c;
        if(knownClusters.containsKey(clusterID)){
            c = knownClusters.get(clusterID);

        }else{
            c = new Cluster(clusterID, map.computeZoneId());
            knownClusters.put(clusterID, c);
        }
        c.addRoad(roadIdLookup.get(roadId));
        roadClusterLookup.put(roadIdLookup.get(roadId), c);
    }

    public void computeIntersectionClusterInfo(){
        for(Intersection itx: map.intersections().values()){
            calculateIntersectionCluster(itx);
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
