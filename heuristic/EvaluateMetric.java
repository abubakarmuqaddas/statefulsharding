package statefulsharding.heuristic;

import statefulsharding.MilpOpt.OptimizationAlgo;
import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPOptimization;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Centrality;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/*
 * TODO: Incomplete!
 */

public class EvaluateMetric {

    static String graphType = "MANHATTAN-UNWRAPPED";
    static String experimentType = "deterministicTfc";

    public static void main(String[] args){

        try {

            int runs = 10;
            boolean verbose = false;
            boolean multipleStates = false;
            boolean fixConstraints = false;
            int copies = 1;

            OptimizationOptions options = new OptimizationOptions(copies, verbose, fixConstraints, multipleStates);

            for (int graphsize = 4; graphsize <= 4; graphsize++) {

                String experiment = graphType + "_" + experimentType + "_" + graphsize;

                for (int run = 1; run <= runs; run++) {

                    System.out.println();
                    System.out.println("####################################################################");
                    System.out.println("################     Manhattan, Size: " + graphsize + " Run: "
                            + run + "    ##################");
                    System.out.println("####################################################################");
                    System.out.println();

                    LinkedHashMap<String, LinkedHashMap<Vertex, Double>> heuristicLocations = new LinkedHashMap<>();
                    HashMap<Integer, HashMap<String, Double>> heuristicError = new HashMap<>();
                    TrafficStore trafficStore = new TrafficStore();
                    HashMap<Integer,FileWriter> errorFW = new HashMap<>();
                    HashMap<Integer,BufferedWriter> errorBW = new HashMap<>();
                    HashMap<Integer,PrintWriter> errorOut = new HashMap<>();

                    ManhattanGraphGen manhattanGraphGenerator = new ManhattanGraphGen(graphsize, Integer.MAX_VALUE,
                                                ManhattanGraphGen.mType.UNWRAPPED,false, true);
                    ListGraph graph = manhattanGraphGenerator.getManhattanGraph();
                    TrafficGenerator.fromFile(graph, trafficStore,
                            experiment + "/" + experiment + "_run_" + run + "_traffic.txt");

                    //heuristicLocations.put("betCen", Centrality.BetweennessTfc(graph, trafficStore));
                    heuristicLocations.put("flowTfcCen", Centrality.FlowTfc(graph, trafficStore));
                    heuristicLocations.put("flowTfcAlternateCen", Centrality.FlowTfcAlternate(graph, trafficStore));
                    heuristicLocations.put("residualFlowCen", Centrality.ResidualFlow(graph, trafficStore));

                    String firstline = "run";
                    for (String string : heuristicLocations.keySet()){
                        firstline = firstline + " " + string;
                    }

                    for (int copy = 1 ; copy<=3 ; copy++){
                        String completeExperiment = experiment + "/" + experiment +
                                "_StateLocation_copy_" + copy + "_run_" + run + "_error.txt";
                        errorFW.putIfAbsent(copy,new FileWriter(completeExperiment,true));
                        errorBW.putIfAbsent(copy,new BufferedWriter(errorFW.get(copy)));
                        errorOut.putIfAbsent(copy,new PrintWriter(errorBW.get(copy)));
                        errorOut.get(copy).print(firstline);
                    }

                    for (int copy = 1; copy <= 3; copy++) {

                        options.setCopies(copy);
                        System.out.println("\nSNAP model with " + copy + " copy\n");

                        ShardedSNAPOptimization snap = new ShardedSNAPOptimization(graph, trafficStore, options);
                        snap.optimize();
                        LinkedList<Vertex> optimalStateLocations = snap.getStateLocation();
                        snap.clear();
                        snap = null;

                        if (copy == 1) {
                            Vertex vertex = optimalStateLocations.getFirst();

                            for (String string : heuristicLocations.keySet()) {
                                ArrayList<Double> values = new ArrayList<>(heuristicLocations.get(string).values());
                                double max = values.get(values.size() - 1);
                                double min = values.get(0);
                                double error = (max - heuristicLocations.get(string).get(vertex)) / (max - min);

                                heuristicError.putIfAbsent(copy, new HashMap<>());
                                heuristicError.get(copy).put(string, error);
                            }
                        }
                        else {
                            for (String string : heuristicLocations.keySet()) {
                                ArrayList<Double> values = new ArrayList<>(heuristicLocations.get(string).values());
                                double max = values.get(values.size() - 1);
                                double min = values.get(0);

                                double errorSum = 0.0;

                                for (Vertex vertex : optimalStateLocations) {
                                    errorSum = errorSum + (max - heuristicLocations.get(string).get(vertex)) / (max - min);
                                }

                                double error = errorSum / optimalStateLocations.size();
                                heuristicError.putIfAbsent(copy, new HashMap<>());
                                heuristicError.get(copy).put(string, error);

                            }
                        }


                    }

                    trafficStore.clear();
                    trafficStore = null;

                    for (int copy = 1 ; copy <=3 ; copy++){
                        errorOut.get(copy).print(run + " ");
                        for(String string : heuristicLocations.keySet()){
                            errorOut.get(copy).print(heuristicError.get(copy).get(string) + " ");
                        }
                        errorOut.get(copy).println();
                    }

                }


            }

        }
        catch (IOException e){
            //
        }


    }

}
