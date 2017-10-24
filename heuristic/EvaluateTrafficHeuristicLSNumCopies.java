package statefulsharding.heuristic;

import com.sun.corba.se.impl.orbutil.concurrent.Sync;
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

import javax.xml.crypto.Data;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static statefulsharding.heuristic.TrafficHeuristic.hType.fixedcopies;
import static statefulsharding.heuristic.TrafficHeuristic.hType.shortestpath;

public class EvaluateTrafficHeuristicLSNumCopies {

    public static void main(String[] args){

        int size = 5;
        //int startCopies = 7;
        //int endCopies = 7;
        int numCopies = 15;
        double alphaStart = 0.0;
        double alphaEnd = 1.0;
        double alphaInterval = 0.1;
        int startPartitionRuns = 1;
        int endPartitionRuns = 10;
        int tabuRunStart = 1;
        int tabuRunFinish = 500;
        int startTraffic = 1;
        int endTraffic = 10;


        /* Size, CopyNum*/
        HashMap<Double, ArrayList<Double>> TotalTraffic = new HashMap<>();
        HashMap<Double, ArrayList<Double>> DataTraffic = new HashMap<>();
        HashMap<Double, ArrayList<Double>> SyncTraffic = new HashMap<>();
        HashMap<Double, ArrayList<Double>> copiesUsed = new HashMap<>();
        //HashMap<Integer, HashMap<Integer, Double>> TrafficShortestPath = new HashMap<>();

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, Integer.MAX_VALUE,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        String trafficFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" + size +
                ".csv";

        for(double alpha = alphaStart ; alpha<=alphaEnd ; alpha+=alphaInterval) {

            System.out.println("Alpha: " + alpha);

            TotalTraffic.put(alpha, new ArrayList<>());
            DataTraffic.put(alpha, new ArrayList<>());
            SyncTraffic.put(alpha, new ArrayList<>());
            copiesUsed.put(alpha, new ArrayList<>());

            for (int traffic = startTraffic; traffic <= endTraffic; traffic++) {

                System.out.println("traffic: " + traffic);

                TrafficStore trafficStore = new TrafficStore();

                TrafficGenerator.fromFileLinebyLine(
                        graph,
                        trafficStore,
                        traffic,
                        1,
                        false,
                        trafficFile
                );

                //for (int numCopies = startCopies; numCopies <= endCopies; numCopies++) {

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
                    sortedVertices.forEach(vertex -> usage.put(vertex, 0));

                    checkedVertices.add(new ArrayList<>(sortedVertices));

                    Pair<Double, ArrayList<Vertex>>
                            afterDataRouted = routeGetDataTraffic(sortedVertices, dist, trafficStore);

                    double syncTraffic = getSyncTraffic(afterDataRouted.getSecond(), graph);
                    double bestTraffic = afterDataRouted.getFirst() + alpha * syncTraffic;
                    double bestSyncTraffic=0.0;
                    double bestDataTraffic=0.0;


                    for (int tabuRun = tabuRunStart; tabuRun <= tabuRunFinish; tabuRun++) {

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

                        afterDataRouted = routeGetDataTraffic(sortedVertices, dist, trafficStore);
                        syncTraffic = getSyncTraffic(afterDataRouted.getSecond(), graph);

                        double currentTraffic = afterDataRouted.getFirst() + alpha * syncTraffic;

                        if (currentTraffic <= bestTraffic) {
                            bestTraffic = currentTraffic;
                            bestDataTraffic = afterDataRouted.getFirst();
                            bestSyncTraffic = alpha*syncTraffic;

                            if(bestDataTraffic<10.0){
                                int d=1;
                            }



                        }
                        else {
                            sortedVertices.set(targetVertexNo, targetVertex);
                        }
                    }




                    TotalTraffic.get(alpha).add(bestTraffic);
                    DataTraffic.get(alpha).add(bestDataTraffic);
                    SyncTraffic.get(alpha).add(bestSyncTraffic);
                    copiesUsed.get(alpha).add((double)afterDataRouted.getSecond().size());

                }


                //}

                trafficStore.clear();
            }
        }

