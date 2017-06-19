package statefulsharding.MilpOpt;

import statefulsharding.State.GenerateStates;
import statefulsharding.State.StateCopy;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class BruteForceStateDependency {

    public static void main(String[] args) {

        /*
         * Generate Graph
         */

        int capacity = Integer.MAX_VALUE;
        int size = 4;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        /*
         * Generate distances from all vertices
         */

        HashMap<Vertex, HashMap<Vertex, Integer>> dist = ShortestPath.FloydWarshall(graph, false,
                null);

        /*
         * Generate states and numCopies
         */

        int dependencySize = 3;
        StateStore stateStore = new StateStore();
        LinkedList<LinkedList<StateVariable>> allDependencies =
                GenerateStates.BinaryTreeGenerator(dependencySize, stateStore);
        stateStore.setStateCopies("a",2);
        stateStore.setStateCopies("b",2);
        stateStore.setStateCopies("c",1);
        //stateStore.setStateCopies("c",1);

        /*
         * Generate all state combinations
         */

        HashMap<StateVariable, LinkedList<LinkedList<StateCopy>>> stateCopyCombinations = new HashMap<>();

        LinkedList<Integer> vertices = new LinkedList<>();
        for (int i = 0; i < graph.getNumVertices(); i++) {
            vertices.add(i);
        }

        for (StateVariable stateVariable : stateStore.getStateVariables()) {
            getNCombinations getCombinations = new getNCombinations(vertices, stateVariable.getCopies());
            LinkedList<LinkedList<Integer>> result = getCombinations.getResult();
            stateCopyCombinations.put(stateVariable, new LinkedList<>());
            for(LinkedList<Integer> combination : result){

                LinkedList<StateCopy> temp = new LinkedList<>();

                int i=1;
                for(Integer integer : combination){
                    StateCopy stateCopy = new StateCopy(stateVariable, i, graph.getVertex(integer));
                    temp.add(stateCopy);
                    stateStore.addStateCopy(stateVariable, stateCopy);
                    i++;
                }
                stateCopyCombinations.get(stateVariable).add(temp);
            }
        }


        ArrayList<StateVariable> stateVariables = new ArrayList(stateStore.getStateVariables());
        LinkedList<LinkedList<StateCopy>> combinations = new LinkedList<>();
        int numStates = stateStore.getNumStates();
        CartesianProduct(stateCopyCombinations, combinations, numStates, stateVariables, 0, null);

        for (LinkedList<StateVariable> dependency : allDependencies) {
            for (StateVariable stateVariable : dependency) {
                System.out.print(stateVariable.getLabel() + " ");
            }
            System.out.println();
        }

        /*
         * Assign traffic to states!!
         */

        HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies = new HashMap<>();

        TrafficStore trafficStore = new TrafficStore();

        TrafficGenerator.fromFile(graph, trafficStore,
                "../Dropbox/PhD_Work/Stateful_SDN/" +
                        "snapsharding/analysis/" +
                        "MANHATTAN-UNWRAPPED_deterministicTfc_optimal_4/" +
                        "MANHATTAN-UNWRAPPED_deterministicTfc_optimal_4_run_2_traffic.txt"
        );


        for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            dependencies.put(trafficDemand,
                    allDependencies.get(ThreadLocalRandom.current().nextInt(
                            0, allDependencies.size()
                    )));
        }

        System.out.println();
        dependencies.forEach((trafficDemand, states) -> {
            System.out.println(trafficDemand.getSource().getLabel() + " -> " +
                    trafficDemand.getDestination().getLabel());
            System.out.print("Var: ");
            states.forEach(stateVariable -> {
                System.out.print(stateVariable.getLabel() + " ");
            });
            System.out.println();
            System.out.println();
        });


        /*
         * Loop over all combinations!!
         */

        HashMap<LinkedList<StateCopy>, Integer> traffic = new HashMap<>();

        LinkedList<StateCopy> bestCombination = null;
        int x=1;
        int minCombination = Integer.MAX_VALUE;

        for (LinkedList<StateCopy> combination : combinations) {

            int combinationTraffic = 0;
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                System.out.println("Combination: " + x + " Tfc: " + trafficDemand.getSource().getLabel()
                        + " -> " + trafficDemand.getDestination().getLabel());

                int pathSize = 0;
                LinkedList<StateVariable> currentDep = dependencies.get(trafficDemand);
                Vertex currentSrc = trafficDemand.getSource();

                for(int i=0 ; i<currentDep.size() ; i++){
                StateVariable stateVariable = currentDep.get(i);

                    int minStatePathSize = Integer.MAX_VALUE;
                    StateCopy minStateCopy = null;

                    for(StateCopy stateCopy : combination){
                        if(stateCopy.getState().equals(stateVariable)){
                            Vertex currentDst = stateCopy.getVertex();
                            int statePathSize = dist.get(currentSrc).get(currentDst);

                            if(statePathSize < minStatePathSize){
                                minStatePathSize = statePathSize;
                                minStateCopy = stateCopy;
                            }
                        }
                    }

                    pathSize = pathSize + minStatePathSize;

                    currentSrc = minStateCopy.getVertex();
                }

                Vertex currentDst = trafficDemand.getDestination();
                pathSize = pathSize + dist.get(currentSrc).get(currentDst);
                combinationTraffic = combinationTraffic + pathSize;
            }
            x++;
            traffic.put(combination, combinationTraffic);


            if(combinationTraffic<minCombination){
                bestCombination = combination;
                minCombination = combinationTraffic;
            }
        }

        System.out.println("Best combination traffic: " + traffic.get(bestCombination));
        for (StateCopy stateCopy : bestCombination) {
            System.out.println(stateCopy.getLabel() + " copy: " + stateCopy.getCopyNumber()
             + " Vertex: " + stateCopy.getVertex().getLabel());
        }

    }

    private static void CartesianProduct(HashMap<StateVariable, LinkedList<LinkedList<StateCopy>>> stateCombinations,
                                        LinkedList<LinkedList<StateCopy>> prod,
                                        int numStates,
                                        ArrayList<StateVariable> stateVariables,
                                        int currentLevel,
                                        LinkedList<StateCopy> buildup){

        if(currentLevel==numStates){
            LinkedList<StateCopy> stateCopies = new LinkedList<>(buildup);
            /*
            stateCopies.forEach(stateCopy -> {
                System.out.print("Var: " + stateCopy.getLabel() +
                        " Copy: " + stateCopy.getCopyNumber() +
                        " Vertex: " + stateCopy.getVertex().getLabel());
                System.out.println();
            });
            System.out.println();
            */

            prod.add(stateCopies);
        }
        else{
            for(int i=currentLevel ; i<stateCombinations.keySet().size(); i++){

                if(currentLevel==0)
                    buildup = new LinkedList<>();

                for(LinkedList<StateCopy> linkedList : stateCombinations.get(stateVariables.get(currentLevel))){

                    buildup.addAll(linkedList);

                    CartesianProduct(stateCombinations, prod, numStates, stateVariables,
                            currentLevel+1, buildup);

                    for(StateCopy stateCopy : linkedList)
                        buildup.remove(stateCopy);

                }
            }
        }





    }

}
