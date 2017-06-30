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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by root on 4/5/17.
 */
public class TestingClass {

    public static void main(String[] args){

        LinkedList<Integer> data = new LinkedList<>();

        for(int i=0 ; i<9 ; i++){
            data.add(i);
        }

        LinkedList<LinkedList<Integer>> comb = getNCombinations.getPermutations(
                2, data
        );

        comb.forEach(combination -> {
            System.out.println(combination.toString());
        });


    }
}
