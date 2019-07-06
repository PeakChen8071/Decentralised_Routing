package CustomDataParsing;

import DataParsing.Resource;
import MapCreation.MapCreator;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

public class CSVNYRoadTimeParser {
    // absolute path to csv file to be parsed
    private String path;

    // list of all resources
    public RoadTimePickupProbability probabilityMap;

    DateTimeFormatter dtf;

    ZoneId zoneId;
    public CSVNYRoadTimeParser(String path, ZoneId zoneId) {
        this.path = path;
        dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.zoneId = zoneId;
        this.probabilityMap = new RoadTimePickupProbability(zoneId);
    }
    private Long dateConversion(String timestamp) {
        long l = 0L;
        LocalDateTime ldt = LocalDateTime.parse(timestamp, dtf);
        ZonedDateTime zdt = ZonedDateTime.of(ldt, zoneId);
        l = zdt.toEpochSecond();
        return l;
    }

    public RoadTimePickupProbability parse() {

        try {

            Scanner sc = new Scanner(new File(path));
            sc.useDelimiter(",|\n");    //scanner will skip over "," and "\n" found in file
            sc.nextLine(); // skip the header
            while (sc.hasNext()) {
                //,pickup_roadId,pickup_time,pickup_count,total_pickup,probability
                //0,0,21:00:00,10,130458,0.000076653022429

                sc.next();// skip first VendorID
                long roadId = Long.parseLong(sc.next());
                String pickUpDateAndTime = "2016-06-01 ";
                String HMS = sc.next();
                if (HMS.length()<8){
                    pickUpDateAndTime = pickUpDateAndTime+"0"+HMS;
                }else{
                    pickUpDateAndTime = pickUpDateAndTime+HMS;
                }
                long time = dateConversion(pickUpDateAndTime);
                int pickUpCount = Integer.parseInt(sc.next());// skip pickup count
                sc.next();// skip totalPickUP;
                double pickUpProbabiltiy = Double.parseDouble(sc.next());


                probabilityMap.addInfo(roadId, time, pickUpProbabiltiy); //create new resource with the above fields
            }
            sc.close();

        } catch (Exception e) {

            e.printStackTrace();
        }

        return probabilityMap;
    }
}
