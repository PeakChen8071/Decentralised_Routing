package COMSETsystem;



import DataParsing.Resource;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.*;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimeEvent extends Event {
    public TimeEvent(long time, Simulator simulator){
        super(time, simulator);

        TreeSet<ResourceEvent> resourceEvents = simulator.waitingResources;
        for (ResourceEvent r: resourceEvents){
            r.time-=600;
        }
        TreeSet<AgentEvent> agents = simulator.emptyAgents;
        Graph<Event, DefaultWeightedEdge> g = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<Event> agentPartition = new HashSet<>();
        Set<Event> resourcePartition = new HashSet<>();
        for (AgentEvent a: agents){
            for (ResourceEvent r: resourceEvents){
                long eta = getApproachTime(a, r);
                if (eta + r.time < r.expirationTime){
                    DefaultWeightedEdge e = new DefaultWeightedEdge();
                    if (!g.containsVertex(a)){
                        g.addVertex(a);
                    }
                    if (!g.containsVertex(r)){
                        g.addVertex(r);
                    }
                    g.addEdge(a, r, e);
                    g.setEdgeWeight(e, 10000 - eta);
                    agentPartition.add(a);
                    resourcePartition.add(r);
                }

            }
        }
        System.out.println("Number of agent"+agents.size());
        System.out.println("Number of resources"+resourceEvents.size());
        MaximumWeightBipartiteMatching matchObject = new MaximumWeightBipartiteMatching(g, agentPartition, resourcePartition);

        MatchingAlgorithm.Matching<Event, DefaultWeightedEdge> matching =  matchObject.getMatching();
        System.out.println("Number of matches "+matching.getEdges().size());

        for (DefaultWeightedEdge e: matching){
            AgentEvent agent = (AgentEvent) g.getEdgeSource(e);
            ResourceEvent resource = (ResourceEvent) g.getEdgeTarget(e);
            AssignAgentToResource(agent, resource);
            simulator.events.remove(resource);
        }
        for (ResourceEvent r: resourceEvents){
            r.time+=600;
        }
    }

    private long getApproachTime(AgentEvent a, ResourceEvent r){
        long travelTimeToEndIntersection = a.time - r.time;
//        System.out.println("A time r time: "+ a.time + " "+r.time);
        long travelTimeFromStartIntersection = a.loc.road.travelTime - travelTimeToEndIntersection;
        LocationOnRoad agentLocationOnRoad = new LocationOnRoad(a.loc.road, travelTimeFromStartIntersection);
        long travelTime = simulator.map.travelTimeBetween(agentLocationOnRoad, r.pickupLoc);

        return travelTime;
    }

    @Override
    Event trigger() throws Exception {
        System.out.println("Time "+time);
        return new TimeEvent(this.time+10, simulator);
    }

    private void AssignAgentToResource(AgentEvent a, ResourceEvent r){

        long travelTimeToEndIntersection = a.time - r.time;
        long travelTimeFromStartIntersection = a.loc.road.travelTime - travelTimeToEndIntersection;
        LocationOnRoad agentLocationOnRoad = new LocationOnRoad(a.loc.road, travelTimeFromStartIntersection);

        long cruiseTime = r.time - a.startSearchTime;
        long approachTime = getApproachTime(a, r);

        long searchTime = cruiseTime + approachTime;
        long waitTime = approachTime - r.availableTime;

        simulator.totalAgentCruiseTime += cruiseTime;
        simulator.totalAgentApproachTime += approachTime;
        simulator.totalAgentSearchTime += searchTime;
        simulator.totalResourceWaitTime += waitTime;
        simulator.totalResourceTripTime += r.tripTime;
        simulator.totalAssignments++;


        // Inform the assignment to the agent.
        a.assignedTo(agentLocationOnRoad, time, id, r.pickupLoc, r.dropoffLoc);

        // "Label" the agent as occupied.
        simulator.emptyAgents.remove(a);
        simulator.events.remove(a);
        simulator.waitingResources.remove(r);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Assigned to agent id = " + a.id + " currently at " + a.loc, this);

        a.setEvent(getApproachTime(a, r) + r.time + r.tripTime, r.dropoffLoc, AgentEvent.DROPPING_OFF);
        simulator.events.add(a);

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "From agent to resource = " + approachTime + " seconds.", this);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "From pickupLoc to dropoffLoc = " + r.tripTime + " seconds.", this);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "cruise time = " + cruiseTime + " seconds.", this);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "approach time = " + approachTime + " seconds.", this);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "search time = " + searchTime + " seconds.", this);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "wait time = " + waitTime + " seconds.", this);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Next agent trigger time = " + a.time, this);

    }

}
