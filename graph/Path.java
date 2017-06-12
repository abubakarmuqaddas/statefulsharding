package statefulsharding.graph;

import java.util.LinkedList;

public class Path {

    private LinkedList<Edge> edges;

    public Path(){
        edges = new LinkedList<>();
    }

    public void add(Edge edge){
        edges.add(edge);
    }

    public void addFirst(Edge edge){
        edges.addFirst(edge);
    }

    public void remove(Edge edge){
        edges.remove(edge);
    }

    public LinkedList<Edge> getEdges()
    {
        return edges;
    }

    public boolean containsVertex(Vertex vertex){

        for (Edge edge: getEdges()){
            if (edge.getSource().equals(vertex) || edge.getDestination().equals(vertex)){
                return true;
            }
        }

        return false;
    }

    public int getSize(){
        return edges.size();
    }

    public Vertex getSource(){
        return edges.getFirst().getSource();
    }

    public Vertex getDestination(){
        return edges.getLast().getDestination();
    }

    public static Path merge(Path path1, Path path2){
        Path newPath = new Path();
        path1.getEdges().forEach(edge -> newPath.add(edge));
        path2.getEdges().forEach(edge -> newPath.add(edge));
        return newPath;
    }

}
