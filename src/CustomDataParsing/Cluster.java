package CustomDataParsing;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.Road;

import java.util.*;


public class Cluster {
    public static HashMap<Integer, HashSet<Long>> clusterRoadMap;
    public static HashMap<Long, Integer> roadClusterLookup;
    public static HashMap<Intersection, Integer> intersectionClusterLookup;
    public static HashMap<Integer, HashSet<Intersection>> clusterIntersectionMap;
    private static HashMap<Integer,Double> clusterInitialReward;
    private static HashSet<Long> knownRoad;
    private static HashSet<Long> knownIntersections;
    static CityMap cmap;
    private <T> T getRandomValueFromSet(Set<T> set){
        int size = set.size();
        int itemIdx = new Random().nextInt(size);
        int i = 0;
        for(T t: set){
            if(i==itemIdx){
                return t;
            }
            i++;
        }
        return set.iterator().next();
    }

    public Cluster(CityMap c){
        cmap = c;
        clusterRoadMap = new HashMap<>();
        roadClusterLookup = new HashMap<>();
        for(Road r: c.roads()){
            roadClusterLookup.put(r.id, -1);
        }
        clusterInitialReward = new HashMap<>();
        intersectionClusterLookup = new HashMap<>();
        clusterIntersectionMap = new HashMap<>();
        knownRoad = new HashSet<>();

    }
    public Set<Integer> getClusterIds(){
        return clusterRoadMap.keySet();
    }

    public void addInfo(long roadId, int clusterID){
        HashSet clusterSet;
        if(clusterRoadMap.containsKey(clusterID)){
            clusterSet = clusterRoadMap.get(clusterID);
        }else{
            clusterSet = new HashSet<>();
        }
        clusterSet.add(roadId);
        clusterRoadMap.put(clusterID, clusterSet);
        roadClusterLookup.put(roadId, clusterID);
        knownRoad.add(roadId);

    }

    public void calculateInitialReward(){

        List<Road> roads = cmap.roads();
        for(Integer i: getClusterIds()){
            clusterInitialReward.put(i,0.0);
        }
        for (Road r: roads){
            Integer clusterId = roadClusterLookup.get(r.id);
            clusterInitialReward.put(clusterId, clusterInitialReward.get(clusterId)+r.length/r.travelTime);
        }

    }

    public double getClusterInitialReward(int clusterId){
        if(clusterId==-1){
            return clusterInitialReward.get(clusterId) / (clusterRoadMap.get(clusterId).size()+cmap.roads().size() - knownRoad.size());
        }
        return clusterInitialReward.get(clusterId) / clusterRoadMap.get(clusterId).size();
    }

    public int getIntersectionCluster(Intersection itx){
        if (intersectionClusterLookup.containsKey(itx)) return intersectionClusterLookup.get(itx);

        int clusterId = -1;
        HashSet<Road> neighbourRoads = new HashSet<>(itx.getRoadsFrom());
        neighbourRoads.addAll(itx.getRoadsTo());
        HashMap<Integer, Integer> majorityCount = new HashMap<>();
        int maxVal = 0;

        for(Road r: neighbourRoads){//let

            Integer roadClusterId = roadClusterLookup.get(r.id);

            if(roadClusterId==-1) continue;
            majorityCount.merge(roadClusterId, 1, (a, b) -> a + b);

            if (majorityCount.get(roadClusterId) > maxVal ){
                maxVal = majorityCount.get(roadClusterId);
                clusterId = roadClusterId;
            }
            if (majorityCount.get(roadClusterId)==maxVal && !clusterIntersectionMap.containsKey(roadClusterId)){
                maxVal = majorityCount.get(roadClusterId);
                clusterId = roadClusterId;
            }
        }

        intersectionClusterLookup.put(itx, clusterId);
        if(!clusterIntersectionMap.containsKey(clusterId)){
            clusterIntersectionMap.put(clusterId, new HashSet<>());
        }
        clusterIntersectionMap.get(clusterId).add(itx);
        return clusterId;
    }

    public HashSet<Intersection> getClusterIntersections(Integer clusterId){
        HashSet<Intersection> itxs = clusterIntersectionMap.get(clusterId);

        return itxs;
    }

    public Integer getClusterID(long roadId){
        //-1 is outlier
        if (roadClusterLookup.containsKey(roadId)){
            return roadClusterLookup.get(roadId);
        }
        return -1;
    }

    public HashSet<Long> getRoadIDFromCluster(Integer clusterId){
        return clusterRoadMap.get(clusterId);
    }
//    public Intersection getRandomInterSectionFromCluster(int clusterId){
//        int clusterKey = clusterId;
//        if(clusterId==-1){
//            clusterKey = getRandomValueFromSet(clusterRoadMap.keySet());
//        }
//        HashSet<Long> clusterVal = clusterRoadMap.get(clusterKey);
//        return getRandomValueFromSet(clusterVal);
//    }
}
