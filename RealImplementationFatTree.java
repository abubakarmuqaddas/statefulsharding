package statefulsharding;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPOptimizationReactivity;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Centrality;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.heuristic.StateSync;
import statefulsharding.heuristic.TrafficHeuristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class RealImplementationFatTree {


    public static void main(String[] args){
        double capacity = Integer.MAX_VALUE;

        int numCopies=1;
        boolean multipleStates = true;
        boolean rearrangeStates = false;
        boolean verbose = false;
        boolean fixConstraints = false;
        TrafficHeuristic.hType shortestpath = TrafficHeuristic.hType.shortestpath;
        TrafficHeuristic.hType betweenness = TrafficHeuristic.hType.betweenness;
        TrafficHeuristic.hType partitioning = TrafficHeuristic.hType.fixedcopies;

        ListGraph graph = new ListGraph();
        TrafficStore trafficStore = new TrafficStore();

        int numNodes = 7;

        for (int i=0 ; i<numNodes ; i++){
            graph.addVertex(i);
        }

        graph.insertDoubleEdge(0,1,capacity);
        graph.insertDoubleEdge(0,2,capacity);
        graph.insertDoubleEdge(1,3,capacity);
        graph.insertDoubleEdge(1,4,capacity);
        graph.insertDoubleEdge(2,5,capacity);
        graph.insertDoubleEdge(2,6,capacity);


        for(int i=0 ; i<numNodes ; i++){
            for(int j=0 ; j<numNodes ; j++){
                if(i!=j) {
                    trafficStore.addTrafficDemand(
                            new TrafficDemand(graph.getVertex(i),
                                    graph.getVertex(j),
                                    1));
                }
            }
        }

        LinkedHashMap<Vertex, Double> cen = Centrality.Betweenness(graph);

        cen.forEach((vertex, aDouble) -> System.out.println(vertex.getLabel() + ": " + aDouble));

        /*
        TrafficHeuristic trafficHeuristicSP = new TrafficHeuristic(graph,
                                                                trafficStore,
                                                                numCopies,
                                                                shortestpath,
                                                                multipleStates,
                                                                rearrangeStates);

        System.out.println("Shortest Path traffic: " + trafficHeuristicSP.getTotalTraffic());
        */

        /*
        OptimizationOptions options = new OptimizationOptions(numCopies, verbose, fixConstraints, multipleStates);


        ShardedSNAPOptimizationReactivity shardedSNAPOptimization = new ShardedSNAPOptimizationReactivity
                (graph, trafficStore, options);
        shardedSNAPOptimization.optimize();
        shardedSNAPOptimization.printSolution();
        shardedSNAPOptimization.getStateLocation().forEach(vertex -> {
            System.out.println("StateVariable placed at: " + vertex.getLabel());
        });
            System.out.println(shardedSNAPOptimization.getObjectiveValue());

        */



        LinkedList<Double> trafficValuesBefore = new LinkedList<>();



        for(int h=0; h<1; h++) {

            System.out.println();
            System.out.println();
            System.out.println();

            HashMap<Vertex, ListGraph> partitions = Partitioning.EvolutionaryPartition(graph, numCopies,
                            100,
                            "random",
                            "betweennesstfc",
                            trafficStore,
                            false,
                            false,
                            null);
            System.out.println("Copy after partitions at: ");
            partitions.keySet().forEach(vertex -> System.out.print(vertex.getLabel() + " "));
            System.out.println();

            ArrayList<Vertex> sortedVertices = new ArrayList<>(partitions.keySet());


            TrafficHeuristic trafficHeuristicPart = new TrafficHeuristic(graph,
                    trafficStore,
                    numCopies,
                    partitioning,
                    sortedVertices,
                    multipleStates,
                    rearrangeStates);

            System.out.println("partitioning Traffic: " + trafficHeuristicPart.getTotalTraffic());
            trafficValuesBefore.add(trafficHeuristicPart.getTotalTraffic());

        }

        System.out.println(trafficValuesBefore.toString());



    }



}
