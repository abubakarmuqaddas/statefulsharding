package statefulsharding.Traffic;

import statefulsharding.graph.Vertex;

/**
 * Created by build on 09/11/16.
 */
public class TrafficDemand {

    private Vertex source;
    private Vertex destination;
    private double demand;

    public TrafficDemand(Vertex source, Vertex destination, double demand){
        this.source=source;
        this.destination=destination;
        this.demand=demand;
    }

    public double getDemand(){
        return demand;
    }

    public Vertex getSource(){
        return source;
    }

    public Vertex getDestination(){
        return destination;
    }


}
