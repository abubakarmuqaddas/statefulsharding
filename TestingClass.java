package statefulsharding;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPDependency2;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by root on 4/5/17.
 */
public class TestingClass {

    public static void main(String[] args){

        for(int j=0 ; j<10 ; j++) {

            for (int i = 0; i < 81; i++) {

                System.out.print(ThreadLocalRandom.current().nextInt(
                        0, 5) + ",");

            }
            System.out.println();
        }

    }
}
