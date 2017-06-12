package statefulsharding.heuristic;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GeneratePartitionCopies {

    public static void main(String[] args){

        int size = 6;

        String filename = "MANHATTAN-UNWRAPPED-deterministicTfc-PartitionCopies_" + size;
        for (int j=5 ; j<=6 ; j++) {

            for (int i=1 ; i<=10 ; i++) {

                System.out.println(i + "th iteration");

               ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size,
                                                                            Integer.MAX_VALUE,
                                                                            ManhattanGraphGen.mType.UNWRAPPED,
                                                                            false,
                                                                            true);

                ListGraph graph = manhattanGraphGen.getManhattanGraph();

                int numParts = j;

                String filename1 = "analysis/" + filename + "/" +  filename + "_" +  numParts + "_" + i;

                HashMap<Vertex, ListGraph> partitions = Partitioning.EvolutionaryPartition(graph, numParts,
                        100,
                        "random",
                        "betweenness",
                        null,
                        false,
                        false,
                        filename1);

                graph.clear();

            }
        }
    }

    public static void writePartitionGraphSample(){
                /*
        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(6, Integer.MAX_VALUE,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        String start = "analysis/MANHATTAN-UNWRAPPED-deterministicTfc-PartitionCopies_6/" +
                "MANHATTAN-UNWRAPPED-deterministicTfc-PartitionCopies_6";

        for (int i=2 ; i<=6 ; i++){

            for (int j=1 ; j<=10 ; j++){

                ListGraph[] subGraphs = Partitioning.getPartitions(graph,start + "_" + i + "_" + j);
                ArrayList<Vertex> leaders = Partitioning.getCopies(graph,start + "_" + i + "_" + j);

                Partitioning.writePartitionGraph(graph,subGraphs,leaders.size(),leaders,
                        start + "_" + i + "_" + j + ".dot");

            }

        }
        */
    }





}
