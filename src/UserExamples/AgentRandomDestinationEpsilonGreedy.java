package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.LocationOnRoad;
import COMSETsystem.Road;

import java.io.FileInputStream;
import java.util.*;

public class AgentRandomDestinationEpsilonGreedy extends AgentRandomWalk{
    static int candidateNumber = 20;
    //static DataModelRouteSpeedWeight dm;
    static int repeatLimit = 35;

    static double eps = -1;

    static int choosenCounter = 0;
    static int allCounter = 0;
    static HashMap<String, Double> ODAverageSpeedMap;
    /**
     * AgentRandomWalk constructor.
     *
     * @param id  An id that is unique among all agents and resources
     * @param map The map
     */
    public AgentRandomDestinationEpsilonGreedy(long id, CityMap map) {
        super(id, map);
        ODAverageSpeedMap = new HashMap<>();
        if(eps==-1){
            try {
                String configFile = "etc/config.properties";
                Properties prop = new Properties();
                prop.load(new FileInputStream(configFile));
                String epsStr = prop.getProperty("eps.current").trim();
                if (epsStr != null) {
                    eps = Double.parseDouble(epsStr);
                }

            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {

        route.clear();
        Intersection sourceIntersection = currentLocation.road.to;
        HashMap<Intersection, Double> rewardTable = new HashMap<>();
        int i = 0;
        int repeatCount = 0;
        while(i < candidateNumber && repeatCount < repeatLimit) {
            LinkedList<Intersection> path = getRoute(sourceIntersection);
            Intersection destination = path.get(path.size()-1);
            if (rewardTable.containsKey(destination)){
                repeatCount++;
                continue;
            }
            rewardTable.put(destination, getTravelTimeOfPath(path));
            i++;
        }

        HashMap<Intersection,Double> probTable = getProbabilityTable(rewardTable);
        Intersection choosenDestination = getRandom(probTable);
        route = map.shortestTravelTimePath(sourceIntersection, choosenDestination);
        route.poll(); // Ensure that route.get(0) != currentLocation.road.to.
    }
    public Intersection getRandom(HashMap<Intersection,Double> probTable){
        allCounter++;
        if(allCounter%100000==0){
            System.out.println("% choosen using eps : "+ eps+" = "+(double)choosenCounter/allCounter);
        }
        Intersection largest = null;
        double largestVal = -1.0;
        List<Map.Entry<Intersection,Double>> rangeAxis = new ArrayList<>();
        //calculate cumulative average speed of roads
        double totalWeight = 0;
        for (Intersection itx: probTable.keySet()){
            Map.Entry<Intersection, Double> pair = new AbstractMap.SimpleEntry<>(itx, probTable.get(itx));
            rangeAxis.add(pair);
            double val = pair.getValue();
            totalWeight+= val;
            if(largestVal<val){
                largestVal = val;
                largest = itx;
            }
        }

        int randomIndex = -1;
        double random = Math.random()*totalWeight;
        for (int i = 0; i < rangeAxis.size(); ++i)
        {
            random -= rangeAxis.get(i).getValue();
            if (random <= 0.0d)
            {
                randomIndex = i;
                break;
            }
        }
        Intersection destination = rangeAxis.get(randomIndex).getKey();
        if(largest.equals(destination)) choosenCounter++;
        return destination;
    }
    public HashMap<Intersection, Double> getProbabilityTable(HashMap<Intersection, Double> rewardTable){
        double maxReward = -1;
        Intersection maxChoice = null;
        int numOfChoices = (rewardTable.size()-1);
        double one_minus_eps_averaged;
        HashMap<Intersection, Double> probTable = new HashMap<>();
        if(numOfChoices==0){
            Intersection itx = rewardTable.keySet().iterator().next();
            probTable.put(itx, (double)1);
        }else{
            one_minus_eps_averaged = eps/numOfChoices;
            for (Intersection itx: rewardTable.keySet()){
                double thisReward = rewardTable.get(itx);

                if(maxReward < thisReward){
                    maxReward = thisReward;
                    maxChoice = itx;
                }
                probTable.put(itx, one_minus_eps_averaged);
            }
            probTable.put(maxChoice, 1-eps);
        }
        return probTable;
    }
    public LinkedList<Intersection> getRoute(Intersection sourceIntersection){

        int destinationIndex = rnd.nextInt(map.intersections().size());
        Intersection[] intersectionArray = map.intersections().values().toArray(new Intersection[map.intersections().size()]);
        Intersection destinationIntersection = intersectionArray[destinationIndex];
        if (destinationIntersection == sourceIntersection) {
            // destination cannot be the source
            // if destination is the source, choose a neighbor to be the destination
            Road[] roadsFrom = sourceIntersection.roadsMapFrom.values().toArray(new Road[sourceIntersection.roadsMapFrom.values().size()]);
            destinationIntersection = roadsFrom[0].to;
        }
        route = map.shortestTravelTimePath(sourceIntersection, destinationIntersection);
        return route;
    }
    public double getTravelTimeOfPath(LinkedList<Intersection> path){


        int length = path.size();
        String key = path.get(0).toString()+" "+path.get(length-1).toString();
        if (ODAverageSpeedMap.containsKey(key))return ODAverageSpeedMap.get(key);
        double total = 0;
        for(int i = 0;i<length-1;i++){
            Road r = path.get(i).roadTo(path.get(i+1));
            total+=r.length/r.travelTime;
        }
        double res = total/(path.size()-1);
        ODAverageSpeedMap.put(key, res);
        return res;
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
