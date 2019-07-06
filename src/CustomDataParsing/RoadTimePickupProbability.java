package CustomDataParsing;

import COMSETsystem.Intersection;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.TreeMap;

public class RoadTimePickupProbability {
    HashMap<Long, HashMap<Integer, Double>> map;
    ZoneId zid;

    public RoadTimePickupProbability(ZoneId z){
        zid = z;
        map = new HashMap<>();
    }
    public void addInfo(long roadId, long time, double probabiltiy){
        if (!map.containsKey(roadId)){
            HashMap<Integer, Double> roadMap = new HashMap<>();
            map.put(roadId, roadMap);
        }
        HashMap<Integer, Double> roadMap = map.get(roadId);
        int timekey = mapTime(time);
        roadMap.put(timekey, probabiltiy);
        map.put(roadId, roadMap);

    }
    public double getProbability(long roadId, long time){
        if (!map.containsKey(roadId)){
            return 0;
        }
        HashMap<Integer, Double> roadMap = map.get(roadId);
        int timekey = mapTime(time);
        if (roadMap.containsKey(timekey)){
            return roadMap.get(timekey);
        }else{
            return 0;
        }
    }
    public int mapTime(long time){
        Instant i = Instant.ofEpochSecond(time);
        ZonedDateTime z = ZonedDateTime.ofInstant(i, zid);

        int hour = z.getHour();
        int minute = z.getMinute();
        int timeKey = hour*60 + mapMinute(minute);

        return timeKey;
    }
    public int mapMinute(int minute){
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Long roadId : map.keySet()){
            sb.append("Road ID: ");
            sb.append(roadId);
            sb.append(": ");
            sb.append(map.get(roadId).toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
