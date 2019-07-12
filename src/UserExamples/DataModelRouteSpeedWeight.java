package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.Road;

import javax.xml.crypto.Data;
import java.io.FileInputStream;
import java.util.*;

class DataAtIntersection{
    HashMap<String, Double> probabilityTable;
    HashMap<String, Double> epsGreedyTable;
    public DataAtIntersection (HashMap<String, Double> p,HashMap<String, Double> e) {
        probabilityTable = p;
        epsGreedyTable = e;
    }
    public HashMap<String, Double>  getProbabilityBySpeed(){
        return probabilityTable;
    }
    public HashMap<String, Double>  getProbabilityByEpsilonGreedy(){
        return epsGreedyTable;
    }
}

public class DataModelRouteSpeedWeight {
    // A reference to the map.
    CityMap map;

    private HashMap<Intersection, DataAtIntersection> cache;
    //private HashMap<Intersection, HashMap<String, Double>> epsilonGreedyTable;
    double eps = -1;

    int layer;


    public DataModelRouteSpeedWeight(CityMap map, int layer) {
        this.map = map;
        this.cache = new HashMap<>();
        this.layer = layer;

        if(eps==-1){
            try {
                Properties prop = new Properties();
                prop.load(new FileInputStream("etc/config.properties"));
                String epsStr = prop.getProperty("eps.current").trim();
                if (epsStr != null) {
                    eps = Double.parseDouble(epsStr);
                }
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    //calls planRoute to get the next set of intersections to travel.
    public ArrayList<Intersection> planRoute(Intersection currentIntersection, long time){

        DataAtIntersection infoTable = weightedSpeedOfNeighbours(currentIntersection, time);

        HashMap<String, Double> neighboursWeightedByEpsilonGreedy = infoTable.getProbabilityByEpsilonGreedy();

        return getWeightedRandomChoice(neighboursWeightedByEpsilonGreedy);

    }
    public ArrayList<Intersection> getWeightedRandomChoice(HashMap<String, Double> weights){
        double totalWeight = 0.0d;
        List<Map.Entry<String,Double>> totalSpeeds = new ArrayList<>();

        //Randomly select an intersection according to sum of path speed weight.
        //calculate cumulative average speed of roads
        //in the hashmap, path is encoded as a string: "id1 id2 id3... idn", id names seperated by space.
        for (String encodedPaths: weights.keySet()){

            double pathWeight = weights.get(encodedPaths);
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


        ArrayList<Intersection> route = decodePath(choosenPath); //decodes string key back to a list of intersections.
        route.remove(0); //make sure the first intersection is not itself.
        return route;
    }
    public DataAtIntersection weightedSpeedOfNeighbours(Intersection root, long time){
        if (cache.containsKey(root)){
            return cache.get(root); //memorization.
        }

        HashMap<String, Double> speedTable = new HashMap<>();

        ArrayList<Intersection> path = new ArrayList<>();
        path.add(root);//root.

        DFS(root, layer, 0, path, speedTable, time); //calls this recursive function to compute the tree.

        HashMap<String, Double> epsTable = getEpsProbabilityTable(speedTable);

        DataAtIntersection result = new DataAtIntersection(speedTable, epsTable);
        cache.put(root, result);
        return result;
    }

    public HashMap<String, Double> getEpsProbabilityTable(HashMap<String, Double> speedTable){
        HashMap<String, Double> epsTable = new HashMap<>();
        double highestRewardProbability = 1;
        double remainingRewardProbability = 0;

        if(speedTable.size()!= 1){
            highestRewardProbability = 1 - eps;
            remainingRewardProbability = eps / (speedTable.size() - 1);
        }
        String highestRouteKey = "";
        Double highestRouteReward = -1.0;
        for(String s:speedTable.keySet()){
            Double val = speedTable.get(s);
            if(highestRouteReward < val){
                highestRouteReward = val;
                highestRouteKey = s;
            }
            epsTable.put(s, remainingRewardProbability);
        }
        epsTable.put(highestRouteKey, highestRewardProbability);
        return epsTable;
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
