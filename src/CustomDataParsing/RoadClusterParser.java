package CustomDataParsing;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class RoadClusterParser{
    private String path;
    private static CityMap map;
    // list of all resources
    public Cluster clusterInfo;
    public RoadClusterParser(String path, CityMap cmap) {
        this.path = path;
        this.clusterInfo = new Cluster(cmap);
        map = cmap;
    }
    public Cluster parse() {

        try {

            Scanner sc = new Scanner(new File(path));
            sc.useDelimiter(",|\n");    //scanner will skip over "," and "\n" found in file
            sc.nextLine(); // skip the header
            while (sc.hasNext()) {
                String str = sc.nextLine();

                String[] s = str.split(",");
                Long roadId = Long.parseLong(s[0]);
                Integer clusterId = Integer.parseInt(s[1]);
                clusterInfo.addInfo(roadId, clusterId);
            }
            sc.close();

        } catch (Exception e) {

            e.printStackTrace();
        }

        for(Intersection itx: map.intersections().values()){
            clusterInfo.getIntersectionCluster(itx);
        }
        return clusterInfo;

    }
}
