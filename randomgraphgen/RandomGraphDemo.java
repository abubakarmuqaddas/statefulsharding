package statefulsharding.randomgraphgen;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPDependency;
import statefulsharding.State.GenerateStates;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.*;
import statefulsharding.graph.algorithms.BinaryTree;
import statefulsharding.graph.algorithms.ShortestPath;

import javax.swing.plaf.nimbus.State;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RandomGraphDemo {


    public static void main(String[] args){

        int size = 3;
        int dependencySize = 2;
        int capacity = Integer.MAX_VALUE;
        Random RandomNumberGen = new Random();

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);

        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        TrafficStore trafficStore = new TrafficStore();

        TrafficGenerator.fromFile(graph, trafficStore,
                "../Dropbox/PhD_Work/Stateful_SDN/" +
                        "snapsharding/analysis/" +
                        "MANHATTAN-UNWRAPPED_deterministicTfc_optimal_3/" +
                        "MANHATTAN-UNWRAPPED_deterministicTfc_optimal_3_run_1_traffic.txt"
                );

        StateStore stateStore = new StateStore();
        LinkedList<LinkedList<StateVariable>> allDependencies =
                GenerateStates.BinaryTreeGenerator(dependencySize, stateStore);

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

