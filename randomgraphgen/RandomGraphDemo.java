package statefulsharding.randomgraphgen;

import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.*;
import statefulsharding.graph.algorithms.BinaryTree;

import java.util.ArrayList;

public class RandomGraphDemo {


    public static void main(String[] args){

        int size = 8;
        BinaryTree binaryTree = new BinaryTree(size);
        ListGraph graph = binaryTree.getGraph();

        graph.getVertices().forEach(vertex -> {
            if(graph.getSuccessors(vertex)!=null){
                graph.getSuccessors(vertex).forEach(vertex1 -> {
                    System.out.println(vertex.getLabel() + " -> " + vertex1.getLabel());
                });
            }
        });

    }



}

