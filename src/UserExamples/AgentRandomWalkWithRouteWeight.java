package UserExamples;

import COMSETsystem.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AgentRandomWalkWithRouteWeight extends BaseAgent {
    /**
     * AgentRandomWalk constructor.
     *
     * @param id  An id that is unique among all agents and resources
     * @param map The map
     */
    // search route stored as a list of intersections.
    LinkedList<Intersection> route = new LinkedList<Intersection>();

    Random rnd;

    // a static singleton object of a data model, shared by all agents
    static DataModelRouteSpeedWeight dataModel = null;

    public AgentRandomWalkWithRouteWeight(long id, CityMap map) {
        super(id, map);
        if (dataModel == null) {

            //Its a O(2^N) method.. so layer number is within [3~15]
            dataModel = new DataModelRoutePickupWeight(map, 3);
        }
    }

    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {

        route.clear();
        Intersection currentIntersection = currentLocation.road.to;

        ArrayList<Intersection> nextIntersection = dataModel.planRoute(currentIntersection, currentTime);

        route.addAll(nextIntersection);
    }

    @Override
    public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {
        if (route.size() != 0) {
            // Route is not empty, take the next intersection.
            Intersection nextIntersection = route.poll();
            return nextIntersection;
        } else {
            // Finished the planned route. Plan a new route.
            planSearchRoute(currentLocation, currentTime);
            return route.poll();
        }
    }

    @Override
    public void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePikcupLocation, LocationOnRoad resourceDropoffLocation) {
        // Clear the current route.


        route.clear();

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Agent " + this.id + " assigned to resource " + resourceId);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "currentLocation = " + currentLocation);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "currentTime = " + currentTime);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "resourcePickupLocation = " + resourcePikcupLocation);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "resourceDropoffLocation = " + resourceDropoffLocation);
    }


}
