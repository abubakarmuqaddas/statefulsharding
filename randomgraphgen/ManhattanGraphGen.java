package statefulsharding.randomgraphgen;

import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ManhattanGraphGen{

    private int dimension;
    private int numVertices;
    private ListGraph ManhattanGraph;
    private double capacity;
    private boolean selfEdges;

    public enum mType{
        WRAPPED, UNWRAPPED
    }

    private mType type;

    public ManhattanGraphGen(int dimension, double capacity, mType type, boolean saveFiles, boolean selfEdges){

        this.dimension = dimension;
        this.numVertices = dimension*dimension;
        this.capacity=capacity;
        this.type=type;
        this.selfEdges=selfEdges;
        ManhattanGraph = new ListGraph();

        generateGraph();

        if(saveFiles) {
            try {
                flushGraph();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

    public ListGraph getManhattanGraph(){
        return ManhattanGraph;
    }

    private void generateGraph(){
        generateVertices();
        generateEdges();
    }

    private void generateVertices(){
        int counter=0;
        double xCoord=5;
        double yCoord=5;
        for (int i=0 ; i<dimension ; i++){
            for (int j=0 ; j<dimension; j++){
                ManhattanGraph.addVertex(counter++,xCoord,yCoord);
                xCoord+=10;
            }
            xCoord=5;
            yCoord+=10;
        }
    }

    private void generateEdges(){
        //Adding edges to Rows
        for (int i=0 ; i<numVertices ; i=i+dimension){
            for (int j=i ; j<i+dimension-1; j++){
                if(type==mType.WRAPPED && j==i){
                    ManhattanGraph.insertDoubleEdge(ManhattanGraph.getVertex(j),
                            ManhattanGraph.getVertex(i+dimension-1),capacity);
                }
                ManhattanGraph.insertDoubleEdge(ManhattanGraph.getVertex(j),
                        ManhattanGraph.getVertex(j+1),capacity);
            }
        }

        //Adding edges to Columns
        int x;
        for (int i=0 ; i<dimension ; i++){
            x=i;
            for (int j=0; j<dimension-1; j++){
                if(type==mType.WRAPPED && j==0){
                    ManhattanGraph.insertDoubleEdge(ManhattanGraph.getVertex(i),
                            ManhattanGraph.getVertex(i+dimension*(dimension-1)),capacity);
                }
                ManhattanGraph.insertDoubleEdge(ManhattanGraph.getVertex(x),
                        ManhattanGraph.getVertex(x+dimension),capacity);
                x+=dimension;
            }
        }

        if(selfEdges){
            for(Vertex vertex : ManhattanGraph.getVertices()){
                ManhattanGraph.insertEdge(vertex,vertex,capacity);
            }
        }
    }

    private void flushGraph() throws FileNotFoundException {
        try {
            PrintWriter pw = new PrintWriter(new File("Manhattan_"+type+"_Dimension_"+
                    dimension+"_vertices.csv"));
            PrintWriter pw2 = new PrintWriter(new File("Manhattan_"+type+"_Dimension_"+
                    dimension+"_edges.csv"));
            StringBuilder sb = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();

            for (Vertex vertex: ManhattanGraph.getVertices()) {
                sb.append(vertex.getXcoord());
                sb.append(',');
                sb.append(vertex.getYcoord());
                sb.append('\n');
            }

            for (Edge e : ManhattanGraph.getallEdges()) {
                sb2.append(e.getSource().getXcoord()); sb2.append(',');
                sb2.append(e.getSource().getYcoord());
                sb2.append('\n');
                sb2.append(e.getDestination().getXcoord()); sb2.append(',');
                sb2.append(e.getDestination().getYcoord());
                sb2.append('\n'); sb2.append('\n');
            }

            pw.write(sb.toString());
            pw2.write(sb2.toString());
            pw.close();
            pw2.close();

        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

}
