package COMSETsystem;


import java.io.*;
import java.util.*;

public class TimeEvent extends Event {

    static int version;

    public TimeEvent(long time, Simulator simulator) throws IOException, InterruptedException {
        super(time, simulator);

        int totalClusterSize = simulator.clusterSet.size();
        int agentSize = simulator.emptyAgents.size();
//        TreeSet<ResourceEvent> resources = simulator.waitingResources;
//        TreeSet<AgentEvent> agents = simulator.emptyAgents;

        // Java output to "Optimiser IO" for Python solver
        File inputFile = new File("Optimiser IO/input.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile))) {
            String sb = simulator.clusterResourceCount.values().toString().replaceAll("[\\[\\]]", "") + "\n" +
                    agentSize + "\n" +
                    "hashcode," + version;
            writer.write(sb);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        if (simulator.probabilityTable == null) {
            simulator.probabilityTable = new ProbabilityMatrix(transitionMatrix);
        } else {
            simulator.probabilityTable.updateMatrix(transitionMatrix);
        }
        assert (simulator.probabilityTable.Version == version);
        version++;
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

    @Override
    Event trigger() throws Exception {
        // Define time interval for repeating triggers here, in seconds
        //        System.out.println("Time "+time);
        return new TimeEvent(this.time + 300, simulator);
    }
}
