package statefulsharding.Traffic;

import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.algorithms.ShortestPath;

/**
 * Created by build on 02/12/16.
 */
public class TrafficAllocator {

    private TrafficStore trafficStore;

    public TrafficAllocator(TrafficStore trafficStore){
        this.trafficStore=trafficStore;
    }

    public void allocateTrafficDijkstraConstrained(ListGraph graph){
        for(TrafficDemand trafficDemand: trafficStore.getTrafficDemands()){
            Path shortestPath = ShortestPath.dijsktraCapacityConstrained(
                    graph,trafficDemand.getSource(),
                    trafficDemand.getDestination(),
                    trafficDemand.getDemand());

            if(shortestPath==null){
                System.out.println("No Path available for Source: " + trafficDemand.getSource().getLabel() +
                        ", Destination: " + trafficDemand.getDestination().getLabel());
                continue;
            }

            if(ShortestPath.checkPathCapacity(shortestPath,trafficDemand)){
                trafficStore.addActiveTraffic(trafficDemand,shortestPath);
            }
        }
    }
}
