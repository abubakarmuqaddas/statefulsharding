package statefulsharding.randomgraphgen;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPDependency2;
import statefulsharding.State.GenerateStates;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RandomGraphDemo {

    public static void main(String[] args){

        int size = 9;
        int dependencySize = 1;
        int capacity = Integer.MAX_VALUE;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);

        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        TrafficStore trafficStore = new TrafficStore();

        TrafficGenerator.fromFile(graph, trafficStore,
                "../Dropbox/PhD_Work/Stateful_SDN/" +
                        "snapsharding/analysis/" +
                        "MANHATTAN-UNWRAPPED_deterministicTfc-evaluateTrafficHeuristic-fixCopies_9/" +
                        "MANHATTAN-UNWRAPPED_deterministicTfc-evaluateTrafficHeuristic-fixCopies_9_run_" +
                        "1" +
                        "_traffic.txt"
                );

        StateStore stateStore = new StateStore();

        LinkedList<LinkedList<StateVariable>> allDependencies =
                GenerateStates.BinaryTreeGenerator(dependencySize, stateStore);
        stateStore.setStateCopies("a",2);
        //stateStore.setStateCopies("b",2);
        //stateStore.setStateCopies("c",1);

        /*

        StateVariable a = new StateVariable("a", 2);
        StateVariable b = new StateVariable("b", 2);
        StateVariable c = new StateVariable("c", 1);
        stateStore.addStateVariable(a);
        stateStore.addStateVariable(b);
        stateStore.addStateVariable(c);
        stateStore.setStateCopies("a",2);
        stateStore.setStateCopies("b",2);
        stateStore.setStateCopies("c",1);

        HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies = new HashMap<>();



        for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){

            if(trafficDemand.getSource().getLabel()==14 && trafficDemand.getDestination().getLabel()==1){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
            }
            if(trafficDemand.getSource().getLabel()==2 && trafficDemand.getDestination().getLabel()==11){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
            }
            if(trafficDemand.getSource().getLabel()==13 && trafficDemand.getDestination().getLabel()==6){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("c"));
            }
            if(trafficDemand.getSource().getLabel()==9 && trafficDemand.getDestination().getLabel()==7){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("b"));
            }
            if(trafficDemand.getSource().getLabel()==10 && trafficDemand.getDestination().getLabel()==14){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("b"));
            }
            if(trafficDemand.getSource().getLabel()==15 && trafficDemand.getDestination().getLabel()==4){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("c"));
            }
            if(trafficDemand.getSource().getLabel()==3 && trafficDemand.getDestination().getLabel()==0){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("c"));
            }
            if(trafficDemand.getSource().getLabel()==4 && trafficDemand.getDestination().getLabel()==13){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("c"));
            }
            if(trafficDemand.getSource().getLabel()==7 && trafficDemand.getDestination().getLabel()==3){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("c"));
            }
            if(trafficDemand.getSource().getLabel()==6 && trafficDemand.getDestination().getLabel()==15){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
            }
            if(trafficDemand.getSource().getLabel()==11 && trafficDemand.getDestination().getLabel()==9){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("b"));
            }
            if(trafficDemand.getSource().getLabel()==1 && trafficDemand.getDestination().getLabel()==12){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("c"));
            }
            if(trafficDemand.getSource().getLabel()==12 && trafficDemand.getDestination().getLabel()==8){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("c"));
            }
            if(trafficDemand.getSource().getLabel()==0 && trafficDemand.getDestination().getLabel()==5){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
            }
            if(trafficDemand.getSource().getLabel()==5 && trafficDemand.getDestination().getLabel()==2){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("a"));
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("b"));
            }
            if(trafficDemand.getSource().getLabel()==8 && trafficDemand.getDestination().getLabel()==10){
                dependencies.put(trafficDemand, new LinkedList<>());
                dependencies.get(trafficDemand).add(stateStore.getStateVariable("b"));
            }
        }

        */

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
        boolean fixConstraints = false;

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

