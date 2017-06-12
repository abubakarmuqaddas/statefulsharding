/**
 * Created by build on 31/10/16.
 */

package statefulsharding.randomgraphgen;

import statefulsharding.graph.Edge;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.algorithms.UndirectedConnectedComponent;

import java.util.Random;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;


public class RandomGraphGenerator {

    private int numVertices;
    private ListGraph RandomGraph;
    private UndirectedConnectedComponent checkCC;
    private Random RandomNumberGen;
    private double capacity;
    private int run;
    private double probability;
    private Type type;

    public enum Type{
        GEOMETRIC, WAXMAR, ERDOS, GABRIEL, RNEIGHBOUR
    }

    public RandomGraphGenerator(int numVertices, int capacity, int run, Type type, boolean saveFiles){

        this.numVertices = numVertices;
        this.capacity=capacity;
        this.run=run;
        this.type=type;
        RandomNumberGen = new Random();
        RandomGraph = new ListGraph();
        checkCC = new UndirectedConnectedComponent(RandomGraph);

        generateVertices();
        generateEdges();

        if(saveFiles)
            generateGraphFiles();
    }

    public RandomGraphGenerator(int numVertices, int capacity, int run,
                                Type type, double probability, boolean saveFiles){

        this.numVertices = numVertices;
        this.capacity=capacity;
        this.run=run;
        this.type=type;
        this.probability=probability;
        RandomNumberGen = new Random();
        RandomGraph = new ListGraph();
        checkCC = new UndirectedConnectedComponent(RandomGraph);

        generateVertices();
        generateEdges();

        if(saveFiles)
            generateGraphFiles();
    }

    private void generateVertices(){
        for (int i=0 ; i<numVertices ; i++){
            double xcoord = (double)RandomNumberGen.nextInt(100)/100.0;
            double ycoord = (double)RandomNumberGen.nextInt(100)/100.0;
            RandomGraph.addVertex(i,xcoord,ycoord);
        }
    }

    private void generateEdges(){

        double EucDistance;

        for (Vertex src: RandomGraph.getVertices())
        {
            for (Vertex dst: RandomGraph.getVertices())
            {
                EucDistance=getEuclideanDistance(src,dst);
                if (!(dst.equals(src) || RandomGraph.checkEdge(src,dst) || RandomGraph.checkEdge(dst,src))){
                    if (type==Type.GEOMETRIC){
                        if (getGeometric(EucDistance)){
                            RandomGraph.insertEdge(src, dst, capacity);
                            RandomGraph.insertEdge(dst, src, capacity);
                        }
                    }
                    if(type==Type.WAXMAR){
                        if (getWaxmar(EucDistance)){
                            RandomGraph.insertEdge(src, dst, capacity);
                            RandomGraph.insertEdge(dst, src, capacity);
                        }
                    }
                    if(type==Type.ERDOS){
                        if (getErdos()) {
                            RandomGraph.insertEdge(src, dst, capacity);
                            RandomGraph.insertEdge(dst, src, capacity);
                        }
                    }
                    if(type==Type.GABRIEL){
                        if (!getGabriel(src,dst)) {
                            RandomGraph.insertEdge(src, dst, capacity);
                            RandomGraph.insertEdge(dst, src, capacity);
                        }
                    }
                    if(type==Type.RNEIGHBOUR){
                        if (!getRNeighbour(src,dst)) {
                            RandomGraph.insertEdge(src, dst, capacity);
                            RandomGraph.insertEdge(dst, src, capacity);
                        }
                    }
                }
            }
        }
    }

    private boolean getGeometric(double EucDistance){
        double radius = Math.sqrt((1/Math.PI)*(1-1/(Math.pow(2*(double)numVertices,(1/((double)numVertices-1))))));
        return (EucDistance<1.1*radius);
    }

    private boolean getWaxmar(double EucDistance){
        /**
         * TODO: Change WAXMAR probability criterion
         */
        double p=0.5*Math.exp(-EucDistance/0.5);
        return (p>0.38);
    }

    private boolean getErdos(){
        return RandomNumberGen.nextDouble() < probability;
    }

