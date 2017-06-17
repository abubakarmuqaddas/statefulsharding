package statefulsharding;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPDependency;
import statefulsharding.MilpOpt.ShardedSNAPDependency2;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;

/**
 * Created by root on 4/5/17.
 */
public class TestingClass {

    public static void main(String[] args){

        int capacity = Integer.MAX_VALUE;

        int size = 3;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, false);

        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        TrafficStore trafficStore = new TrafficStore();

        TrafficDemand trafficDemand1 = new TrafficDemand(
                graph.getVertex(0),graph.getVertex(8),1
        );

        trafficStore.addTrafficDemand(trafficDemand1);

        StateVariable A = new StateVariable("A", 2);
        StateVariable B = new StateVariable("B", 1);
        StateStore stateStore = new StateStore();
        stateStore.addStateVariable(A);
        stateStore.addStateVariable(B);
        stateStore.setStateCopies("A",2);
        stateStore.setStateCopies("B",1);


        HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies = new HashMap<>();

        dependencies.put(trafficDemand1, new LinkedList<>());

        dependencies.get(trafficDemand1).add(A);
        dependencies.get(trafficDemand1).add(B);

        boolean verbose = false;
        boolean fixConstraints = true;

        OptimizationOptions optimizationOptions = new OptimizationOptions(verbose, fixConstraints);

        ShardedSNAPDependency2 shardedSNAPDependency = new ShardedSNAPDependency2(graph,
                                                                                trafficStore,
                                                                                dependencies,
                                                                                optimizationOptions,
                                                                                stateStore);

        System.out.println("Result: " + shardedSNAPDependency.optimize());
        shardedSNAPDependency.printSolution();

    }
}
