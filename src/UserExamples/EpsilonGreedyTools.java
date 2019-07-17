package UserExamples;

import COMSETsystem.Intersection;

import java.util.*;

public class EpsilonGreedyTools {
    public double eps;
    public long allCounter = 0;
    public long choosenCounter = 0;
    public EpsilonGreedyTools(double epsilon){
        this.eps = epsilon;
    }
    public <T> T getRandomFromSet(Set<T> set){
        if(set==null) System.out.println("Null set exception");
        int size = set.size();
        int itemIdx = new Random().nextInt(size);
        int i = 0;
        for(T t: set){
            if(i==itemIdx){
                return t;
            }
            i++;
        }
        return set.iterator().next();
    }
    public <T> T getRandomWithWeight(HashMap<T,Double> probTable){
        allCounter++;
        if(allCounter%100000==0){
            System.out.println("% choosen using eps : "+ eps+" = "+(double)choosenCounter/allCounter);
        }
        T largest = null;
        double largestVal = -1.0;
        List<Map.Entry<T,Double>> rangeAxis = new ArrayList<>();
        //calculate cumulative average speed of roads
        double totalWeight = 0;
        for (T itx: probTable.keySet()){
            Map.Entry<T, Double> pair = new AbstractMap.SimpleEntry<>(itx, probTable.get(itx));
            rangeAxis.add(pair);
            double val = pair.getValue();
            totalWeight+= val;
            if(largestVal<val){
                largestVal = val;
                largest = itx;
            }
        }

        int randomIndex = -1;
        double random = Math.random()*totalWeight;
        for (int i = 0; i < rangeAxis.size(); ++i)
        {
            random -= rangeAxis.get(i).getValue();
            if (random <= 0.0d)
            {
                randomIndex = i;
                break;
            }
        }
        T destination = rangeAxis.get(randomIndex).getKey();
        if(largest.equals(destination)) choosenCounter++;
        return destination;
    }
    public <T> HashMap<T, Double> getProbabilityTable(HashMap<T, Double> rewardTable){
        double maxReward = -1;
        T maxChoice = null;
        int numOfChoices = (rewardTable.size()-1);
        double one_minus_eps_averaged;
        HashMap<T, Double> probTable = new HashMap<>();
        if(numOfChoices==0){
            T itx = rewardTable.keySet().iterator().next();
            probTable.put(itx, (double)1);
        }else{
            one_minus_eps_averaged = eps/numOfChoices;
            for (T itx: rewardTable.keySet()){
                double thisReward = rewardTable.get(itx);

                if(maxReward < thisReward){
                    maxReward = thisReward;
                    maxChoice = itx;
                }
                probTable.put(itx, one_minus_eps_averaged);
            }
            probTable.put(maxChoice, 1-eps);
        }
        return probTable;
    }
}
