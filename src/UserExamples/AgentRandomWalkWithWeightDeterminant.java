package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.LocationOnRoad;
import COMSETsystem.Road;


/*
***Statistics***
average agent search time: 911 seconds
average resource wait time: 432 seconds
resource expiration percentage: 34%

average agent cruise time: 131 seconds
average agent approach time: 342 seconds
average resource trip time: 844 seconds
total number of assignments: 154161
 */
public class AgentRandomWalkWithWeightDeterminant extends AgentRandomWalk {
    /**
     * AgentRandomWalk constructor.
     *
     * @param id  An id that is unique among all agents and resources
     * @param map The map
     */
    public AgentRandomWalkWithWeightDeterminant(long id, CityMap map) {
        super(id, map);
    }

    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {

        route.clear();
        Intersection currentIntersection = currentLocation.road.to;
        int s = currentIntersection.getAdjacentFrom().size();

        Double maxSpeed = 0.0d;
        Road nextRoad = null;

        for (Road r: currentIntersection.getRoadsFrom()){
            double averageSpeed = r.length/r.travelTime;
            if (averageSpeed > maxSpeed){
                nextRoad = r;
                maxSpeed = averageSpeed;
            }
        }

        Intersection nextIntersection = nextRoad.to;
        route.add(nextIntersection);
    }

}
