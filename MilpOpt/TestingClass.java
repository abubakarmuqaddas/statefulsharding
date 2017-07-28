package statefulsharding.MilpOpt;


import statefulsharding.State.GenerateStates;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class TestingClass {

    public static void main(String[] args){

        int size = 3;
        int trafficNo = 1;
        int dependencySize = 1;
        int capacity = Integer.MAX_VALUE;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, false);

        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        StateStore stateStore = new StateStore();

        TrafficStore trafficStore = new TrafficStore();
        String trafficFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" + size +
                ".csv";

        /*
        TrafficGenerator.fromFileLinebyLine(graph, trafficStore, trafficNo, 1, true,
                trafficFile);
                */

        trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(0),graph.getVertex(2),1));

        LinkedList<LinkedList<StateVariable>> allDependencies =
                GenerateStates.BinaryTreeGenerator(dependencySize, stateStore);
        stateStore.setStateCopies("a",2);
        //stateStore.setStateCopies("b",2);

        for (LinkedList<StateVariable> dependency : allDependencies) {
            for (StateVariable stateVariable : dependency) {
                System.out.print(stateVariable.getLabel() + " ");
            }
            System.out.println();
        }

        HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies = new HashMap<>();

        for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            dependencies.put(trafficDemand,
                    allDependencies.get(ThreadLocalRandom.current().nextInt(
                            0, allDependencies.size()
                    )));
        }

        System.out.println();
        dependencies.forEach((trafficDemand, stateVariables) -> {
            System.out.println(trafficDemand.getSource().getLabel() + " -> " +
                    trafficDemand.getDestination().getLabel());
            System.out.print("Var: ");
            stateVariables.forEach(stateVariable -> {
                System.out.print(stateVariable.getLabel() + " ");
            });
            System.out.println();
            System.out.println();
        });


        boolean verbose = true;
        boolean fixConstraints = true;

        OptimizationOptions optimizationOptions = new OptimizationOptions(verbose, fixConstraints);

        ShardedDependencyMinTrafficStateSync opt = new ShardedDependencyMinTrafficStateSync(graph,
                                                                                trafficStore,
                                                                                dependencies,
                                                                                optimizationOptions,
                                                                                stateStore,
                                                                                0.2);

        System.out.println("Result: " + opt.optimize());
        opt.printSolution();

    }

}
