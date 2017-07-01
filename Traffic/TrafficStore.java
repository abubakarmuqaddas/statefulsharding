package statefulsharding.Traffic;

import statefulsharding.graph.Edge;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;

import java.util.HashMap;
import java.util.LinkedList;


public class TrafficStore {

    private LinkedList<TrafficDemand> trafficDemands;
    private HashMap<TrafficDemand,Path> activeTraffic;

    public TrafficStore(){
        trafficDemands = new LinkedList<>();
        activeTraffic = new HashMap<>();
    }

    public void addTrafficDemand(TrafficDemand trafficDemand){
        trafficDemands.add(trafficDemand);
    }

    public void addActiveTraffic(TrafficDemand trafficDemand,Path allocatedPath){
        allocatedPath.getEdges().forEach(edge -> edge.setCarriedTraffic(trafficDemand));

        activeTraffic.put(trafficDemand,allocatedPath);
    }

    public LinkedList<TrafficDemand> getTrafficDemands(){
        return trafficDemands;
    }

    public HashMap<TrafficDemand,Path> getActiveTraffic(){
        return activeTraffic;
    }

    public TrafficDemand getTrafficDemand(Vertex source, Vertex destination){
        for (TrafficDemand trafficDemand : trafficDemands){
            if (trafficDemand.getSource().equals(source) && trafficDemand.getDestination().equals(destination))
                return trafficDemand;
        }
        return null;
    }

    public void removeActiveTraffic(TrafficDemand trafficDemand){
        for(Edge edge : activeTraffic.get(trafficDemand).getEdges()){

        }
    }

    public void clear(){

        for (TrafficDemand trafficDemand : getTrafficDemands()){
            trafficDemand = null;
        }
        trafficDemands.clear();
        activeTraffic.clear();
    }

    public void printTrafficDemands(){
        System.out.println("Traffic Demands: ");
        System.out.println("u v d");
        for (TrafficDemand trafficDemand : trafficDemands){
            System.out.println(trafficDemand.getSource().getLabel() + " " +
                    trafficDemand.getDestination().getLabel() + " " + trafficDemand.getDemand());
        }
    }

    public int getNumTrafficDemands(){
        return trafficDemands.size();
    }



}
