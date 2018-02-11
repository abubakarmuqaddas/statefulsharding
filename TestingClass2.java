package statefulsharding;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.StatAlgorithms;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class TestingClass2 {

    public static void main(String[] args) {


        boolean copySameSwitchAllowed = false;
        double alpha=0.9;
        int capacity = Integer.MAX_VALUE;

        int trafficStart = 1;
        int trafficEnd = 100;
        int numCopies = 4;
        int size = 5;
        ArrayList<Vertex> bestCombination = new ArrayList<>();

        double minCombination = Double.MAX_VALUE;
        double bestSyncTraffic = 0;

        String initial2 = "../Google Drive/PhD_Work/Stateful_SDN/snapsharding/";
        String trafficFile = initial2 +
                "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" + size +
                ".csv";

        /*
         * Generate graph
         */

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        /*
         * Generate distances from all vertices
         */

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        /*
         * Generate all state combinations
         */

        LinkedList<LinkedList<Integer>> combinations;

        if (copySameSwitchAllowed) {
            combinations = getNCombinations.getPermutations(numCopies, graph.getVerticesInt());
        }
        else {
            combinations = getNCombinations.getCombinations(graph.getVerticesInt(), numCopies);
        }

        LinkedList<Double> bestTraffic = new LinkedList<>();
        LinkedList<Double> syncTraffic = new LinkedList<>();

        // Single traffic no

        for (int trafficNo = trafficStart; trafficNo <= trafficEnd; trafficNo++) {

            TrafficStore trafficStore = new TrafficStore();
            TrafficGenerator.fromFileLinebyLine(
                    graph,
                    trafficStore,
                    trafficNo,
                    1,
                    false,
                    trafficFile
            );

            for (LinkedList<Integer> combination : combinations) {

                ArrayList<Vertex> vertices = getVerticesFromInteger(graph, combination);

                double currentTraffic = 0.0;

                for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                    Vertex source = trafficDemand.getSource();
                    Vertex destination = trafficDemand.getDestination();

                    int minDist = Integer.MAX_VALUE;

                    for (Vertex vertex : vertices) {

                        int currentDist = dist.get(source).get(vertex) + dist.get(vertex).get(destination);
                        if (currentDist < minDist) {
                            minDist = currentDist;
                        }
                    }

                    currentTraffic += minDist;
                }

                double currentSyncTraffic = alpha * getSyncTraffic(vertices, graph);
                currentTraffic += currentSyncTraffic;

                if (currentTraffic < minCombination) {
                    bestCombination = new ArrayList<>(vertices);
                    minCombination = currentTraffic;
                    bestSyncTraffic = currentSyncTraffic;
                }
            }

            System.out.print("[");
            for(Vertex vertex : bestCombination){
                System.out.print(vertex.getLabel() + ", ");
            }
            System.out.print("] ");
            System.out.println();

            bestTraffic.add(minCombination);
            syncTraffic.add(bestSyncTraffic);

            minCombination = Integer.MAX_VALUE;



        }

        System.out.println("totalTraffic dataTraffic syncTraffic");

        for (int i = 0; i < bestTraffic.size(); i++) {
            System.out.println(//currentAlpha + " " +
                    bestTraffic.get(i) + " "
                            + (bestTraffic.get(i) - syncTraffic.get(i)) + " "
                            + syncTraffic.get(i));

        }

        System.out.println("Mean Traffic: " + StatAlgorithms.Mean(bestTraffic) + ", ConfInterval: " +
            StatAlgorithms.ConfIntervals(bestTraffic,96).getSecond());

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

    private static ArrayList<Vertex> getVerticesFromInteger(ListGraph graph, LinkedList<Integer> integers){

        ArrayList<Vertex> vertices = new ArrayList<>();
        for (Integer integer : integers) {
            vertices.add(graph.getVertex(integer));
        }

        return vertices;

    }


}
