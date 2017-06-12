package statefulsharding;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPOptimization;
import statefulsharding.MilpOpt.ShardedSNAPOptimizationReactivity;
import statefulsharding.Traffic.TrafficAllocator;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.heuristic.StateSync;
import statefulsharding.heuristic.TrafficHeuristic;

import java.util.*;

public class RealImplementation {


    public static void main(String[] args){

        double capacity = Integer.MAX_VALUE;

        int numCopies=3;
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
        graph.addVertex(3);
        graph.addVertex(4);
        graph.addVertex(5);
        graph.addVertex(6);
        graph.addVertex(7);
        graph.insertDoubleEdge(0,1,capacity);
        graph.insertDoubleEdge(1,2,capacity);
        graph.insertDoubleEdge(2,3,capacity);
        graph.insertDoubleEdge(3,4,capacity);
        graph.insertDoubleEdge(4,5,capacity);
        graph.insertDoubleEdge(5,6,capacity);
        graph.insertDoubleEdge(6,7,capacity);
        graph.insertDoubleEdge(7,0,capacity);

        LinkedList<Double> trafficValuesOpt = new LinkedList<>();
        LinkedList<Double> trafficValuesAfterOpt = new LinkedList<>();

        LinkedList<Double> trafficValuesBefore = new LinkedList<>();
        LinkedList<Double> trafficValuesAfter = new LinkedList<>();
        /*
        double demand = 1;

        TrafficStore trafficStore = new TrafficStore();

        trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(0), graph.getVertex(1), demand));
        trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(2), graph.getVertex(3), demand));
        trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(4), graph.getVertex(5), demand));
        trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(6), graph.getVertex(7), demand));
        */

        for (int h=0 ; h<100 ; h++) {

            double demand = 1+0.01*h;

            TrafficStore trafficStore = new TrafficStore();
            /*
            for (int i = 0; i <= 6; i = i + 2) {
                for (int j = 1; j <= 7; j = j + 2) {
                    trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(i), graph.getVertex(j), demand));
                }
            }
            */


            trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(0), graph.getVertex(1), demand));
            trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(2), graph.getVertex(3), demand));
            trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(4), graph.getVertex(5), demand));
            trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(6), graph.getVertex(7), demand));


            ArrayList<Vertex> copies = new ArrayList<>();

            OptimizationOptions options = new OptimizationOptions(numCopies, verbose, fixConstraints, multipleStates);

            ShardedSNAPOptimizationReactivity shardedSNAPOptimization = new ShardedSNAPOptimizationReactivity
                    (graph, trafficStore, options);
            shardedSNAPOptimization.optimize();
            shardedSNAPOptimization.printSolution();
            shardedSNAPOptimization.getStateLocation().forEach(vertex -> {
                System.out.println("StateVariable placed at: " + vertex.getLabel());
                copies.add(vertex);
            });

            trafficValuesOpt.add(shardedSNAPOptimization.getObjectiveValue());

            LinkedHashMap<TrafficDemand, Path> routingSolution = shardedSNAPOptimization.getLoopFreeRoutingSolution();

            if(numCopies>1) {

                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                System.out.println();
                System.out.println();
                System.out.println();

                LinkedHashMap<TrafficDemand, Path> routingSolutionAugmented =
                        StateSync.augmentEdges(graph, copies, routingSolution);


                for (TrafficDemand trafficDemand : routingSolutionAugmented.keySet()) {
                    System.out.println("Augmented Traffic demand: " + trafficDemand.getSource().getLabel() + " -> " +
                            trafficDemand.getDestination().getLabel());

                    for (Edge edge : routingSolutionAugmented.get(trafficDemand).getEdges()) {

                        System.out.println(edge.getSource().getLabel() + " -> " + edge.getDestination().getLabel());
                    }
                    System.out.println();
                }

                double ds = TrafficHeuristic.getTrafficFromDemands(routingSolutionAugmented);
                trafficValuesAfterOpt.add(ds);
                if(ds>0){
                    System.out.println("Traffic value after state sync: " + ds + " d_s");
                }

            }
            System.out.println(trafficValuesOpt.toString());
            System.out.println(trafficValuesAfterOpt.toString());

        }
        /*
        System.out.println("Optimum traffic values: ");
        System.out.println(trafficValuesOpt.toString());
        if(numCopies>1) {
            System.out.println("StateVariable sync values: ");
            System.out.println(trafficValuesAfterOpt.toString());
        }
        */

        /*

        for(int h=1; h<=100; h++) {

            System.out.println();
            System.out.println();
            System.out.println();

            HashMap<Vertex, ListGraph> partitions = Partitioning.EvolutionaryPartition(graph, numCopies,
                    100,
                    "random",
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
            trafficValuesAfter.add((TrafficHeuristic.getTrafficFromDemands(routingSolutionAugmented)));

        }

        System.out.println(trafficValuesBefore.toString());
        System.out.println(trafficValuesAfter.toString());

        */

    }
}



        /*
        TrafficHeuristic trafficHeuristicSP = new TrafficHeuristic(graph,
                                                                trafficStore,
                                                                numCopies,
                                                                shortestpath,
                                                                multipleStates,
                                                                rearrangeStates);

        System.out.println("Shortest Path traffic: " + trafficHeuristicSP.getTotalTraffic());
        */