        for(double alpha = alphaStart ; alpha<=alphaEnd ; alpha+=alphaInterval) {

            for(int i=0 ; i<TotalTraffic.get(alpha).size() ; i++){
                //System.out.println(TotalTraffic.get(alpha).get(i) + " " + DataTraffic.get(alpha).get(i) +
                //", " + (TotalTraffic.get(alpha).get(i)-DataTraffic.get(alpha).get(i)));

                if(DataTraffic.get(alpha).get(i)<1.0){
                    TotalTraffic.get(alpha).remove(i);
                    DataTraffic.get(alpha).remove(i);
                    SyncTraffic.get(alpha).remove(i);
                }


            }

            Collection<Double> totalTraffic = TotalTraffic.get(alpha);
            Collection<Double> dataTraffic = DataTraffic.get(alpha);
            Collection<Double> syncTraffic = SyncTraffic.get(alpha);
            Collection<Double> copies = copiesUsed.get(alpha);

            Pair<Double, Double> totalTrafficStats = interval(totalTraffic);
            Pair<Double, Double> dataTrafficStats = interval(dataTraffic);
            Pair<Double, Double> syncTrafficStats = interval(syncTraffic);
            Pair<Double, Double> copyStats = interval(copies);

            System.out.println(
                    alpha + " " +
                            round2(totalTrafficStats.getFirst()) + " " +
                            round2(totalTrafficStats.getFirst()-totalTrafficStats.getSecond()) + " " +
                            round2(totalTrafficStats.getFirst()+totalTrafficStats.getSecond()) + " " +
                            round2(dataTrafficStats.getFirst()) + " " +
                            round2(dataTrafficStats.getFirst()-dataTrafficStats.getSecond()) + " " +
                            round2(dataTrafficStats.getFirst()+dataTrafficStats.getSecond()) + " " +
                            round2(syncTrafficStats.getFirst()) + " " +
                            round2(syncTrafficStats.getFirst()-syncTrafficStats.getSecond()) + " " +
                            round2(syncTrafficStats.getFirst()+syncTrafficStats.getSecond()) + " " +
                            round2(copyStats.getFirst()) + " " +
                            round2(copyStats.getFirst()-copyStats.getSecond()) + " " +
                            round2(copyStats.getFirst()+copyStats.getSecond())
            );


        }






    }

    private static double getSyncTraffic(ArrayList<Vertex> vertices, ListGraph graph){

        int numCopies = vertices.size();
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


    private static Pair<Double, ArrayList<Vertex>> routeGetDataTraffic(ArrayList<Vertex> copies,
                                              HashMap<Vertex, HashMap<Vertex, Integer>> dist,
                                              TrafficStore trafficStore){


        HashMap<Vertex, Integer> usage = new HashMap<>();
        for (Vertex vertex : copies) {
            usage.put(vertex,0);
        }

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
                        i++;
                        continue;
                    }
                    if (sortedDistPerCopy.get(vertex) == minTraffic)
                        minDistVertices.add(vertex);

                    i++;
                }

                for (Vertex minDistVertex : minDistVertices) {
                    //if(usage.)
                }

                double currentUsage = Integer.MIN_VALUE;
                Vertex targetVertex = null;

                if (minDistVertices.size() > 1) {
                    int d=1;
                    for (Vertex vertex : minDistVertices) {
                        try {
                            if (usage.get(vertex) > currentUsage) {
                                currentUsage = usage.get(vertex);
                                targetVertex = vertex;
                            }
                        }
                        catch(NullPointerException e){
                            System.out.println("Caught null pointer " + e);
                            System.out.println("Min dist vertices: ");
                            for (Vertex minDistVertex : minDistVertices) {
                                System.out.println(minDistVertex.getLabel());
                            }
                            System.out.println("Usage vertices: ");
                            for (Vertex vertex1 : usage.keySet()) {
                                System.out.println(vertex1.getLabel());
                            }

                        }
                    }
                    dataTraffic += sortedDistPerCopy.get(targetVertex);
                    int temp = usage.get(targetVertex);
                    temp++;
                    usage.put(targetVertex, temp);
                }
                else {
                    dataTraffic += sortedDistPerCopy.get(minDistVertices.getFirst());
                    try {
                        int temp = usage.get(minDistVertices.getFirst());

                    temp++;
                    usage.put(minDistVertices.getFirst(),temp);
                    }
                    catch(NullPointerException e){
                        System.out.println("Caught null pointer " + e);
                        System.out.println("Min dist vertices: ");
                        for (Vertex minDistVertex : minDistVertices) {
                            System.out.println(minDistVertex.getLabel());
                        }
                        System.out.println("Usage vertices: ");
                        for (Vertex vertex1 : usage.keySet()) {
                            System.out.println(vertex1.getLabel());
                        }
                    }
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

        ArrayList<Vertex> usedVertices = new ArrayList<>();
        for (Vertex copy : copies) {
            usedVertices.add(copy);
        }

        for(Vertex vertex : usage.keySet()){
            if (usage.get(vertex)==0){
                usedVertices.remove(vertex);
            }
        }

        //System.out.println("Used: " + usedVertices.size());

        return new Pair<>(dataTraffic, usedVertices);
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

    private static Pair<Double, Double> interval(Collection<Double> values){
        double sum = 0.0;
        double num = 0.0;

        for(Double d : values){
            sum+=d;
        }

        double mean = sum/values.size();

        for(Double d : values){
            double numi = Math.pow(d-mean,2);
            num+=numi;
        }

        double dev = Math.sqrt(num/values.size());

        return new Pair<>(mean, (dev*1.96)/Math.sqrt(values.size()));
    }

    private static double round2(double number){
        return Math.round((number*100.0))/100.0;
    }








}
