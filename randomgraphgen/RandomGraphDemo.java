package statefulsharding.randomgraphgen;

import statefulsharding.State.GenerateStates;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.*;
import statefulsharding.graph.algorithms.BinaryTree;
import statefulsharding.graph.algorithms.ShortestPath;

import javax.swing.plaf.nimbus.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class RandomGraphDemo {


    public static void main(String[] args){

        int size = 4;

        LinkedList<LinkedList<StateVariable>> dependencies = GenerateStates.BinaryTreeGenerator(size);


        for (LinkedList<StateVariable> dependency : dependencies) {
            for (StateVariable stateVariable : dependency) {
                System.out.print(stateVariable.getLabel() + " ");
            }
            System.out.println();
        }

    }




}

