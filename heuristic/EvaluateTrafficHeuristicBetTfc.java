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
import java.util.*;

import static statefulsharding.heuristic.TrafficHeuristic.hType.*;

public class EvaluateTrafficHeuristicBetTfc {

    public static void main(String[] args) {

        int size = 9;
        boolean nullp = false;
        int[] trafficValues = new int[]{3,2,7,4,5,6,1,10,8,9};
        int initialRun = 1;
        int finalRun = 10;
        int numCopies = 4;

        boolean multipleStates = true;
        boolean rearrangeStates = false;
        boolean saveSolution = false;
        boolean readTraffic = true;
        boolean writeTraffic = false;
        boolean createDirectory = false;
        boolean printCentrality = false;

        String insidePartitionMethod = "betweennesstfc";
        String initialPartitionMethod = "random";
        TrafficHeuristic.hType type = fixedcopies;

        String graphType = "MANHATTAN-UNWRAPPED";
        String experimentType = "deterministicTfc-evaluateTrafficHeuristic-Partition";
        String experiment = graphType + "_" + experimentType + "_" + size;

        String graphTypeOld = "MANHATTAN-UNWRAPPED";
        String experimentTypeOld = "deterministicTfc-evaluateTrafficHeuristic-fixCopies";
        String oldExperiment = graphTypeOld + "_" + experimentTypeOld + "_" + size;


        if (createDirectory) {
            File dir = new File("analysis/" + experiment);
            dir.mkdir();
        }
        LinkedHashMap<Integer,LinkedList<Double>> trafficValuesBefore = new LinkedHashMap<>();

        for(int i = 0; i<10; i++){

            int traffic = trafficValues[i];

            trafficValuesBefore.put(traffic,new LinkedList<>());

            for(int run = initialRun ; run <= finalRun ; run++) {

                System.out.println();
                System.out.println();
                System.out.println("############################################################");
                System.out.println("############################################################");
                System.out.println("#####################              #########################");
                System.out.println("#########  Traffic: " + traffic + "  ################");
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
                            "analysis/" + oldExperiment + "/" + oldExperiment + "_run_" + traffic
                                    + "_traffic.txt");
                }
                else {
                    TrafficGenerator.FisherYates(graph, 1.0, trafficStore);
                    if (writeTraffic)
                        TrafficGenerator.writeTraffic(trafficStore,
                                "analysis/" + experiment + "/" + experiment + "_run_" +
                                        traffic + "_traffic.txt");
                }


                HashMap<Vertex, ListGraph> partitions = Partitioning.EvolutionaryPartition(graph, numCopies,
                        100,
                        initialPartitionMethod,
                        insidePartitionMethod,
                        trafficStore,
                        false,
                        false,
                        null);

                ArrayList<Vertex> sortedVertices = new ArrayList<>(partitions.keySet());


                TrafficHeuristic trafficHeuristicPart = new TrafficHeuristic(graph,
                        trafficStore,
                        numCopies,
                        type,
                        sortedVertices,
                        multipleStates,
                        rearrangeStates);


                if (printCentrality) {
                    System.out.println();
                    System.out.println("Centrality: ");
                    LinkedHashMap<Vertex, Double> centrality = trafficHeuristicPart.getCentrality();
                    centrality.keySet().forEach(vertex -> {
                        System.out.println("Vertex: " + vertex.getLabel() +
                                " Centrality: " + centrality.get(vertex));
                    });
                }

                System.out.println("Copies:");
                for (Vertex vertex : trafficHeuristicPart.getCopies()) {
                    System.out.println(vertex.getLabel());
                }
                System.out.println();


                /*
                for (TrafficDemand trafficDemand : routingSolution.keySet()) {
                    try {
                    System.out.println("Source: " + trafficDemand.getSource().getLabel() +
                            " Destination: " + trafficDemand.getDestination().getLabel());
                    routingSolution
                            .get(trafficDemand)
                            .getEdges()
                            .forEach(edge -> System.out.println(edge.getSource().getLabel() + " -> " +
                                    edge.getDestination().getLabel()));

                    }
                    catch (NullPointerException npe) {
                        nullp = true;
                        break;
                    }
                }
                */

                trafficStore.clear();
                graph.clear();
                graph = null;
                trafficStore = null;

                if (!nullp) {
                    System.out.println("Traffic: " + trafficHeuristicPart.getTotalTraffic());
                    trafficValuesBefore.get(traffic).add(trafficHeuristicPart.getTotalTraffic());
                    if (saveSolution) {
                        String writeSol = "analysis/" + experiment + "/" + experiment
                                + "_traffic_" + traffic + "_run_" + run + "_" + insidePartitionMethod;
                        trafficHeuristicPart.writeSolution(writeSol);

                    }
                } else {
                    System.out.println("FOUND A NULL POINTER!!!");
                    nullp = false;
                }

            }

        }

        for (Integer integer : trafficValuesBefore.keySet()) {
            System.out.println(trafficValuesBefore.get(integer));
        }




    }




}
