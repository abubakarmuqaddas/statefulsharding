package statefulsharding.MilpOpt;


import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class TestingClass {

    private static int d;
    private static int numVarCopy;

    static {
        d = 0;
        numVarCopy = 0;
    }

    public static void main(String[] args){

        int capacity = Integer.MAX_VALUE;
        int size = 7;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, false);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        StateStore stateStore = new StateStore();
        stateStore.addStateVariable("a",2);
        stateStore.addStateVariable("b",1);
        stateStore.addStateVariable("c",1);
        stateStore.addStateVariable("d",1);

        int numCombinations = 1;

        HashMap<StateVariable, LinkedList<LinkedList<Integer>>> stateCopyCombinations = new HashMap<>();

        for (StateVariable stateVariable : stateStore.getStateVariables()) {

            LinkedList<LinkedList<Integer>> result = getNCombinations.getCombinations(
                    graph.getVerticesInt(), stateVariable.getCopies());

            stateCopyCombinations.put(stateVariable, new LinkedList<>());
            for (LinkedList<Integer> combination : result) {

                LinkedList<Integer> temp = new LinkedList<>();

                for (Integer integer : combination) {
                    temp.add(integer);
                }

                stateCopyCombinations.get(stateVariable).add(temp);
            }
            numCombinations = numCombinations*stateCopyCombinations.get(stateVariable).size();
        }

        for(StateVariable stateVariable : stateStore.getStateVariables()){
            numVarCopy += stateVariable.getCopies();
        }


        ArrayList<StateVariable> stateVariables = new ArrayList(stateStore.getStateVariables());

        int[][] combinations = new int[numCombinations][numVarCopy];
        int numStates = stateStore.getNumStates();

        CartesianProduct(stateCopyCombinations, combinations, numStates, stateVariables, 0,
                new LinkedList<>());

    }

    private static void CartesianProduct(HashMap<StateVariable, LinkedList<LinkedList<Integer>>> stateCombinations,
                                         int[][] combinations,
                                         int numStates,
                                         ArrayList<StateVariable> stateVariables,
                                         int currentLevel,
                                         LinkedList<Integer> buildup){



        if(currentLevel==numStates){

            for(int i=0 ; i<numVarCopy ; i++){
                combinations[d][i]= buildup.get(i);
            }

            System.out.println(Arrays.toString(combinations[d]));

        }
        else{
            for(LinkedList<Integer> linkedList : stateCombinations.get(stateVariables.get(currentLevel))){
                buildup.addAll(linkedList);
                CartesianProduct(stateCombinations, combinations, numStates,
                        stateVariables,currentLevel+1, buildup);
                for(Integer integer: linkedList)
                    buildup.remove(integer);
            }
        }
    }


}
