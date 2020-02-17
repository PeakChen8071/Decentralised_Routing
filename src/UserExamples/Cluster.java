package UserExamples;

import COMSETsystem.Intersection;
import COMSETsystem.Road;

import java.util.*;


public class Cluster {
    public int id;
    public double total_travel_time;
    double attr;
    private double reward;
    public HashSet<Intersection> intersections;
    public HashSet<Road> roads;

    public double distance;
    public double totalClusterSpeed;
    public HashSet<Integer> nbs;

    public HashMap<Integer, Integer> timePickup;
    public HashMap<Integer, Integer> timeResource;
    public HashMap<Integer, Integer> timeEmptyAgent;

    public double searchTimeBase;
    public static int timeslice;

    public Cluster(int ID, double attr,  double distance, double searchTimeBase){
        id = ID;
        this.attr = attr;
        this.distance = distance;
        totalClusterSpeed = 0;
        total_travel_time = 0;
        this.searchTimeBase = searchTimeBase;
        intersections = new HashSet<>();
        roads = new HashSet<>();
        nbs = new HashSet<>();
        timePickup = new HashMap<>();
        timeResource = new HashMap<>();
        timeEmptyAgent = new HashMap<>();
        int start = 7 * 60;
        int finish = 10 * 60;
        timeslice = 5;
        for (int i = start; i <= finish; i += timeslice) {
            timePickup.put(i, 0);
            timeResource.put(i, 0);
            timeEmptyAgent.put(i, 0);
        }
    }

//    public double getSearchTime(){
//        return 1000000000;//this.searchTimeBase;//5*(1/Math.log(this.attr));
//    }
    public double getClusterAttractiveness(){
        return attr;
    }
//    public double getCurrentReward(Long time, double travelTime){
//        Integer mappedTime = mapTime(time);
//        reward =  pickupTimeMap.getOrDefault(mappedTime, 0);
//        double Ti_r = Math.max(0, 2000 - travelTime);
//        return reward/distance;
//    }

    public void addIntersction(Intersection i) {
        intersections.add(i);
    }

    public void addRoad(Road r){
        roads.add(r);
        total_travel_time += r.travelTime;
    }


    //    public static void setZoneId(ZoneId z){
//        zid = z;
//    }
//    public static int mapTime(long time){
//        Instant i = Instant.ofEpochSecond(time);
//        ZonedDateTime z = ZonedDateTime.ofInstant(i, zid);
//
//        int hour = z.getHour();
//        int minute = z.getMinute();
//        int timeKey = hour*60 + mapMinute(minute);
//
//        return timeKey;
//    }
//    public static int mapTimeAccurate(long time){
//        Instant i = Instant.ofEpochSecond(time);
//        ZonedDateTime z = ZonedDateTime.ofInstant(i, zid);
//
//        int hour = z.getHour();
//        int minute = z.getMinute();
//        int timeKey = hour*60 + minute;
//
//        return timeKey;
//    }
}
