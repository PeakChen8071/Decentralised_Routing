//package UserExamples;
//
//import COMSETsystem.*;
//import CustomDataParsing.RoadClusterParser;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileWriter;
//import java.io.PrintWriter;
//import java.lang.reflect.Array;
//import java.time.Instant;
//import java.time.ZonedDateTime;
//import java.util.*;
//
//public class AgentQTableDestination extends BaseAgent {
//
//    /* qtable
//     *    c1  c2  c3  < inner key
//     * c1 r11 r12 r13  < inner map value
//     * c2 r21 r22 r23
//     * c3 r31 r32 r33
//     * ^
//     * outer key
//     */
//
////    class M{
////        int t_cap;
////        public void setT_cap(long currentTime){
////            t_cap = Cluster.mapTimeAccurate(currentTime);
////        }
////        public double toDouble(long currentTime){
////            double res = 0.5*(Math.tanh(Cluster.mapTimeAccurate(currentTime) - t_cap - 10) + 1);
////            return res;
////        }
////    }
//
//
//    double[][] qTable; //index 0 is cluster -1. index
//    EpsilonGreedyTools epsTool;
////    M[] mVector;
//
//    LinkedList<Intersection> route = new LinkedList<>();
//    boolean firstPlanning = true;
//
//
//    Integer internalSearchTime = 0;
//    Integer currentClusterId = 0;
//
//
//    String MvectorFileName = "default.txt";
//    static int chooseOuter = 0;
//    static int chooseInner = 0;
//    static int totalClusterNumber;
//    static double [] epsValueTracker;
//    static boolean rcpDone = false;
//    static RoadClusterParser rcp = null;
//    static Map<Integer, Cluster> clusters;
//    static ClusterTool ct;
//
//    static int counter= 0;
//    static double [][] shortestClusterTravelTime;
//
//
//
//    static TreeMap<Integer, TreeMap<Integer, Integer>> lossTable;
//    int updateRcounter=  0;
//    static int instance= 0;
//    int newId;
//    ArrayList<Integer> times;
//
//    public double[] generateEpsTracker(){
//        String configFile = "etc/config.properties";
//        int num = 0;
//        try {
//            Properties prop = new Properties();
//            prop.load(new FileInputStream(configFile));
//
//            num = Integer.parseInt(prop.getProperty("comset.number_of_agents").trim());
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//        return new double[num];
//    }
//
//
//    public void fill(double [] epsValueTracker){
//        //todo: fill this array with your method.
//
//        Arrays.fill(epsValueTracker, 0.4); //comment out this line. Now it sets eps value for all agents to be 0.3
//    }
//
//    public AgentQTableDestination(long id, CityMap map) {
//        super(id, map);
//
//        newId = instance;
//
//        if(epsValueTracker==null){
//            epsValueTracker = generateEpsTracker();
//            fill(epsValueTracker);
//        }
//        lossTable = new TreeMap<Integer, TreeMap<Integer, Integer>>();
//        epsTool = new EpsilonGreedyTools(epsValueTracker[newId]);
//
//        if (rcp==null && rcpDone ==false){
//            rcp = new RoadClusterParser(map);
//            System.out.println("started reading IO");
//
//            clusters = rcp.parseRoadClusterFile();
//            System.out.println("Started Parsing pickupCountTimeSlise");
//            rcp.parseRoadPickupFile();
//            System.out.println("Finished reading IO");
//            rcp = null;
//            rcpDone = true;
//            System.out.println("cluster size is:"+clusters.size());
//            totalClusterNumber = clusters.size();
//
//
//        }
//        if (ct==null){
//            ct = new ClusterTool(map, clusters, epsTool);
//
//        }
//        if (times==null){
//            times = new ArrayList<>();
//            for(int i = 8*60;i<24*60;i+=15){
//                times.add(i);
//                lossTable.put(i, new TreeMap<>());
//            }
//
//        }
////        if (shortestClusterTravelTime == null){
////            shortestClusterTravelTime = ct.getClusterShortestTravelTime();
////        }
////        //fill it in the first plan.
////        qTable = new double[totalClusterNumber][totalClusterNumber];
////        mVector = new M[totalClusterNumber];
////        //System.out.println(mVector);
////        for(int i = 0;i<totalClusterNumber;i++){
////            mVector[i] = new M();
////            mVector[i].t_cap = 0;
////        }
////        instance++;
//        if(id%1000==0){
//
//            MvectorFileName = Long.toString(id)+"_Mvector.txt";
//            try {
//                PrintWriter writer = new PrintWriter(MvectorFileName, "UTF-8");
//                writer.write("intitialized");
//                writer.close();
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//
//    }
//
//    public void updateR(long currentTime){
//
//
//        for(int i = 0;i<totalClusterNumber;i++){
//            for(int j = 0;j<totalClusterNumber;j++){
//                qTable[i][j]= clusters.get(j).getCurrentReward(currentTime, shortestClusterTravelTime[i][j]);
//            }
//        }
//
//
//    }
//    public void logMvector(long currentTime){
//        try {
////            FileWriter fileWriter = new FileWriter(MvectorFileName, true); //Set true for append mode
////            PrintWriter printWriter = new PrintWriter(fileWriter);
////            StringBuilder sb = new StringBuilder();
////            sb.append(Cluster.mapTimeAccurate(currentTime));
////            sb.append(',');
////            for(M m:mVector){
////                sb.append(String.format("%.4f", m.toDouble(currentTime)));
////                sb.append(',');
////            }
////            System.out.println(sb.toString());
////            printWriter.println(sb.toString());  //New line
////            printWriter.close();
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//    }
//    @Override
//    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {
//        counter++;
//        route.clear();
//
//        //update Qtable
//        if (times.size()>0 && Cluster.mapTime(currentTime) >= times.get(0)){
//            //8:01 -> smaller than 8:15. use 8:00.
//            //8:00, 8:15, 8:30.
//            //if comes in at 8:01.  do not update . if comes in at 8:15, update and remove 8:00.
//            while(Cluster.mapTime(currentTime) >= times.get(0)){
//                times.remove(0);
//            }
//            updateR(currentTime);
//        }
//
//
//        Cluster c = clusters.get(currentClusterId);
//
//        //if plan inside we do nothing.
//        Integer destinationClusterId = currentClusterId;
//
//        //first time planning
//        if (firstPlanning){
//
//            destinationClusterId = initialPlanRoute(currentLocation, currentTime);
//            firstPlanning = false;
//
//        // plan outside due to search time.
//        }else if (internalSearchTime >= c.searchTime){
//            int mappedTime = Cluster.mapTime(currentTime);
//            TreeMap<Integer, Integer> thisTimeClusterLossMap = lossTable.get(mappedTime);
//            thisTimeClusterLossMap.put(currentClusterId, thisTimeClusterLossMap.getOrDefault(currentClusterId, 0)+1);
//            updateMvector(currentClusterId, currentTime);
//            updateQTable(currentTime);
//            destinationClusterId = chooseOutsideCluster(currentLocation, currentTime);
//
//        }
//
//
//        if(destinationClusterId==currentClusterId){
//            chooseInner++;
//        }else{
//            chooseOuter++;
//        }
//        if(newId==10){
//            System.out.println("inner outer: "+chooseInner+ " "+chooseOuter);
//        }
//
//        //planRoute ---- same procedures.
//        HashSet<Intersection> itxs = clusters.get(destinationClusterId).intersections;
//        if(itxs==null || itxs.size()==0){
//            itxs = clusters.get(0).intersections;
//            destinationClusterId = 0;
//        }
//        Intersection sourceIntersection = currentLocation.road.to;
//
//
//        Intersection destinationIntersection = epsTool.getRandomFromSet(itxs);
//
//
//        if (sourceIntersection == destinationIntersection) {
//            // destination cannot be the source
//            // if destination is the source, choose a neighbor to be the destination
//            Road[] roadsFrom = sourceIntersection.roadsMapFrom.values().toArray(new Road[sourceIntersection.roadsMapFrom.values().size()]);
//            destinationIntersection = roadsFrom[0].to;
//        }
//        route = map.shortestTravelTimePath(sourceIntersection, destinationIntersection);
//        if (destinationClusterId==currentClusterId){
//
//            internalSearchTime += (int) map.travelTimeBetween(sourceIntersection, destinationIntersection);
//
//        }
//        if(id%1000==0){
//            logMvector(currentTime);
//        }
//        if(counter%20000==0){
//            System.out.println(lossTable);
//        }
//        // Ensure that route.get(0) != currentLocation.road.to.
//        route.poll();
//
//        // because we don't know if this plan will lead it to a passenger or not - only assigned to can tell.
//
//
//    }
//
//    private Integer chooseOutsideCluster(LocationOnRoad currentLocation, long currentTime){
//        /*
//            internalSearchTime = 0;
//            double [] probTable = epsTool.getProbabilityTable(qTable[currentClusterId]);
//            probTable[currentClusterId] = 0;
//            Integer choosenClusterId = epsTool.getRandomWithWeight(probTable);
//            return choosenClusterId;
//        */
//        internalSearchTime = 0;
//
//        Integer choosenClusterId = epsTool.getRandomWithWeight(qTable[currentClusterId]);
//        return choosenClusterId;
//    }
//
//    public void updateQTable(long currentTime){
//        double [] mDoubleVector = new double[totalClusterNumber];
//        for (int i = 0;i<totalClusterNumber;i++){
//            mDoubleVector[i] = mVector[i].toDouble(currentTime);
//        }
//        for (int i = 0;i<totalClusterNumber;i++){
//            for(int j = 0;j<totalClusterNumber;j++){
//                qTable[i][j] = qTable[i][j]*mDoubleVector[j];
//            }
//        }
//    }
//    public void updateMvector(Integer clusterId, long currentTime){
//
//        mVector[clusterId].setT_cap(currentTime);
//
//
//    }
//
//
//
//    private int initialPlanRoute(LocationOnRoad currentLocation, long currentTime){
//        updateR(currentTime);
//
//
//
//        Integer choosenClusterId = epsTool.getRandomWithWeight(qTable[currentClusterId]);
//        return choosenClusterId;
//    }
//
//
//    @Override
//    public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {
//        Integer clusterId = ct.getClusterFromRoad(currentLocation.road).id;
//
//        //entered a new cluster.
//        if (clusterId!= currentClusterId){
//            currentClusterId = clusterId;
//        }
//
//        //standard update nextIntersection
//        if (route.size() != 0) {
//            // Route is not empty, take the next intersection.
//            Intersection nextIntersection = route.poll();
//            return nextIntersection;
//        } else {
//            // Finished the planned route. Plan a new route.
//            planSearchRoute(currentLocation, currentTime);
//            return route.poll();
//        }
//    }
//
//    @Override
//    public void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePickupLocation, LocationOnRoad resourceDropoffLocation) {
//        route.clear();
//        //reset internal search time.
//        internalSearchTime = 0;
//
//
//    }
//
//    private void printQtablePretty(){
//        StringBuilder sb = new StringBuilder();
//
//        for (int i = 0;i<totalClusterNumber;i++){
//            sb.append("\t");
//            sb.append("c"+i);
//            sb.append("\t");
//        }
//        sb.append("\n");
//        for (int i = 0;i<totalClusterNumber;i++){
//            sb.append("c"+i);
//            sb.append("\t");
//            double[] innerTable = qTable[i];
//            for(int j = 0;j<totalClusterNumber;j++){
//                sb.append(innerTable[j]);
//                sb.append("\t");
//            }
//            sb.append("\n");
//        }
//        System.out.println(sb);
//    }
//
//
//
//
//}
