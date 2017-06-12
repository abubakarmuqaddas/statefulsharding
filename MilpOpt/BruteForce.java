package statefulsharding.MilpOpt;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.heuristic.TrafficHeuristic;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class BruteForce {

    public static void main(String[] args) {

        int capacity = Integer.MAX_VALUE;

        int size = 9;
        int maxTraffic = 10;
        int minCopy = 2;
        int numCopies = 3;
        boolean saveSolution = true;
        boolean createDirectory = true;

        int[] trafficValues = new int[]{3,2,7,4,5,6,1,10,8,9};

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        HashMap<Vertex, HashMap<Vertex, Integer>> dist = ShortestPath.FloydWarshall(graph, false,
                null);

        String graphTypeOld = "MANHATTAN-UNWRAPPED";
        String experimentTypeOld = "deterministicTfc-evaluateTrafficHeuristic-fixCopies";
        String oldExperiment = graphTypeOld + "_" + experimentTypeOld + "_" + size;

        String graphType = "MANHATTAN-UNWRAPPED";
        String experimentType = "deterministicTfc-BruteForce";
        String experiment = graphType + "_" + experimentType + "_" + size;

        if (createDirectory) {
            File dir = new File("analysis/" + experiment);
            dir.mkdir();
        }

        for(int copy = minCopy ; copy<=numCopies ; copy++) {

            /*
             * Creating a List of possible candidate state copy combinations
             */

            LinkedList<Integer> vertices = new LinkedList<>();
            for (int i = 0; i < graph.getNumVertices(); i++) {
                vertices.add(i);
            }

            getNCombinations getCombinations = new getNCombinations(vertices, copy);
            LinkedList<LinkedList<Integer>> copyCombinations = getCombinations.getResult();

            HashMap<LinkedList<Integer>, Integer> Ttot = new HashMap<>();

            LinkedList<Double> trafficSoln = new LinkedList<>();

            for (int i = 0 ; i<maxTraffic ; i++) {

                int traffic = trafficValues[i];

                TrafficStore trafficStore = new TrafficStore();
                TrafficGenerator.fromFile(graph, trafficStore,
                        "analysis/" + oldExperiment + "/" + oldExperiment + "_run_" + traffic
                                + "_traffic.txt");

                LinkedList<Integer> bestCombination = null;
                int minCombination = Integer.MAX_VALUE;

                for (LinkedList<Integer> copyCombination : copyCombinations) {
                    int temp = 0;

                    for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                        int min = Integer.MAX_VALUE;
                        Vertex source = trafficDemand.getSource();
                        Vertex destination = trafficDemand.getDestination();

                        for (Integer integer : copyCombination) {
                            Vertex vertex = graph.getVertex(integer);
                            int currentTfc = dist.get(source).get(vertex) + dist.get(vertex).get(destination);
                            if (currentTfc < min)
                                min = currentTfc;
                        }
                        temp = temp + min;
                    }

                    if (temp < minCombination) {
                        minCombination = temp;
                        bestCombination = copyCombination;
                    }

                    Ttot.put(copyCombination, temp);
                }

                ArrayList<Vertex> copies = new ArrayList<>();

                for (Integer integer : bestCombination) {
                    copies.add(graph.getVertex(integer));
                }

                TrafficHeuristic trafficHeuristic = new TrafficHeuristic(graph,
                        trafficStore,
                        copy,
                        TrafficHeuristic.hType.fixedcopies,
                        copies,
                        true,
                        false);


                if (saveSolution) {
                    String writeSol = "analysis/" + experiment + "/" + experiment
                            + "_traffic_" + traffic + "_BruteForce";
                    trafficHeuristic.writeSolution(writeSol);
                    trafficSoln.add(trafficHeuristic.getTotalTraffic());
                }

            }

            System.out.println(trafficSoln.toString());

        }

    }


}
