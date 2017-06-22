package statefulsharding.MilpOpt;


import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class TestingClass {

    private static int d;

    static {
        d = 0;
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

        HashMap<StateVariable, LinkedList<LinkedList<String>>> stateCopyCombinations = new HashMap<>();

        for (StateVariable stateVariable : stateStore.getStateVariables()) {

            LinkedList<LinkedList<Integer>> result = getNCombinations.getCombinations(
                    graph.getVerticesInt(), stateVariable.getCopies());

            stateCopyCombinations.put(stateVariable, new LinkedList<>());
            for (LinkedList<Integer> combination : result) {

                LinkedList<String> temp = new LinkedList<>();

                for (Integer integer : combination) {
                    String stateCopy = stateVariable.getLabel() + "," + integer;
                    temp.add(stateCopy);
                }

                stateCopyCombinations.get(stateVariable).add(temp);
            }
            numCombinations = numCombinations*stateCopyCombinations.get(stateVariable).size();
        }


        ArrayList<StateVariable> stateVariables = new ArrayList(stateStore.getStateVariables());

        String[] combinations = new String[numCombinations];
        int numStates = stateStore.getNumStates();

        CartesianProduct(stateCopyCombinations, combinations, numStates, stateVariables, 0,
                new LinkedList<>());

    }

    private static void CartesianProduct(HashMap<StateVariable, LinkedList<LinkedList<String>>> stateCombinations,
                                         String[] combinations,
                                         int numStates,
                                         ArrayList<StateVariable> stateVariables,
                                         int currentLevel,
                                         LinkedList<String> buildup){



        if(currentLevel==numStates){
            String temp = buildup.get(0);
            for(int i=1 ; i<buildup.size() ; i++){
                temp = temp + "-" + buildup.get(i);
            }
            combinations[d]=(temp);
            d++;
            System.out.println(temp);
        }
        else{
            for(LinkedList<String> linkedList : stateCombinations.get(stateVariables.get(currentLevel))){
                buildup.addAll(linkedList);
                CartesianProduct(stateCombinations, combinations, numStates,
                        stateVariables,currentLevel+1, buildup);
                for(String stateCopy: linkedList)
                    buildup.remove(stateCopy);
            }
        }
    }


}
