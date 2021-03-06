package statefulsharding.graph.algorithms;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class getNCombinations {

    public static LinkedList<LinkedList<Integer>> getCombinations(LinkedList<Integer> data, int N){
        LinkedList<LinkedList<Integer>> result = new LinkedList<>();
        LinkedList<Integer> v = new LinkedList();
        recursive(0, N, result, v, data);
        return result;
    }

    private static void recursive(int n, int N, LinkedList<LinkedList<Integer>> result,
                                  LinkedList<Integer> v, LinkedList<Integer> data){
        int p;
        if(n==N)
            result.add(new LinkedList<>(v));
        else{
            if(v.size()!=0)
                p = data.indexOf(v.getLast());
            else
                p=-1;

            for(int j = p+1 ; j<data.size() ; j++){
                v.add(data.get(j));
                recursive(n+1, N, result, v, data);
                v.removeLast();
            }
        }
    }

    public static LinkedList<LinkedList<Integer>> getPermutations(int N, LinkedList<Integer> data){
        LinkedList<LinkedList<Integer>> result = new LinkedList<>();
        LinkedList<Integer> v = new LinkedList<>();
        recursive2(0, N, result, v, data);
        return result;
    }

    private static void recursive2(int n, int N, LinkedList<LinkedList<Integer>> result,
                                   LinkedList<Integer> v, LinkedList<Integer> data){
        int p;
        if(n==N) {
            HashSet<Integer> temp = new HashSet<>();
            for(Integer integer : v){
                temp.add(integer);
            }
            result.add(new LinkedList<>(temp));
        }
        else{
            if(v.size()!=0)
                p = data.indexOf(v.getLast());
            else
                p=0;
            for(int j = p ; j<data.size() ; j++){
                v.add(data.get(j));
                recursive2(n+1, N, result, v, data);
                v.removeLast();
            }
        }
    }


}
