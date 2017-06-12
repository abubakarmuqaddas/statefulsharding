package statefulsharding.graph.algorithms;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.Path;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by build on 30/11/16.
 */
public class ShortestPath {

    public static Path dijsktra(ListGraph graph, Vertex source, Vertex destination){

        if (source.equals(destination)){
            Path sp = new Path();
            sp.add(graph.getEdge(source,destination));
            return sp;
        }

        HashMap<Vertex,Long> distance = new HashMap<>();
        HashMap<Vertex,Vertex> previous = new HashMap<>();

        LinkedList<Vertex> Q = new LinkedList<>(graph.getVertices());

        Q.forEach(vertex -> {
            distance.put(vertex,Long.MAX_VALUE);
            previous.put(vertex,null);
        });
        distance.put(source,new Long(0));

        while (!Q.isEmpty()){

            Vertex u = getMinDist(Q,distance);

            Q.remove(u);

            if(u.equals(destination))
            {
                return getPath(graph,previous,source,destination);
            }

            for (Vertex v: graph.getSuccessors(u)){
                Long alt = distance.get(u)+1;
                if(alt<distance.get(v)){
                    distance.put(v,alt);
                    previous.put(v,u);
                }
            }
        }
        return null;
    }

    public static Path dijsktraCapacityConstrained(ListGraph graph, Vertex source, Vertex destination,
                                                   double demand){

        HashMap<Vertex,Long> distance = new HashMap<>();
        HashMap<Vertex,Vertex> previous = new HashMap<>();

        LinkedList<Vertex> Q = new LinkedList<>(graph.getVertices());

        Q.forEach(vertex -> {
            distance.put(vertex,Long.MAX_VALUE-2);
            previous.put(vertex,null);
        });
        distance.put(source,new Long(0));

        while (!Q.isEmpty()){

            Vertex u = getMinDist(Q,distance);

            Q.remove(u);

            if(u.equals(destination)){
                if (previous.get(u)!=null)
                    return getPath(graph,previous,source,destination);
                else
                    break;
            }

            for (Vertex v: graph.getSuccessors(u)){
                Long alt = distance.get(u)+1;
                if(alt<distance.get(v) && demand<=graph.getEdge(u,v).getResidualCapacity()){
                    distance.put(v,alt);
                    previous.put(v,u);
                }
            }
        }
        return null;
    }

    public static Path dijsktraNodeConstrained(ListGraph graph, Vertex source, Vertex destination, Vertex target){

        HashMap<Vertex,Long> distance = new HashMap<>();
        HashMap<Vertex,Vertex> previous = new HashMap<>();

        LinkedList<Vertex> Q = new LinkedList<>(graph.getVertices());

        Q.forEach(vertex -> {
            distance.put(vertex,Long.MAX_VALUE-2);
            previous.put(vertex,null);
        });
        distance.put(source,new Long(0));

        while (!Q.isEmpty()){

            Vertex u = getMinDist(Q,distance);

            Q.remove(u);

            if(u.equals(destination)){
                if (previous.get(u)!=null)
                    return getPath(graph,previous,source,destination);
                else
                    break;
            }

            for (Vertex v: graph.getSuccessors(u)){
                Long alt = distance.get(u)+1;
                if(alt<distance.get(v) && !v.equals(target)){
                    distance.put(v,alt);
                    previous.put(v,u);
                }
            }
        }
        return null;
    }

    public static Path dijsktraNodeAndCapacityConstrained(ListGraph graph, Vertex source, Vertex destination,
                                                          Vertex target, double demand){

        HashMap<Vertex,Long> distance = new HashMap<>();
        HashMap<Vertex,Vertex> previous = new HashMap<>();

        LinkedList<Vertex> Q = new LinkedList<>(graph.getVertices());

        Q.forEach(vertex -> {
            distance.put(vertex,Long.MAX_VALUE-2);
            previous.put(vertex,null);
        });
        distance.put(source,new Long(0));

        //if(!source.equals(target) && !destination.equals(target)) {
            while (!Q.isEmpty()) {

                Vertex u = getMinDist(Q, distance);

                Q.remove(u);

                if (u.equals(destination)) {
                    if (previous.get(u) != null)
                        return getPath(graph, previous, source, destination);
                    else
                        break;
                }

                for (Vertex v : graph.getSuccessors(u)) {
                    Long alt = distance.get(u) + 1;
                    if (alt < distance.get(v) && !v.equals(target) && demand <= graph.getEdge(u, v).getResidualCapacity()) {
                        distance.put(v, alt);
                        previous.put(v, u);
                    }
                }
            }
        //}
        return null;
    }

    public static Path dijsktraNodesConstrained(ListGraph graph, Vertex source, Vertex destination,
                                                LinkedList<Vertex> Nodes){

        HashMap<Vertex,Long> distance = new HashMap<>();
        HashMap<Vertex,Vertex> previous = new HashMap<>();

        LinkedList<Vertex> Q = new LinkedList<>(graph.getVertices());

        Q.forEach(vertex -> {
            distance.put(vertex,Long.MAX_VALUE-2);
            previous.put(vertex,null);
        });
        distance.put(source,new Long(0));

        while (!Q.isEmpty()){

            Vertex u = getMinDist(Q,distance);

            Q.remove(u);

            if(u.equals(destination)){
                if (previous.get(u)!=null)
                    return getPath(graph,previous,source,destination);
                else
                    break;
            }

            for (Vertex v: graph.getSuccessors(u)){
                Long alt = distance.get(u)+1;
                if(alt<distance.get(v) && !Nodes.contains(v)){
                    distance.put(v,alt);
                    previous.put(v,u);
                }
            }
        }
        return null;
    }


