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

        /**
         * Generate Graph
         */

        int capacity = Integer.MAX_VALUE;
        int size = 7;
        int trafficNo = 1;
        int depSize = 3;
        int depRun = 1;
        int assignmentLine = 1;
        String initial = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/";

        /**
         * Generate states and numCopies
         */

        String filename = initial + "StateDependencies/" + "StateDependencies_" + depSize + "_" + depRun + ".txt";

        StateStore stateStore = new StateStore();

        LinkedList<LinkedList<StateVariable>> allDependencies =
                StateStore.readStateDependency(filename, stateStore);

        stateStore.setStateCopies("a",1);
        stateStore.setStateCopies("b",1);
        stateStore.setStateCopies("c",1);
        //stateStore.setStateCopies("d",1);

        /**
         * Generate graph
         */

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        /*
         * Generate distances from all vertices
         */

        HashMap<Vertex, HashMap<Vertex, Integer>> dist = ShortestPath.FloydWarshall(graph, false,
                null);



        /**
         * Generate all state combinations
         */

        HashMap<StateVariable, LinkedList<LinkedList<String>>> stateCopyCombinations = new HashMap<>();

        LinkedList<Integer> vertices = new LinkedList<>();
        for (int i = 0; i < graph.getNumVertices(); i++) {
            vertices.add(i);
        }

        for (StateVariable stateVariable : stateStore.getStateVariables()) {
            getNCombinations getCombinations = new getNCombinations(vertices, stateVariable.getCopies());
            LinkedList<LinkedList<Integer>> result = getCombinations.getResult();

            stateCopyCombinations.put(stateVariable, new LinkedList<>());
            for(LinkedList<Integer> combination : result){

                LinkedList<String> temp = new LinkedList<>();

                int i=1;
                for(Integer integer : combination){

                    String stateCopy = stateVariable.getLabel() + ","+ i+ "," + integer;

                    temp.add(stateCopy);

                    i++;
                }

                stateCopyCombinations.get(stateVariable).add(temp);
            }
        }

        ArrayList<StateVariable> stateVariables = new ArrayList(stateStore.getStateVariables());

        LinkedList<LinkedList<String>> combinations = new LinkedList<>();
        int numStates = stateStore.getNumStates();

        CartesianProduct(stateCopyCombinations, combinations, numStates,stateVariables, 0, null);

        for (LinkedList<StateVariable> dependency : allDependencies) {
            for (StateVariable stateVariable : dependency) {
                System.out.print(stateVariable.getLabel() + " ");
            }
            System.out.println();
        }

        /**
         * Assign traffic to states!!
         */

        HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies = new HashMap<>();

        TrafficStore trafficStore = new TrafficStore();

        TrafficGenerator.fromFile(graph, trafficStore,
                initial +
                        "MANHATTAN-UNWRAPPED_deterministicTfc-7/" +
                        "MANHATTAN-UNWRAPPED_deterministicTfc-7_" + trafficNo + "_traffic.txt"
        );

        /*

        for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            dependencies.put(trafficDemand,
                    allDependencies.get(ThreadLocalRandom.current().nextInt(
                            0, allDependencies.size()
                    )));
        }
        */

        String trafficAssignmentFile = initial + "Size_TfcNo_NumStates_DependencyNo/"
                 + size + "_" + trafficNo + "_" + numStates + "_" + depRun + ".txt";

        dependencies = StateStore.assignStates2Traffic(trafficStore, allDependencies,
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

        LinkedList<String> bestCombination = null;
        int x=1;
        int minCombination = Integer.MAX_VALUE;

        for (LinkedList<String> combination : combinations) {

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
                            Vertex currentDst = graph.getVertex(Integer.parseInt(splitted[2]));
                            int statePathSize = dist.get(currentSrc).get(currentDst);

                            if(statePathSize < minStatePathSize){
                                minStatePathSize = statePathSize;
                                minStateCopy = string;
                            }
                        }
                    }

                    pathSize = pathSize + minStatePathSize;

                    String[] splitted = minStateCopy.split(",");
                    currentSrc = graph.getVertex(Integer.parseInt(splitted[2]));
                }

                Vertex currentDst = trafficDemand.getDestination();
                pathSize = pathSize + dist.get(currentSrc).get(currentDst);
                combinationTraffic = combinationTraffic + pathSize;
            }

            if(x%100 ==0)
                System.out.println("Processed: " + x + "/" + combinations.size() + ", " +
                        Math.round(((double)x/combinations.size())*10000)/100.0 + "%");

            x++;

            if(combinationTraffic<minCombination){
                bestCombination = combination;
                minCombination = combinationTraffic;
            }
        }

        System.out.println("Best combination traffic: " + minCombination);

        System.out.println(bestCombination);
    }

    private static void CartesianProduct(HashMap<StateVariable, LinkedList<LinkedList<String>>> stateCombinations,
                                         LinkedList<LinkedList<String>> prod,
                                         int numStates,
                                         ArrayList<StateVariable> stateVariables,
                                         int currentLevel,
                                         LinkedList<String> buildup){

        if(currentLevel==numStates){
            LinkedList<String> stateCopies = new LinkedList<>(buildup);
            prod.add(stateCopies);
            System.out.println(stateCopies);
        }
        else{
            for(int i=currentLevel ; i<stateCombinations.keySet().size(); i++){

                if(currentLevel==0)
                    buildup = new LinkedList<>();

                for(LinkedList<String> linkedList : stateCombinations.get(stateVariables.get(currentLevel))){

                    buildup.addAll(linkedList);
                    CartesianProduct(stateCombinations, prod, numStates, stateVariables,
                            currentLevel+1, buildup);
                    for(String stateCopy: linkedList)
                        buildup.remove(stateCopy);

                }
            }
        }
    }

}
