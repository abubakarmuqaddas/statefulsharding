package statefulsharding;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPOptimization;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.heuristic.StateSync;
import statefulsharding.heuristic.TrafficHeuristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Created by root on 4/24/17.
 */
public class RealImplementationRome {


    public static void main(String[] args){

        int capacity = Integer.MAX_VALUE;
        int demand = 1;
        int numCopies=2;
        boolean multipleStates = true;
        boolean rearrangeStates = false;
        boolean verbose = false;
        boolean fixConstraints = false;
        TrafficHeuristic.hType shortestpath = TrafficHeuristic.hType.shortestpath;
        TrafficHeuristic.hType betweenness = TrafficHeuristic.hType.betweenness;
        TrafficHeuristic.hType partitioning = TrafficHeuristic.hType.fixedcopies;



        ListGraph graph = new ListGraph();

        graph.addVertex(0);
        graph.addVertex(1);
        graph.addVertex(2);
        graph.insertDoubleEdge(0,1,capacity);
        graph.insertDoubleEdge(1,2,capacity);
        graph.insertDoubleEdge(2,1,capacity);
        graph.insertDoubleEdge(1,0,capacity);

        TrafficStore trafficStore = new TrafficStore();

        trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(0),graph.getVertex(1),demand));
        trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(2),graph.getVertex(1),demand));


/*
        TrafficHeuristic trafficHeuristicSP = new TrafficHeuristic(graph,
                                                                trafficStore,
                                                                numCopies,
                                                                shortestpath,
                                                                multipleStates,
                                                                rearrangeStates);

        System.out.println("Shortest Path traffic: " + trafficHeuristicSP.getTotalTraffic());
*/

        OptimizationOptions options = new OptimizationOptions(numCopies, verbose, fixConstraints, multipleStates);


        ShardedSNAPOptimization shardedSNAPOptimization = new ShardedSNAPOptimization(graph, trafficStore, options);
        shardedSNAPOptimization.optimize();
        shardedSNAPOptimization.printSolution();
        shardedSNAPOptimization.getStateLocation().forEach(vertex -> {
            System.out.println("StateVariable placed at: " + vertex.getLabel());
        });
            System.out.println(shardedSNAPOptimization.getObjectiveValue());



        LinkedList<Double> trafficValuesBefore = new LinkedList<>();
        LinkedList<Double> trafficValuesAfter = new LinkedList<>();


        for(int h=1; h<=100; h++) {

            System.out.println();
            System.out.println();
            System.out.println();

            HashMap<Vertex, ListGraph> partitions = Partitioning.EvolutionaryPartition(graph, numCopies,
                    100,
                    "random",
                    "betweenness",
                    null,
                    false,
                    false,
                    null);

            System.out.println("Copy after partitions at: ");
            partitions.keySet().forEach(vertex -> {
                System.out.print(vertex.getLabel()+" ");
            });
            System.out.println();

            ArrayList<Vertex> sortedVertices = new ArrayList<>(partitions.keySet());


            TrafficHeuristic trafficHeuristicPart = new TrafficHeuristic(graph,
                    trafficStore,
                    numCopies,
                    partitioning,
                    sortedVertices,
                    multipleStates,
                    rearrangeStates);


            LinkedHashMap<TrafficDemand, Path> routingSolutionPart = trafficHeuristicPart.getRoutingSolution();
            System.out.println("partitioning Traffic: " + trafficHeuristicPart.getTotalTraffic());
            trafficValuesBefore.add(trafficHeuristicPart.getTotalTraffic());

            LinkedHashMap<TrafficDemand, Path> routingSolutionAugmented =
                    StateSync.augmentEdges(graph, sortedVertices, routingSolutionPart);


            System.out.println(TrafficHeuristic.getTrafficFromDemands(routingSolutionAugmented));
            trafficValuesAfter.add(TrafficHeuristic.getTrafficFromDemands(routingSolutionAugmented));

        }

        System.out.println(trafficValuesBefore.toString());
        System.out.println(trafficValuesAfter.toString());





    }



}
