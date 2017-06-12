package statefulsharding.graph.algorithms;

import statefulsharding.MapUtils;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;

import java.util.*;

public class Centrality {

    public static LinkedHashMap<Vertex, Double> BetweennessTest(ListGraph graph){
        HashMap<Vertex,Double> betCen = new HashMap<>();
        double numOccurence;

        graph.getVertices().forEach(vertex -> {
            betCen.put(vertex,0.0);
        });

        for (Vertex source : graph.getVertices()){
            for (Vertex destination: graph.getVertices()){

                if (source.equals(destination))
                    continue;

                List<Path> paths = ShortestPath.AllShortestPaths(graph,source,destination);
                Set<Vertex> uniqueVertices = new HashSet<>();

                paths.forEach(path -> {
                    path.getEdges().forEach(edge -> {
                        if(!edge.getSource().equals(source)){
                            uniqueVertices.add(edge.getSource());
                        }

                        if(!edge.getDestination().equals(destination)){
                            uniqueVertices.add(edge.getDestination());
                        }
                    });
                });

                for (Vertex vertex : uniqueVertices){
                    numOccurence=0.0;
                    for (Path path : paths){
                        if(path.containsVertex(vertex)){
                            numOccurence++;
                        }
                    }
                    double temp = betCen.get(vertex);
                    temp += numOccurence/paths.size();
                    betCen.put(vertex,temp);
                }
            }
        }

        for (Vertex vertex : betCen.keySet()){
            double temp = betCen.get(vertex);
            betCen.put(vertex,Math.round(temp/(graph.getNumVertices()-1)/(graph.getNumVertices()-2)*10000)/10000.0);
        }

        LinkedHashMap<Vertex,Double> sortedbetCen =  MapUtils.sortMapByValue(betCen);

        return sortedbetCen;
    }

    public static LinkedHashMap<Vertex, Double> BetweennessTemp(ListGraph graph, TrafficStore trafficStore){
        HashMap<Vertex,Double> betCenTfc = new HashMap<>();

        graph.getVertices().forEach(vertex -> {
            betCenTfc.put(vertex,0.0);
        });

        HashMap<TrafficDemand,Path> sp = new HashMap<>();

        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            sp.put(trafficDemand, ShortestPath.dijsktra(graph,
                    trafficDemand.getSource(),trafficDemand.getDestination()));
        }

        for (TrafficDemand trafficDemand : sp.keySet()){
            for (Edge edge : sp.get(trafficDemand).getEdges()){
                double temp = betCenTfc.get(edge.getDestination());
                temp++;
                betCenTfc.put(edge.getDestination(),temp);
            }
        }

        LinkedHashMap<Vertex,Double> sortedbetCenTfc=  MapUtils.sortMapByValue(betCenTfc);

