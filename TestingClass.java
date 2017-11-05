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

        /*
        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(13,Double.MAX_VALUE,
                ManhattanGraphGen.mType.UNWRAPPED,false, false);

        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        double totalDistance = 0;
        double occurences = 0;

        for (Vertex src : graph.getVertices()) {
            for (Vertex dst : graph.getVertices()) {
                if(!src.equals(dst)){
                    totalDistance += dist.get(src).get(dst);
                    occurences++;
                }
            }
        }


        System.out.println(totalDistance/occurences);
        */

        Random rand = new Random(2500);

        for(int i=1 ; i<=100 ; i++){
            System.out.print(rand.nextInt(2) + " ");
        }

    }




}
