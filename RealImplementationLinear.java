package statefulsharding;

import statefulsharding.MilpOpt.OptimizationOptions;
import statefulsharding.MilpOpt.ShardedSNAPOptimization;
import statefulsharding.MilpOpt.ShardedSNAPOptimizationReactivity;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Centrality;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.heuristic.StateSync;
import statefulsharding.heuristic.TrafficHeuristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class RealImplementationLinear {


    public static void main(String[] args){
        double capacity = Integer.MAX_VALUE;

        int numCopies=2;
        boolean multipleStates = true;
        boolean rearrangeStates = false;
        boolean verbose = false;
        boolean fixConstraints = false;
        TrafficHeuristic.hType shortestpath = TrafficHeuristic.hType.shortestpath;
        TrafficHeuristic.hType betweenness = TrafficHeuristic.hType.betweenness;
        TrafficHeuristic.hType partitioning = TrafficHeuristic.hType.fixedcopies;

        double demand = 10;
        double alpha = 0.8;



        ListGraph graph = new ListGraph();

        int numNodes = 100;

        for (int i=0 ; i<numNodes ; i++){
            graph.addVertex(i);
        }

        for (int i=0 ; i<numNodes-1 ; i++){
            graph.insertDoubleEdge(i,i+1,capacity);
        }

        TrafficStore trafficStore = new TrafficStore();

        trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(0),graph.getVertex(1),
                alpha*demand));
       // trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(35),graph.getVertex(37),
       //         1));
        trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(numNodes-1),
                graph.getVertex(numNodes-2),
                (1-alpha)*demand));



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
        LinkedList<Double> trafficValuesAfter = new LinkedList<>();



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


            LinkedHashMap<TrafficDemand, Path> routingSolutionPart = trafficHeuristicPart.getRoutingSolution();
            System.out.println("partitioning Traffic: " + trafficHeuristicPart.getTotalTraffic());
            trafficValuesBefore.add(trafficHeuristicPart.getTotalTraffic());

            /*
            LinkedHashMap<TrafficDemand, Path> routingSolutionAugmented =
                    StateSync.augmentEdges(graph, sortedVertices, routingSolutionPart);


            System.out.println(TrafficHeuristic.getTrafficFromDemands(routingSolutionAugmented));
            //trafficValuesAfter.add(TrafficHeuristic.getTrafficFromDemands(routingSolutionAugmented));
            trafficValuesAfter.add((TrafficHeuristic.getTrafficFromDemands(routingSolutionAugmented)));
            */

        }

        System.out.println(trafficValuesBefore.toString());
        System.out.println(trafficValuesAfter.toString());




    }



}
