package statefulsharding;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.LoadGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.heuristic.TrafficHeuristic;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.IntStream;

import static statefulsharding.heuristic.TrafficHeuristic.hType.fixedcopies;


public class TestingClass {

    public static void main(String[] args) {

        int size = 5;

        ManhattanGraphGen m = new ManhattanGraphGen(size,Integer.MAX_VALUE,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);

        ListGraph graph = m.getManhattanGraph();

        HashMap<Vertex, ListGraph> partitions =
                Partitioning.EvolutionaryPartition(
                        graph,
                        2,
                        50,
                        "random",
                        "betweenness",
                        null,
                        false,
                        false,
                        null
                );

        ArrayList<Vertex> sortedVertices = new ArrayList<>(partitions.keySet());

        for (Vertex sortedVertex : sortedVertices) {
            System.out.println(sortedVertex.getLabel());
        }







    }





}
