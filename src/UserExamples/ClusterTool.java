package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.Road;

import java.util.*;

public class ClusterTool {

    public static HashMap<Road, Integer> roadClusterLookup;
    public static HashMap<Intersection, Integer> intersectionClusterLookup;
    static CityMap map;

    static EpsilonGreedyTools epsTool;
    public static Map<Integer, Cluster> clusters;

    private <T> T getRandomValueFromSet(Set<T> set) {
        int size = set.size();
        int itemIdx = new Random().nextInt(size);
        int i = 0;

        for (T t : set) {
            if (i == itemIdx) {
                return t;
            }
            i++;
        }
        return set.iterator().next();
    }


    public ClusterTool(CityMap Cmap, Map<Integer, Cluster> Clusters, EpsilonGreedyTools epstool) {
        map = Cmap;
        clusters = Clusters;
        roadClusterLookup = new HashMap<>();
        intersectionClusterLookup = new HashMap<>();
        epsTool = epstool;

        for (Road r : map.roads()) {
            roadClusterLookup.put(r, 0);//initialize them as unknown.
        }
        for (Intersection itx : map.intersections().values()) {
            intersectionClusterLookup.put(itx, 0);//initialize them as unknown.
        }
        for (Cluster c : clusters.values()) {

            for (Road r : c.roads) {
                roadClusterLookup.put(r, c.id);
            }
            for (Intersection itx : c.intersections) {
                intersectionClusterLookup.put(itx, c.id);
            }
        }
    }

    //get all clusters's id.
    public ArrayList<Integer> getTimes() {
        ArrayList<Integer> times = new ArrayList<>();
        for (Integer t : clusters.values().iterator().next().pickupTimeMap.keySet()) {
            times.add(t);
        }
        return times;
    }


    public Cluster getClusterFromRoad(Road r) {
        return clusters.get(roadClusterLookup.get(r));
    }

    public Cluster getClusterFromIntersection(Intersection itx) {
        return clusters.get(intersectionClusterLookup.get(itx));
    }

//    public double [][] getClusterShortestTravelTime(){
//        int totalClusterNumber = clusters.size();
//        double [][] shortestClusterTravelTime = new double [totalClusterNumber][totalClusterNumber];
//        StringBuilder sb = new StringBuilder();
//
//        for (int i = 0;i<totalClusterNumber;i++){
//
//            sb.append("c"+i);
//            sb.append(",");
//        }
//        sb.append("\n");
//        for (int i = 0;i<totalClusterNumber;i++){
//            sb.append("c"+i);
//            sb.append(",");
//            for(int j = 0;j<totalClusterNumber;j++){
//                int randomTime = 50;
//                int totalTravelTime = 0;
//
//                Set<Intersection> os = clusters.get(i).intersections;
//                Set<Intersection> ds = clusters.get(j).intersections;
//
//                if(i!=j && os.size()!=0 && ds.size()!=0){
//                    for(int k = 0;k<randomTime;k++){
//
//                        Intersection o = epsTool.getRandomFromSet(os);
//                        Intersection d = epsTool.getRandomFromSet(ds);
//
//                        totalTravelTime += map.travelTimeBetween(o, d);
//                    }
//                }
//
//                shortestClusterTravelTime[i][j] = (double) totalTravelTime/randomTime;
//                sb.append(totalTravelTime/randomTime);
//                sb.append(",");
//            }
//            sb.append("\n");
//        }
//        System.out.println(sb);
//        return shortestClusterTravelTime;
//
//    }
}

