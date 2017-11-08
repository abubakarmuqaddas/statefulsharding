package statefulsharding.heuristic;

import statefulsharding.MapUtils;
import statefulsharding.Pair;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.StatAlgorithms;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;

/**
 * Evaluate Traffic Heuristic Local Search Realistic sync restructured
 */


public class NewTfcHeurTempGraphWrite{

    private static double syncAlpha = 0.05;

    public static void main(String[] args){

        int size = 10;
        int startCopies = 4;
        int endCopies = 4;
        int startTraffic = 1;
        int endTraffic = 10;
        int startPartitionRuns = 1;
        int endPartitionRuns = 10;
        int numLSIterOuter = 10;
        int numLSIterInner = 100;

        /* Size, CopyNum*/
        HashMap<Integer, ArrayList<Double>> TotalTfcColl = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> DataTfcColl = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> SyncTfcCol = new HashMap<>();
        HashMap<Integer, HashMap<Integer, ArrayList<Double>>> AvgPathLengthCol = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> copiesUsedCol = new HashMap<>();

        ListGraph graph = ManhattanGraphGen.generateManhattanUnwrapped(size, Integer.MAX_VALUE, true);

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        String trafficFile =
                "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                        "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" +
                        size +  ".csv";



        for (int numCopies = startCopies; numCopies <= endCopies; numCopies++) {

            TotalTfcColl.put(numCopies, new ArrayList<>());
            DataTfcColl.put(numCopies, new ArrayList<>());
            SyncTfcCol.put(numCopies, new ArrayList<>());
            copiesUsedCol.put(numCopies, new ArrayList<>());
            AvgPathLengthCol.put(numCopies, new HashMap<>());


            for (int traffic = startTraffic; traffic <= endTraffic; traffic++) {

                TrafficStore trafficStore = new TrafficStore();
                TrafficGenerator.fromFileLinebyLine(graph, trafficStore, traffic, 1, false,
                        trafficFile);

                for (int partitionNum = startPartitionRuns; partitionNum <= endPartitionRuns; partitionNum++){

                    System.out.println("Numcopies: " + numCopies + ", Traffic: " + traffic + ", Partition: " +
                            partitionNum);


                    String PartitionFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/" +
                            "MANHATTAN-UNWRAPPED-Partitions/" +
                            "MANHATTAN-UNWRAPPED-Partitions_Size_" + size +
                            "_NumCopies_" + numCopies + "_PartitionRun_" + partitionNum;

                    long seed = 20000*traffic + 1000*partitionNum + (long)(1200*syncAlpha) + 500*numCopies
                            + 4*size;
                    Random rand = new Random(seed);

                    String writeFileInitial = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/" +
                            "partitioning/CheckPartitions/" +
                            "MANHATTAN-UNWRAPPED-Partitions_Size_" + size +
                            "_NumCopies_" + numCopies + "_PartitionRun_" + partitionNum + "_Traffic_"
                            + traffic + ".dot";
                    Partitioning.writePartitionGraphLeaders(graph, Partitioning.getCopies(graph, PartitionFile,
                            false), writeFileInitial);



                    for (int outerLSIter = 1; outerLSIter <= numLSIterOuter; outerLSIter++) {

                        /*
                            Initial computation of LS
                         */

                        ArrayList<Vertex> currentCopies = Partitioning.getCopies(graph, PartitionFile,
                                false);
                        ArrayList<Vertex> bestCopies = new ArrayList<>(currentCopies);
                        double bestDataTraffic = routeTraffic(currentCopies, dist, trafficStore);
                        ArrayList<Vertex> bestUsedCopies = getUsage(trafficStore);
                        double bestSyncTraffic = getSyncTraffic(bestUsedCopies, graph, trafficStore);
                        double bestTotalTraffic = bestDataTraffic + bestSyncTraffic;


                        /*
                            Now disturb the existing copies (i.e. the current copies)
                         */

                        ArrayList<ArrayList<Vertex>> checkedCopies = new ArrayList<>();
                        checkedCopies.add(new ArrayList<>(currentCopies));

                        for (int innerLSIter = 1; innerLSIter <= numLSIterInner; innerLSIter++) {

                            /*
                                Pick the random vertex to move out of currentCopies
                             */

                            int targetVertexNo = rand.nextInt(numCopies);
                            Vertex targetVertex = currentCopies.get(targetVertexNo);

                            /*
                                Get all successors and remove targetVertex from it
                             */

                            LinkedList<Vertex> successors = graph.getSuccessorsList(targetVertex);
                            if (successors.contains(targetVertex))
                                successors.remove(targetVertex);

                            /*
                                Pick one successor randomly
                             */

                            int newTargetVertexNo = rand.nextInt(successors.size());
                            Vertex newTargetVertex = successors.get(newTargetVertexNo);

                            /*
                                Move the picked vertex
                             */

                            currentCopies.set(targetVertexNo, newTargetVertex);

                            if (!nestedListContainsList(checkedCopies, currentCopies)){

                                ArrayList<Vertex> temp = new ArrayList<>(currentCopies);
                                checkedCopies.add(temp);

                                double dataTraffic = routeTraffic(currentCopies, dist, trafficStore);
                                ArrayList<Vertex> usedCopies = getUsage(trafficStore);
                                double syncTraffic = getSyncTraffic(usedCopies, graph, trafficStore);
                                double totalTraffic = dataTraffic + syncTraffic;

                                if (totalTraffic < bestTotalTraffic) {

                                    bestCopies.clear();
                                    bestCopies.addAll(currentCopies);
                                    bestUsedCopies.clear();
                                    bestUsedCopies.addAll(usedCopies);

                                    bestDataTraffic = dataTraffic;
                                    bestSyncTraffic = syncTraffic;
                                    bestTotalTraffic = totalTraffic;

                                }
                                else {
                                    currentCopies.set(targetVertexNo, targetVertex);
                                }
                            }
                            else {
                                currentCopies.set(targetVertexNo, targetVertex);
                            }
                        }
                        /*
                        System.out.println("LSIter: " + outerLSIter + ", TotalTraffic: " + bestTotalTraffic
                                + ", DataTraffic: " + bestDataTraffic + ", SyncTraffic: " + bestSyncTraffic
                                + ", Copies: " + bestCopies.size() + ", CopiesUsed: " + bestUsedCopies.size());
                                */
                        /*
                        String writeFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/" +
                                "partitioning/CheckPartitions/" +
                                "MANHATTAN-UNWRAPPED-Partitions_Size_" + size +
                                "_NumCopies_" + numCopies + "_PartitionRun_" + partitionNum + "_Traffic_"
                                + traffic + "_LocalSearchIter_" + outerLSIter + ".dot";
                        Partitioning.writePartitionGraphLeaders(graph, bestCopies, writeFile);
                        */

                        TotalTfcColl.get(numCopies).add(bestTotalTraffic);
                        DataTfcColl.get(numCopies).add(bestDataTraffic);
                        SyncTfcCol.get(numCopies).add(bestSyncTraffic);
                        copiesUsedCol.get(numCopies).add((double)bestUsedCopies.size());

                        AvgPathLengthCol.get(numCopies).putIfAbsent(bestUsedCopies.size(), new ArrayList<>());
                        AvgPathLengthCol
                                .get(numCopies)
                                .get(bestUsedCopies.size())
                                .add(getAveragePathLength(bestUsedCopies, graph));
                        /*
                        System.out.println("NumCopiesUsed: " + bestUsedCopies.size() +

                        ", AvgPathLength: " + StatAlgorithms.round2(getAveragePathLength(bestUsedCopies, graph)));
                        */
                    }
                }

                trafficStore.clear();
            }
        }

        System.out.println("NumCopies TotalTfcMean DataTfcMean SyncTfcMean copiesUsedMean");

        for (int numCopies = startCopies; numCopies <= endCopies; numCopies++){

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

        System.out.println("NumCopies UsedCopies AvgPathLength");
        for (int numCopies = startCopies; numCopies <= endCopies; numCopies++) {
            System.out.println("----------");
            for (Integer usedCopies : AvgPathLengthCol.get(numCopies).keySet()) {
                Pair<Double, Double> AvgPathLength =
                        StatAlgorithms.ConfIntervals(AvgPathLengthCol.get(numCopies).get(usedCopies), 95);
                System.out.println(numCopies + " " + usedCopies + " " +
                        StatAlgorithms.round2(AvgPathLength.getFirst()));
            }
        }



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
                        double currentUsage = Integer.MIN_VALUE;
                        Vertex targetVertex = null;

                        for (Vertex copy : minDistVertices) {
                            if (usage.get(copy) > currentUsage) {
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
            for (int j = 0; j < copies.size(); j++) {

                if (i == j)
                    continue;

                int multiplier = occurrence.get(copies.get(i));

                try {
                    syncTraffic += syncAlpha *
                            multiplier *
                            ShortestPath.dijsktra(graph, copies.get(i), copies.get(j))
                                    .getSize();
                }
                catch (NullPointerException e) {
                    //
                }

            }
        }

        return syncTraffic;

    }

    private static boolean nestedListContainsList(ArrayList<ArrayList<Vertex>> nestedList,
                                           ArrayList<Vertex> list){

        boolean contains = false;

        for(ArrayList<Vertex> innerList : nestedList){
            if(innerList.containsAll(list)){
                contains = true;
                break;
            }
        }

        return contains;
    }

    private static double getAveragePathLength(ArrayList<Vertex> vertices, ListGraph graph){

        double totalPathLength = 0;

        for(Vertex vertex1 : vertices){
            for(Vertex vertex2 : vertices){
                if(!vertex1.equals(vertex2)){
                    totalPathLength += ShortestPath.dijsktra(graph,vertex1,vertex2).getSize();
                }
            }
        }
        return totalPathLength/(vertices.size()*(vertices.size()-1));
    }


}
