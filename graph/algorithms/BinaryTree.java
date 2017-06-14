package statefulsharding.graph.algorithms;

import statefulsharding.State.StateVariable;
import statefulsharding.graph.ListGraph;

import javax.swing.plaf.nimbus.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

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

        for(int level = 0 ; level<levels.keySet().size()-2 ; level++){

            //for(int currentNode = 0 ; )

        }


        return graph;
    }








}
