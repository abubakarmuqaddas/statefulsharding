package statefulsharding.graph;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.algorithms.ShortestPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Given the location of the states, traffic and graph, this
 * class routes the traffic to the closest copy in the network
 * Two variants:
 *  - A flow can only cross one state
 *  - A flow can cross multiple states
 */

public class RouteThroughState {

    public static Path route(ListGraph graph, TrafficDemand trafficDemand,
                                      LinkedList<Vertex> copies, boolean multipleStates){

        Vertex source = trafficDemand.getSource();
        Vertex destination = trafficDemand.getDestination();

        LinkedList<Vertex> otherCopies = new LinkedList<>(copies);
        HashMap<Vertex,Path> Paths = new HashMap<>();
        int min=Integer.MAX_VALUE;
        Vertex candidate = null;
        Path path=null;

        Vertex initialTarget=null;

        if (copies.contains(source) && copies.contains(destination) && source.equals(destination)){
            return ShortestPath.dijsktra(graph,source,destination);
        }

        if (copies.contains(source)|| copies.contains(destination)){
            if (copies.contains(source) && copies.contains(destination) && !multipleStates){
                //System.out.println("Blocking condition: Source and Destination are both copies");
                return null;
                //return ShortestPath.dijsktra(graph,source,destination);
            }
            else if(copies.contains(source)) {
                initialTarget = source;
            }
            else if(copies.contains(destination)){
                initialTarget = destination;
            }

            otherCopies.remove(initialTarget);

            if(multipleStates)
                return ShortestPath.dijsktra(graph,source,destination);
            else
                return ShortestPath.dijsktraNodesConstrained(graph,source,destination,otherCopies);
        }

        for(Vertex target : copies){

            Path path1 = null;
            Path path2 = null;

            if(!multipleStates) {
                otherCopies.remove(target);
                path1 = ShortestPath.dijsktraNodesConstrained(graph, source, target, otherCopies);
                path2 = ShortestPath.dijsktraNodesConstrained(graph, target, destination, otherCopies);

                if (path1 == null || path2 == null) {
                    otherCopies.add(target);
                    continue;
                }
            }
            else{
                path1 = ShortestPath.dijsktra(graph, source, target);
                path2 = ShortestPath.dijsktra(graph, target, destination);

                if (path1 == null || path2 == null) {
                    continue;
                }
            }

            path = Path.merge(path1,path2);
            Paths.put(target,path);
            if(!multipleStates)
                otherCopies.add(target);
        }

        for(Vertex target : copies){
            if(Paths.get(target)!=null) {
                if (Paths.get(target).getSize() < min) {
                    min = Paths.get(target).getSize();
                    candidate = target;
                }
            }
        }
        return Paths.get(candidate);

    }

}
