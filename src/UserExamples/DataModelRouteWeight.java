package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.Road;

import java.util.*;

public class DataModelRouteWeight {
    // A reference to the map.
    CityMap map;

    HashMap<Intersection, HashMap<String, Double>> probabilityTable;

    // random number generator
    Random rnd;

    int layer;

    public DataModelRouteWeight(CityMap map, int layer) {
        this.map = map;
        this.probabilityTable = new HashMap<>();
        this.layer = layer;
    }

    public HashMap<String, Double> weightedSpeedOfNeighbours(Intersection root){
        if (probabilityTable.containsKey(root)){
            return probabilityTable.get(root);
        }


        HashMap<String, Double> speedTable = new HashMap<>();

        ArrayList<Intersection> path = new ArrayList<>();
        path.add(root);//root.

        DFS(root, layer, 0, path, speedTable);

        ArrayList<Long> nids = new ArrayList<>();
        for (Intersection n: root.getAdjacentFrom()){
            nids.add(n.id);
        }


        //System.out.println("Root: {" + root.id + "} has neibours: {"+root.getAdjacentFrom().toString()+"}, speed map: "+ speedTable.toString());


        probabilityTable.put(root, speedTable);
        return speedTable;
    }


    public void DFS(Intersection intersection, int layer, double totalSpeed, ArrayList<Intersection> path, HashMap<String, Double> speedWeight){
        //if leaf or end of layer.
        if (layer==0 || intersection.getAdjacentFrom().size()==0){

            if(path.size()<=1){
                //dead end. won't be loaded to graph.
                return;
            }

            String pathKey = encodePath(path);
            speedWeight.put(pathKey, totalSpeed);

            return;
        }
        Set<Intersection> neighbours = intersection.getAdjacentFrom();

        for(Intersection n: neighbours){
            if (path.contains(n)){
                continue;
            }
            Road r = intersection.roadTo(n);
            double averageSpeed = Math.pow(2, r.length/r.travelTime);

            ArrayList<Intersection> new_path = (ArrayList<Intersection>)path.clone();
            new_path.add(n);
            DFS(n, layer-1, totalSpeed + averageSpeed, new_path, speedWeight);
        }
    }



    public ArrayList<Intersection> planRoute(Intersection currentIntersection){
        HashMap<String, Double> pathTable = weightedSpeedOfNeighbours(currentIntersection);



        double totalWeight = 0.0d;
        List<Map.Entry<String,Double>> totalSpeeds = new ArrayList<>();

        //calculate cumulative average speed of roads
        for (String encodedPaths: pathTable.keySet()){

            double netspeed = pathTable.get(encodedPaths);
            totalWeight += netspeed;
            Map.Entry<String,Double> pair = new AbstractMap.SimpleEntry<>(encodedPaths, netspeed);
            totalSpeeds.add(pair);
        }

        // Now choose a random road
        int randomIndex = -1;
        double random = Math.random() * totalWeight;
        for (int i = 0; i < totalSpeeds.size(); ++i)
        {
            random -= totalSpeeds.get(i).getValue();
            if (random <= 0.0d)
            {
                randomIndex = i;
                break;
            }
        }

        String choosenPath = totalSpeeds.get(randomIndex).getKey();
        ArrayList<Intersection> route = decodePath(choosenPath);
        route.remove(0);
        return route;

    }

    private String encodePath(ArrayList<Intersection> path){
        StringBuilder sb = new StringBuilder();
        for(Intersection i:path){
            sb.append(i.id);
            sb.append(" ");
        }
        return sb.toString();
    }
    private ArrayList<Intersection> decodePath(String pathStr){
        ArrayList<Intersection> res = new ArrayList<>();
        String[] intersectionStrs = pathStr.split(" ");
        for(String str: intersectionStrs){
            try{
                Long interSectionId = Long.parseLong(str);
                res.add(map.intersections().get(interSectionId));

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return res;
    }
}
