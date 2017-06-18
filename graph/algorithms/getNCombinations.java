package statefulsharding.graph.algorithms;

import java.util.LinkedList;

/**
 * Created by root on 5/12/17.
 */
public class getNCombinations {

    private LinkedList<Integer> v;
    private LinkedList<Integer> data;
    private int N;
    private LinkedList<LinkedList<Integer>> result;

    public getNCombinations(LinkedList<Integer> data, int N){
        this.data = data;
        this.N = N;
        result = new LinkedList<>();
        v = new LinkedList();
        recursive(0);
    }

    private void recursive(int n){
        int p;
        if(n==N)
            result.add(new LinkedList<>(v));
        else{
            if(v.size()!=0)
                p = data.indexOf(v.getLast());
            else
                p=-1;

            for(int j = p+1 ; j<data.size() ; j++){
                int i = data.get(j);
                v.add(i);
                recursive(n+1);
                v.removeLast();
            }
        }
    }

    public LinkedList<LinkedList<Integer>> getResult(){
        return result;
    }




}
