package UserExamples;

import COMSETsystem.CityMap;
import COMSETsystem.Intersection;
import COMSETsystem.Road;
import CustomDataParsing.CSVNYRoadTimeParser;
import CustomDataParsing.RoadTimePickupProbability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class DataModelRoutePickupWeight extends DataModelRouteSpeedWeight {
    RoadTimePickupProbability pickUpTimeMap;
    CSVNYRoadTimeParser parser;

    public DataModelRoutePickupWeight(CityMap map, int layer) {
        super(map, layer);
        System.out.println("evoked");
        parser = new CSVNYRoadTimeParser("trial_data/count_probability_201606_formatted.csv", super.map.computeZoneId());
        pickUpTimeMap = parser.parse();
    }

    @Override
    public HashMap<String, Double> weightedSpeedOfNeighbours(Intersection root, long time){
        //this time no memorization because assuming there won't be too many repeating results within the time frame.
        HashMap<String, Double> speedTable = new HashMap<>();

        ArrayList<Intersection> path = new ArrayList<>();
        path.add(root);//root.

        DFS(root, layer, 0, path, speedTable, time); //calls this recursive function to compute the tree.

        //System.out.println("Root: {" + root.id + "} has neibours: {"+root.getAdjacentFrom().toString()+"}, probability map: "+ speedTable.toString());
        return speedTable;
    }

    @Override
    public void DFS(Intersection intersection, int layer, double totalProbabiltiy, ArrayList<Intersection> path, HashMap<String, Double> speedWeight, long time){
        //if leaf or end of layer.
        if (layer==0 || intersection.getAdjacentFrom().size()==0){

            if(path.size()<=1){
                //dead end. won't be loaded to graph.
                return;
            }

            String pathKey = super.encodePath(path);
            speedWeight.put(pathKey, totalProbabiltiy);

            return;
        }
        Set<Intersection> neighbours = intersection.getAdjacentFrom();

        for(Intersection n: neighbours){
            if (path.contains(n)){
                continue;
            }
            Road r = intersection.roadTo(n);
            double probabiltiy = pickUpTimeMap.getProbability(r.id, time);

            ArrayList<Intersection> new_path = (ArrayList<Intersection>) path.clone();
            new_path.add(n);
            DFS(n, layer-1, totalProbabiltiy + probabiltiy, new_path, speedWeight, time);
        }
    }

}
