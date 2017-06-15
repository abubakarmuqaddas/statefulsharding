package statefulsharding.State;

import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.BinaryTree;
import statefulsharding.graph.algorithms.ShortestPath;

import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * Created by root on 6/15/17.
 */
public class GenerateStates {

    public static LinkedList<LinkedList<StateVariable>> BinaryTreeGenerator(int numStates){

        BinaryTree binaryTree = new BinaryTree(numStates);
        ListGraph graph = binaryTree.getGraph();

        StateStore stateStore = new StateStore();

        LinkedList<LinkedList<StateVariable>> dependencies = new LinkedList<>();

        for(Vertex src : graph.getVertices()){

            String srcString = getCharForNumber(src.getLabel());

            if(!stateStore.checkStateVariable(srcString)) {
                StateVariable srcStateVariable = new StateVariable(srcString);
                stateStore.addStateVariable(srcStateVariable);
            }

            LinkedList<StateVariable> onlySrc = new LinkedList<>();
            onlySrc.add(stateStore.getStateVariable(srcString));
            dependencies.add(onlySrc);

            for(Vertex dst : graph.getVertices()){

                if(src.equals(dst))
                    continue;

                Path path = null;

                try {
                    path = ShortestPath.dijsktra(graph, src, dst);
                    if(path==null)
                        continue;
                }
                catch (NullPointerException e){
                    //System.out.println("No path exists");
                    continue;
                }

                LinkedList<StateVariable> currentSrcDst = new LinkedList<>();

                LinkedHashSet<Vertex> vertices = path.getVertices();

                for(Vertex vertex : vertices){

                    String vertexString = getCharForNumber(vertex.getLabel());

                    if(!stateStore.checkStateVariable(vertexString)) {
                        StateVariable vertexStateVariable = new StateVariable(vertexString);
                        stateStore.addStateVariable(vertexStateVariable);
                    }

                    currentSrcDst.add(stateStore.getStateVariable(vertexString));
                }

                if(currentSrcDst != null && !currentSrcDst.isEmpty())
                    dependencies.add(currentSrcDst);
            }
        }

        return dependencies;
    }

    private static String getCharForNumber(int i){
        return i >-1 && i<26 ? String.valueOf((char)(i+97)) : null;
    }

}
