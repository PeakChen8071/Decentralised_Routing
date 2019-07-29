package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.Road;

import java.util.*;

public class ClusterTool {

    public static HashMap<Road, Integer> roadClusterLookup;
    public static HashMap<Intersection, Integer> intersectionClusterLookup;
    static CityMap map;

    public static HashMap<Integer, Cluster> clusters;

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


    public ClusterTool(CityMap Cmap, HashMap<Integer, Cluster> Clusters) {
        map = Cmap;
        clusters = Clusters;
        roadClusterLookup = new HashMap<>();
        intersectionClusterLookup = new HashMap<>();

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
}

