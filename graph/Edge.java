package statefulsharding.graph;

import statefulsharding.Traffic.TrafficDemand;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by build on 28/10/16.
 */

public class Edge {

    private final Vertex source;
    private final Vertex destination;
    private double capacity;
    private double totaltraffic;
    private double residualcapacity;
    private Set<TrafficDemand> carriedTraffic;

    public Edge (Vertex source, Vertex destination, double capacity){

        this.source=source;
        this.destination=destination;
        this.capacity=capacity;
        this.totaltraffic=0;
        this.residualcapacity=capacity;
    }

    public Vertex getSource(){
        return source;
    }

    public Vertex getDestination(){
        return destination;
    }

    public double getCapacity(){
        return capacity;
    }

    public void setCarriedTraffic(TrafficDemand trafficdemand){
        if(carriedTraffic==null && trafficdemand.getDemand()<=residualcapacity){
            carriedTraffic = new HashSet<>();
            carriedTraffic.add(trafficdemand);
            totaltraffic+=trafficdemand.getDemand();
            residualcapacity=capacity-totaltraffic;
        }
        else if(trafficdemand.getDemand()+totaltraffic<=residualcapacity){
            carriedTraffic.add(trafficdemand);
            totaltraffic+=trafficdemand.getDemand();
            residualcapacity=capacity-totaltraffic;
        }
    }

    public Set<TrafficDemand> getCarriedTraffic(){
        return carriedTraffic;
    }

    public double getTotalTraffic(){
        return totaltraffic;
    }

    public double getResidualCapacity(){
        return residualcapacity;
    }

    public void removeCarriedTraffic(TrafficDemand trafficDemand){
        if(carriedTraffic.remove(trafficDemand)) {
            residualcapacity = residualcapacity + trafficDemand.getDemand();
            totaltraffic = totaltraffic - trafficDemand.getDemand();
        }

    }

    public void printEdge(){
        System.out.println(source.getLabel() + " -> " + destination.getLabel());
    }


}