    private boolean getGabriel(Vertex source, Vertex destination){
        double EucDistance=getEuclideanDistance(source,destination);
        for (Vertex vertex : RandomGraph.getVertices()){
            if (vertex.equals(source) || vertex.equals(destination))
                continue;
            else{
                if(Math.pow(EucDistance,2) >
                        Math.pow(getEuclideanDistance(source,vertex),2)
                                + Math.pow(getEuclideanDistance(destination,vertex),2))
                    return true;
            }
        }
        return false;
    }

    private boolean getRNeighbour(Vertex source, Vertex destination){
        double EucDistance=getEuclideanDistance(source,destination);
        for (Vertex vertex : RandomGraph.getVertices()){
            if (vertex.equals(source) || vertex.equals(destination))
                continue;
            else{
                if(EucDistance > Math.max(getEuclideanDistance(source,vertex),getEuclideanDistance(destination,vertex)))
                    return true;
            }
        }
        return false;
    }

    private double getEuclideanDistance(Vertex a, Vertex b){
        return Math.sqrt(Math.pow(a.getXcoord() - b.getXcoord(), 2) + Math.pow(a.getYcoord() - b.getYcoord(), 2));
    }

    /**
     * TODO: Add in a method to get random k-neighbours graph
     */

    public ListGraph getRandomGraph(){
        return RandomGraph;
    }

    private void generateGraphFiles(){


        String filename="plot_" + numVertices + "_" + type + "_" + run + ".dat";

        double avgdegree_before = ((int)(RandomGraph.getAvgDeg()*100))/100.0;

        String typFile="plot_" + this.numVertices + "_" + type + "_" + run + "_before";
        String[] filename_before =new String[]{typFile + "_vertices.csv",typFile + "_edges.csv",typFile + ".dat"};
        typFile="plot_" + this.numVertices + "_" + type + "_" + run + "_after";
        String[] filename_after =new String[]{typFile + "_vertices.csv",typFile + "_edges.csv",typFile + ".dat"};


        /**
         * Publish file before retaining the largest connected component
         */

        try{
            flushGraph(filename_before);
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        /**
         * Publish the file after retaining the largest connected component
         */

        int max=checkCC.RetainLargestComponent();
        double avgdegree_after = ((int)(RandomGraph.getAvgDeg()*100))/100.0;

        try{
            flushGraph(filename_after);
            flushInfo(filename,new double[]{avgdegree_before,avgdegree_after}, new int[]{this.numVertices,max});
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

    }

    private void flushGraph(String[] filename) throws FileNotFoundException{
        try {

            PrintWriter pw = new PrintWriter(new File(filename[0]));
            PrintWriter pw2 = new PrintWriter(new File(filename[1]));
            PrintWriter pw3 = new PrintWriter(new File(filename[2]));
            StringBuilder sb = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            StringBuilder sb3 = new StringBuilder();

            for (Vertex vertex: RandomGraph.getVertices()) {
                sb.append(vertex.getXcoord());
                sb.append(',');
                sb.append(vertex.getYcoord());
                sb.append('\n');
            }

            for (Edge e : RandomGraph.getallEdges()) {
                sb2.append(e.getSource().getXcoord()); sb2.append(',');sb2.append(e.getSource().getYcoord());
                sb2.append('\n');
                sb2.append(e.getDestination().getXcoord()); sb2.append(',');sb2.append(e.getDestination().getYcoord());
                sb2.append('\n'); sb2.append('\n');
                sb3.append(e.getSource().getLabel()); sb3.append(" "); sb3.append(e.getDestination().getLabel());
                sb3.append(" "); sb3.append(e.getCapacity()); sb3.append('\n');
                }

            pw.write(sb.toString());
            pw2.write(sb2.toString());
            pw3.write(sb3.toString());
            pw.close();
            pw2.close();
            pw3.close();
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private void flushInfo(String filename, double[] degree, int[] nverts) throws FileNotFoundException{
        try {

            PrintWriter pw = new PrintWriter(new File(filename));
            StringBuilder sb = new StringBuilder();

            sb.append(degree[0]); sb.append(' ');
            sb.append(degree[1]); sb.append('\n');
            sb.append(nverts[0]); sb.append(' ');
            sb.append(nverts[1]); sb.append('\n');

            pw.write(sb.toString());
            pw.close();

        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

}
