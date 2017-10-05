package statefulsharding;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.LoadGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
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

    public static void main(String[] args){

        /*
        HashMap<Integer, Integer> values= new HashMap<>();

        values.put(1,2);
        values.put(2,10);
        values.put(3,2);
        values.put(4,5);

        LinkedHashMap<Integer, Integer> newValues = MapUtils.sortMapByValue(values);

        newValues.forEach((integer, integer2) -> System.out.println(integer+ " " + integer2));

        System.out.println("Key");
        newValues.keySet().forEach(integer -> System.out.println(integer));
        */

        String initial = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/topologies_traffic/zoo/";

        for(int i=1 ; i<=100 ; i++) {

            ListGraph graph = LoadGraph.GraphParserJ(initial +
                    i + ".txt", Integer.MAX_VALUE, false);

            System.out.println(graph.getNumVertices() + " " + graph.getNumEdges() );

        }




    }


    /*
    private static void shuffleArray(int[] array) {
        int index, temp;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
    */





}
