package UserExamples;
import COMSETsystem.*;
import CustomDataParsing.CSVNYRoadTimeParser;
import CustomDataParsing.RoadTimePickupProbability;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.Properties;

public class AgentRandomWalkEpsilonGreedy extends AgentRandomWalk{
    /**
     * AgentRandomWalk constructor.
     *
     * @param id  An id that is unique among all agents and resources
     * @param map The map
     */
    static double eps = -1;
    static RoadTimePickupProbability rtpp;
    static int choosenCounter = 0;
    static int allCounter = 0;
    public AgentRandomWalkEpsilonGreedy(long id, CityMap map) {

        super(id, map);
        if(rtpp==null) {

            CSVNYRoadTimeParser parser = new CSVNYRoadTimeParser("trial_data/count_probability_201606_formatted.csv", super.map.computeZoneId());
            rtpp = parser.parse();
        }
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
        Intersection currentIntersection = currentLocation.road.to;

        Road nextRoad = getRandomRoadWithWeightEpsilonGreedy(currentIntersection, currentTime);
        //set the other end section of the choosen road to be the next Intersection
        Intersection nextIntersection = nextRoad.to;
        route.add(nextIntersection);
    }
    public Road getRandomRoadWithNoEpsilon(Intersection currentIntersection, long currentTime){
        allCounter++;
        if(allCounter%100000==0){
            System.out.println("% choosen using eps : "+ eps+" = "+(double)choosenCounter/allCounter);
        }
        Map<Long,Double> probability = new LinkedHashMap<>();

        Road max_rd = null;
        double max_prob = -1;
        for (Road r: currentIntersection.getRoadsFrom()){
            double thisProb = r.length/r.travelTime;//rtpp.getProbability(r.id, currentTime);
            if(max_prob < thisProb){
                max_prob = thisProb;
                max_rd = r;
            }
            probability.put(r.id, thisProb);
        }

        List<Map.Entry<Road,Double>> roadSpeeds = new ArrayList<>();
        //calculate cumulative average speed of roads
        double totalWeight = 0;
        for (Road r: currentIntersection.getRoadsFrom()){
            Map.Entry<Road,Double> pair = new AbstractMap.SimpleEntry<>(r, probability.get(r.id));
            roadSpeeds.add(pair);
            totalWeight+=pair.getValue();
        }

        int randomIndex = -1;
        double random = Math.random()*totalWeight;
        for (int i = 0; i < roadSpeeds.size(); ++i)
        {
            random -= roadSpeeds.get(i).getValue();
            if (random <= 0.0d)
            {
                randomIndex = i;
                break;
            }
        }
        Road nextRoad = roadSpeeds.get(randomIndex).getKey();

        if(nextRoad.equals(max_rd)){
            choosenCounter++;
        }
        return nextRoad;

    }
    public Road getRandomRoadWithWeightEpsilonGreedy(Intersection currentIntersection, long currentTime){
        /*
        choose a random road from this intersection outgoind roads.
        the probability is proportional to road speed.
        Modified from: https://stackoverflow.com/a/6737362
         */

        allCounter++;
        if(allCounter%100000==0){
            System.out.println("% choosen using eps : "+ eps+" = "+(double)choosenCounter/allCounter);
//            try {
//                Properties prop = new Properties();
//                FileInputStream fin = new FileInputStream("etc/config.properties");
//                prop.load(fin);
//                double actualEps = (double)choosenCounter/allCounter ;
//                fin.close();
//                FileOutputStream fr = new FileOutputStream("etc/config.properties");
//                prop.setProperty("eps.actual", Double.toString(actualEps));
//                prop.store(fr, null);
//            }catch (Exception ex) {
//                ex.printStackTrace();
//            }
        }
        Map<Long,Double> probability = new LinkedHashMap<>();
        //{road ID: eps, or (1-eps)/(n-1)}
        //3 -> {1: 0.2, 2: 0.3: 3:0.5}
        //eps = 0.5
        //{1:0.25, 2:0.25, 3:0.5}
        //{3:1}
        double max_prob = -1;
        Road max_rd = null;
        int num_of_intersections = (currentIntersection.getRoadsFrom().size()-1);
        double one_minus_eps_averaged;

        if(num_of_intersections==0){
            max_rd = currentIntersection.getRoadsFrom().iterator().next();
            probability.put(max_rd.id, (double)1);
        }else{
            one_minus_eps_averaged = eps/num_of_intersections;
            for (Road r: currentIntersection.getRoadsFrom()){
                double thisProb = r.length/r.travelTime;//rtpp.getProbability(r.id, currentTime);

                if(max_prob < thisProb){
                    max_prob = thisProb;
                    max_rd = r;
                }
                probability.put(r.id, one_minus_eps_averaged);
            }
            probability.put(max_rd.id, 1-eps);
        }


        /*
         [0.......|.....1]
         eps = 0.1
         [0~0.9][0.9~0.95][0.95~1]
         random = 0~1 -> 0.565

          0
         */

        //System.out.println(probability.toString());

        List<Map.Entry<Road,Double>> roadSpeeds = new ArrayList<>();
        //calculate cumulative average speed of roads
        for (Road r: currentIntersection.getRoadsFrom()){

            Map.Entry<Road,Double> pair = new AbstractMap.SimpleEntry<>(r, probability.get(r.id));
            roadSpeeds.add(pair);
        }

        // Now choose a random road
        int randomIndex = -1;

        double random = Math.random();

        for (int i = 0; i < roadSpeeds.size(); ++i)
        {
            random -= roadSpeeds.get(i).getValue();
            if (random <= 0.0d)
            {
                randomIndex = i;
                break;
            }
        }
        //System.out.println("Here "+random);

        Road nextRoad = roadSpeeds.get(randomIndex).getKey();

        if(nextRoad.equals(max_rd)){
            choosenCounter++;
        }
        return nextRoad;
    }

}
