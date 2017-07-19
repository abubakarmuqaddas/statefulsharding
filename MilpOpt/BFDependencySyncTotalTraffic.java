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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Brute force, minimizing the total traffic in the network
 * State dependency and state sync included
 */


public class BFDependencySyncTotalTraffic {

    private static long numCombinations;
    private static long currentCombination;
    private static double minCombination;
    private static LinkedList<String> bestCombination;
    private static boolean stateSyncRequired;
    private static double bestSyncTraffic;

    static {
        numCombinations = 1;
        currentCombination = 0;
        minCombination = Double.MAX_VALUE;
        bestCombination = new LinkedList<>();
        stateSyncRequired = true;
        bestSyncTraffic = 0;
    }

    public static void main(String[] args) {

        /**
         * Generate Graph
         */

        boolean copySameSwitchAllowed = true;
        double alpha;
        double alphaStart = 0.2;
        double alphaEnd = 0.2;
        double alphaInterval = 0.1;
        int capacity = Integer.MAX_VALUE;
        int size = 9;
        int trafficNo = 1;
        int depSize = 1;
        int depRun = 1;
        int trafficStart = 1;
        int trafficEnd = 1;
        int assignmentLineStart = 1;
        int assignmentLineFinish = 1;
        boolean copiesLimited = false;
        int numStatesPerSwitch = 1;
        int[] numCopies = new int[]{3,1,1,1};

        String initial = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/";
        String initial2 = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/";

        String trafficFile = initial2 +
                            "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" + size +
                            ".csv";


        /**
         * Generate graph
         */

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        /**
         * Generate distances from all vertices
         */

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        LinkedList<LinkedList<String>> allBestCombinations = new LinkedList<>();
        LinkedList<Double> bestTraffic = new LinkedList<>();
        LinkedList<Double> syncTraffic = new LinkedList<>();
        LinkedList<Boolean> used = new LinkedList<>();
        LinkedList<Integer> numLocationsUsed = new LinkedList<>();

        /**
         * Generate states and numCopies
         */

        String filename = initial + "StateDependencies/" + "StateDependencies_" + depSize + "_" + depRun + ".txt";

        StateStore stateStore = new StateStore();

        LinkedList<LinkedList<StateVariable>> allDependencies =
                StateStore.readStateDependency(filename, stateStore);

        for (int i = 0; i < depSize; i++) {
            stateStore.setStateCopies(GenerateStates.getCharForNumber(i), numCopies[i]);
        }
        /**
         * Generate all state combinations
         */

        HashMap<StateVariable, LinkedList<LinkedList<String>>> stateCopyCombinations = new HashMap<>();

        for (StateVariable stateVariable : stateStore.getStateVariables()) {

            LinkedList<LinkedList<Integer>> result = null;

            if (copySameSwitchAllowed) {
                result = getNCombinations.getPermutations(stateVariable.getCopies(), graph.getVerticesInt());
            }
            else {
                result = getNCombinations.getCombinations(graph.getVerticesInt(), stateVariable.getCopies());
            }

            stateCopyCombinations.put(stateVariable, new LinkedList<>());
            for (LinkedList<Integer> combination : result) {
                LinkedList<String> temp = new LinkedList<>();
                for (Integer integer : combination) {
                    String stateCopy = stateVariable.getLabel() + "," + integer;
                    temp.add(stateCopy);
                }

                stateCopyCombinations.get(stateVariable).add(temp);
            }
            numCombinations = numCombinations * stateCopyCombinations.get(stateVariable).size();
        }

        ArrayList<StateVariable> stateVariables = new ArrayList(stateStore.getStateVariables());

        int numStates = stateStore.getNumStates();


        for (LinkedList<StateVariable> dependency : allDependencies) {
            for (StateVariable stateVariable : dependency) {
                System.out.print(stateVariable.getLabel() + " ");
            }
            System.out.println();
        }

        for(trafficNo = trafficStart ; trafficNo<=trafficEnd ; trafficNo++) {

            /**
             * Get Traffic!
             */

            TrafficStore trafficStore = new TrafficStore();

            TrafficGenerator.fromFileLinebyLine(
                    graph,
                    trafficStore,
                    trafficNo,
                    1,
                    true,
                    trafficFile
            );


            /**
             * Assign traffic to states!!
             */


            String trafficAssignmentFile = initial + "Size_TfcNo_NumStates_DependencyNo/"
                    + size + "_" + trafficNo + "_" + numStates + "_" + depRun + ".txt";

            for(alpha = alphaStart ; alpha<=alphaEnd ; alpha = alpha + alphaInterval) {

                System.out.println("Alpha: " + alpha);

                for (int assignmentLine = assignmentLineStart; assignmentLine <= assignmentLineFinish;
                     assignmentLine++) {

                    HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies =
                            StateStore.assignStates2Traffic(trafficStore, allDependencies,
                                    trafficAssignmentFile, assignmentLine);

                    /*
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
                    */

                    HashMap<StateVariable, LinkedList<LinkedList<Integer>>> stateSyncInfo =
                            getStateSyncInfo(stateStore);

                    CartesianProduct(stateCopyCombinations,
                            numStates,
                            stateVariables,
                            0,
                            new LinkedList<>(),
                            trafficStore,
                            stateStore,
                            dependencies,
                            graph,
                            dist,
                            copiesLimited,
                            numStatesPerSwitch,
                            assignmentLine,
                            stateSyncInfo,
                            alpha);


                    System.out.println("Best combination traffic: " + minCombination);

                    System.out.println(bestCombination);

                    LinkedList<String> currentBestcombination = new LinkedList<>();
                    for (String string : bestCombination) {
                        currentBestcombination.add(string);
                    }

                    allBestCombinations.add(currentBestcombination);
                    bestTraffic.add(Math.round(minCombination * 100.0) / 100.0);
                    syncTraffic.add(Math.round(bestSyncTraffic * 100.0) / 100.0);

                    currentCombination = 0;
                    minCombination = Integer.MAX_VALUE;
                    bestCombination = new LinkedList<>();

                    used.add(checkAllCopiesUsed(graph,
                            trafficStore,
                            stateStore,
                            dependencies,
                            dist,
                            currentBestcombination));



                    numLocationsUsed.add(currentBestcombination.size());

                }

            }

        }

        System.out.println(allBestCombinations.toString());
        System.out.println(bestTraffic.toString());
        System.out.println(syncTraffic.toString());
        System.out.println("allCopiesUsed: " + used.toString());
        System.out.println("LocationsUsed: " + numLocationsUsed.toString());

        double currentAlpha = alphaStart;
        System.out.println("alpha totalTraffic dataTraffic syncTraffic numCopiesUsed");
        for(int i=0 ; i<bestTraffic.size() ; i++){
            System.out.println(Math.round(currentAlpha*100)/100.0 + " "
                    + Math.round(bestTraffic.get(i)*100.0/graph.getVertices().size())/100.0 + " "
                    + Math.round((bestTraffic.get(i)-syncTraffic.get(i))*100.0/graph.getVertices().size())/100.0 + " "
                    + Math.round(syncTraffic.get(i)*100.0/graph.getVertices().size())/100.0 + " " +
                    numLocationsUsed.get(i));
            currentAlpha+=alphaInterval;
        }

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
                                         HashMap<Vertex, HashMap<Vertex, Integer>> dist,
                                         boolean copiesLimited,
                                         int numStatesPerSwitch,
                                         int assignmentLine,
                                         HashMap<StateVariable, LinkedList<LinkedList<Integer>>> stateSyncInfo,
                                         double alpha){

        if(currentLevel==numStates){

            if(copiesLimited){
                for(int i=0 ; i<buildup.size() ; i++){

                    String[] splitted = buildup.get(i).split(",");
                    int occurence = 1;
                    int target = Integer.parseInt(splitted[1]);

                    for(int j=i+1 ; j<buildup.size() ; j++){

                        String[] splitted2 = buildup.get(j).split(",");

                        int other = Integer.parseInt(splitted2[1]);


                        if(other==target)
                            occurence++;
                    }

                    if(occurence>numStatesPerSwitch) {
                        return;
                    }
                }
            }




            double combinationTraffic = 0;
            double syncTraffic = 0;
            double dataTraffic = 0;

            LinkedList<Integer> temp = new LinkedList<>();

            for(String string : buildup){
                String[] splitted = string.split(",");
                temp.add(Integer.parseInt(splitted[1]));
            }

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                int pathSize = 0;
                LinkedList<StateVariable> currentDep = dependencies.get(trafficDemand);
                Vertex currentSrc = trafficDemand.getSource();

                for(int i=0 ; i<currentDep.size() ; i++){
                    StateVariable stateVariable = currentDep.get(i);

                    int minStatePathSize = Integer.MAX_VALUE;
                    String minStateCopy = null;

                    for(String string : buildup){

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

            if(stateSyncRequired) {
                syncTraffic = alpha * getSyncTraffic(graph, stateStore, buildup, dist, stateSyncInfo);
                combinationTraffic += syncTraffic;
            }

            if(currentCombination%1000 ==0){

                double pCent = Math.round(((double) currentCombination / numCombinations) * 100000000) / 1000000.0;

                System.out.println("Run: " + assignmentLine
                        + " Processed: " + currentCombination + "/" + numCombinations + ", " +
                        pCent + "%");

            }

            currentCombination++;

            if(combinationTraffic<minCombination){
                bestCombination = new LinkedList<>();
                for(String string: buildup){
                    bestCombination.add(string);
                }
                minCombination = combinationTraffic;
                bestSyncTraffic = syncTraffic;
            }

        }
        else{
            for(LinkedList<String> linkedList : stateCombinations.get(stateVariables.get(currentLevel))){

                buildup.addAll(linkedList);

                CartesianProduct(
                        stateCombinations,
                        numStates,
                        stateVariables,
                        currentLevel+1,
                        buildup,
                        trafficStore,
                        stateStore,
                        dependencies,
                        graph,
                        dist,
                        copiesLimited,
                        numStatesPerSwitch,
                        assignmentLine,
                        stateSyncInfo,
                        alpha
                );

                for(String stateCopy: linkedList)
                    buildup.remove(stateCopy);
            }
        }
    }

    private static HashMap<StateVariable, LinkedList<LinkedList<Integer>>> getStateSyncInfo(StateStore stateStore) {

        HashMap<StateVariable, LinkedList<LinkedList<Integer>>> stateSyncInfo = new HashMap<>();

        for (StateVariable stateVariable : stateStore.getStateVariables()) {

            if (!(stateVariable.getCopies() > 1))
                continue;

            LinkedList<Integer> temp = new LinkedList<>();

            for (int i = 1; i <= stateVariable.getCopies(); i++) {
                temp.add(i);
            }

            LinkedList<LinkedList<Integer>> comb = getNCombinations.getCombinations(temp, 2);

            stateSyncInfo.put(stateVariable, comb);
        }
        return stateSyncInfo;
    }

    private static int getSyncTraffic(ListGraph graph,
                               StateStore stateStore,
                               LinkedList<String> buildup,
                               HashMap<Vertex, HashMap<Vertex, Integer>> dist,
                               HashMap<StateVariable, LinkedList<LinkedList<Integer>>> stateSyncInfo){

        int syncTraffic = 0;

        for(StateVariable stateVariable : stateStore.getStateVariables()){

            if(!(stateVariable.getCopies()>1))
                continue;

            LinkedList<Vertex> locations = new LinkedList<>();

            for(String string : buildup){
                String[] splitted = string.split(",");

                if(stateStore.getStateVariable(splitted[0]).equals(stateVariable))
                    locations.add(graph.getVertex(Integer.parseInt(splitted[1])));
            }

            if(locations.size()==1)
                continue;

            LinkedList<LinkedList<Integer>> combinations = stateSyncInfo.get(stateVariable);

            for(LinkedList<Integer> combination: combinations){
                try {
                    syncTraffic +=
                            dist.get(locations.get(combination.get(0) - 1)).get(locations.get(combination.get(1) - 1));
                    syncTraffic +=
                            dist.get(locations.get(combination.get(1) - 1)).get(locations.get(combination.get(0) - 1));
                }
                catch(IndexOutOfBoundsException e){
                    //
                }

            }
        }


        return syncTraffic;

    }

    private static boolean checkAllCopiesUsed(ListGraph graph,
                                              TrafficStore trafficStore,
                                              StateStore stateStore,
                                              HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies,
                                              HashMap<Vertex, HashMap<Vertex, Integer>> dist,
                                              LinkedList<String> buildup){

        HashMap<StateCopy, Boolean> stateUsage = new HashMap<>();

        for(StateVariable stateVariable : stateStore.getStateVariables()){
            LinkedList<StateCopy> stateCopies = stateStore.getStateCopies(stateVariable);

            int i=0;
            for(String string : buildup){

                String[] splitted = string.split(",");
                StateVariable splittedVariable = stateStore.getStateVariable(splitted[0]);

                if(splittedVariable.equals(stateVariable)){
                    stateCopies.get(i).setVertex(graph.getVertex(Integer.parseInt(splitted[1])));
                    stateUsage.put(stateCopies.get(i), false);
                    i++;
                }
            }
        }

        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

            LinkedList<StateVariable> currentDep = dependencies.get(trafficDemand);
            Vertex currentSrc = trafficDemand.getSource();

            for(int i=0 ; i<currentDep.size() ; i++){
                StateVariable stateVariable = currentDep.get(i);

                int minStatePathSize = Integer.MAX_VALUE;
                String minStateCopy = null;

                for(String string : buildup){

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


                String[] splitted = minStateCopy.split(",");
                currentSrc = graph.getVertex(Integer.parseInt(splitted[1]));
                stateUsage.put(stateStore.getStateCopy(stateVariable, currentSrc), true);
            }

            boolean currentAllUsed = true;
            for(StateCopy stateCopy : stateUsage.keySet()){
                if(!stateUsage.get(stateCopy)){
                    currentAllUsed = false;
                }
            }

            if(currentAllUsed) {
                return true;
            }
        }

        return false;
    }





}
