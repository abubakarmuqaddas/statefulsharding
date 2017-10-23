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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static statefulsharding.heuristic.TrafficHeuristic.hType.fixedcopies;


public class TestingClass {

    public static void main(String[] args) {

        long seed = 17092011;

        Random rand = new Random(seed);

        for(int i=1 ; i<=10 ; i++) {
            System.out.println(rand.nextInt(6));
        }







    }





}
