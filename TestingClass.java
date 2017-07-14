package statefulsharding;

import statefulsharding.graph.algorithms.getNCombinations;

import java.util.*;


public class TestingClass {

    public static void main(String[] args){

        LinkedList<Integer> data = new LinkedList<>();

        for(int i=0 ; i<9 ; i++){
            data.add(i);
        }

        LinkedList<LinkedList<Integer>> result = getNCombinations.getPermutations(2, data);

        int i=1;
        for (LinkedList<Integer> linkedList : result) {
            System.out.println(i++ + ": " + linkedList);
        }



    }
}
