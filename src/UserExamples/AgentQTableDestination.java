package UserExamples;

import COMSETsystem.*;
import CustomDataParsing.RoadClusterParser;

import java.lang.reflect.Array;
import java.util.*;

public class AgentQTableDestination extends BaseAgent {

    /* qtable
     *    c1  c2  c3  < inner key
     * c1 r11 r12 r13  < inner map value
     * c2 r21 r22 r23
     * c3 r31 r32 r33
     * ^
     * outer key
     */

    class M{
        int t_cap;
        public void setT_cap(long currentTime){
            t_cap = Cluster.mapTime(currentTime);
        }
        public double toDouble(long currentTime){
            return 0.5*(Math.tanh(Cluster.mapTime(currentTime) - t_cap - 5*60) + 1);
        }
    }
    double[][] qTable; //index 0 is cluster -1. index
    static double [][] shortestClusterTravelTime;
    M[] mVector;

    LinkedList<Intersection> route = new LinkedList<>();
    boolean firstPlanning = true;
    boolean prevPickup = false;

    Integer internalSearchTime = 0;
    Integer currentClusterId = 0;

    int currentTimeSlice = 0;

    static int totalClusterNumber;
    static EpsilonGreedyTools epsTool;
    static boolean rcpDone = false;
    static RoadClusterParser rcp = null;
    static HashMap<Integer, Cluster> clusters;
    static ClusterTool ct;
    static ArrayList<Integer> times;
    static int counter= 0;


    int updateRcounter=  0;


    public AgentQTableDestination(long id, CityMap map) {
        super(id, map);

        if (epsTool==null){

            epsTool = new EpsilonGreedyTools(0.6);
        }

        if (rcp==null && rcpDone ==false){
            rcp = new RoadClusterParser(map);
            System.out.println("started reading IO");
            clusters = rcp.parseRoadClusterFile("trial_data/75_RoadCluster.csv");
            System.out.println("Started Parsing pickupCountTimeSlise");
            rcp.parseRoadPickupFile("ClusterData/pickupCount_timeslice.csv");
            System.out.println("Finished reading IO");
            rcp = null;
            rcpDone = true;
            System.out.println("cluster size is:"+clusters.size());
            totalClusterNumber = clusters.size();

        }
        if (ct==null){
            ct = new ClusterTool(map, clusters);

        }
        if (times==null){
            times = new ArrayList<>();
            for(int i = 8*60;i<23*60+45;i+=15){
                times.add(i);
            }

        }

        //fill it in the first plan.
        qTable = new double[totalClusterNumber][totalClusterNumber];
        mVector = new M[totalClusterNumber];
        //System.out.println(mVector);
        for(int i = 0;i<totalClusterNumber;i++){
            mVector[i] = new M();
            mVector[i].setT_cap(0);
        }

    }

    public void updateR(long currentTime){

        currentTimeSlice = times.get(0);
        for(int i = 0;i<totalClusterNumber;i++){
            for(int j = 0;j<totalClusterNumber;j++){
                qTable[i][j]= clusters.get(j).getCurrentReward(currentTime);
            }
        }
    }
    @Override
    public void planSearchRoute(LocationOnRoad currentLocation, long currentTime) {
        route.clear();

        //update Qtable
        if (times.size()>0 && Cluster.mapTime(currentTime) >= times.get(0)){
            while(Cluster.mapTime(currentTime) >= times.get(0)){
                times.remove(0);
            }
            updateR(currentTime);
        }
        if (Cluster.mapTime(currentTime) < times.get(0)){

            updateR(currentTime);
        }

        if(id==240000) System.out.println(Cluster.mapTime(currentTime)+ " "+updateRcounter+" "+currentTimeSlice);
        Cluster c = clusters.get(currentClusterId);

        //if plan inside we do nothing.
        Integer destinationClusterId = currentClusterId;

        //first time planning
        if (firstPlanning){

            destinationClusterId = initialPlanRoute(currentLocation, currentTime);
            firstPlanning = false;

        // plan outside due to search time.
        }else if (internalSearchTime >= c.searchTime){

            updateMvector(currentClusterId, currentTime);
            updateQTable(currentTime);
            destinationClusterId = chooseOutsideCluster(currentLocation, currentTime);

        }
        //ignores the searchtime rule
        //destinationClusterId = chooseOutsideCluster(currentLocation, currentTime);



        //planRoute ---- same procedures.
        HashSet<Intersection> itxs = clusters.get(destinationClusterId).intersections;
        if(itxs==null || itxs.size()==0){
            itxs = clusters.get(0).intersections;
            destinationClusterId = 0;
        }
        Intersection sourceIntersection = currentLocation.road.to;


        Intersection destinationIntersection = epsTool.getRandomFromSet(itxs);


        if (sourceIntersection == destinationIntersection) {
            // destination cannot be the source
            // if destination is the source, choose a neighbor to be the destination
            Road[] roadsFrom = sourceIntersection.roadsMapFrom.values().toArray(new Road[sourceIntersection.roadsMapFrom.values().size()]);
            destinationIntersection = roadsFrom[0].to;
        }
        route = map.shortestTravelTimePath(sourceIntersection, destinationIntersection);
        if (destinationClusterId==currentClusterId){

            internalSearchTime += (int) map.travelTimeBetween(sourceIntersection, destinationIntersection);
        }

        // Ensure that route.get(0) != currentLocation.road.to.
        route.poll();

        // because we don't know if this plan will lead it to a passenger or not - only assigned to can tell.
        prevPickup = false;

    }

