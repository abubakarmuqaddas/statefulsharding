package statefulsharding.heuristic;

import statefulsharding.MapUtils;
import statefulsharding.Pair;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.LoadGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static statefulsharding.heuristic.TrafficHeuristic.hType.fixedcopies;
import static statefulsharding.heuristic.TrafficHeuristic.hType.shortestpath;

public class EvaluateTrafficHeuristicTabuSearchNumCopies {

    public static void main(String[] args){

        int size = 7;
        int traffic = 1;
        int startCopies = 1;
        int endCopies = 1;
        double alpha = 1.0;
        int startPartitionRuns = 1;
        int endPartitionRuns = 10;
        int tabuRunStart = 1;
        int tabuRunFinish = 500;


        /* Size, CopyNum*/
        HashMap<Integer, HashMap<Integer, ArrayList<Double>>>
                TrafficPartition = new HashMap<>();

        TrafficPartition.put(size, new HashMap<>());

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, Integer.MAX_VALUE,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        TrafficStore trafficStore = new TrafficStore();

        TrafficGenerator.fromFileLinebyLine(graph, trafficStore, traffic, 1, false,
                "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                        "topologies_traffic/Traffic/WS_Traffic/WS_Traffic" + size +
                        ".csv");

        for (int numCopies = startCopies; numCopies <= endCopies; numCopies++) {

            TrafficPartition.get(size).putIfAbsent(numCopies, new ArrayList<>());

            for (int partitionNum = startPartitionRuns; partitionNum <= endPartitionRuns; partitionNum++) {

                System.out.println("Partition num: " + partitionNum);

                HashMap<Vertex, ListGraph> partitions =
                        Partitioning.EvolutionaryPartition(
                                graph,
                                numCopies,
                                50,
                                "random",
                                "betweennesstfc",
                                trafficStore,
                                false,
                                false,
                                null
                        );

                ArrayList<Vertex> sortedVertices = new ArrayList<>(partitions.keySet());
                LinkedList<ArrayList<Vertex>> checkedVertices = new LinkedList<>();

                HashMap<Vertex, Integer> usage = new HashMap<>();

                sortedVertices.forEach(vertex -> usage.put(vertex,0));

                if(numCopies!=1){

                    checkedVertices.add(new ArrayList<>(sortedVertices));
                    double dataTraffic = routeGetDataTraffic(sortedVertices, usage, dist, trafficStore);
                    double syncTraffic = getSyncTraffic(numCopies, sortedVertices, graph);

                    double bestTraffic = dataTraffic + alpha*syncTraffic;

                    ArrayList<Vertex> bestSoln = null;


                    for (int tabuRun = tabuRunStart; tabuRun <= tabuRunFinish; tabuRun++) {

                        // System.out.println("Tabu run: " + tabuRun);

                        int targetVertexNo = 0;
                        Vertex targetVertex = null;

                        int j = 0;
                        int numSuccessors = 0;
                        for (Vertex vertex : sortedVertices) {
                            numSuccessors += graph.getSuccessors(vertex).size();
                        }

                        while (listContainsSol(checkedVertices, sortedVertices) && j <= numSuccessors) {
                            /*
                            Pick the random vertex to move
                            */

                            targetVertexNo = ThreadLocalRandom.current().nextInt(0, numCopies);
                            targetVertex = sortedVertices.get(targetVertexNo);

                            /*
                                Get all successors
                             */

                            LinkedList<Vertex> successors = graph.getSuccessorsList(targetVertex);
                            if (successors.contains(targetVertex))
                                successors.remove(targetVertex);

                            /*
                                Pick one successor randomly
                             */

                            int newTargetVertexNo = ThreadLocalRandom.current().nextInt(0, successors.size());
                            Vertex newTargetVertex = successors.get(newTargetVertexNo);

                            /*
                                Generate new vertices
                             */

                            sortedVertices.set(targetVertexNo, newTargetVertex);
                            j++;
                        }
                        checkedVertices.add(new ArrayList<>(sortedVertices));

                        dataTraffic = routeGetDataTraffic(sortedVertices, usage, dist, trafficStore);
                        syncTraffic = getSyncTraffic(numCopies, sortedVertices, graph);

                        double currentTraffic = dataTraffic + alpha * syncTraffic;

                        if (currentTraffic <= bestTraffic) {
                                        /*
                                        System.out.println("Traffic: "+ traffic + ", Copy: " + numCopies +
                                                ", DataTraffic: " + dataTraffic + ", SyncTraffic: "
                                                + syncTraffic + ", TotalTraffic: " + currentTraffic);
                                        */

                            bestTraffic = currentTraffic;
                            bestSoln = new ArrayList<>(sortedVertices);
                        }
                        else {
                            sortedVertices.set(targetVertexNo, targetVertex);
                        }
                    }
                    TrafficPartition.get(size).get(numCopies).add(bestTraffic);








                }
                else{
                    double dataTraffic = routeGetDataTraffic(sortedVertices, usage, dist, trafficStore);
                    TrafficPartition.get(size).get(numCopies).add(dataTraffic);
                }

















            }


        }



    }

