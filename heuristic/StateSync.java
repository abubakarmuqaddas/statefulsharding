package statefulsharding.heuristic;

import statefulsharding.MapUtils;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.DirectedConnectedComponent;
import statefulsharding.graph.algorithms.ShortestPath;

import java.util.*;

/**
 * Created by root on 4/24/17.
 */
public class StateSync {

    public static ListGraph getStateGraph(ListGraph graph, ArrayList<Vertex> copies,
                                           LinkedHashMap<TrafficDemand, Path> routingSolution,
                                           TrafficStore trafficStore){

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

        return auxGraph;
    }

    public static LinkedHashMap<TrafficDemand, Path> augmentEdges(ListGraph graph, ArrayList<Vertex> copies,
                                          LinkedHashMap<TrafficDemand, Path> routingSolution){


        LinkedHashMap<TrafficDemand, Path> augmentedSolution = new LinkedHashMap<>();
        ListGraph auxGraph = new ListGraph();
        LinkedHashSet<Integer> candidatesSet = new LinkedHashSet<>();
        ArrayList<Integer> candidates = new ArrayList<>();

        copies.forEach(vertex -> {
            auxGraph.addVertex(vertex.getLabel());
        });

        for (TrafficDemand trafficDemand : routingSolution.keySet()) {

            candidates.clear();
            candidatesSet.clear();

            for (Edge edge: routingSolution.get(trafficDemand).getEdges()){
                if(copies.contains(edge.getSource()))
                    candidatesSet.add(edge.getSource().getLabel());
                if(copies.contains(edge.getDestination()))
                    candidatesSet.add(edge.getDestination().getLabel());
            }

            if (candidatesSet.size() > 1) {

                candidatesSet.forEach((Integer integer) -> {
                    candidates.add(integer);
                });

                for (int j = 0; j < candidates.size() - 1; j++) {
                    auxGraph.insertEdge(candidates.get(j),
                            candidates.get(j+1),
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

                        augmentedSolution.put(new TrafficDemand(decidedSource, decidedDestination, 1.0),
                                shortest);

                        auxGraph.insertEdge(decidedSource.getLabel(), decidedDestination.getLabel(),
                                Double.MAX_VALUE);

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

        //System.out.println("Augmented traffic: " + TrafficHeuristic.getTrafficFromDemands(augmentedSolution) + " d_s");
        return augmentedSolution;


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
                        componentGraph.insertEdge(currentComponent, successorComponent, Integer.MAX_VALUE);
                    }
                }
            }
        }

        return componentGraph;

    }


}
