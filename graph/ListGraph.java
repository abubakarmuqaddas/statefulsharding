package statefulsharding.graph;

import java.util.*;

/**
 *
 * Graph implementation based on adjacency lists per vertex depicting edges
 *
 */

public class ListGraph {

    /**
     * vertexMap: Integer as label for each vertex
     */
    private HashMap<Integer,Vertex> vertexMap;

    /**
     * Adjacency List:
     * Vertex as label and a list of edges against each vertex
     */
    private HashMap<Vertex,LinkedList<Edge>> adjList;

    /**
     * Number of Vertices
     */
    private int numVertices;

    private LinkedList<Integer> vertices;

    /**
     * Constructor
     */
    public ListGraph(){
        adjList = new HashMap<>();
        vertexMap = new HashMap<>();
        vertices = new LinkedList<>();
        numVertices=0;
    }

    /**
     * Adds a Vertex to the graph
     * @param label: label identifies a vertex
     */
    public void addVertex(Integer label){

        vertexMap.put(label,new Vertex(label));
        vertices.add(label);
        numVertices++;
    }

    /**
     * Add a vertex with the coordinates. Coordinates
     * can be used to draw the graph
     *
     * @param label: label identifies a vertex
     * @param xcoord: Abscissa of the vertex
     * @param ycoord: Ordinate of the vertex
     *
     */
    public void addVertex(Integer label, double xcoord, double ycoord){
        vertexMap.put(label,new Vertex(label, xcoord, ycoord));
        vertices.add(label);
        numVertices++;
    }

    /**
     * Get a vertex using its label
     * @param label: label identifies a vertex
     * @return Vertex
     */

    public Vertex getVertex(Integer label){
        return vertexMap.get(label);
    }

    /**
     *
     * @return Gets all vertices in the graph
     */
    public Collection<Vertex> getVertices(){
        return vertexMap.values();
    }

    /**
     * Insert an edge in the graph
     * @param source: Integer label of source vertex
     * @param destination: Integer label of destination vertex
     * @param capacity: Capacity of the edge
     */
    public void insertEdge(Integer source, Integer destination, double capacity){

        if(!checkEdge(source,destination)) {

            Vertex srcvertex = getVertex(source);
            Vertex dstvertex = getVertex(destination);

            insertEdge(srcvertex,dstvertex,capacity);
        }
    }

    public void insertDoubleEdge(Integer vertex1, Integer vertex2, double capacity){
        if(!checkEdge(vertex1,vertex2)){
            insertEdge(getVertex(vertex1),getVertex(vertex2),capacity);
            insertEdge(getVertex(vertex2),getVertex(vertex1),capacity);
        }
    }

    public void insertDoubleEdge(Vertex vertex1, Vertex vertex2, double capacity){
        if(!checkEdge(vertex1,vertex2)){
            insertEdge(vertex1,vertex2,capacity);
            insertEdge(vertex2,vertex1,capacity);
        }
    }

    /**
     * Insert an edge in the graph
     * @param source: Source vertex
     * @param destination: Destination vertex
     * @param capacity: Capacity of the edge
     */
    public void insertEdge(Vertex source, Vertex destination, double capacity){

        if(!checkEdge(source,destination)) {

            LinkedList<Edge> edgeList = adjList.get(source);
            if (edgeList == null) {
                edgeList = new LinkedList<>();
                adjList.put(source, edgeList);
            }
            edgeList.add(new Edge(source, destination, capacity));
        }
    }

    /**
     *
     * @param source: Source Vertex
     * @param destination: Destination Vertex
     * @return Edge specified by a source vertex and destination vertex
     */
    public Edge getEdge(Vertex source, Vertex destination){

        Edge targetEdge=null;

        if (adjList.get(source)!=null){
            for (Edge edge : adjList.get(source)){
                if(edge.getDestination().equals(destination))
                    return edge;
            }
        }

        return targetEdge;

    }

    /**
     * Removes a vertex from the graph
     * First removes all edges, then removes the vertex
     * @param vertex: Vertex to be removed
     */
    public void removeVertex(Vertex vertex){

        HashMap<Vertex,Vertex> RemoveableLinks = new HashMap<>();

        for (Vertex predecessor: getPredecessors(vertex)){
            for (Edge edge: adjList.get(predecessor)){
                if (edge.getDestination().equals(vertex)){
                    RemoveableLinks.put(predecessor,vertex);//removeEdge(predecessor,vertex);
                }
            }
        }

        RemoveableLinks.forEach((src, dst) -> removeEdge(src,dst));

        adjList.remove(vertex);
        vertexMap.remove(vertex.getLabel());
        numVertices--;

    }

