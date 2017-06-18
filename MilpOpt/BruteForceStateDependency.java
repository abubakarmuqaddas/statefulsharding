package statefulsharding.MilpOpt;

import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by abubakar on 14/6/17.
 */
public class BruteForceStateDependency {

    public static void main(String[] args) {

        int capacity = Integer.MAX_VALUE;

        int size = 3;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        HashMap<Vertex, HashMap<Vertex, Integer>> dist = ShortestPath.FloydWarshall(graph, false,
                null);

        StateStore stateStore = new StateStore();
        stateStore.addStateVariable("A",2);
        stateStore.addStateVariable("B",1);

        LinkedList<Integer> vertices = new LinkedList<>();
        for (int i = 0; i < graph.getNumVertices(); i++) {
            vertices.add(i);
        }

        HashMap<StateVariable, LinkedList<LinkedList<Integer>>> stateCombinations = new HashMap<>();

        for (StateVariable stateVariable : stateStore.getStateVariables()) {
            getNCombinations getCombinations = new getNCombinations(vertices, stateVariable.getCopies());
            stateCombinations.put(stateVariable, getCombinations.getResult());
        }
        





    }

}
