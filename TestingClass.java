package statefulsharding;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.heuristic.TrafficHeuristic;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;

import static statefulsharding.heuristic.TrafficHeuristic.hType.fixedcopies;


public class TestingClass {

    public static void main(String[] args){

        int capacity = Integer.MAX_VALUE;
        int size = 9;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        ArrayList<Vertex> sortedVertices = new ArrayList<>();

        sortedVertices.add(graph.getVertex(48));
        sortedVertices.add(graph.getVertex(68));
        sortedVertices.add(graph.getVertex(23));

        TrafficStore trafficStore = new TrafficStore();

        int numCopies = 3;
        double alpha = 0.2;

        String trafficFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" + size +
                ".csv";

        int traffic = 1;

        TrafficGenerator.fromFileLinebyLine(
                graph,
                trafficStore,
                traffic,
                1,
                true,
                trafficFile
        );

        TrafficHeuristic trafficHeuristicPart = new TrafficHeuristic(graph,
                trafficStore,
                numCopies,
                fixedcopies,
                sortedVertices,
                true,
                false);

        double bestTraffic = trafficHeuristicPart.getTotalTraffic() +
                alpha * getSyncTraffic(numCopies, sortedVertices, graph);

        System.out.println(bestTraffic);

        double totalTraffic = 0.0;

        for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){

            Vertex source = trafficDemand.getSource();
            Vertex destination = trafficDemand.getDestination();

            int minDist = Integer.MAX_VALUE;

            for(Vertex vertex : sortedVertices){
                if((dist.get(source).get(vertex) + dist.get(vertex).get(destination))<minDist){
                    minDist = dist.get(source).get(vertex) + dist.get(vertex).get(destination);
                }
            }

            totalTraffic += minDist;
        }

        System.out.println(totalTraffic + alpha * getSyncTraffic(numCopies, sortedVertices, graph));





    }







    private static double getSyncTraffic(int numCopies, ArrayList<Vertex> vertices, ListGraph graph){

        double syncTraffic = 0.0;

        for (int i = 0; i < numCopies; i++) {
            for (int j = 0; j < numCopies; j++) {

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
