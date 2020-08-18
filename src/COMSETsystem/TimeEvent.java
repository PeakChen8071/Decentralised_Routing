package COMSETsystem;


import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimeEvent extends Event {

    static int version;
    static int triggerInterval;

    public TimeEvent(long time, Simulator simulator) throws IOException, InterruptedException {
        super(time, simulator);

        if (time < simulator.simulationBeginTime + simulator.WarmUpTime) {
            // Central matching during simulation warm-up
            triggerInterval = 10;

            TreeSet<ResourceEvent> resources = simulator.waitingResources;
            TreeSet<AgentEvent> agents = simulator.emptyAgents;
            Graph<Event, DefaultWeightedEdge> g = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            Set<Event> agentPartition = new HashSet<>();
            Set<Event> resourcePartition = new HashSet<>();
            for (AgentEvent a: agents) {
                for (ResourceEvent r : resources) {
                    long eta = getApproachTime(a, r);
                    if (eta < simulator.ResourceMaximumLifeTime) {
                        DefaultWeightedEdge e = new DefaultWeightedEdge();
                        if (!g.containsVertex(a)) {
                            g.addVertex(a);
                        }
                        if (!g.containsVertex(r)) {
                            g.addVertex(r);
                        }
                        g.addEdge(a, r, e);
                        g.setEdgeWeight(e, simulator.ResourceMaximumLifeTime - eta); // max match with positive weighting
                        agentPartition.add(a);
                        resourcePartition.add(r);
                    }
                }
            }
//            simulator.waitingResources.removeAll(expiredEvents);
            //        System.out.println("Number of agent"+agents.size());
            //        System.out.println("Number of resources"+resourceEvents.size());
            MaximumWeightBipartiteMatching matchObject = new MaximumWeightBipartiteMatching(g, agentPartition, resourcePartition);
            MatchingAlgorithm.Matching<Event, DefaultWeightedEdge> matching = matchObject.getMatching();
            //        System.out.println("Number of matches "+matching.getEdges().size());

            for (DefaultWeightedEdge e: matching) {
                AgentEvent agent = (AgentEvent) g.getEdgeSource(e);
                ResourceEvent resource = (ResourceEvent) g.getEdgeTarget(e);
                AssignAgentToResource(agent, resource);
            }

            simulator.initialAgents = agents.size();
        } else {
            // Java output to "Optimiser IO" for Python solver, after warm-up
            triggerInterval = 300;

            int totalClusterSize = simulator.clusterSet.size();
            int agentSize = simulator.emptyAgents.size();
//            int agentSize = simulator.initialAgents;
//            System.out.println(agentSize);

            File inputFile = new File("Optimiser IO/input.csv");
            BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile));
            String sb = simulator.clusterResourceCount.values().toString().replaceAll("[\\[\\]]", "") + "\n" +
                    agentSize + "\n" +
                    "hashcode," + version;
            writer.write(sb);
            writer.close();

            // Run Python Optimiser and fetch output matrix as probability table
            ProcessBuilder pb = new ProcessBuilder("python", "S:\\USYD\\Research\\Decentralised Cruising\\Taxi\\src\\UserExamples\\Optimiser.py");
            pb.start().waitFor();

//        int T = 0;
//        while (T < 60) {
//            if (checkOutput(hashcode)) {
//                break;
//            }
//            T++;
//            Thread.sleep(1000);
//        }

            File matrixFile = new File("Optimiser IO/output_" + version + ".csv");
            BufferedReader br = new BufferedReader(new FileReader(matrixFile));
            String line;
            int lineIdx = 0;
            double[][] transitionMatrix = new double[totalClusterSize][totalClusterSize];
            while (!((line = br.readLine()) == null) && !line.contains("hashcode")) {
                for (int i = 0; i < line.split(",").length; i++) {
                    transitionMatrix[lineIdx][i] = Double.parseDouble(line.split(",")[i]);
                }
                lineIdx++;
            }
            simulator.probabilityTable.updateMatrix(transitionMatrix);

            assert simulator.probabilityTable.Version == version : "version mismatch";
            version++;
        }
    }

    private boolean checkOutput(int hashcode) throws IOException {
        File outputFile = new File("Optimiser IO/output_" + hashcode + ".csv");
        try (BufferedReader br = new BufferedReader(new FileReader(outputFile))) {
            String lastline = null, line;
            while ((line = br.readLine()) != null) {
                lastline = line;
            }
            assert lastline != null;
            if ((Integer.parseInt(lastline.split(",")[1]) == hashcode)) {
                return true;
            }
        }
        return false;
    }

    private long getApproachTime(AgentEvent a, ResourceEvent r) {
        long travelTimeToEndIntersection = a.time - r.time;
        long travelTimeFromStartIntersection = a.loc.road.travelTime - travelTimeToEndIntersection;
        LocationOnRoad agentLocationOnRoad = new LocationOnRoad(a.loc.road, travelTimeFromStartIntersection);
        long travelTime = simulator.map.travelTimeBetween(agentLocationOnRoad, r.pickupLoc);

        return travelTime;
    }

    private void AssignAgentToResource(AgentEvent a, ResourceEvent r) {
//        //record meeting/assignment of resource to agent
//        FileWriter fw = new FileWriter(simulator.meetingLogName, true);
//        PrintWriter pw = new PrintWriter(fw);
//        long meetTime = time + getApproachTime(a, r);
//        pw.write( meetTime + "," + r.pickupLoc.road.id + "\n");
//        pw.close();

        long travelTimeToEndIntersection = a.time - r.time;
        long travelTimeFromStartIntersection = a.loc.road.travelTime - travelTimeToEndIntersection;
        LocationOnRoad agentLocationOnRoad = new LocationOnRoad(a.loc.road, travelTimeFromStartIntersection);

//        long cruiseTime = time - a.startSearchTime;
//        long approachTime = getApproachTime(a, r);
//
//        long searchTime = cruiseTime + approachTime;
//        long waitTime = time + approachTime - r.availableTime;

//        simulator.totalAgentCruiseTime += cruiseTime;
//        simulator.totalAgentApproachTime += approachTime;
//        simulator.totalAgentSearchTime += searchTime;
//        simulator.totalResourceWaitTime += waitTime;
//        simulator.totalResourceTripTime += r.tripTime;
        simulator.centralAssignments++;

        // Inform the assignment to the agent.
        a.assignedTo(agentLocationOnRoad, time, id, r.pickupLoc, r.dropoffLoc);

        // "Label" the agent as occupied.
        simulator.emptyAgents.remove(a);
        simulator.events.remove(a);
        simulator.waitingResources.remove(r);
        simulator.events.remove(r);
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Assigned to agent id = " + a.id + " currently at " + a.loc, this);

        a.setEvent(getApproachTime(a, r) + r.time + r.tripTime, r.dropoffLoc, AgentEvent.DROPPING_OFF);
        simulator.events.add(a);

//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "From agent to resource = " + approachTime + " seconds.", this);
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "From pickupLoc to dropoffLoc = " + r.tripTime + " seconds.", this);
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "cruise time = " + cruiseTime + " seconds.", this);
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "approach time = " + approachTime + " seconds.", this);
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "search time = " + searchTime + " seconds.", this);
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "wait time = " + waitTime + " seconds.", this);
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Next agent trigger time = " + a.time, this);
    }

    @Override
    Event trigger() throws Exception {
        return new TimeEvent(this.time + triggerInterval, simulator);
    }
}
