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

    static int version = 1;
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
                    long eta = simulator.map.travelTimeBetween(a.loc, r.pickupLoc);
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

        } else {
            // Java output to "Optimiser_IO" for Python solver, after warm-up
            triggerInterval = 300;

            int totalClusterSize = simulator.clusterSet.size();
            int agentSize = 2000; // Fixed guess fleet size
//            int agentSize = simulator.emptyAgents.size();
//            System.out.println(agentSize);

            // Actual resource distribution
            FileWriter fw2 = new FileWriter(new File("Optimiser_IO/Actual R.csv"), true);
            BufferedWriter writer2 = new BufferedWriter(fw2);
            String sb2 = simulator.clusterResourceCount.values().toString().replaceAll("[\\[\\]]", "") + "\n";
            writer2.write(sb2);
            writer2.close();

            // Validation of transition M
            if (version >= 8 && version <= 10) {
                FileWriter fw3;
                BufferedWriter writer3;
                StringBuilder output;

                // matrix A, successful assignment
                fw3 = new FileWriter(new File("Optimiser_IO/Matrix " + version + "A.csv"));
                writer3 = new BufferedWriter(fw3);
                output = new StringBuilder();
                for (int i=0; i<simulator.matrixA.length; i++) {
                    for (int j=0; j<simulator.matrixA[0].length; j++) {
                        output.append(simulator.matrixA[i][j]);
                        if (j!=simulator.matrixA[0].length - 1) output.append(",");
                    }
                    output.append("\n");
                }
                writer3.write(output.toString());
                writer3.close();

                // matrix B, unsuccessful assignment inside destination cluster
                fw3 = new FileWriter(new File("Optimiser_IO/Matrix " + version + "B.csv"));
                writer3 = new BufferedWriter(fw3);
                output = new StringBuilder();
                for (int i=0; i<simulator.matrixB.length; i++) {
                    for (int j=0; j<simulator.matrixB[0].length; j++) {
                        output.append(simulator.matrixB[i][j]);
                        if (j!=simulator.matrixB[0].length - 1) output.append(",");
                    }
                    output.append("\n");
                }
                writer3.write(output.toString());
                writer3.close();

                // matrix C, unsuccessful assignment outside destination cluster
                fw3 = new FileWriter(new File("Optimiser_IO/Matrix " + version + "C.csv"));
                writer3 = new BufferedWriter(fw3);
                output = new StringBuilder();
                for (int i=0; i<simulator.matrixC.length; i++) {
                    for (int j=0; j<simulator.matrixC[0].length; j++) {
                        output.append(simulator.matrixC[i][j]);
                        if (j!=simulator.matrixC[0].length - 1) output.append(",");
                    }
                    output.append("\n");
                }
                writer3.write(output.toString());
                writer3.close();
            }
            simulator.matrixA = new int[totalClusterSize][totalClusterSize];
            simulator.matrixB = new int[totalClusterSize][totalClusterSize];
            simulator.matrixC = new int[totalClusterSize][totalClusterSize];

            // Estimate resources in Python optimiser
            File f = new File("Optimiser_IO/input.csv");
            FileWriter fw = new FileWriter(f, true);
            BufferedWriter writer = new BufferedWriter(fw);
            String sb;
            if (simulator.probabilityTable.Version == 0) {
                sb = simulator.clusterResourceCount.values().toString().replaceAll("[\\[\\]]", "") + "\n" +
                    agentSize + "\n" +
                    "hashcode," + version;
            } else {
                sb = agentSize + "\n" + "hashcode," + version;
            }
            writer.write(sb);
            writer.close();

            // Run Python Optimiser and fetch output matrix as probability table
            Process p = new ProcessBuilder("python3", "/home/ubuntu/Decentralised_Routing/src/UserExamples/Optimiser.py").start();
            p.waitFor();
            int len;
            if ((len = p.getErrorStream().available()) > 0) {
                byte[] buf = new byte[len];
                p.getErrorStream().read(buf);
                System.err.println("Command error:\t\"" + new String(buf) + "\"");
            }

//        int T = 0;
//        while (T < 60) {
//            if (checkOutput(hashcode)) {
//                break;
//            }
//            T++;
//            Thread.sleep(1000);
//        }

            File matrixFile = new File("Optimiser_IO/output_" + version + ".csv");
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
        File outputFile = new File("Optimiser_IO/output_" + hashcode + ".csv");
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

    private void AssignAgentToResource(AgentEvent a, ResourceEvent r) {

//        long travelTimeToEndIntersection = a.time - r.time;
//        long travelTimeFromStartIntersection = a.loc.road.travelTime - travelTimeToEndIntersection;
//        LocationOnRoad agentLocationOnRoad = new LocationOnRoad(a.loc.road, travelTimeFromStartIntersection);

//        long cruiseTime = time - a.startSearchTime;
//        long approachTime = simulator.map.travelTimeBetween(a.loc, r.pickupLoc);
//        long searchTime = cruiseTime + approachTime;
//        long waitTime = time + approachTime - r.availableTime;

//        simulator.totalAgentCruiseTime += cruiseTime;
//        simulator.totalAgentApproachTime += approachTime;
////        simulator.totalAgentSearchTime += searchTime;
//        simulator.totalResourceWaitTime += waitTime;
//        simulator.totalResourceTripTime += r.tripTime;
        simulator.centralAssignments++;

        // Inform the assignment to the agent.
        a.assignedTo(a.loc, time, id, r.pickupLoc, r.dropoffLoc);

        // "Label" the agent as occupied.
        simulator.emptyAgents.remove(a);
        simulator.events.remove(a);
        simulator.waitingResources.remove(r);
        simulator.events.remove(r);
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Assigned to agent id = " + a.id + " currently at " + a.loc, this);

        a.setEvent(time + simulator.map.travelTimeBetween(a.loc, r.pickupLoc) + r.tripTime, r.dropoffLoc, AgentEvent.DROPPING_OFF);
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
        // if (this.time + triggerInterval < simulator.simulationBeginTime + simulator.WarmUpTime) {
        if (this.time + triggerInterval < simulator.simulationEndTime) {
            return new TimeEvent(time + triggerInterval, simulator);
        } else {
            return null;
        }
    }
}
