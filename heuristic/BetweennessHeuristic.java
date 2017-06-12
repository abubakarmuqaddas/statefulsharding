package statefulsharding.heuristic;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Centrality;
import statefulsharding.graph.algorithms.ShortestPath;

import java.util.*;

public class BetweennessHeuristic {

    private ListGraph graph;
    private TrafficStore trafficStore;
    private int copies;
    private LinkedHashSet<Vertex> stateLocation;

    public BetweennessHeuristic(ListGraph graph, TrafficStore trafficStore, int copies){
        this.graph=graph;
        this.trafficStore=trafficStore;
        this.copies=copies;
    }

    public void execute(){

        LinkedHashMap<Vertex,Double> betCen =  Centrality.Betweenness(graph);

        ArrayList<Vertex> sortedVertices = new ArrayList<>(betCen.keySet());

        stateLocation = new LinkedHashSet<>();

        for (int i=1 ; i<=copies ; i++){
            stateLocation.add(sortedVertices.get(betCen.size()-i));
        }

        Vertex state = sortedVertices.get(betCen.size()-1);

        stateLocation.forEach(vertex -> System.out.println(vertex.getLabel()));

        int pathLength;
        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            Path pathSourceState = ShortestPath.dijsktra(graph,trafficDemand.getSource(),state);
            Path pathStateDestination = ShortestPath.dijsktra(graph,state,trafficDemand.getDestination());

            Path mergedPath = pathSourceState.merge(pathSourceState,pathStateDestination);

            trafficStore.addActiveTraffic(trafficDemand,mergedPath);
        }
    }

    private boolean containsVertex(Path path, Vertex vertex){
        for(Edge edge : path.getEdges()){
            if (edge.getSource().equals(vertex) || edge.getDestination().equals(vertex))
                return true;
        }
        return false;
    }

}
