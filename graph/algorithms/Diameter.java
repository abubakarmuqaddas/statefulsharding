package statefulsharding.graph.algorithms;

import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;

/**
 * Created by root on 1/24/17.
 */
public class Diameter {

    public static int getDiameter(ListGraph graph){
        int max=Integer.MIN_VALUE;
        for (Vertex source : graph.getVertices()){
            for (Vertex destination : graph.getVertices()){
                if (!source.equals(destination)){
                    Path sp = ShortestPath.dijsktra(graph,source,destination);
                    if(sp.getSize()>max) {
                        max = sp.getSize();
                    }
                }
            }
        }
        return max;
    }

}