        return sortedbetCenTfc;

    }

    public static LinkedHashMap<Vertex, Double> BetweennessTfc(ListGraph graph, TrafficStore trafficStore){

        HashMap<Vertex,Double> betCen = new HashMap<>();
        double numOccurrence;

        graph.getVertices().forEach(vertex -> betCen.put(vertex,0.0));

        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){

            Vertex source = graph.getVertex(trafficDemand.getSource().getLabel());
            Vertex destination = graph.getVertex(trafficDemand.getDestination().getLabel());

            if(source==null || destination==null)
                continue;

            if (source.equals(destination))
                continue;

            List<Path> paths = ShortestPath.AllShortestPaths(graph,source,destination);
            Set<Vertex> uniqueVertices = new HashSet<>();

            paths.forEach(path ->
                path.getEdges().forEach(edge -> {
                    //if(!edge.getSource().equals(source)) {
                        uniqueVertices.add(edge.getSource());
                    //}
                    //if(!edge.getDestination().equals(destination)) {
                        uniqueVertices.add(edge.getDestination());
                    //}
                }));

            for (Vertex vertex : uniqueVertices){
                numOccurrence=0.0;
                for (Path path : paths){
                    if(path.containsVertex(vertex)){
                        numOccurrence++;
                    }
                }
                double temp = betCen.get(vertex);
                temp += numOccurrence/paths.size();
                betCen.put(vertex,trafficDemand.getDemand()*temp); //Scaled with the demand!!!
            }

        }


        /*
            for (Vertex vertex : betCen.keySet()){
                double temp = betCen.get(vertex);
                betCen.put(vertex,Math.round(temp/(graph.getNumVertices()-1)/(graph.getNumVertices()-2)*100)/100.0);
            }
        */



        LinkedHashMap<Vertex,Double> sortedbetCen =  MapUtils.sortMapByValue(betCen);

        return sortedbetCen;
    }

    public static LinkedHashMap<Vertex, Double> Flow(ListGraph graph){
        HashMap<Vertex,Double> flowCen = new HashMap<>();
        HashMap<Vertex,HashMap<Vertex,Integer>> hops = new HashMap<>();

        int hopSum=0;

        for(Vertex source: graph.getVertices()){
            hops.putIfAbsent(source,new HashMap<>());
            for (Vertex destination: graph.getVertices()){
                if(!source.equals(destination)){
                    Path sp = ShortestPath.dijsktra(graph,source,destination);
                    hops.get(source).put(destination,sp.getSize());
                    hopSum+=sp.getSize();
                }
            }
        }

        for (Vertex target : graph.getVertices()){
            int numSum=0;
            for (Vertex source : graph.getVertices()){
                for (Vertex destination: graph.getVertices()){
                    if(!source.equals(destination) && !source.equals(target) && !destination.equals(target)) {
                        Path spNodeConstrained = ShortestPath.dijsktraNodeConstrained(graph, source,
                                                                                destination, target);
                        numSum += Math.abs(spNodeConstrained.getSize() - hops.get(source).get(destination));
                    }
                }
            }
            flowCen.put(target,Math.round((numSum*1.0/hopSum)*1000.0)/1000.0);
        }

        LinkedHashMap<Vertex,Double> sortedflowCen=  MapUtils.sortMapByValue(flowCen);

        return sortedflowCen;
    }

    public static LinkedHashMap<Vertex, Double> FlowTfc(ListGraph graph, TrafficStore trafficStore){

        HashMap<Vertex,Double> flowCen = new HashMap<>();
        HashMap<Vertex,HashMap<Vertex,Integer>> hops = new HashMap<>();

        double hopSum=0;

        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            Vertex source = trafficDemand.getSource();
            Vertex destination = trafficDemand.getDestination();
            hops.putIfAbsent(source,new HashMap<>());
            if(!source.equals(destination)){
                Path sp = ShortestPath.dijsktra(graph,source,destination);
                hops.get(source).put(destination,sp.getSize());
                hopSum+=sp.getSize()*1.0;
            }
        }

        for (Vertex target : graph.getVertices()){
            double numSum=0;
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){

                Vertex source = trafficDemand.getSource();
                Vertex destination = trafficDemand.getDestination();

                if(!source.equals(destination) && !source.equals(target) && !destination.equals(target)) {
                    Path spNodeConstrained = ShortestPath.dijsktraNodeConstrained(graph, source,
                            destination, target);

                    numSum += Math.abs(spNodeConstrained.getSize() - hops.get(source).get(destination));
                }
            }
            flowCen.put(target,numSum/hopSum);
        }

        LinkedHashMap<Vertex,Double> sortedflowCen=  MapUtils.sortMapByValue(flowCen);
        return sortedflowCen;
    }

    public static LinkedHashMap<Vertex, Double> FlowTfcAlternate(ListGraph graph, TrafficStore trafficStore){

        HashMap<Vertex,Double> flowCen = new HashMap<>();
        HashMap<Edge,Double> beforeEdge= new HashMap<>();
        HashMap<Vertex,HashMap<Edge,Double>> afterEdge= new HashMap<>();

        graph.getallEdges().forEach(edge -> beforeEdge.put(edge,0.0));
        graph.getVertices().forEach(vertex -> {
            afterEdge.put(vertex,new HashMap<>());
            graph.getallEdges().forEach(edge -> {
                afterEdge.get(vertex).put(edge,0.0);
            });
        });

        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            Vertex source = trafficDemand.getSource();
            Vertex destination = trafficDemand.getDestination();
            Path spBefore = ShortestPath.dijsktra(graph,source,destination);
            for (Edge edge : spBefore.getEdges()) {
                if(!edge.getSource().equals(edge.getDestination())) {
                    double temp = beforeEdge.get(edge);
                    temp++;
                    beforeEdge.put(edge, temp);
                }
            }
        }

        for (Vertex target : graph.getVertices()){
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                Vertex source = trafficDemand.getSource();
                Vertex destination = trafficDemand.getDestination();

                if(!source.equals(destination) && !source.equals(target) && !destination.equals(target)) {
                    Path spAfter = ShortestPath.dijsktraNodeConstrained(graph,source,destination,target);

                    for (Edge edge : spAfter.getEdges()) {
                        if(!edge.getSource().equals(edge.getDestination()) &&
                                (!edge.getSource().equals(target) || !edge.getDestination().equals(target))) {
                            double temp = afterEdge.get(target).get(edge);
                            temp++;
                            afterEdge.get(target).put(edge, temp);
                        }
                    }
                }
            }
        }

        int E = graph.getNumEdges();

        for (Vertex target: graph.getVertices()){

            double edgeSum = 0.0;

            for(Edge edge : afterEdge.get(target).keySet()){

                if (beforeEdge.get(edge)>0 &&
                        (!edge.getSource().equals(target) || !edge.getDestination().equals(target))){
                    edgeSum += Math.abs((afterEdge.get(target).get(edge) - beforeEdge.get(edge)*1.0))/beforeEdge.get(edge);
                }
            }

            flowCen.put(target,Math.round((1.0/E)*edgeSum*1000)/1000.0);

        }

        LinkedHashMap<Vertex,Double> sortedflowCen=  MapUtils.sortMapByValue(flowCen);
        return sortedflowCen;
    }

    public static LinkedHashMap<Vertex, Double> ResidualFlow(ListGraph graph,TrafficStore trafficStore){

        HashMap<Vertex,Double> flowCen = new HashMap<>();
        HashMap<Edge,Double> beforeEdge= new HashMap<>();
        HashMap<Vertex,HashMap<Edge,Double>> afterEdge= new HashMap<>();

        graph.getallEdges().forEach(edge -> beforeEdge.put(edge,0.0));
        graph.getVertices().forEach(vertex -> {
            afterEdge.put(vertex,new HashMap<>());
            graph.getallEdges().forEach(edge -> {
                afterEdge.get(vertex).put(edge,0.0);
            });
        });

        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            Vertex source = trafficDemand.getSource();
            Vertex destination = trafficDemand.getDestination();
            Path spBefore = ShortestPath.dijsktra(graph,source,destination);
            for (Edge edge : spBefore.getEdges()) {
                if(!edge.getSource().equals(edge.getDestination())) {
                    double temp = beforeEdge.get(edge);
                    temp++;
                    beforeEdge.put(edge, temp);
                }
            }
        }

        for (Vertex target : graph.getVertices()){
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                Vertex source = trafficDemand.getSource();
                Vertex destination = trafficDemand.getDestination();

                if(!source.equals(destination) && !source.equals(target) && !destination.equals(target)) {
                    Path spAfter = ShortestPath.dijsktraNodeConstrained(graph,source,destination,target);

                    for (Edge edge : spAfter.getEdges()) {
                        if(!edge.getSource().equals(edge.getDestination()) &&
                                (!edge.getSource().equals(target) || !edge.getDestination().equals(target))) {
                            double temp = afterEdge.get(target).get(edge);
                            temp++;
                            afterEdge.get(target).put(edge, temp);
                        }
                    }
                }
            }
        }

        for (Vertex target: graph.getVertices()){
            double afterEdgeSum = 0.0;

            for(Edge edge : afterEdge.get(target).keySet()){
                if (beforeEdge.get(edge)>0 &&
                        (!edge.getSource().equals(target) || !edge.getDestination().equals(target))){
                    afterEdgeSum += Math.abs((afterEdge.get(target).get(edge) - beforeEdge.get(edge)*1.0));
                }
            }
            flowCen.put(target,afterEdgeSum);
        }

        double beforeEdgeSum = 0.0;

        for (Edge edge : beforeEdge.keySet()){
            beforeEdgeSum= beforeEdgeSum + beforeEdge.get(edge);
        }

        for(Vertex target: graph.getVertices()){
            double temp = flowCen.get(target);
            flowCen.put(target,temp/(beforeEdgeSum*1.0));
        }

        LinkedHashMap<Vertex,Double> sortedflowCen=  MapUtils.sortMapByValue(flowCen);
        return sortedflowCen;
    }

    public static LinkedHashMap<Vertex, Double> Fmeasure(ListGraph graph, TrafficStore trafficStore){
        HashMap<Vertex,Double> flowCen = new HashMap<>();
        HashMap<Vertex,HashMap<Vertex, Double>> duv = new HashMap<>();

        double duvSum=0;

        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            Vertex source = graph.getVertex(trafficDemand.getSource().getLabel());
            Vertex destination = graph.getVertex(trafficDemand.getDestination().getLabel());

            if(source==null || destination==null)
                continue;

            duv.putIfAbsent(source, new HashMap<>());

            if(!source.equals(destination)){
                Path sp = ShortestPath.dijsktraCapacityConstrained(graph,source,destination,trafficDemand.getDemand());
                duv.get(source).put(destination, sp.getSize()*trafficDemand.getDemand());
                duvSum+=sp.getSize()*trafficDemand.getDemand();
            }
        }

        for (Vertex target : graph.getVertices()){
            double numSum=0;
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){

                Vertex source = graph.getVertex(trafficDemand.getSource().getLabel());
                Vertex destination = graph.getVertex(trafficDemand.getDestination().getLabel());

                if(source==null || destination==null)
                    continue;

                if(!source.equals(destination)&& !source.equals(target) && !destination.equals(target)){
                    Path spNodeConstrained = ShortestPath.dijsktraNodeAndCapacityConstrained(graph, source,
                            destination, target,trafficDemand.getDemand());

                    double pathSize=0;

                    if (spNodeConstrained!=null)
                        pathSize = spNodeConstrained.getSize();

                    numSum += Math.abs(pathSize*trafficDemand.getDemand() -
                            duv.get(source).get(destination));
                }
            }
            flowCen.put(target,numSum/duvSum);
        }

        LinkedHashMap<Vertex,Double> sortedflowCen=  MapUtils.sortMapByValue(flowCen);
        return sortedflowCen;

    }

    public static LinkedHashMap<Vertex, Double> Closeness(ListGraph graph){

        HashMap<Vertex,Double> closenessCen = new HashMap<>();

        for (Vertex targetVertex : graph.getVertices()){

            double denomSum = 0.0;

            for (Vertex vertex : graph.getVertices()){
                if (!vertex.equals(targetVertex)){
                    Path path = ShortestPath.dijsktra(graph,targetVertex,vertex);
                    if(path!=null)
                        denomSum += path.getSize();
                }
            }
            closenessCen.put(targetVertex,(graph.getNumVertices()-1.0)/denomSum);
        }

        LinkedHashMap<Vertex,Double> sortedflowCen=  MapUtils.sortMapByValue(closenessCen);
        return sortedflowCen;
    }

    /**
     *
     * Implementation of Brandes' algorithm for Betweenness computation
     * @param graph: Target Graph for which betweenness per vertex is calculated
     */

    public static LinkedHashMap<Vertex, Double> Betweenness(ListGraph graph){

        int numVertices = graph.getNumVertices();

        HashMap<Vertex, Double> betCen = new HashMap<>();
        graph.getVertices().forEach(vertex -> {
            betCen.put(vertex,0.0);
        });

        for (Vertex s : graph.getVertices()){

            Stack<Vertex> S = new Stack<>();
            HashMap<Vertex, Set<Vertex>> P = new HashMap<>();
            graph.getVertices().forEach(vertex -> {
                P.put(vertex, new HashSet<>());
            });

            HashMap<Vertex, Double> Sigma = new HashMap<>();
            HashMap<Vertex, Double> D = new HashMap<>();
            HashMap<Vertex, Double> Delta = new HashMap<>();

            graph.getVertices().forEach(vertex -> {
                Sigma.put(vertex,0.0);
                Delta.put(vertex,0.0);
                D.put(vertex,-1.0);
            });

            Sigma.put(s,1.0);
            D.put(s,0.0);

            Queue<Vertex> Q = new LinkedList<>();

            Q.add(s);

            while(Q.size()>0){
                Vertex v = Q.remove();
                S.push(v);

                for (Vertex w : graph.getSuccessors(v)){

                    if (S.contains(w) || w.equals(s))
                        continue;

                    if (D.get(w)<0){
                        Q.add(w);
                        D.put(w, D.get(v)+1);
                    }

                    if (D.get(w) == D.get(v) + 1){
                        double temp = Sigma.get(w);
                        Sigma.put(w, temp + Sigma.get(v));
                        P.get(w).add(v);
                    }
                }
            }

            while(S.size()>0){
                Vertex w = S.pop();

                for (Vertex v : P.get(w)) {
                    double temp = Delta.get(v);
                    Delta.put(v, temp + (Sigma.get(v)/ Sigma.get(w)) * (1 + Delta.get(w)));
                }

                if (!w.equals(s)) {
                    double temp = betCen.get(w);
                    betCen.put(w,temp + Delta.get(w));
                }
            }
        }


        for (Vertex vertex : betCen.keySet()){
            double temp = betCen.get(vertex);
            betCen.put(vertex,Math.round(temp/(graph.getNumVertices()-1)/(graph.getNumVertices()-2)*10000)/10000.0);
        }

        LinkedHashMap<Vertex,Double> sortedbetCen =  MapUtils.sortMapByValue(betCen);

        return sortedbetCen;
    }

    public static LinkedHashMap<Vertex, Double> BetweennessEndPoints(ListGraph graph){

        double temp;

        int numVertices = graph.getNumVertices();

        HashMap<Vertex, Double> betCen = new HashMap<>();
        graph.getVertices().forEach(vertex -> {
            betCen.put(vertex,0.0);
        });

        for (Vertex s : graph.getVertices()){

            Stack<Vertex> S = new Stack<>();
            HashMap<Vertex, Set<Vertex>> P = new HashMap<>();
            graph.getVertices().forEach(vertex -> {
                P.put(vertex, new HashSet<>());
            });

            HashMap<Vertex, Double> Sigma = new HashMap<>();
            HashMap<Vertex, Double> D = new HashMap<>();
            HashMap<Vertex, Double> Delta = new HashMap<>();

            graph.getVertices().forEach(vertex -> {
                Sigma.put(vertex,0.0);
                Delta.put(vertex,0.0);
                D.put(vertex,-1.0);
            });

            Sigma.put(s,1.0);
            D.put(s,0.0);

            Queue<Vertex> Q = new LinkedList<>();

            Q.add(s);

            while(Q.size()>0){
                Vertex v = Q.remove();
                S.push(v);

                for (Vertex w : graph.getSuccessors(v)){

                    if (S.contains(w) || w.equals(s))
                        continue;

                    if (D.get(w)<0){
                        Q.add(w);
                        D.put(w, D.get(v)+1);
                    }

                    if (D.get(w) == D.get(v) + 1){
                        temp = Sigma.get(w);
                        Sigma.put(w, temp + Sigma.get(v));
                        P.get(w).add(v);
                    }
                }
            }

            temp = betCen.get(s);
            betCen.put(s,temp+S.size()-1);

            while(S.size()>0){
                Vertex w = S.pop();

                for (Vertex v : P.get(w)) {
                    temp = Delta.get(v);
                    Delta.put(v, temp + (Sigma.get(v)/ Sigma.get(w)) * (1 + Delta.get(w)));
                }

                if (!w.equals(s)) {
                    temp = betCen.get(w);
                    betCen.put(w,temp + Delta.get(w)+1);
                }
            }
        }


        for (Vertex vertex : betCen.keySet()){
            temp = betCen.get(vertex);
            betCen.put(vertex,Math.round(temp/(graph.getNumVertices()-1)/(graph.getNumVertices()-2)*10000)/10000.0);
        }

        LinkedHashMap<Vertex,Double> sortedbetCen =  MapUtils.sortMapByValue(betCen);

        return sortedbetCen;
    }



}
