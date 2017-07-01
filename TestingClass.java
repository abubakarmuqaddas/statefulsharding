package statefulsharding;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPDependency2;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;


public class TestingClass {

    public static void main(String[] args){

        LinkedList<Integer> data = new LinkedList<>();

        for(int i=0 ; i<9 ; i++){
            data.add(i);
        }

        LinkedList<LinkedList<Integer>> result = getNCombinations.getPermutations(2, data);

        int i=1;
        for (LinkedList<Integer> linkedList : result) {
            System.out.println(i++ + ": " + linkedList);
        }



    }
}
