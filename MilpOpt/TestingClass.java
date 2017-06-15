package statefulsharding.MilpOpt;

import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;


public class TestingClass {

    public static void main(String[] args){

        int capacity = Integer.MAX_VALUE;

        int size = 3;

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, capacity,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);

        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        TrafficStore trafficStore = new TrafficStore();

        TrafficDemand trafficDemand1 = new TrafficDemand(graph.getVertex(0),graph.getVertex(8),1);
        //TrafficDemand trafficDemand2 = new TrafficDemand(graph.getVertex(1),graph.getVertex(2),1);
        //TrafficDemand trafficDemand3 = new TrafficDemand(graph.getVertex(2),graph.getVertex(0),1);
        //TrafficDemand trafficDemand4 = new TrafficDemand(graph.getVertex(3),graph.getVertex(0),1);


        trafficStore.addTrafficDemand(trafficDemand1);
        /*
        trafficStore.addTrafficDemand(trafficDemand2);
        trafficStore.addTrafficDemand(trafficDemand3);
        trafficStore.addTrafficDemand(trafficDemand4);
        */

        StateVariable A = new StateVariable("A");
        StateVariable B = new StateVariable("B");

        Set<StateVariable> states = new HashSet<>();

        states.add(A);
        states.add(B);

        HashMap<TrafficDemand, LinkedList<StateVariable>> Xf = new HashMap<>();

        Xf.put(trafficDemand1, new LinkedList<>());
        Xf.get(trafficDemand1).add(A);
        Xf.get(trafficDemand1).add(B);


        /*

        Xf.put(trafficDemand2, new LinkedList<>());
        Xf.get(trafficDemand2).add(B);

        Xf.put(trafficDemand3, new LinkedList<>());
        Xf.get(trafficDemand3).add(A);
        Xf.get(trafficDemand3).add(B);

        Xf.put(trafficDemand4, new LinkedList<>());
        Xf.get(trafficDemand4).add(A);
        Xf.get(trafficDemand4).add(B);
        */

        SNAPDependency snapDependency = new SNAPDependency(graph, trafficStore, false,
                true, states, Xf);

        snapDependency.optimize();
        snapDependency.printSolution();


    }
}
