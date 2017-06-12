package statefulsharding.heuristic;

import statefulsharding.MapUtils;
import statefulsharding.Pair;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Centrality;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;

/**
 * Created by root on 5/28/17.
 */
public class PaoloAlgo {

    public static void main(String[] args) {

        int capacity = Integer.MAX_VALUE;

        int size = 3;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);

        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        TrafficStore trafficStore = new TrafficStore();

        TrafficDemand trafficDemand1 = new TrafficDemand(graph.getVertex(0),graph.getVertex(5),1);
        TrafficDemand trafficDemand2 = new TrafficDemand(graph.getVertex(8),graph.getVertex(6),5);
        TrafficDemand trafficDemand3 = new TrafficDemand(graph.getVertex(2),graph.getVertex(4),3);

        trafficStore.addTrafficDemand(trafficDemand1);
        trafficStore.addTrafficDemand(trafficDemand2);
        trafficStore.addTrafficDemand(trafficDemand3);

        StateVariable s = new StateVariable("s");
        StateVariable t = new StateVariable("t");

        Set<StateVariable> states = new HashSet<>();
        states.add(s);
        states.add(t);
        HashMap<TrafficDemand, LinkedList<StateVariable>> Xf = new HashMap<>();
        Xf.put(trafficDemand1, new LinkedList<>());
        Xf.get(trafficDemand1).add(s);
        Xf.get(trafficDemand1).add(t);

        Xf.put(trafficDemand2, new LinkedList<>());
        Xf.get(trafficDemand2).add(s);

        Xf.put(trafficDemand3, new LinkedList<>());
        Xf.get(trafficDemand3).add(t);


        /*
         * Following is Paolo's Algorithm
         */

        LinkedHashMap<TrafficDemand, Double> F = new LinkedHashMap<>();

        for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            F.put(trafficDemand,trafficDemand.getDemand());
        }

        F = MapUtils.sortMapByValueDescending(F);


        HashMap<StateVariable, LinkedList<TrafficDemand>> Fx = new LinkedHashMap<>();
        HashMap<StateVariable, Double> bx = new HashMap<>();

        for(StateVariable state : states){

            Fx.put(state, new LinkedList<>());
            bx.putIfAbsent(state, 0.0);

            for(TrafficDemand trafficDemand : F.keySet()){
                if(Xf.get(trafficDemand).contains(state)){

                    double temp = bx.get(state);
                    temp = temp + trafficDemand.getDemand();
                    bx.put(state,temp);

                    Fx.get(state).add(trafficDemand);

                }
            }
        }

        LinkedList<StateVariable> S = new LinkedList<>(MapUtils.sortMapByValueDescending(bx).keySet());

        HashMap<StateVariable, Vertex> Yx = new HashMap<>();

        for(StateVariable state : S){
            HashMap<Pair<Vertex, Vertex>, Double> OmegaX = new HashMap<>();

            for (TrafficDemand trafficDemand : Fx.get(state)){
                Pair<Vertex, Vertex> tuple = findChain(trafficDemand, Xf.get(trafficDemand), state, Yx);
                OmegaX.put(tuple, trafficDemand.getDemand());
                place(graph, state, OmegaX, Yx);
            }
        }
    }

    private static Pair<Vertex, Vertex> findChain(TrafficDemand trafficDemand,
                                                  LinkedList<StateVariable> Xf, StateVariable x, HashMap<StateVariable, Vertex> Yk){

        Vertex s = trafficDemand.getSource();
        boolean found = false;
        Vertex d;

        for(StateVariable k : Xf) {
            if (k.equals(x)) {
                found = true;
                continue;
            }

            if (Yk.get(k) != null) {
                if (found) {
                    d = Yk.get(k);
                    return new Pair<>(s, d);
                }
                else
                    s = Yk.get(k);
            }
        }

        d = trafficDemand.getDestination();
        return new Pair<>(s, d);
    }

    private static void place(ListGraph graph, StateVariable x, HashMap<Pair<Vertex, Vertex>, Double> OmegaX,
                              HashMap<StateVariable, Vertex> Yx){

        TrafficStore trafficStore = new TrafficStore();

        for(Pair<Vertex, Vertex> pair: OmegaX.keySet()){
            trafficStore.addTrafficDemand(new TrafficDemand(pair.getFirst(), pair.getSecond(), OmegaX.get(pair)));
        }

        LinkedHashMap<Vertex, Double> centrality = Centrality.BetweennessTfc(graph,trafficStore);
        ArrayList<Vertex> sortedVertices = new ArrayList<>(centrality.keySet());
        Yx.put(x, sortedVertices.get(centrality.size()-1));
    }

}
