package statefulsharding;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPDependency;
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

        int size = 4;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);

        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        TrafficStore trafficStore = new TrafficStore();

        TrafficDemand trafficDemand1 = new TrafficDemand(
                graph.getVertex(0),graph.getVertex(11),1
        );
        TrafficDemand trafficDemand2 = new TrafficDemand(
                graph.getVertex(15),graph.getVertex(12),100
        );
        TrafficDemand trafficDemand3 = new TrafficDemand(
                graph.getVertex(4),graph.getVertex(12),1
        );

        trafficStore.addTrafficDemand(trafficDemand1);
        trafficStore.addTrafficDemand(trafficDemand2);
        trafficStore.addTrafficDemand(trafficDemand3);

        StateVariable A = new StateVariable("A", 2);
        StateVariable B = new StateVariable("B", 2);
        StateVariable C = new StateVariable("C", 1);
        StateStore stateStore = new StateStore();
        stateStore.addStateVariable(A);
        stateStore.addStateVariable(B);
        stateStore.addStateVariable(C);

        HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies = new HashMap<>();

        dependencies.put(trafficDemand1, new LinkedList<>());
        dependencies.put(trafficDemand2, new LinkedList<>());
        dependencies.put(trafficDemand3, new LinkedList<>());

        dependencies.get(trafficDemand1).add(A);
        dependencies.get(trafficDemand1).add(B);
        dependencies.get(trafficDemand2).add(A);
        dependencies.get(trafficDemand3).add(C);

        boolean verbose = false;
        boolean fixConstraints = false;

        OptimizationOptions optimizationOptions = new OptimizationOptions(verbose, fixConstraints);

        ShardedSNAPDependency shardedSNAPDependency = new ShardedSNAPDependency(graph,
                                                                                trafficStore,
                                                                                dependencies,
                                                                                optimizationOptions,
                                                                                stateStore);

        System.out.println("Result: " + shardedSNAPDependency.optimize());
        shardedSNAPDependency.printSolution();

    }
}
