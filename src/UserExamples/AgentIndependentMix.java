package UserExamples;

import COMSETsystem.BaseAgent;
import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.LocationOnRoad;

import java.util.Random;

public class AgentIndependentMix extends BaseAgent {
    Random rnd;
    int ratio;
    BaseAgent actualAgent;
    static int totalNormal = 0;

    public AgentIndependentMix(long id, CityMap map) {
        super(id, map);
        rnd = new Random(id);
        ratio = rnd.nextInt(100);

        if (ratio<100){
            totalNormal+=1;
            actualAgent = new AgentIndependent(id, map);
        }else{
            actualAgent = new AgentIndependentStrict(id, map);
        }
        System.out.println(totalNormal);
    }
    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {

        actualAgent.planSearchRoute(currentLocation, currentTime);
        canpickup = actualAgent.canpickup;
    }

    @Override
    public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {
        Intersection res = actualAgent.nextIntersection(currentLocation, currentTime);
        canpickup = actualAgent.canpickup;
        return res;

    }

    @Override
    public void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePickupLocation, LocationOnRoad resourceDropoffLocation) {
        actualAgent.assignedTo(currentLocation, currentTime, resourceId, resourceDropoffLocation, resourceDropoffLocation);
    }
}
