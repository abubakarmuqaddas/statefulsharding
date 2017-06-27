package statefulsharding.MilpOpt;


import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class TestingClass {

    public static void main(String[] args){

        int size = 3;
        int capacity = Integer.MAX_VALUE;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, false);

        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        StateStore stateStore = new StateStore();

        stateStore.addStateVariable("a"); stateStore.setStateCopies("a",2);
        stateStore.addStateVariable("b"); stateStore.setStateCopies("b",1);

        boolean verbose = true;
        boolean fixConstraints = true;

        OptimizationOptions optimizationOptions = new OptimizationOptions(verbose, fixConstraints);

        StateSyncOptimization stateSyncOptimization = new StateSyncOptimization(graph,
                                                                                optimizationOptions,
                                                                                stateStore);

        System.out.println("Result: " + stateSyncOptimization.optimize());
        stateSyncOptimization.printSolution();

    }

}
