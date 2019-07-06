package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.Road;

import java.util.*;

public class DataModelRouteSpeedWeight {
    // A reference to the map.
    CityMap map;

    private HashMap<Intersection, HashMap<String, Double>> probabilityTable;

    int choosenCounter = 0;
    int allCounter = 0;

    int layer;

    public DataModelRouteSpeedWeight(CityMap map, int layer) {
        this.map = map;
        this.probabilityTable = new HashMap<>();
        this.layer = layer;
    }

    //calls planRoute to get the next set of intersections to travel.
    public ArrayList<Intersection> planRoute(Intersection currentIntersection, long time){
        allCounter++;
        if(allCounter%10000==0){
            System.out.println(choosenCounter);
            System.out.println("Rate of choosing the most likely one: "+(double)choosenCounter/allCounter);
        }
        HashMap<String, Double> pathTable = weightedSpeedOfNeighbours(currentIntersection, time);
        Double most_likely = Double.MIN_VALUE;
        String most_likely_path = "";
        for(String path:pathTable.keySet()){
            if (pathTable.get(path)>most_likely){
                most_likely = pathTable.get(path);
                most_likely_path = path;
            }

        }

        double totalWeight = 0.0d;
        List<Map.Entry<String,Double>> totalSpeeds = new ArrayList<>();

        //Randomly select an intersection according to sum of path speed weight.
        //calculate cumulative average speed of roads
        //in the hashmap, path is encoded as a string: "id1 id2 id3... idn", id names seperated by space.
        for (String encodedPaths: pathTable.keySet()){

            double pathWeight = pathTable.get(encodedPaths);
            totalWeight += pathWeight;
            Map.Entry<String,Double> pair = new AbstractMap.SimpleEntry<>(encodedPaths, pathWeight);
            totalSpeeds.add(pair);
        }

        // Now choose a random set of intersections.
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

        if (choosenPath == most_likely_path){
            choosenCounter++;
        }
        ArrayList<Intersection> route = decodePath(choosenPath); //decodes string key back to a list of intersections.
        route.remove(0); //make sure the first intersection is not itself.

        return route;

    }
    public HashMap<String, Double> weightedSpeedOfNeighbours(Intersection root, long time){
        if (probabilityTable.containsKey(root)){
            return probabilityTable.get(root); //memorization.
        }

        HashMap<String, Double> speedTable = new HashMap<>();

        ArrayList<Intersection> path = new ArrayList<>();
        path.add(root);//root.

        DFS(root, layer, 0, path, speedTable, time); //calls this recursive function to compute the tree.

        //System.out.println("Root: {" + root.id + "} has neibours: {"+root.getAdjacentFrom().toString()+"}, speed map: "+ speedTable.toString());

        probabilityTable.put(root, speedTable);
        return speedTable;
    }


    public void DFS(Intersection intersection, int layer, double totalSpeed, ArrayList<Intersection> path, HashMap<String, Double> speedWeight, long time){
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

            ArrayList<Intersection> new_path = (ArrayList<Intersection>) path.clone();
            new_path.add(n);
            DFS(n, layer-1, totalSpeed + averageSpeed, new_path, speedWeight, time);
        }
    }





    protected String encodePath(ArrayList<Intersection> path){
        //input: a list of intersections ( a route/path)
        //output: a string representation of the route/path so it can be used as a key.
        StringBuilder sb = new StringBuilder();
        for(Intersection i:path){
            sb.append(i.id);
            sb.append(" ");
        }
        return sb.toString();
    }

    protected ArrayList<Intersection> decodePath(String pathStr){
        //reverse of the encodePath.
        StringBuilder sb = new StringBuilder();
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
