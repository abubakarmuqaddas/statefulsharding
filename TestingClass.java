package statefulsharding;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.LoadGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.StatAlgorithms;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.heuristic.TrafficHeuristic;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static statefulsharding.heuristic.TrafficHeuristic.hType.fixedcopies;


public class TestingClass {

    private static double syncAlpha = 1;

    public static void main(String[] args) {

        int size = 9;

        int startTraffic = 1;
        int endTraffic = 1;


        HashMap<Integer, ArrayList<Double>> TotalTfcColl = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> DataTfcColl = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> SyncTfcCol = new HashMap<>();

        HashMap<Integer, ArrayList<Double>> copiesUsedCol = new HashMap<>();

        ListGraph graph = ManhattanGraphGen.generateManhattanUnwrapped(size, Integer.MAX_VALUE, true);

        ArrayList<Vertex> copies = new ArrayList<>();

        /*
        9 x9 9 copies
         */


        copies.add(graph.getVertex(10));
        copies.add(graph.getVertex(13));
        copies.add(graph.getVertex(16));
        copies.add(graph.getVertex(37));
        copies.add(graph.getVertex(40));
        copies.add(graph.getVertex(43));
        copies.add(graph.getVertex(64));
        copies.add(graph.getVertex(67));
        copies.add(graph.getVertex(70));




        /*
        9 x9 4 copies
         */

        /*
        copies.add(graph.getVertex(20));
        copies.add(graph.getVertex(24));
        copies.add(graph.getVertex(56));
        copies.add(graph.getVertex(60));
        */

        /*
        9 x9 1 copies
         */

        //copies.add(graph.getVertex(40));

        /*
        15 * 15 4 copies
         */
        /*
        copies.add(graph.getVertex(48));
        copies.add(graph.getVertex(56));
        copies.add(graph.getVertex(168));
        copies.add(graph.getVertex(176));
        */

        /*
        15 * 15 9 copies
         */
        /*
        copies.add(graph.getVertex(32));
        copies.add(graph.getVertex(37));
        copies.add(graph.getVertex(42));
        copies.add(graph.getVertex(107));
        copies.add(graph.getVertex(112));
        copies.add(graph.getVertex(117));
        copies.add(graph.getVertex(182));
        copies.add(graph.getVertex(187));
        copies.add(graph.getVertex(192));
        */

        int numCopies = copies.size();

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        String trafficFile =
                "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                        "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" +
                        size +  ".csv";



        TotalTfcColl.put(numCopies, new ArrayList<>());
        DataTfcColl.put(numCopies, new ArrayList<>());
        SyncTfcCol.put(numCopies, new ArrayList<>());
        copiesUsedCol.put(numCopies, new ArrayList<>());

        for (int traffic = startTraffic; traffic <= endTraffic; traffic++) {

            TrafficStore trafficStore = new TrafficStore();
            TrafficGenerator.fromFileLinebyLine(graph, trafficStore, traffic, 1, false,
                    trafficFile);

            double dataTraffic = routeTraffic(copies, dist, trafficStore);
            ArrayList<Vertex> usedCopies = getUsage(trafficStore);
            double syncTraffic = getSyncTraffic(usedCopies, graph, trafficStore);
            double totalTraffic = dataTraffic + syncTraffic;

            TotalTfcColl.get(numCopies).add(totalTraffic);
            DataTfcColl.get(numCopies).add(dataTraffic);
            SyncTfcCol.get(numCopies).add(syncTraffic);
            copiesUsedCol.get(numCopies).add((double)usedCopies.size());

            trafficStore.clear();
        }


        Pair<Double, Double> totalTraffic = StatAlgorithms.ConfIntervals(TotalTfcColl.get(numCopies),95);
        Pair<Double, Double> dataTraffic = StatAlgorithms.ConfIntervals(DataTfcColl.get(numCopies),95);
        Pair<Double, Double> syncTraffic= StatAlgorithms.ConfIntervals(SyncTfcCol.get(numCopies),95);
        Pair<Double, Double> copiesUsed = StatAlgorithms.ConfIntervals(copiesUsedCol.get(numCopies),95);

        System.out.println(
                numCopies + " " +
                        StatAlgorithms.round2(totalTraffic.getFirst()) + " " +
                        StatAlgorithms.round2(dataTraffic.getFirst()) + " " +
                        StatAlgorithms.round2(syncTraffic.getFirst()) + " " +
                        StatAlgorithms.round2(copiesUsed.getFirst())
        );








    }

