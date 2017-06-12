package statefulsharding.randomgraphgen;

import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.*;

public class RandomGraphDemo {


    public static void main(String[] args){


        int size = 12;
        double p = 0.1;
        boolean decrement = true;
        String graphLocation = "topologies_traffic/Traffic/WS_graph_" + p +
                "/WS_graph" + size + "_" + p + "_8.csv";
        ListGraph graph = LoadGraph.GraphParserJ(graphLocation, Integer.MAX_VALUE, decrement);

        TrafficStore trafficStore = new TrafficStore();
        int run=1;

        TrafficGenerator.fromFileLinebyLine(graph,trafficStore,run,1,decrement,
                    "topologies_traffic/Traffic/WS_Traffic/WS_Traffic" + size +
                            ".csv");

        int d=1;







    }



}

