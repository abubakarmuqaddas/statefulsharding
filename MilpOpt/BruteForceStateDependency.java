package statefulsharding.MilpOpt;

import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.HashMap;

/**
 * Created by abubakar on 14/6/17.
 */
public class BruteForceStateDependency {

    public static void main(String[] args) {

        int capacity = Integer.MAX_VALUE;

        int size = 9;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        HashMap<Vertex, HashMap<Vertex, Integer>> dist = ShortestPath.FloydWarshall(graph, false,
                null);

        





    }

}
