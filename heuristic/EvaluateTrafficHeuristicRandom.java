package statefulsharding.heuristic;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static statefulsharding.heuristic.TrafficHeuristic.hType.*;

public class EvaluateTrafficHeuristicRandom {

    public static void main(String[] args) {

        int size = 6;
        boolean nullp = false;
        int runs = 10;
        int actualRuns = 0;

        int totalTrafficRuns = 10;

        TrafficHeuristic.hType type = random;

        boolean multipleStates = true;
        boolean rearrangeStates = true;
        boolean saveSolution = true;
        boolean readTraffic = true;
        boolean writeTraffic = false;
        boolean createDirectory = true;
        boolean printCentrality = false;

        int numCopies = 6;

        String graphType = "MANHATTAN-UNWRAPPED";
        String experimentType = "deterministicTfc-evaluateTrafficHeuristic";
        String experiment = graphType + "_" + experimentType + "_" + size;

        String graphTypeOld = "MANHATTAN-UNWRAPPED";
        String experimentTypeOld = "deterministicTfc_optimal";
        String oldExperiment = graphTypeOld + "_" + experimentTypeOld + "_" + size;


        if (createDirectory) {
            File dir = new File("analysis/" + experiment);
            dir.mkdir();
        }

        for (int currentTrafficRun = 1 ; currentTrafficRun<=totalTrafficRuns ; currentTrafficRun++) {

            int run = 1;

            while (run <= runs) {

                System.out.println();
                System.out.println();
                System.out.println("############################################################");
                System.out.println("############################################################");
                System.out.println("#####################              #########################");
                System.out.println("#####################    Run: " + run + "    #########################");
                System.out.println("#####################              #########################");
                System.out.println("############################################################");
                System.out.println("############################################################");
                System.out.println();
                System.out.println();

                TrafficStore trafficStore = new TrafficStore();

                ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, Integer.MAX_VALUE,
                        ManhattanGraphGen.mType.UNWRAPPED, false, true);

                ListGraph graph = manhattanGraphGen.getManhattanGraph();

                if (readTraffic) {
                    TrafficGenerator.fromFile(graph, trafficStore,
                            "analysis/" + oldExperiment + "/" + oldExperiment + "_run_" + currentTrafficRun
                                    + "_traffic.txt");
                }
                else {
                    TrafficGenerator.FisherYates(graph, 1.0, trafficStore);
                    if (writeTraffic)
                        TrafficGenerator.writeTraffic(trafficStore,
                                "analysis/" + experiment + "/" + experiment + "_run_" + run + "_traffic.txt");
                }


                TrafficHeuristic trafficHeuristic = new TrafficHeuristic(graph,
                                                                        trafficStore,
                                                                        numCopies,
                                                                        type,
                                                                        multipleStates,
                                                                        rearrangeStates);

                if (printCentrality) {
                    System.out.println();
                    System.out.println("Centrality: ");
                    LinkedHashMap<Vertex, Double> centrality = trafficHeuristic.getCentrality();
                    centrality.keySet().forEach(vertex -> {
                        System.out.println("Vertex: " + vertex.getLabel() +
                                " Centrality: " + centrality.get(vertex));
                    });
                }

                Map<TrafficDemand, Path> routingSolution = trafficHeuristic.getRoutingSolution();
                /*
                System.out.println("Copies:");
                for (Vertex vertex : trafficHeuristic.getCopies()) {
                    System.out.println(vertex.getLabel());
                }
                System.out.println();
                */

                for (TrafficDemand trafficDemand : routingSolution.keySet()) {
                    try {
                   /* System.out.println("Source: " + trafficDemand.getSource().getLabel() +
                            " Destination: " + trafficDemand.getDestination().getLabel());
                    routingSolution
                            .get(trafficDemand)
                            .getEdges()
                            .forEach(edge -> System.out.println(edge.getSource().getLabel() + " -> " +
                                    edge.getDestination().getLabel()));
                                    */
                    } catch (NullPointerException npe) {
                        nullp = true;
                        break;
                    }
                    //System.out.println();
                }

                trafficStore.clear();
                graph.clear();
                graph = null;
                trafficStore = null;

                if (!nullp) {
                    System.out.println("Traffic: " + trafficHeuristic.getTotalTraffic());
                    if (saveSolution) {
                        String writeSol = "analysis/" + experiment + "/" + experiment
                                + "_run_" + currentTrafficRun + "_random_" + run;
                        trafficHeuristic.writeSolution(writeSol);
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


        System.out.println("Actual runs: " + actualRuns);

    }



}