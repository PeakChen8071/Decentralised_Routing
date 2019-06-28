package UserExamples;

import COMSETsystem.BaseAgent;
import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.LocationOnRoad;


/**
 * A proxy class uses both AgentRandomWalk and AgentRandomDestination at 50/50 ratio.
 */
public class AgentRandomDestinationAndWalk extends BaseAgent {

    BaseAgent actualAgent;

    /**
     * AgentRandomDestinationAndWalk constructor.
     *
     * @param id  An id that is unique among all agents and resources
     * @param map The map
     */
    public AgentRandomDestinationAndWalk(long id, CityMap map) {
        super(id, map);
        if (id%4==0){
            actualAgent = new AgentRandomDestination(id, map);
        }else{
            actualAgent = new AgentRandomWalk(id, map);

        }
    }

    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {
        actualAgent.planSearchRoute(currentLocation, currentTime);
    }

    @Override
    public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {
        return actualAgent.nextIntersection(currentLocation, currentTime);
    }

    @Override
    public void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePickupLocation, LocationOnRoad resourceDropoffLocation) {
        actualAgent.assignedTo(currentLocation, currentTime, resourceId, resourcePickupLocation, resourceDropoffLocation);
    }
}
