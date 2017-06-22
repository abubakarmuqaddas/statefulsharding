package statefulsharding.MilpOpt;

import statefulsharding.State.GenerateStates;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.heuristic.TrafficHeuristic;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;


public class BruteForceStateDependency2{

    private static int numCombinations;
    private static int currentCombination;
    private static int minCombination;
    private static LinkedList<String> bestCombination;

    static {
        numCombinations = 1;
        currentCombination = 0;
        minCombination = Integer.MAX_VALUE;
        bestCombination = new LinkedList<>();
    }

    public static void main(String[] args) {

        /**
         * Generate Graph
         */

        int capacity = Integer.MAX_VALUE;
        int size = 7;
        int trafficNo = 1;
        int depSize = 3;
        int depRun = 1;
        int assignmentLine = 1;
        int[] numCopies = new int[]{2,1,1,1,1,1,1,1};

        String initial = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/";
        String trafficFile = initial +
                "MANHATTAN-UNWRAPPED_deterministicTfc-7/" +
                "MANHATTAN-UNWRAPPED_deterministicTfc-7_" + trafficNo + "_traffic.txt";

        /**
         * Generate graph
         */

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        /**
         * Generate distances from all vertices
         */

        HashMap<Vertex, HashMap<Vertex, Integer>> dist = ShortestPath.FloydWarshall(graph, false,
                null);

        /**
         * Get Traffic!
         */

        TrafficStore trafficStore = new TrafficStore();

        TrafficGenerator.fromFile(graph, trafficStore, trafficFile);


        /**
         * Generate states and numCopies
         */

        String filename = initial + "StateDependencies/" + "StateDependencies_" + depSize + "_" + depRun + ".txt";

        StateStore stateStore = new StateStore();

        LinkedList<LinkedList<StateVariable>> allDependencies =
                StateStore.readStateDependency(filename, stateStore);

        for(int i=0 ; i<depSize ; i++){
            stateStore.setStateCopies(GenerateStates.getCharForNumber(i), numCopies[i]);
        }
        /**
         * Generate all state combinations
         */

        HashMap<StateVariable, LinkedList<LinkedList<String>>> stateCopyCombinations = new HashMap<>();

        for (StateVariable stateVariable : stateStore.getStateVariables()) {

            LinkedList<LinkedList<Integer>> result = getNCombinations.getCombinations(
                                                                    graph.getVerticesInt(), stateVariable.getCopies());

            stateCopyCombinations.put(stateVariable, new LinkedList<>());
            for(LinkedList<Integer> combination : result){

                LinkedList<String> temp = new LinkedList<>();


                for(Integer integer : combination){

                    String stateCopy = stateVariable.getLabel() + "," + integer;

                    temp.add(stateCopy);
                }

                stateCopyCombinations.get(stateVariable).add(temp);
            }
            numCombinations = numCombinations*stateCopyCombinations.get(stateVariable).size();
        }

        ArrayList<StateVariable> stateVariables = new ArrayList(stateStore.getStateVariables());

        int numStates = stateStore.getNumStates();



        for (LinkedList<StateVariable> dependency : allDependencies) {
            for (StateVariable stateVariable : dependency) {
                System.out.print(stateVariable.getLabel() + " ");
            }
            System.out.println();
        }

        /**
         * Assign traffic to states!!
         */

        String trafficAssignmentFile = initial + "Size_TfcNo_NumStates_DependencyNo/"
                 + size + "_" + trafficNo + "_" + numStates + "_" + depRun + ".txt";

        HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies =
                StateStore.assignStates2Traffic(trafficStore, allDependencies,
                trafficAssignmentFile, assignmentLine);

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

        CartesianProduct(stateCopyCombinations, numStates, stateVariables,
                0, new LinkedList<>(), trafficStore, stateStore, dependencies, graph, dist);


        System.out.println("Best combination traffic: " + minCombination);

        System.out.println(bestCombination);



        TrafficHeuristic trafficHeuristic = new TrafficHeuristic(graph,
                                                                trafficStore,
                                                                1,
                                                                TrafficHeuristic.hType.shortestpath,
                                                                false,
                                                                false
        );

        System.out.println("Shortest path traffic is: " + trafficHeuristic.getTotalTraffic());


    }

    private static void CartesianProduct(HashMap<StateVariable, LinkedList<LinkedList<String>>> stateCombinations,
                                         int numStates,
                                         ArrayList<StateVariable> stateVariables,
                                         int currentLevel,
                                         LinkedList<String> buildup,
                                         TrafficStore trafficStore,
                                         StateStore stateStore,
                                         HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies,
                                         ListGraph graph,
                                         HashMap<Vertex, HashMap<Vertex, Integer>> dist){

        if(currentLevel==numStates){
            LinkedList<String> combination = buildup;

            int combinationTraffic = 0;
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                int pathSize = 0;
                LinkedList<StateVariable> currentDep = dependencies.get(trafficDemand);
                Vertex currentSrc = trafficDemand.getSource();

                for(int i=0 ; i<currentDep.size() ; i++){
                    StateVariable stateVariable = currentDep.get(i);

                    int minStatePathSize = Integer.MAX_VALUE;
                    String minStateCopy = null;

                    for(String string : combination){

                        String[] splitted = string.split(",");
                        StateVariable splittedVariable = stateStore.getStateVariable(splitted[0]);

                        if(splittedVariable.equals(stateVariable)){
                            Vertex currentDst = graph.getVertex(Integer.parseInt(splitted[1]));
                            int statePathSize = dist.get(currentSrc).get(currentDst);

                            if(statePathSize < minStatePathSize){
                                minStatePathSize = statePathSize;
                                minStateCopy = string;
                            }
                        }
                    }

                    pathSize = pathSize + minStatePathSize;

                    String[] splitted = minStateCopy.split(",");
                    currentSrc = graph.getVertex(Integer.parseInt(splitted[1]));
                }

                Vertex currentDst = trafficDemand.getDestination();
                pathSize = pathSize + dist.get(currentSrc).get(currentDst);
                combinationTraffic = combinationTraffic + pathSize;
            }

            if(currentCombination%500 ==0){
                System.out.println(combination + "\t" + "Processed: " + currentCombination + "/" + numCombinations + ", " +
                        Math.round(((double) currentCombination / numCombinations) * 10000) / 100.0 + "%");
            }

            currentCombination++;

            if(combinationTraffic<minCombination){
                bestCombination = new LinkedList<>();
                for(String string: combination){
                    bestCombination.add(string);
                }
                minCombination = combinationTraffic;
            }

        }
        else{
            for(LinkedList<String> linkedList : stateCombinations.get(stateVariables.get(currentLevel))){
                buildup.addAll(linkedList);
                CartesianProduct(stateCombinations, numStates, stateVariables,
                        currentLevel+1, buildup, trafficStore, stateStore, dependencies, graph, dist);
                for(String stateCopy: linkedList)
                    buildup.remove(stateCopy);
            }
        }
    }

}
