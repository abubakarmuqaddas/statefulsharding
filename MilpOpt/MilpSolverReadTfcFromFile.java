package statefulsharding.MilpOpt;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.*;

/**
 * Batch MILP Solver, uses existing traffic from specified files
 * TODO: Run this by allowing a flow to cross multiple states, i.e. multipleStates = true
 */

public class MilpSolverReadTfcFromFile {

    static String graphType = "MANHATTAN-UNWRAPPED";
    static String experimentType = "deterministicTfc";
    static String newexperimentType = "deterministicTfc_CanCross";

    public static void main(String[] args) {

        int runs = 10;
        boolean verbose = true;
        boolean generateFiles = true;
        boolean multipleStates = true;

        for (int graphsize = 5; graphsize <= 6; graphsize++) {
            try {

                String oldexperiment = graphType + "_" + experimentType + "_" + graphsize;
                String newexperiment = graphType + "_" + newexperimentType + "_" + graphsize;
                System.out.println(newexperiment);
                File dir = new File(newexperiment);
                dir.mkdir();


                for (int run = 1; run <= runs; run++) {

                    System.out.println();
                    System.out.println("####################################################################");
                    System.out.println("#################     Manhattan, Size: " + graphsize + " Run: "
                            + run + "    ###################");
                    System.out.println("####################################################################");
                    System.out.println();

                    ManhattanGraphGen manhattanGraphGenerator = new
                            ManhattanGraphGen(graphsize, Integer.MAX_VALUE, ManhattanGraphGen.mType.UNWRAPPED,
                            false, true);

                    ListGraph graph = manhattanGraphGenerator.getManhattanGraph();

                    TrafficStore trafficStore = new TrafficStore();

                    TrafficGenerator.fromFile(graph, trafficStore,
                            oldexperiment + "/" + oldexperiment + "_run_" + run + "_traffic.txt");

                    FileWriter tfcfw = new FileWriter(newexperiment + "/" + newexperiment +
                            "_run_" + run + "_traffic.txt", true);

                    BufferedWriter tfcbw = new BufferedWriter(tfcfw);
                    PrintWriter tfcout = new PrintWriter(tfcbw);

                    tfcout.println("u v d");

                    for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                        tfcout.println(trafficDemand.getSource().getLabel() +
                                " " + trafficDemand.getDestination().getLabel() +
                                " " + trafficDemand.getDemand());
                    }
                    tfcout.flush();


                    String optExperiment = newexperiment + "/" + newexperiment + "_run_" + run;

                    /*
                    System.out.println("\nShortestPath\n");
                    OptimizationAlgo.ShortestPath(graph, trafficStore, verbose,
                            optExperiment, true);

                    System.out.println("\nSNAP model with 1 copy\n");
                    OptimizationAlgo.ShardedSNAP(graph, trafficStore, verbose, 1,
                            optExperiment + "_copy_" + 1, generateFiles, multipleStates);
                     */

                    for (int c = 2 ; c<=3 ; c++){
                        if (graphsize==6 && c==3)
                            continue;
                        System.out.println("\nSNAP model with " + c + " copy\n");
                        OptimizationAlgo.ShardedSNAP(graph, trafficStore, verbose, c,
                                optExperiment + "_copy_" + c, generateFiles, multipleStates);
                    }

                    graph.clear();
                    trafficStore.clear();
                    graph = null;
                    trafficStore = null;
                    manhattanGraphGenerator = null;
                }
            }
            catch (IOException e) { //

            }
        }
    }
}