    private static double getSyncTraffic(int numCopies, ArrayList<Vertex> vertices, ListGraph graph){

        double syncTraffic = 0.0;

        for (int i = 0; i < numCopies; i++) {
            for (int j = 0; j < numCopies; j++) {

                if (i == j)
                    continue;

                try {
                    syncTraffic += (double) ShortestPath.dijsktra(
                            graph, vertices.get(i), vertices.get(j)
                    ).getSize();
                }
                catch (NullPointerException e) {
                    //
                }

            }
        }

        return syncTraffic;

    }

    private static double routeGetDataTraffic(ArrayList<Vertex> copies,
                                              HashMap<Vertex, Integer> usage,
                                              HashMap<Vertex, HashMap<Vertex, Integer>> dist,
                                              TrafficStore trafficStore){

        double dataTraffic = 0.0;

        if(copies.size()>1) {
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                HashMap<Vertex, Integer> distPerCopy = new HashMap<>();

                Vertex source = trafficDemand.getSource();
                Vertex destination = trafficDemand.getDestination();

                for (Vertex vertex : copies) {
                    distPerCopy.put(vertex, dist.get(source).get(vertex) + dist.get(vertex).get(destination));
                }
                LinkedHashMap<Vertex, Integer> sortedDistPerCopy = MapUtils.sortMapByValue(distPerCopy);

                int i = 1;

                double minTraffic = 0;

                LinkedList<Vertex> minDistVertices = new LinkedList<>();

                for (Vertex vertex : sortedDistPerCopy.keySet()) {

                    if (i == 1) {
                        minDistVertices.add(vertex);
                        minTraffic = sortedDistPerCopy.get(vertex);
                        continue;
                    }
                    if (sortedDistPerCopy.get(vertex) == minTraffic)
                        minDistVertices.add(vertex);

                    i++;
                }

                double currentUsage = Integer.MAX_VALUE;
                Vertex targetVertex = null;

                if (minDistVertices.size() > 1) {
                    for (Vertex vertex : minDistVertices) {
                        if (usage.get(vertex) < currentUsage) {
                            currentUsage = usage.get(vertex);
                            targetVertex = vertex;
                        }
                    }
                    dataTraffic += sortedDistPerCopy.get(targetVertex);
                    int temp = usage.get(targetVertex);
                    temp++;
                    usage.put(targetVertex, temp);
                } else {
                    dataTraffic += sortedDistPerCopy.get(minDistVertices.getFirst());
                }
            }
        }
        else{

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                Vertex source = trafficDemand.getSource();
                Vertex destination = trafficDemand.getDestination();

                dataTraffic+= dist.get(source).get(copies.get(0)) + dist.get(copies.get(0)).get(destination);

            }

        }

        return dataTraffic;
    }


    private static boolean listContainsSol(LinkedList<ArrayList<Vertex>> checkedVertices,
                                           ArrayList<Vertex> currentCombination){

        boolean currentIter = true;

        for(ArrayList<Vertex> arrayList : checkedVertices){
            currentIter = true;

            for(Vertex vertex : currentCombination){
                if(!arrayList.contains(vertex)){
                    currentIter = false;
                    break;
                }
            }

            if(currentIter)
                break;

        }

        return currentIter;
    }






}
