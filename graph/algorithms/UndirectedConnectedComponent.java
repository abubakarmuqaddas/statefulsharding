package statefulsharding.graph.algorithms;

import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Implementation of Tarjan's algorithm? Maybe, but this is for undirected graph!
 */



public class UndirectedConnectedComponent {

    private ListGraph TargetGraph;
    private HashMap<Integer,HashSet<Vertex>> connectedVertices;
    private int NumberOfCCs;

    public UndirectedConnectedComponent(ListGraph TargetGraph){
        this.TargetGraph=TargetGraph;
    }

    public void FindConnectedComponents(){

        connectedVertices = new HashMap<>();

        TargetGraph.getVertices().forEach(vertex -> vertex.unsetVisited());

        NumberOfCCs=0;

        for (Vertex vertex : TargetGraph.getVertices()){
            if (!vertex.getVisited()){
                NumberOfCCs++;
                vertex.setVisited();
                connectedVertices.put(NumberOfCCs,new HashSet<>());
                connectedVertices.get(NumberOfCCs).add(vertex);
                DepthFirstSearch(vertex);
            }
        }

        TargetGraph.getVertices().forEach(vertex -> vertex.unsetVisited());
    }

    public void DepthFirstSearch(Vertex v){

        TargetGraph.getSuccessors(v).forEach(successor -> {
            if (!successor.getVisited()){
                successor.setVisited();
                connectedVertices.get(NumberOfCCs).add(successor);
                DepthFirstSearch(successor);
            }
        });
    }

    public int RetainLargestComponent(){
        FindConnectedComponents();

        int max=0;
        int index=0;

        for (int i=1 ; i<=NumberOfCCs ; i++){
            if(connectedVertices.get(i).size()>max) {
                max = connectedVertices.get(i).size();
                index=i;
            }
        }

        for (Map.Entry<Integer,HashSet<Vertex>> entry : connectedVertices.entrySet())
        {
            if (index!=entry.getKey()){
                entry.getValue().forEach(vertex -> TargetGraph.removeVertex(vertex));
            }
        }
        return max;
    }

    public int getNumberOfCCs(){
        return NumberOfCCs;
    }

    public HashMap<Integer, HashSet<Vertex>> getConnectedVertices(){
        return connectedVertices;
    }





}
