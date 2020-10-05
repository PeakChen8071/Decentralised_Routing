package UserExamples;

import COMSETsystem.Intersection;

import java.util.*;

public class ChoiceModel {
    public double eps;
    public long allCounter = 0;
    public long choosenCounter = 0;
    public ChoiceModel(double epsilon){
        this.eps = epsilon;
    }


    // choose a random object from set
    public <T> T getRandomFromSet(Set<T> set){
        if(set==null || set.size() == 0) System.out.println("Null set exception");
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


    // choice based on uniform probability, e.g. p(i) = A(i) / sum(A(j)) where j are elements in the choice set
    public <T> T choiceByProbability(HashMap<T, Double> probTable) {
        List<Map.Entry<T, Double>> rangeAxis = new ArrayList<>();
        double totalWeight = 0;

        for (T itx: probTable.keySet()) {
            Map.Entry<T, Double> pair = new AbstractMap.SimpleEntry<>(itx, probTable.get(itx));
            rangeAxis.add(pair);
            double val = pair.getValue();
            totalWeight += val;
        }

        int randomIndex = -1;
        double random = Math.random() * totalWeight;
        for (int i=0; i<rangeAxis.size(); i++) {
            random -= rangeAxis.get(i).getValue();
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        return rangeAxis.get(randomIndex).getKey();
    }


    public int getRandomWithWeight(double[] probTable){
        allCounter++;
        if(allCounter%100000==0){
            System.out.println("% choosen using eps : "+ eps+" = "+(double)choosenCounter/allCounter);
        }
        int largest = 0;
        double largestVal = -1.0;
        List<Map.Entry<Integer,Double>> rangeAxis = new ArrayList<>();
        //calculate cumulative average speed of roads
        double totalWeight = 0;
        for (int itx = 0; itx<probTable.length;itx++){
            Map.Entry<Integer, Double> pair = new AbstractMap.SimpleEntry<>(itx, probTable[itx]);
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
        int destination = rangeAxis.get(randomIndex).getKey();
        if(largest == destination) choosenCounter++;
        return destination;
    }


    public <T> HashMap<T, Double> getProbabilityTableWithEps(HashMap<T, Double> rewardTable){
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


    public double[] getProbabilityTableWithEps(double[] rewardTable){
        double maxReward = -1;
        int maxChoice = 0;
        int numOfChoices = (rewardTable.length-1);
        double one_minus_eps_averaged;

        double[] probTable = new double[rewardTable.length];
        if(numOfChoices==0){
            probTable[0] = 1;
        }else{
            one_minus_eps_averaged = eps/numOfChoices;
            for (int i = 0;i<rewardTable.length;i++){
                double thisReward = rewardTable[i];

                if(maxReward < thisReward){
                    maxReward = thisReward;
                    maxChoice = i;
                }
                probTable[i] = one_minus_eps_averaged;
            }
            probTable[maxChoice] = 1-eps;
        }
        return probTable;
    }

}
