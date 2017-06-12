package statefulsharding.MilpOpt;

import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.LoadGraph;
import statefulsharding.randomgraphgen.ManhattanGraphGen;
import java.io.File;

/**
 * Batch MILP Solver
 */

public class MilpSolver {

    public static void main(String[] args) {

        String graphType = "WattsStrogatz";
        String experimentType = "deterministicTfc_optimal_CanCross";
        String newexperimentType = "deterministicTfc_optimal_CanCross";

        /*
        Wattz Strogatz specific info:
         */
        double p = 0.1;
        boolean decrement = true;

        int firstRun =1;
        int runs = 1;
        int minSize = 36;
        int maxSizeTopology = 36;
        int sizeincrement = 12;
        int minCopy = 2;
        int maxCopy = 2;
        boolean shortestPath = false;
        boolean verbose = true;
        boolean saveSolution = false;
        boolean multipleStates = true;
        boolean readTraffic = true;
        boolean createNewDirectory = true;

        for (int size = minSize; size <= maxSizeTopology; size = size + sizeincrement) {

            String oldexperiment = graphType + "_" + experimentType + "_" + size;
            String newexperiment = graphType + "_" + newexperimentType + "_" + size;

            if (createNewDirectory) {
                File dir = new File("analysis/" + newexperiment);
                dir.mkdir();
            }

            for (int run = firstRun; run <= runs; run++) {

                ListGraph graph = null;
                TrafficStore trafficStore = new TrafficStore();

                System.out.println();
                System.out.println("####################################################################");
                System.out.println("####################     Size: " + size + " Run: " + run
                                    + "    ####################");
                System.out.println("####################################################################");

                if(graphType.equals("MANHATTAN-UNWRAPPED")) {
                    ManhattanGraphGen manhattanGraphGenerator = new ManhattanGraphGen(size,
                            Integer.MAX_VALUE,
                            ManhattanGraphGen.mType.UNWRAPPED,
                            false,
                            true);

                    graph = manhattanGraphGenerator.getManhattanGraph();
                }
                else if(graphType.equals("WattsStrogatz")){
                    String graphLocation = "topologies_traffic/Traffic/WS_graph_" + p +
                                            "/WS_graph" + size + "_" + p + "_8.csv";
                    graph = LoadGraph.GraphParserJ(graphLocation, Integer.MAX_VALUE, decrement);
                }

                if (readTraffic){
                    if (graphType.equals("MANHATTAN-UNWRAPPED"))
                        TrafficGenerator.fromFile(graph, trafficStore, "analysis/" + oldexperiment
                                                + "/" + oldexperiment + "_run_" + run + "_traffic.txt");
                    else if(graphType.equals("WattsStrogatz"))
                        TrafficGenerator.fromFileLinebyLine(graph,trafficStore,run,1,decrement,
                                                "topologies_traffic/Traffic/WS_Traffic/WS_Traffic" + size +
                                                        ".csv");
                }
                else{
                    TrafficGenerator.FisherYates(graph, 1.0, trafficStore);
                    TrafficGenerator.writeTraffic(trafficStore, "analysis/" + newexperiment + "/"
                                                    + newexperiment + "_run_" + run + "_traffic.txt");
                }

                String optExperiment = "analysis/" + newexperiment + "/" + newexperiment + "_run_" + run;

                System.out.println("\nShortestPath\n");
                if(shortestPath)
                    OptimizationAlgo.ShortestPath(graph, trafficStore, verbose, optExperiment, saveSolution);

                for (int c = minCopy ; c<=maxCopy ; c++){
                    if (size==6 && c==3)
                        continue;

                    System.out.println("\nSNAP model with " + c + " copy\n");

                    OptimizationAlgo.ShardedSNAP(graph,
                                                 trafficStore,
                                                 verbose,
                                                 c,
                                     optExperiment + "_copy_" + c,
                                                 saveSolution,
                                                 multipleStates);
                }

                graph.clear();
                trafficStore.clear();
                graph = null;
                trafficStore = null;


            }
        }
    }
}
