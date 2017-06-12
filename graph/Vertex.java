package statefulsharding.graph;

/**
 * Created by build on 25/10/16.
 */

public class Vertex {

    private final Integer label;
    private double xcoord;
    private double ycoord;
    private boolean visited;

    //Constructor
    public Vertex (int label){
        this.label=label;
    }

    public Vertex (int label, double xcoord, double ycoord){
        this.label=label;
        this.xcoord=xcoord;
        this.ycoord=ycoord;
    }

    public void setXcoord(double xcoord){
        this.xcoord=xcoord;
    }

    public void setYcoord(double ycoord){
        this.ycoord=ycoord;
    }

    public void setVisited(){
        visited=true;
    }

    public void unsetVisited(){
        visited=false;
    }

    public double getXcoord(){
        return xcoord;
    }

    public double getYcoord(){
        return ycoord;
    }

    public Integer getLabel(){
        return label;
    }

    public boolean getVisited(){
        return visited;
    }

    public boolean equals(Object other){
        boolean result=false;

        if (other instanceof Vertex){
            Vertex that = (Vertex) other;
            result = (this.getLabel()==that.getLabel());
        }
        return result;
    }
}