package statefulsharding.graph.algorithms;

import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;

import java.util.*;

/**
 * Implementation from https://sites.google.com/site/indy256/algo/scc_tarjan
 *
 * Tarjan Connected Components
 *
 */

public class DirectedConnectedComponent {

    private ListGraph graph;
    private List<List<Vertex>> components;
    Stack<Vertex> stack;
    int time;
    HashMap<Vertex, Integer> lowLink;


    public DirectedConnectedComponent(ListGraph graph){
        this.graph=graph;

    }

    public void FindConnectedComponents(){

        if(components!=null)
            components.clear();

        if(stack!=null)
            stack.clear();

        graph.getVertices().forEach(vertex -> vertex.unsetVisited());
        time = 0;
        lowLink = new HashMap<>();
        graph.getVertices().forEach(vertex -> {
            lowLink.put(vertex,0);
        });
        components = new ArrayList<>();
        stack = new Stack<>();

        for (Vertex u : graph.getVertices())
            if (!u.getVisited())
                DepthFirstSearch(u);

        graph.getVertices().forEach(vertex -> vertex.unsetVisited());

    }

    private void DepthFirstSearch(Vertex u){

        lowLink.put(u, time++);
        u.setVisited();
        stack.add(u);
        boolean isComponentRoot = true;

        for(Vertex v : graph.getSuccessors(u)){
            if (!v.getVisited())
                DepthFirstSearch(v);

            if (lowLink.get(u) > lowLink.get(v)) {
                lowLink.put(u,lowLink.get(v));
                isComponentRoot = false;
            }
        }

        if (isComponentRoot) {
            List<Vertex> component = new ArrayList<>();
            while (true) {
                Vertex x = stack.pop();
                component.add(x);
                lowLink.put(x,Integer.MAX_VALUE);
                if (x.equals(u))
                    break;
            }
            components.add(component);
        }


    }

    public List<List<Vertex>> getConnectedVertices(){
        return components;
    }

    public int vertexToComponent(Vertex target){
        for (int i=0 ; i<components.size() ; i++)
            for (Vertex vertex: components.get(i)) {
                if(vertex.equals(target))
                    return i;
            }
        return 0;
    }

    public List<Vertex> ComponentToVertex(int componentNumber){
        return components.get(componentNumber);
    }



}