    private static Vertex getMinDist(LinkedList<Vertex> Q, HashMap<Vertex,Long> distance){

        return Q.stream()
                .max((entry1,entry2) -> distance.get(entry1) < distance.get(entry2) ? 1 : -1)
                .get();
    }

    private static Path getPath(ListGraph graph, HashMap<Vertex,Vertex> previous,
                                Vertex source, Vertex destination){

        Path sourceToDestination = new Path();

        Vertex p = previous.get(destination);
        sourceToDestination.add(graph.getEdge(p,destination));

        while(!p.equals(source)){
            sourceToDestination.addFirst(graph.getEdge(previous.get(p),p));
            p = previous.get(p);
        }
        return sourceToDestination;
    }

    public static List<Path> AllShortestPaths(ListGraph graph, Vertex source, Vertex destination){
        HashMap<Vertex,List<Vertex>> parents = new HashMap<>();
        HashMap<Vertex,Integer> level = new HashMap<>();
        LinkedHashSet<Vertex> fifoqueue = new LinkedHashSet<>();
        Set<Vertex> visited = new HashSet<>();

        graph.getVertices().forEach(vertex -> {
            level.put(vertex,-1);
            parents.put(vertex, new ArrayList<>());
        });

        fifoqueue.add(source);
        level.put(source,0);

        Vertex targetNode = null;

        while(fifoqueue.size()>0){
            Vertex next = fifoqueue.iterator().next();

            if (next.equals(destination)) {
                /*
                 Found the destination node, done
                 */
                targetNode = next;
                break;
            }

            for (Vertex n : graph.getSuccessors(next)) {
                if (!visited.contains(n)) {
                    fifoqueue.add(n);
                    if (!level.get(n).equals(level.get(next))){
                        parents.get(n).add(next);
                        level.put(n,level.get(next)+1);
                    }
                }
            }

            fifoqueue.remove(next);
            visited.add(next);
        }

        if (targetNode == null) {
            return Collections.emptyList();
        }

        List<Path> result = new ArrayList<>();
        dfsAllShortestPaths(graph, targetNode, parents, result, new LinkedList<>());

        return result;
    }

    private static void dfsAllShortestPaths(ListGraph graph,
                                            Vertex targetNode,
                                            HashMap<Vertex,List<Vertex>> parents,
                                            List<Path> result,
                                            LinkedList<Vertex> path){
        path.addFirst(targetNode);

        if (parents.get(targetNode).size() == 0) {

            Path tempPath = new Path();

            int i=0;
            while(i<path.size()-1){
                Edge edge = graph.getEdge(path.get(i),path.get(i+1));
                tempPath.add(edge);
                i++;
            }
            result.add(tempPath);
        }

        for (Vertex p : parents.get(targetNode)) {
            dfsAllShortestPaths(graph, p, parents, result, path);
        }

        path.removeFirst();

    }

    public static HashMap<Vertex, HashMap<Vertex, Integer>> FloydWarshall(ListGraph graph,
                                                                          boolean write,
                                                                          String filename){

        HashMap<Vertex, HashMap<Vertex, Double>> distTemp = new HashMap<>();
        HashMap<Vertex, HashMap<Vertex, Integer>> dist = new HashMap<>();

        for (Vertex vertex1 : graph.getVertices()) {
            distTemp.put(vertex1, new HashMap<>());
            dist.put(vertex1, new HashMap<>());
            Set<Vertex> successors = graph.getSuccessors(vertex1);
            for(Vertex vertex2 : graph.getVertices()){
                if(vertex1.equals(vertex2))
                    distTemp.get(vertex1).put(vertex2, 0.0);
                else if (successors.contains(vertex2))
                    distTemp.get(vertex1).put(vertex2, 1.0);
                else
                    distTemp.get(vertex1).put(vertex2, Double.POSITIVE_INFINITY);
            }
        }

        for (Vertex k : graph.getVertices() ){
            for (Vertex i : graph.getVertices() ){
                for (Vertex j : graph.getVertices() ){
                    if(distTemp.get(i).get(j) > distTemp.get(i).get(k) + distTemp.get(k).get(j))
                        distTemp.get(i).put(j, distTemp.get(i).get(k) + distTemp.get(k).get(j));
                }
            }
        }

        distTemp.keySet().forEach(vertex1 -> {
            distTemp.get(vertex1).keySet().forEach(vertex2 ->{
                dist.get(vertex1).put(vertex2,distTemp.get(vertex1).get(vertex2).intValue());
            });
        });

        if(write){
            try{
                FileWriter fw = new FileWriter(filename, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw);

                for(Vertex vertex1 : dist.keySet()){
                    for(Vertex vertex2 : dist.get(vertex1).keySet()){
                        out.println(vertex1.getLabel() + " " +
                                    vertex2.getLabel() + " " +
                                    dist.get(vertex1).get(vertex2));
                    }
                }
            }
            catch(IOException e){
                System.out.println("Cannot write Traffic");
            }
        }

        return dist;

    }


    public static boolean checkPathCapacity(Path path, TrafficDemand trafficDemand){

        return !(path.getEdges()
                .stream()
                .anyMatch(edge -> trafficDemand.getDemand()>edge.getResidualCapacity()));

    }

}