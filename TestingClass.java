package statefulsharding;

import statefulsharding.graph.ListGraph;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;


public class TestingClass {

    public static void main(String[] args){

        int capacity = Integer.MAX_VALUE;
        int size = 5;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        LinkedList<LinkedList<Integer>> result =
                getNCombinations.getPermutations(7, graph.getVerticesInt());

        result.forEach(sequence -> System.out.println(sequence) );

        System.out.println("Size: " + result.size());


    }
}
