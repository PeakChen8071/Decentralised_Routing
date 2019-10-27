package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.Road;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;


public class Cluster {
    public int id;
    public double total_travel_time;
    double attr;
    private double reward;
    public HashSet<Intersection> intersections;
    public HashSet<Road> roads;
    public HashMap<Integer, Integer> pickupTimeMap; //created from parser
    public double distance;
    public double totalSpeedTimesDistance;
    public HashSet<Integer> nbs;


    public static double searchTimeBase;

    public Cluster(int ID, double attr, double distance){
        id = ID;
        this.attr = attr;
        this.distance = distance;
        totalSpeedTimesDistance = 0;
        total_travel_time = 0;
        searchTimeBase = 300.0; //in seconds
        intersections = new HashSet<>();
        roads = new HashSet<>();
        nbs = new HashSet<>();

    }



    public double getSearchTime(){
        return 10*total_travel_time/roads.size()*(1/Math.log(this.attr));
    }
    public double getAttractiveNess(){
        return attr;
    }
//    public double getCurrentReward(Long time, double travelTime){
//        Integer mappedTime = mapTime(time);
//        reward =  pickupTimeMap.getOrDefault(mappedTime, 0);
//        double Ti_r = Math.max(0, 2000 - travelTime);
//        return reward/distance;
//    }

    public void addIntersction(Intersection i){
        intersections.add(i);
    }
    public void addRoad(Road r){
        roads.add(r);
        total_travel_time+=r.travelTime;
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
    private static int mapMinute(int minute){
        if (minute > 60 || minute < 0){
            System.out.println("Error in minute conversion");
            return 0;
        }
        int res;
        if (minute < 15){
            res = 0;
        }else if(minute < 30){
            res = 15;
        }else if(minute < 45){
            res = 30;
        }else{
            res = 45;
        }
        return res;
    }

}
