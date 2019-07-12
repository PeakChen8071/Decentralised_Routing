package UserExamples;
import COMSETsystem.*;

import java.util.*;

public class AgentRandomWalkWithWeight extends AgentRandomWalk{

    /**
     * AgentRandomWalk constructor.
     *
     * @param id  An id that is unique among all agents and resources
     * @param map The map
     */
    public AgentRandomWalkWithWeight(long id, CityMap map) {
        super(id, map);
    }

    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {

        route.clear();
        Intersection currentIntersection = currentLocation.road.to;

        Road nextRoad = getRandomRoadWithWeight(currentIntersection);
        //set the other end section of the choosen road to be the next Intersection
        Intersection nextIntersection = nextRoad.to;
        route.add(nextIntersection);
    }

    public Road getRandomRoadWithWeight(Intersection currentIntersection){
        /*
        choose a random road from this intersection outgoind roads.
        the probability is proportional to road speed.
        Modified from: https://stackoverflow.com/a/6737362
         */

        double totalWeight = 0.0d;
        List<Map.Entry<Road,Double>> roadSpeeds = new ArrayList<>();

        //calculate cumulative average speed of roads
        for (Road r: currentIntersection.getRoadsFrom()){
            //speed is road total length/road total travel time.
            //this is from the definition of road --  check "addLink" function in road.

            double averageSpeed = Math.pow(2,r.length/r.travelTime);
            totalWeight += averageSpeed;
            Map.Entry<Road,Double> pair = new AbstractMap.SimpleEntry<>(r, averageSpeed);
            roadSpeeds.add(pair);
        }

        // Now choose a random road
        int randomIndex = -1;
        double random = Math.random() * totalWeight;
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
        return nextRoad;
    }

}