    private Integer chooseOutsideCluster(LocationOnRoad currentLocation, long currentTime){
        /*
            internalSearchTime = 0;
            double [] probTable = epsTool.getProbabilityTable(qTable[currentClusterId]);
            probTable[currentClusterId] = 0;
            Integer choosenClusterId = epsTool.getRandomWithWeight(probTable);
            return choosenClusterId;
        */
        double[] probTable = epsTool.getProbabilityTable(qTable[currentClusterId]);
        Integer choosenClusterId = epsTool.getRandomWithWeight(probTable);
        return choosenClusterId;
    }

    public void updateQTable(long currentTime){
        double [] mDoubleVector = new double[totalClusterNumber];
        for (int i = 0;i<totalClusterNumber;i++){
            mDoubleVector[i] = mVector[i].toDouble(currentTime);
        }
        for (int i = 0;i<totalClusterNumber;i++){
            for(int j = 0;j<totalClusterNumber;j++){
                qTable[i][j] = qTable[i][j]*mDoubleVector[j];
            }
        }
    }
    public void updateMvector(Integer clusterId, long currentTime){
        mVector[clusterId].setT_cap(currentTime);
    }



    private Integer initialPlanRoute(LocationOnRoad currentLocation, long currentTime){
        updateR(currentTime);
        currentTimeSlice = Cluster.mapTime(currentTime);
        counter++;
        double[] probTable = epsTool.getProbabilityTable(qTable[currentClusterId]);
        Integer choosenClusterId = epsTool.getRandomWithWeight(probTable);
        return choosenClusterId;
    }


    @Override
    public Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime) {
        Integer clusterId = ct.getClusterFromRoad(currentLocation.road).id;

        //entered a new cluster.
        if (clusterId!= currentClusterId){
            currentClusterId = clusterId;
        }




        //standard update nextIntersection
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
    public void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePickupLocation, LocationOnRoad resourceDropoffLocation) {
        route.clear();
        //reset internal search time.
        internalSearchTime = 0;
        prevPickup = true;

    }

    private void printQtablePretty(){
        StringBuilder sb = new StringBuilder();

        for (int i = 0;i<totalClusterNumber;i++){
            sb.append("\t");
            sb.append("c"+i);
            sb.append("\t");
        }
        sb.append("\n");
        for (int i = 0;i<totalClusterNumber;i++){
            sb.append("c"+i);
            sb.append("\t");
            double[] innerTable = qTable[i];
            for(int j = 0;j<totalClusterNumber;j++){
                sb.append(innerTable[j]);
                sb.append("\t");
            }
            sb.append("\n");
        }
        System.out.println(sb);
    }

    private void printClusterShortestTravelTime(){
        StringBuilder sb = new StringBuilder();

        for (int i = 0;i<totalClusterNumber;i++){

            sb.append("c"+i);
            sb.append(",");
        }
        sb.append("\n");
        for (int i = 0;i<totalClusterNumber;i++){
            sb.append("c"+i);
            sb.append(",");
            for(int j = 0;j<totalClusterNumber;j++){
                int randomTime = 30;
                int totalTravelTime = 0;

                Set<Intersection> os = clusters.get(i).intersections;
                Set<Intersection> ds = clusters.get(j).intersections;

                if(i!=j && os.size()!=0 && ds.size()!=0){
                    for(int k = 0;k<randomTime;k++){

                        Intersection o = epsTool.getRandomFromSet(os);
                        Intersection d = epsTool.getRandomFromSet(ds);

                        totalTravelTime += map.travelTimeBetween(o, d);
                    }
                }

                shortestClusterTravelTime[i][j] = (double) totalTravelTime/randomTime;
                sb.append(totalTravelTime/randomTime);
                sb.append(",");
            }
            sb.append("\n");
        }
        System.out.println(sb);
    }


}
