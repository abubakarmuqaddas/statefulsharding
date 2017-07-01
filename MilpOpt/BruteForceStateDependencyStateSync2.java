package statefulsharding.MilpOpt;

import statefulsharding.State.GenerateStates;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;


public class BruteForceStateDependencyStateSync2 {

    private static long numCombinations;
    private static long currentCombination;
    private static double trafficCombination;
    private static double alpha;
    private static boolean stateSyncRequired;
    private static double edgeMin;
    private static LinkedList<String> bestEdgeCombination;

    static {
        numCombinations = 1;
        currentCombination = 0;
        trafficCombination = 0;
        edgeMin = Double.MAX_VALUE;
        bestEdgeCombination = new LinkedList<>();
        alpha=0.1;
        stateSyncRequired = true;
    }

    public static void main(String[] args) {

        /**
         * Generate Graph
         */

        boolean copySameSwitchAllowed = true;
        int capacity = Integer.MAX_VALUE;
        int size = 7;
        int trafficNo = 1;
        int depSize = 1;
        int depRun = 1;
        int trafficStart = 1;
        int trafficEnd = 10;
        int assignmentLineStart = 1;
        int assignmentLineFinish = 1;
        boolean copiesLimited = false;
        int numStatesPerSwitch = 1;
        int[] numCopies = new int[]{3,1,1,1,1,1,1,1};

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
         * Generate list of all paths
         */

        HashMap<Vertex, HashMap<Vertex, Path>> allPaths = new HashMap<>();

        for(Vertex src : graph.getVertices()){
            allPaths.put(src, new HashMap<>());
            for(Vertex dst : graph.getVertices()){
                if(!src.equals(dst)){
                    allPaths.get(src).put(dst, ShortestPath.dijsktra(graph, src, dst));
                }
            }
        }

        /**
         * Traffic utilization per edge
         */

        HashMap<Edge, Double> edgeUsage = new HashMap<>();
        graph.getallEdges().forEach(edge -> edgeUsage.put(edge, 0.0));

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

            LinkedList<LinkedList<Integer>> result = null;

            if(copySameSwitchAllowed) {
                result = getNCombinations
                        .getPermutations(stateVariable.getCopies(), graph.getVerticesInt());
            }
            else{
                result = getNCombinations
                        .getCombinations(graph.getVerticesInt(), stateVariable.getCopies());
            }

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

        ArrayList<StateVariable> stateVariables = new ArrayList<>(stateStore.getStateVariables());

        int numStates = stateStore.getNumStates();



        for (LinkedList<StateVariable> dependency : allDependencies) {
            for (StateVariable stateVariable : dependency) {
                System.out.print(stateVariable.getLabel() + " ");
            }
            System.out.println();
        }

        LinkedList<LinkedList<String>> allBestCombinations = new LinkedList<>();
        LinkedList<Double> bestTraffic = new LinkedList<>();

        for(trafficNo = trafficStart ; trafficNo<=trafficEnd ; trafficNo++) {

            /**
             * Get Traffic!
             */

            TrafficStore trafficStore = new TrafficStore();

            TrafficGenerator.fromFileLinebyLine(graph, trafficStore, trafficNo, 1, true,
                    trafficFile);

            /**
             * Assign traffic to states!!
             */

            String trafficAssignmentFile = initial + "Size_TfcNo_NumStates_DependencyNo/"
                    + size + "_" + trafficNo + "_" + numStates + "_" + depRun + ".txt";



            for (int assignmentLine = assignmentLineStart; assignmentLine <= assignmentLineFinish; assignmentLine++) {

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
                        allPaths,
                        edgeUsage,
                        copiesLimited,
                        numStatesPerSwitch,
                        assignmentLine,
                        stateSyncInfo);


                System.out.println("Best combination: " + bestEdgeCombination + ", traffic: " +
                        trafficCombination);


                //System.out.println(bestCombination);

                LinkedList<String> currentBestcombination = new LinkedList<>();
                for (String string : bestEdgeCombination) {
                    currentBestcombination.add(string);
                }
                allBestCombinations.add(currentBestcombination);
                bestTraffic.add(trafficCombination);

                currentCombination = 0;
                bestEdgeCombination = new LinkedList<>();
                edgeMin = Double.MAX_VALUE;
                bestEdgeCombination = new LinkedList<>();
                trafficCombination = 0.0;

            }
        }