    /**
     * Removes an edge between two vertices
     * @param source: Source vertex
     * @param destination: Destination vertex
     */
    public void removeEdge(Vertex source, Vertex destination){

        ArrayList<Edge> RemoveableEdges = new ArrayList<>();

        for (Edge edge: adjList.get(source)){
            if (edge!=null){
                if (edge.getDestination().equals(destination))
                    RemoveableEdges.add(edge);
            }
        }

        for (Edge edge: RemoveableEdges){
            adjList.get(source).remove(edge);
        }
    }

    /**
     * Removes an edge
     * @param edge: Target edge
     */
    public void removeEdge(Edge edge){
        removeEdge(edge.getSource(),edge.getDestination());
    }

    /**
     *
     * @return Returns all edges in the graph
     */
    public LinkedList<Edge> getallEdges(){

        LinkedList<Edge> allEdges = new LinkedList<>();

        for (Vertex vertex : getVertices()){
            if (adjList.get(vertex)!=null){
                allEdges.addAll(adjList.get(vertex));
            }
        }
        return allEdges;
    }

    /**
     * Checks if an edge exists between two vertices
     * @param source: Label that identifies source vertex
     * @param destination: Label that identifies destination vertex
     * @return true/false result based on edge existence
     */
    public boolean checkEdge(Integer source, Integer destination){

        Vertex srcvertex=getVertex(source);
        Vertex dstvertex=getVertex(destination);

        return checkEdge(srcvertex,dstvertex);
    }

    /**
     * Checks if a vertex exists
     * @param vertex: Target vertex
     * @return true/false result based on vertex existence
     */
    public boolean checkVertex(Integer vertex){
        return (getVertex(vertex)!=null);
    }

    /**
     * Checks if an edge exists between two vertices
     * @param source: Source vertex
     * @param destination: Destination vertex
     * @return true/false result based on edge existence
     */
    public boolean checkEdge(Vertex source, Vertex destination){

        if(adjList.get(source)==null)
            return false;
        else{
            for (Edge e: adjList.get(source)){
                if(e.getDestination().equals(destination))
                    return true;
            }
            return false;
        }
    }

    /**
     *
     * @return Get number of vertices in the graph
     */
    public int getNumVertices(){
        return numVertices;
    }

    /**
     *
     * @return Get number of edges in the graph
     */
    public int getNumEdges(){
        int numEdges=0;
        for (Vertex vertex : getVertices()){
            numEdges+=adjList.get(vertex).size();
        }
        return numEdges;
    }

    /**
     * Get the successors of a vertex (i.e. vertex -> successor)
     * @param vertex: Target vertex
     * @return Set of successors
     */
    public Set<Vertex> getSuccessors(Vertex vertex){

        Set<Vertex> successors = new HashSet<>();

        if (adjList.get(vertex)!=null){
            adjList.get(vertex).forEach(edge -> successors.add(edge.getDestination()));
        }

        return successors;
    }

    public LinkedList<Vertex> getSuccessorsList(Vertex vertex){

        LinkedList<Vertex> successors = new LinkedList<>();

        if (adjList.get(vertex)!=null){
            adjList.get(vertex).forEach(edge -> successors.add(edge.getDestination()));
        }

        return successors;
    }

    /**
     * Get the predecessors of a vertex (i.e. predecessor -> vertex)
     * @param vertex: Target vertex
     * @return Set of predecessors
     */
    public Set<Vertex> getPredecessors(Vertex vertex){

        Set<Vertex> predecessors = new HashSet<>();

        for (Vertex predecessor : getVertices())
        {
              if(adjList.get(predecessor)!=null){
                adjList.get(predecessor)
                        .forEach(edge -> {
                            if (edge.getDestination().equals(vertex))
                                predecessors.add(edge.getSource());
                        });
            }
        }
        return predecessors;
    }

    /**
     *
     * @return Get Average node degree of the graph
     */
    public double getAvgDeg(){

        double totalDegree=0;

        for (Vertex vertex : getVertices()){
            if (adjList.get(vertex)!=null){
                totalDegree += adjList.get(vertex).size();
                if (checkEdge(vertex,vertex))
                    totalDegree--;
            }

        }
        return totalDegree/numVertices;
    }

    /**
     * @return Get integer labels of all vertices
     */
    public LinkedList<Integer> getVerticesInt(){
        return vertices;
    }

    /**
     * Clear the graph (remove all edges and vertices)
     */
    public void clear(){

        for (Vertex vertex : getVertices()){
            if (adjList.get(vertex)!=null) {
                for (Edge edge : adjList.get(vertex)) {
                    edge = null;
                }
            }
        }
        adjList.clear();

        for (Integer integer : vertexMap.keySet()){
            vertexMap.put(integer,null);
        }
        vertexMap.clear();
    }

}