    public static double routeTraffic(ArrayList<Vertex> copies,
                                      HashMap<Vertex, HashMap<Vertex, Integer>> dist,
                                      TrafficStore trafficStore){

        double dataTraffic = 0.0;
        if(copies.size()>1) {

            HashMap<Vertex, Integer> usage = new HashMap<>();
            for (Vertex vertex : copies) {
                usage.put(vertex,0);
            }



            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                Vertex src = trafficDemand.getSource();
                Vertex dst = trafficDemand.getDestination();

                LinkedHashMap<Vertex, Integer> distPerCopy = new LinkedHashMap<>();
                for (Vertex copy : copies) {
                    distPerCopy.put(copy, dist.get(src).get(copy) + dist.get(copy).get(dst));
                }
                distPerCopy = MapUtils.sortMapByValue(distPerCopy);

                double minTraffic = 0;
                LinkedList<Vertex> minDistVertices = new LinkedList<>();

                int iter = 1;
                for (Vertex copy : distPerCopy.keySet()) {

                    if (iter == 1) {
                        minDistVertices.add(copy);
                        minTraffic = distPerCopy.get(copy);
                        iter++;
                        continue;
                    }
                    if (distPerCopy.get(copy) == minTraffic)
                        minDistVertices.add(copy);

                    iter++;

                }

                if (minDistVertices.size() > 1) {
                    double currentUsage = Integer.MAX_VALUE;
                    Vertex targetVertex = null;

                    for (Vertex copy : minDistVertices) {
                        if (usage.get(copy) < currentUsage) {
                            currentUsage = usage.get(copy);
                            targetVertex = copy;
                        }
                    }

                    dataTraffic += distPerCopy.get(targetVertex);
                    trafficDemand.setCopy(targetVertex);

                    usage.put(targetVertex, usage.get(targetVertex) + 1);
                } else {
                    dataTraffic += minTraffic;
                    trafficDemand.setCopy(minDistVertices.getFirst());
                }
            }



        }
        else{

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                Vertex source = trafficDemand.getSource();
                Vertex destination = trafficDemand.getDestination();
                dataTraffic+= dist.get(source).get(copies.get(0)) + dist.get(copies.get(0)).get(destination);
                trafficDemand.setCopy(copies.get(0));
            }

        }

        return dataTraffic;

    }

    private static ArrayList<Vertex> getUsage(TrafficStore trafficStore){

        HashSet<Vertex> tempSet = new HashSet<>();

        trafficStore.getTrafficDemands().forEach(trafficDemand ->
                tempSet.add(trafficDemand.getCopy())
        );

        return new ArrayList<>(tempSet);
    }

    private static double getSyncTraffic(ArrayList<Vertex> copies, ListGraph graph,
                                         TrafficStore trafficStore){

        HashMap<Vertex, Integer> occurrence = new HashMap<>();

        copies.forEach(vertex -> occurrence.put(vertex, 0));

        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
            occurrence.put(trafficDemand.getCopy(), occurrence.get(trafficDemand.getCopy())+1);
        }

        double syncTraffic = 0.0;

        for (int i = 0; i < copies.size(); i++) {

            //int multiplier = occurrence.get(copies.get(i));
            int multiplier = 1;
            int distance = 0;

            for (int j = 0; j < copies.size(); j++) {

                if (i == j)
                    continue;

                try {
                    distance += ShortestPath.dijsktra(graph, copies.get(i), copies.get(j)).getSize();
                }
                catch (NullPointerException e) {
                    //
                }
            }

            double currentSyncTraffic = StatAlgorithms.round2(syncAlpha*multiplier*distance);
            syncTraffic += currentSyncTraffic;
        }

        return syncTraffic;

    }




}
