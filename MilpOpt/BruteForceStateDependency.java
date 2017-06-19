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
        int size = 3;

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

        int dependencySize = 2;
        StateStore stateStore = new StateStore();
        LinkedList<LinkedList<StateVariable>> allDependencies =
                GenerateStates.BinaryTreeGenerator(dependencySize, stateStore);
        stateStore.setStateCopies("a",2);
        stateStore.setStateCopies("b",1);
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
                        "MANHATTAN-UNWRAPPED_deterministicTfc_optimal_4_run_4_traffic.txt"
        );

        for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            dependencies.put(trafficDemand,
                    allDependencies.get(ThreadLocalRandom.current().nextInt(
                            0, allDependencies.size()
                    )));
        }


        /*
         * Loop over all combinations!!
         */


        LinkedList<StateCopy> bestCombination = null;
        int minCombination = Integer.MAX_VALUE;

        for (LinkedList<StateCopy> combination : combinations) {

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                

                LinkedList<StateVariable> currentDep = dependencies.get(trafficDemand);








            }





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
