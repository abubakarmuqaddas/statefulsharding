package statefulsharding.graph.algorithms;

import statefulsharding.graph.ListGraph;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class BinaryTree {


    private HashMap<Integer, LinkedList<Integer>> levels;
    private int activeLevel;
    private ListGraph graph;

    public BinaryTree(int size){

        levels = new HashMap<>();
        activeLevel = 0;

        for(int i = 0 ; i<size ; i++){
            this.addNode(i);
        }
        graph = new ListGraph();
    }

    private void addNode(Integer integer){

        if(activeLevel == 0){
            levels.put(activeLevel, new LinkedList<>());
            levels.get(activeLevel).add(integer);
            activeLevel++;
            levels.put(activeLevel, new LinkedList<>());
        }
        else {
            for (int currentLevel=activeLevel ; currentLevel<levels.size(); currentLevel++) {

                if(levels.get(currentLevel).size() < Math.pow(2, currentLevel) || levels.get(currentLevel)==null) {
                    levels.get(currentLevel).add(integer);
                    break;
                }

                if(levels.get(currentLevel).size() == Math.pow(2, currentLevel)) {
                    activeLevel++;
                    levels.put(activeLevel, new LinkedList<>());
                }
            }
        }
    }

    public ListGraph getGraph(){

        Random RandomNumberGen = new Random();
        double capacity = Double.MAX_VALUE;

        for(int level = 0 ; level<=levels.keySet().size()-2 ; level++){

            for(int currentNode = 0 ; currentNode<levels.get(level).size() ; currentNode++) {

                Integer nodeInFocus = levels.get(level).get(currentNode);

                if(nodeInFocus!=null){
                    if(!graph.checkVertex(nodeInFocus))
                        graph.addVertex(nodeInFocus);
                }

                Integer leftNode = null;
                Integer rightNode = null;

                if(levels.get(level+1).size()>=currentNode*2+1)
                    leftNode = levels.get(level+1).get(currentNode*2);

                if(levels.get(level+1).size()>=currentNode*2+2)
                    rightNode = levels.get(level+1).get(currentNode*2+1);

                if(leftNode!=null){
                    if(!graph.checkVertex(leftNode))
                        graph.addVertex(leftNode);
                    if(RandomNumberGen.nextDouble() > 0.5)
                        graph.insertEdge(graph.getVertex(nodeInFocus),
                                        graph.getVertex(leftNode),capacity);
                    else
                        graph.insertEdge(graph.getVertex(leftNode),
                                graph.getVertex(nodeInFocus),capacity);
                }

                if(rightNode!=null){
                    if(!graph.checkVertex(rightNode))
                        graph.addVertex(rightNode);
                    if(RandomNumberGen.nextDouble() > 0.5)
                        graph.insertEdge(graph.getVertex(nodeInFocus),
                                graph.getVertex(rightNode),capacity);
                    else
                        graph.insertEdge(graph.getVertex(rightNode),
                                graph.getVertex(nodeInFocus),capacity);
                }
            }
        }


        return graph;
    }

}
