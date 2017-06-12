package statefulsharding.heuristic;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.LoadGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static statefulsharding.heuristic.TrafficHeuristic.hType.betweenness;
import static statefulsharding.heuristic.TrafficHeuristic.hType.shortestpath;

public class EvaluateTrafficHeuristicWattsStrogatz {

    public static void main(String[] args) {

        /*
        Wattz Strogatz specific info:
         */
        double pStart = 0.25;
        double pFinish = 0.75;
        //double p = 0.1;
        boolean decrement = true;

        int startsize = 72;
        int endSize = 96;
        boolean nullp = false;
        int runs = 20;
        int actualRuns = 0;
        int startTraffic = 1;
        int endTraffic = 20;
        int startCopies = 1;
        int endCopies = 4;

        TrafficHeuristic.hType type = shortestpath;
        TrafficHeuristic.hType partitioning = TrafficHeuristic.hType.fixedcopies;

        boolean multipleStates = true;
        boolean rearrangeStates = false;
        boolean saveSolution = true;
        boolean createDirectory = true;



        for(double p = pStart ; p<=pFinish ; p = p + 0.25) {
            for (int size = startsize; size <= endSize; size = size + 12) {

                String graphType = "Watts-Strogatz";
                String experimentType = "Heuristic-partition";
                String directory = graphType + "_" + experimentType + "_" + size;
                String experiment = graphType + "_" + experimentType + "_" + size + "_p_" + p;

                if (createDirectory) {
                    File dir = new File("analysis/" + directory);
                    dir.mkdir();
                }

                for (int traffic = startTraffic; traffic <= endTraffic; traffic++) {
                    for (int numCopies = startCopies; numCopies <= endCopies; numCopies++) {

                        int run = 1;

                        while (run <= runs) {

                            System.out.println("Size: " + size + ", p: " + p + ", traffic: " + traffic
                                    + ", copy: " + numCopies + ", run: " + run);

                            TrafficStore trafficStore = new TrafficStore();

                            String graphLocation = "topologies_traffic/Traffic/WS_graph_" + p +
                                    "/WS_graph" + size + "_" + p + "_8.csv";
                            ListGraph graph = LoadGraph.GraphParserJ(graphLocation, Integer.MAX_VALUE, decrement);
                            TrafficGenerator.fromFileLinebyLine(graph, trafficStore, traffic, 1, decrement,
                                    "topologies_traffic/Traffic/WS_Traffic/WS_Traffic" + size +
                                            ".csv");

                            HashMap<Vertex, ListGraph> partitions = Partitioning.EvolutionaryPartition(graph, numCopies,
                                    100,
                                    "random",
                                    "betweenness",
                                    trafficStore,
                                    false,
                                    false,
                                    null);

                            ArrayList<Vertex> sortedVertices = new ArrayList<>(partitions.keySet());


                            TrafficHeuristic trafficHeuristic = new TrafficHeuristic(graph,
                                                                    trafficStore,
                                                                    numCopies,
                                                                    partitioning,
                                                                    sortedVertices,
                                                                    multipleStates,
                                                                    rearrangeStates);

                            LinkedHashMap<TrafficDemand, Path> routingSolution = trafficHeuristic.getRoutingSolution();

                            LinkedHashMap<TrafficDemand, Path> routingSolutionAugmented = null;
                            if (numCopies > 1) {

                                routingSolutionAugmented =
                                        StateSync.augmentEdges(graph, sortedVertices, routingSolution);
                            }

                            for (TrafficDemand trafficDemand : routingSolution.keySet()) {
                                try {
                                    /*
                                    System.out.println("Source: " + trafficDemand.getSource().getLabel() +
                                            " Destination: " + trafficDemand.getDestination().getLabel());
                                    routingSolution
                                    .get(trafficDemand)
                                    .getEdges()
                                    .forEach(edge -> System.out.println(edge.getSource().getLabel() + " -> " +
                                            edge.getDestination().getLabel()));
                                            */

                                }
                                catch (NullPointerException npe) {
                                    nullp = true;
                                    break;
                                }
                            }

                            trafficStore.clear();
                            graph.clear();
                            graph = null;
                            trafficStore = null;

                            if (!nullp) {
                                //System.out.println("Traffic: " + trafficHeuristic.getTotalTraffic());
                                if (saveSolution) {
                                    String writeSol = "analysis/" + directory + "/" + experiment
                                            + "_traffic_" + traffic + "_run_" + run + "_" + type.toString();

                                    trafficHeuristic.writeSolution(writeSol);

                                    if (numCopies > 1) {
                                        String writeSol2 = writeSol + "_copy_" + numCopies + "_augmentedSoln";
                                        trafficHeuristic.writeSolution(routingSolutionAugmented, writeSol2);
                                    }

                                }
                                run++;
                                actualRuns++;
                            } else {
                                System.out.println("FOUND A NULL POINTER!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                nullp = false;
                                actualRuns++;
                            }

                        }
                    }
                }
            }

        }

        System.out.println("Actual runs: " + actualRuns);

    }



}