        System.out.println(allBestCombinations.toString());
        System.out.println(bestTraffic.toString());




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
                                         HashMap<Vertex, HashMap<Vertex, Path>> allPaths,
                                         HashMap<Edge, Double> edgeUsage,
                                         boolean copiesLimited,
                                         int numStatesPerSwitch,
                                         int assignmentLine,
                                         HashMap<StateVariable, LinkedList<LinkedList<Integer>>> stateSyncInfo){

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

            edgeUsage.keySet().forEach(edge -> edgeUsage.put(edge, 0.0));

            double traffic = 0;
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
                            //int statePathSize = dist.get(currentSrc).get(currentDst);

                            int statePathSize;

                            if(!currentSrc.equals(currentDst))
                                statePathSize = allPaths.get(currentSrc).get(currentDst).getSize();
                            else
                                statePathSize=0;

                            if(statePathSize < minStatePathSize){
                                minStatePathSize = statePathSize;
                                minStateCopy = string;
                            }
                        }
                    }

                    traffic += trafficDemand.getDemand()*minStatePathSize;

                    String[] splitted = minStateCopy.split(",");

                    Vertex currentDst = graph.getVertex(Integer.parseInt(splitted[1]));
                    if(!currentSrc.equals(currentDst))
                        for (Edge edge : allPaths.get(currentSrc).get(currentDst).getEdges()) {
                            double temp = edgeUsage.get(edge);
                            temp += trafficDemand.getDemand();
                            edgeUsage.put(edge, temp);
                        }

                    currentSrc = graph.getVertex(Integer.parseInt(splitted[1]));
                }

                Vertex currentDst = trafficDemand.getDestination();

                if(!currentSrc.equals(currentDst)) {
                    traffic += trafficDemand.getDemand() * allPaths.get(currentSrc).get(currentDst).getSize();
                    for (Edge edge : allPaths.get(currentSrc).get(currentDst).getEdges()) {
                        double temp = edgeUsage.get(edge);
                        temp += trafficDemand.getDemand();
                        edgeUsage.put(edge, temp);
                    }
                }

            }

            if(stateSyncRequired) {
                updateSyncTraffic(graph, stateStore, buildup, allPaths, stateSyncInfo, edgeUsage);
            }

            /**
               Get edge with maximum usage
             */
            double currentEdgeMax = Double.MIN_VALUE;
            for (Edge edge : edgeUsage.keySet()) {
                if(edgeUsage.get(edge)>currentEdgeMax)
                    currentEdgeMax = edgeUsage.get(edge);
            }

            /**
             * Compare most congested edge with global minimum
             */
            if(currentEdgeMax<edgeMin){
                bestEdgeCombination = new LinkedList<>();
                for(String string: buildup){
                    bestEdgeCombination.add(string);
                }
                edgeMin = currentEdgeMax;
                trafficCombination = traffic;
            }

            if(((currentCombination/numCombinations)*100.0)%20 ==0){

                double pCent = Math.round(((double) currentCombination / numCombinations) * 100000000) / 1000000.0;

                System.out.println("Run: " + assignmentLine
                        + " Processed: " + currentCombination + "/" + numCombinations + ", " +
                        pCent + "%");

            }

            currentCombination++;
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
                        allPaths,
                        edgeUsage,
                        copiesLimited,
                        numStatesPerSwitch,
                        assignmentLine,
                        stateSyncInfo
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

    private static void updateSyncTraffic(ListGraph graph,
                                          StateStore stateStore,
                                          LinkedList<String> buildup,
                                          HashMap<Vertex, HashMap<Vertex, Path>> allPaths,
                                          HashMap<StateVariable, LinkedList<LinkedList<Integer>>> stateSyncInfo,
                                          HashMap<Edge, Double> edgeUsage){

        for(StateVariable stateVariable : stateStore.getStateVariables()){

            if(!(stateVariable.getCopies()>1))
                continue;

            LinkedList<Vertex> locations = new LinkedList<>();

            for(String string : buildup){
                String[] splitted = string.split(",");

                if(stateStore.getStateVariable(splitted[0]).equals(stateVariable))
                    locations.add(graph.getVertex(Integer.parseInt(splitted[1])));
            }

            if (locations.size()==1)
                continue;

            LinkedList<LinkedList<Integer>> combinations = stateSyncInfo.get(stateVariable);

            for(LinkedList<Integer> combination: combinations){
                try {
                    Vertex left = locations.get(combination.get(0) - 1);
                    Vertex right = locations.get(combination.get(1) - 1);

                /*
                Left to right
                 */

                    for (Edge edge : allPaths.get(left).get(right).getEdges()) {
                        double temp = edgeUsage.get(edge);
                        temp += alpha;
                        edgeUsage.put(edge, temp);
                    }

                /*
                Right to left
                 */

                    for (Edge edge : allPaths.get(right).get(left).getEdges()) {
                        double temp = edgeUsage.get(edge);
                        temp += alpha;
                        edgeUsage.put(edge, temp);
                    }
                }
                catch(IndexOutOfBoundsException e){
                    System.out.println(buildup);
                }
            }
        }
    }






}
