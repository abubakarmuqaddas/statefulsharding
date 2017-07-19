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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Brute force, minimizing the total traffic in the network
 * State dependency and state sync included
 */


public class BruteForce2{

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
        int trafficStart = 1;
        int trafficEnd = 1;
        int numCopies = 2;

        long numCombinations;
        long currentCombination = 0;
        double minCombination = Double.MAX_VALUE;
        ArrayList<Vertex> bestCombination = new ArrayList<>();
        double bestSyncTraffic = 0;

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

        LinkedList<ArrayList<Vertex>> allBestCombinations = new LinkedList<>();
        LinkedList<Double> bestTraffic = new LinkedList<>();
        LinkedList<Double> syncTraffic = new LinkedList<>();
        LinkedList<Integer> numLocationsUsed = new LinkedList<>();

        /**
         * Generate states and numCopies
         */

        /**
         * Generate all state combinations
         */

        LinkedList<LinkedList<Integer>> combinations = null;

        if (copySameSwitchAllowed) {
            combinations = getNCombinations.getPermutations(numCopies, graph.getVerticesInt());
        }
        else {
            combinations = getNCombinations.getCombinations(graph.getVerticesInt(), numCopies);
        }

        numCombinations = combinations.size();

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


            for(alpha = alphaStart ; alpha<=alphaEnd ; alpha = alpha + alphaInterval) {

                System.out.println("Alpha: " + alpha);
                
                for(LinkedList<Integer> combination : combinations){

                    ArrayList<Vertex> vertices = new ArrayList<>();
                    for(Integer integer : combination){
                        vertices.add(graph.getVertex(integer));
                    }

                    double currentTraffic = 0.0;

                    for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){

                        Vertex source = trafficDemand.getSource();
                        Vertex destination = trafficDemand.getDestination();

                        int minDist = Integer.MAX_VALUE;

                        for(Vertex vertex : vertices){
                            if((dist.get(source).get(vertex) + dist.get(vertex).get(destination))<minDist){
                                minDist = dist.get(source).get(vertex) + dist.get(vertex).get(destination);
                            }
                        }

                        currentTraffic += minDist;
                    }

                    double currentSyncTraffic=0;
                    if(combination.size()>1){
                        currentSyncTraffic = alpha * getSyncTraffic(vertices, graph);
                        currentTraffic += currentSyncTraffic;
                    }

                    if(currentTraffic<minCombination){
                        bestCombination = new ArrayList<>(vertices);
                        minCombination = currentTraffic;
                        bestSyncTraffic = currentSyncTraffic;
                    }

                    currentCombination++;

                    if(currentCombination%1000 ==0){

                        double pCent = Math.round(((double) currentCombination / numCombinations) * 100000000)
                                / 1000000.0;

                        System.out.println(" Processed: " + currentCombination + "/" + numCombinations + ", " +
                                pCent + "%");

                    }


                }

                System.out.println("Best combination traffic: " + minCombination);


                bestCombination.forEach(vertex ->
                    System.out.print(vertex.getLabel() + " ")
                );
                System.out.println();

                allBestCombinations.add(bestCombination);
                numLocationsUsed.add(bestCombination.size());

                bestTraffic.add(Math.round(minCombination * 100.0) / 100.0);
                syncTraffic.add(Math.round(bestSyncTraffic * 100.0) / 100.0);

                currentCombination = 0;
                minCombination = Integer.MAX_VALUE;
                bestCombination = new ArrayList<>();



            }
        }

        System.out.println(allBestCombinations.toString());
        System.out.println(bestTraffic.toString());
        System.out.println(syncTraffic.toString());
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


    private static double getSyncTraffic(ArrayList<Vertex> vertices, ListGraph graph){

        double syncTraffic = 0.0;

        for (int i = 0; i < vertices.size(); i++) {
            for (int j = 0; j < vertices.size(); j++) {

                if (i == j)
                    continue;

                try {
                    syncTraffic += (double) ShortestPath.dijsktra(
                            graph, vertices.get(i), vertices.get(j)
                    ).getSize();
                }
                catch (NullPointerException e) {
                    //
                }

            }
        }

        return syncTraffic;

    }





}
