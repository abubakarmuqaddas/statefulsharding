package statefulsharding.heuristic;

import statefulsharding.MapUtils;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.DirectedConnectedComponent;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class PartitioningWithStateSync {

    public static void main(String[] args){

        int size = 9;

        int minTraffic = 1;
        int maxTraffic = 10;
        int minRun = 1;
        int maxRun = 10;
        int minCopy = 2;
        int maxCopy = 6;

        for(int traffic=minTraffic ; traffic<=maxTraffic ; traffic++){
            for(int run=minRun ; run<=maxRun ; run++){
                for(int copy=minCopy ; copy<=maxCopy ; copy++){

                    System.out.println("Traffic: " + traffic + ", Run: " + run + ", copy: " + copy);

                    ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(9, Integer.MAX_VALUE,
                            ManhattanGraphGen.mType.UNWRAPPED, false, true);
                    ListGraph graph = manhattanGraphGen.getManhattanGraph();

                    String trafficFile = "analysis/MANHATTAN-UNWRAPPED_deterministicTfc-evaluateTrafficHeuristic-" +
                            "fixCopies_" + size + "/MANHATTAN-UNWRAPPED_deterministicTfc-evaluateTrafficHeuristic-" +
                            "fixCopies_" + size + "_run_" + traffic + "_traffic.txt";
                    String target = "analysis/MANHATTAN-UNWRAPPED_deterministicTfc-evaluateTrafficHeuristic" +
                            "-Partition_" + size + "/MANHATTAN-UNWRAPPED_deterministicTfc-evaluateTrafficHeuristic" +
                            "-Partition_" + size + "_traffic_" + traffic + "_run_" + run + "_partition_copy_" + copy;

                    LinkedHashMap<TrafficDemand, Path> routingSolution = augmentEdges(graph,trafficFile,target,
                            true);

                    graph.clear();
                    System.out.println();

                    writeRoutingSolution(routingSolution, target);

                }

            }
        }
    }

    private static LinkedHashMap<TrafficDemand, Path> augmentEdges(ListGraph graph, String trafficFile,
                                                                   String target, boolean generateFiles){

        String routingSolutionFile = target + "_RoutingSolution.txt";
        String placementFile = target + "_PlacementSolution.txt";

        TrafficStore trafficStore = new TrafficStore();

        TrafficGenerator.fromFile(graph, trafficStore, trafficFile);

        LinkedHashMap<TrafficDemand, Path> routingSolution = TrafficGenerator.fromFileRouting(graph,
                trafficStore, routingSolutionFile);

        ArrayList<Vertex> copies = Partitioning.getCopies(graph, placementFile, true);

        ListGraph auxGraph = new ListGraph();
        ArrayList<Vertex> candidates = new ArrayList<>();

        copies.forEach(vertex -> {
            auxGraph.addVertex(vertex.getLabel());
        });

        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
            int i = 0;
            candidates.clear();
            for (Vertex vertex : copies) {
                if (routingSolution.get(trafficDemand).containsVertex(vertex)) {
                    i++;
                    candidates.add(vertex);
                }
            }
            if (i > 1) {
                for (int j = 0; j < candidates.size() - 1; j++) {
                    auxGraph.insertEdge(candidates.get(j).getLabel(),
                            candidates.get(j + 1).getLabel(),
                            Integer.MAX_VALUE);
                }
            }
        }

        DirectedConnectedComponent directedConnectedComponent = new DirectedConnectedComponent(auxGraph);

        directedConnectedComponent.FindConnectedComponents();
        List<List<Vertex>> components;
        /*
           Make a subgraph for connected components
         */

        ListGraph componentGraph = null;


        componentGraph = generateComponentFromAux(auxGraph,componentGraph,directedConnectedComponent);
        components = directedConnectedComponent.getConnectedVertices();

        /*
            This Vertex is in componentGraph i.e. iterating over each Component
         */

        boolean noSuccessors = true;
        boolean componentGraphModified;
        LinkedHashMap<Vertex, Integer> importance;
        System.out.println("Traffic Value: " + TrafficHeuristic.getTrafficFromDemands(routingSolution));

        while (noSuccessors) {

            componentGraphModified = false;

            importance = new LinkedHashMap<>();

            for (Vertex vertex : componentGraph.getVertices()) {
                importance.put(vertex,
                        componentGraph.getSuccessors(vertex).size() + componentGraph.getPredecessors(vertex).size());
            }

            importance = MapUtils.sortMapByValue(importance);

            for (Vertex vertex : importance.keySet()) {

                int numSuccessors = componentGraph.getSuccessors(vertex).size();
                if (numSuccessors == 0 && components.size()>1){
                    int currentComponentNum = vertex.getLabel();
                /*
                    The Vertex in "List<Vertex> targetVertices" is from auxGraph (with original id of initial
                    Graph
                 */
                    List<Vertex> targetVertices = directedConnectedComponent.ComponentToVertex(currentComponentNum);

                    List<Vertex> candidateVertices = new ArrayList<>();

                    for (int i = 0; i < components.size(); i++) {
                        if (i != currentComponentNum) {
                        /*
                            Each of this Vertex is from auxGraph
                         */
                            components.get(i).forEach(candidateVertex -> {
                                candidateVertices.add(candidateVertex);
                            });
                        }
                    }

                /*
                    Find the pair of targetVertex/CandidateVertex which has the shortest path length
                */

                    Path shortest = null;
                    int min = Integer.MAX_VALUE;

                    for (Vertex targetVertex : targetVertices) {
                        for (Vertex candidateVertex : candidateVertices) {

                            Vertex originalTargetVertex = graph.getVertex(targetVertex.getLabel());
                            Vertex originalCandidateVertex = graph.getVertex(candidateVertex.getLabel());

                            Path candidatePath =
                                    ShortestPath.dijsktra(graph, originalTargetVertex, originalCandidateVertex);

                            if (candidatePath.getSize() < min) {
                                shortest = candidatePath;
                                min = candidatePath.getSize();
                            }
                        }
                    }

                    if (shortest != null) {
                        Vertex decidedSource = shortest.getSource();
                        Vertex decidedDestination = shortest.getDestination();

                        routingSolution.put(new TrafficDemand(decidedSource, decidedDestination, 1),
                                shortest);

                        auxGraph.insertEdge(decidedSource.getLabel(), decidedDestination.getLabel(),
                                Integer.MAX_VALUE);

                        componentGraph = generateComponentFromAux(auxGraph,componentGraph,directedConnectedComponent);
                        components = directedConnectedComponent.getConnectedVertices();
                        componentGraphModified = true;
                        break;
                    }
                }
            }

            if(!componentGraphModified)
                noSuccessors = false;
        }

        System.out.println("Traffic Value: " + TrafficHeuristic.getTrafficFromDemands(routingSolution));
        return routingSolution;
    }

    private static ListGraph generateComponentFromAux(ListGraph auxGraph, ListGraph componentGraph,
                                                     DirectedConnectedComponent directedConnectedComponent){

        if(componentGraph!=null)
            componentGraph.clear();
        componentGraph = new ListGraph();

        directedConnectedComponent.FindConnectedComponents();

        List<List<Vertex>> components = directedConnectedComponent.getConnectedVertices();

        for (int i=0 ; i<components.size() ; i++){
            componentGraph.addVertex(i);
        }
        /*
            Generate edges between components in component graph
         */
        for (int currentComponent=0 ; currentComponent<components.size() ; currentComponent++) {
            /*
            The vertex from "List<Vertex> component" is from the auxGraph
             */
            List<Vertex> component = components.get(currentComponent);

            /*
            Iterate over each vertex in each component graph
             */
            for (Vertex vertex : component){

                /*
                    Vertices in this line are from the auxGraph
                 */
                Set<Vertex> successors = auxGraph.getSuccessors(vertex);

                /*
                Check if current vertex in this component is
                connected to another vertex of another component
                by iterating over successors.
                If successor is in the same component, skip
                Otherwise search the component holding the successor vertex
                 */

                for (Vertex successor : successors){
                    if(!component.contains(successor)){
                        int successorComponent = directedConnectedComponent.vertexToComponent(successor);
                        componentGraph.insertEdge(currentComponent, successorComponent, Double.MAX_VALUE);
                    }
                }
            }
        }

        return componentGraph;

    }

    private static void writeRoutingSolution(LinkedHashMap<TrafficDemand, Path> routingSolution,
                                             String filename){

        try{

            FileWriter logfw = new FileWriter(filename + "_Logfile_Augmented.txt" ,true);
            BufferedWriter logbw = new BufferedWriter(logfw);
            PrintWriter logout = new PrintWriter(logbw);
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            logout.println(sdf.format(cal.getTime()) + " Objective Value: " +
                    TrafficHeuristic.getTrafficFromDemands(routingSolution));
            logout.flush();

            FileWriter solnRfw = new FileWriter(filename + "_RoutingSolution_Augmented.txt" ,
                    true);
            BufferedWriter solnRbw = new BufferedWriter(solnRfw);
            PrintWriter solnRout = new PrintWriter(solnRbw); solnRout.println("u v i j");

            for (TrafficDemand trafficDemand : routingSolution.keySet()) {
                for (Edge edge : routingSolution.get(trafficDemand).getEdges()) {
                    solnRout.println(trafficDemand.getSource().getLabel() + " " +
                            trafficDemand.getDestination().getLabel() + " " +
                            edge.getSource().getLabel() + " " +
                            edge.getDestination().getLabel());
                }
            }
            solnRout.flush();

        }
        catch (IOException e){
            System.out.println("Error " + e + "while writing solution in Traffic Heuristic");
        }


    }

}
