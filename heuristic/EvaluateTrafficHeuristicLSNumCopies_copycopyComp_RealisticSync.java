package statefulsharding.heuristic;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
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

public class EvaluateTrafficHeuristicLSNumCopies_copycopyComp_RealisticSync {

    private static double syncAlpha = 0.25;

    public static void main(String[] args){

        //long seed = 23102018;
        //Random rand = new Random(seed);
        //Random rand = new Random();

        int size = 5;
        int startCopies = 1;
        int endCopies = 15;
        //int numCopies = 15;

        int lsOuterStart = 1;
        int lsOuterFinish = 10;
        int startTraffic = 1;
        int endTraffic = 10 ;


        /* Size, CopyNum*/
        HashMap<Integer, ArrayList<Double>> TotalTraffic = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> DataTraffic = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> SyncTraffic = new HashMap<>();
        HashMap<Integer, ArrayList<Double>> copiesUsed = new HashMap<>();
        //HashMap<Integer, HashMap<Integer, Double>> TrafficShortestPath = new HashMap<>();

        ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, Integer.MAX_VALUE,
                ManhattanGraphGen.mType.UNWRAPPED, false, true);
        ListGraph graph = manhattanGraphGen.getManhattanGraph();

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        String trafficFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" + size +
                ".csv";

        //for(double alpha = alphaStart ; alpha<=alphaEnd ; alpha+=alphaInterval) {

        for (int numCopies = startCopies; numCopies <= endCopies; numCopies++) {


            TotalTraffic.put(numCopies, new ArrayList<>());
            DataTraffic.put(numCopies, new ArrayList<>());
            SyncTraffic.put(numCopies, new ArrayList<>());
            copiesUsed.put(numCopies, new ArrayList<>());

            for (int traffic = startTraffic; traffic <= endTraffic; traffic++) {

                int startPartitionRuns = 1;
                int endPartitionRuns = 10;

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



                for (int partitionNum = startPartitionRuns; partitionNum <= endPartitionRuns; partitionNum++) {

                    long seed = 20000*traffic + 1000*partitionNum + (long)(1200*syncAlpha);
                    Random rand = new Random(seed);

                    System.out.println("Partition num: " + partitionNum);

                    /*
                    HashMap<Vertex, ListGraph> partitions =
                            Partitioning.EvolutionaryPartition(
                                    graph,
                                    numCopies,
                                    50,
                                    "random",
                                    "betweenness",
                                    trafficStore,
                                    false,
                                    false,
                                    null
                            );
                            */


                    String PartitionFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/" +
                            "MANHATTAN-UNWRAPPED-Partitions/" +
                            "MANHATTAN-UNWRAPPED-Partitions_Size_" + size +
                            "_NumCopies_" + numCopies + "_PartitionRun_" + partitionNum;


                    for(int lsOuter = lsOuterStart ; lsOuter<=lsOuterFinish ; lsOuter++) {

                        //ArrayList<Vertex> sortedVertices = new ArrayList<>(partitions.keySet());

                        ArrayList<Vertex> sortedVertices = Partitioning.getCopies(graph,PartitionFile,false);

                        LinkedList<ArrayList<Vertex>> checkedVertices = new LinkedList<>();

                        HashMap<Vertex, Integer> usage = new HashMap<>();
                        sortedVertices.forEach(vertex -> usage.put(vertex, 0));

                        checkedVertices.add(new ArrayList<>(sortedVertices));

                        Pair<Pair<HashMap<TrafficDemand, Vertex>, Double>, ArrayList<Vertex>>
                                afterDataRouted = routeGetDataTraffic(sortedVertices, dist, trafficStore);

                        double syncTraffic = getSyncTraffic(afterDataRouted.getSecond(), graph,
                                afterDataRouted.getFirst().getFirst());
                        double bestTraffic = afterDataRouted.getFirst().getSecond() + syncTraffic;
                        double bestSyncTraffic = syncTraffic;
                        double bestDataTraffic = afterDataRouted.getFirst().getSecond();

                        if (numCopies != 1) {
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

                                //targetVertexNo = ThreadLocalRandom.current().nextInt(0, numCopies);
                                targetVertexNo = rand.nextInt(numCopies - 1);
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

                                //int newTargetVertexNo = ThreadLocalRandom.current().nextInt(0, successors.size());
                                int newTargetVertexNo = rand.nextInt(successors.size() - 1);
                                Vertex newTargetVertex = successors.get(newTargetVertexNo);

                                /*
                                    Move the picked vertex
                                 */

                                sortedVertices.set(targetVertexNo, newTargetVertex);
                                j++;
                            }

                            checkedVertices.add(new ArrayList<>(sortedVertices));

                            afterDataRouted = routeGetDataTraffic(sortedVertices, dist, trafficStore);
                            syncTraffic = getSyncTraffic(afterDataRouted.getSecond(), graph,
                                    afterDataRouted.getFirst().getFirst());

                            double currentTraffic = afterDataRouted.getFirst().getSecond() + syncTraffic;

                            /*
                            System.out.println("OuterLs: " + lsOuter +
                                    ", currentTraffic: " + currentTraffic + ", bestTraffic: " + bestDataTraffic +
                                    ", j: " + j +
                                    ", Vertices: ");
                            for (Vertex sortedVertex : sortedVertices) {
                                System.out.print(sortedVertex.getLabel() + " ");
                            }
                            System.out.println();
                            */

                            if (currentTraffic < bestTraffic) {


                                /*
                                System.out.println("This is better!!!!!!!!");
                                for (Vertex sortedVertex : sortedVertices) {
                                    System.out.print(sortedVertex.getLabel() + " ");
                                }
                                System.out.println();
                                */

                                bestTraffic = currentTraffic;
                                bestDataTraffic = afterDataRouted.getFirst().getSecond();
                                bestSyncTraffic = syncTraffic;
                            }
                            else {
                                sortedVertices.set(targetVertexNo, targetVertex);
                            }

                        }
                        else {
                            bestTraffic = afterDataRouted.getFirst().getSecond();
                            bestDataTraffic = bestTraffic;
                            bestSyncTraffic = 0.0;
                        }

                        TotalTraffic.get(numCopies).add(bestTraffic);
                        DataTraffic.get(numCopies).add(bestDataTraffic);
                        SyncTraffic.get(numCopies).add(bestSyncTraffic);
                        copiesUsed.get(numCopies).add((double) afterDataRouted.getSecond().size());

                    }

                }


                trafficStore.clear();
            }

            for(int i=0 ; i<TotalTraffic.get(numCopies).size() ; i++){
                //System.out.println(TotalTraffic.get(alpha).get(i) + " " + DataTraffic.get(alpha).get(i) +
                //", " + (TotalTraffic.get(alpha).get(i)-DataTraffic.get(alpha).get(i)));

                if(DataTraffic.get(numCopies).get(i)<1.0){
                    TotalTraffic.get(numCopies).remove(i);
                    DataTraffic.get(numCopies).remove(i);
                    SyncTraffic.get(numCopies).remove(i);
                }


            }

            Collection<Double> totalTraffic = TotalTraffic.get(numCopies);
            Collection<Double> dataTraffic = DataTraffic.get(numCopies);
            Collection<Double> syncTraffic = SyncTraffic.get(numCopies);
            Collection<Double> copies = copiesUsed.get(numCopies);

            Pair<Double, Double> totalTrafficStats = StatAlgorithms.ConfIntervals(totalTraffic,95);
            Pair<Double, Double> dataTrafficStats = StatAlgorithms.ConfIntervals(dataTraffic,95);
            Pair<Double, Double> syncTrafficStats = StatAlgorithms.ConfIntervals(syncTraffic,95);
            Pair<Double, Double> copyStats = StatAlgorithms.ConfIntervals(copies,95);

            System.out.println(
                    numCopies + " " +
                            StatAlgorithms.round2(totalTrafficStats.getFirst()) + " " +
                            //round2(totalTrafficStats.getFirst()-totalTrafficStats.getSecond()) + " " +
                            //round2(totalTrafficStats.getFirst()+totalTrafficStats.getSecond()) + " " +
                            StatAlgorithms.round2(dataTrafficStats.getFirst()) + " " +
                            //round2(dataTrafficStats.getFirst()-dataTrafficStats.getSecond()) + " " +
                            //round2(dataTrafficStats.getFirst()+dataTrafficStats.getSecond()) + " " +
                            StatAlgorithms.round2(syncTrafficStats.getFirst()) + " " +
                            //round2(syncTrafficStats.getFirst()-syncTrafficStats.getSecond()) + " " +
                            //round2(syncTrafficStats.getFirst()+syncTrafficStats.getSecond()) + " " +
                            StatAlgorithms.round2(copyStats.getFirst()) //+ " " +
                    //round2(copyStats.getFirst()-copyStats.getSecond()) + " " +
                    //round2(copyStats.getFirst()+copyStats.getSecond())
            );


        }

        for (int numCopies = startCopies; numCopies <= endCopies; numCopies++) {

            for(int i=0 ; i<TotalTraffic.get(numCopies).size() ; i++){
                //System.out.println(TotalTraffic.get(alpha).get(i) + " " + DataTraffic.get(alpha).get(i) +
                //", " + (TotalTraffic.get(alpha).get(i)-DataTraffic.get(alpha).get(i)));

                if(DataTraffic.get(numCopies).get(i)<1.0){
                    TotalTraffic.get(numCopies).remove(i);
                    DataTraffic.get(numCopies).remove(i);
                    SyncTraffic.get(numCopies).remove(i);
                }


            }

            Collection<Double> totalTraffic = TotalTraffic.get(numCopies);
            Collection<Double> dataTraffic = DataTraffic.get(numCopies);
            Collection<Double> syncTraffic = SyncTraffic.get(numCopies);
            Collection<Double> copies = copiesUsed.get(numCopies);

            Pair<Double, Double> totalTrafficStats = StatAlgorithms.ConfIntervals(totalTraffic,95);
            Pair<Double, Double> dataTrafficStats = StatAlgorithms.ConfIntervals(dataTraffic,95);
            Pair<Double, Double> syncTrafficStats = StatAlgorithms.ConfIntervals(syncTraffic,95);
            Pair<Double, Double> copyStats = StatAlgorithms.ConfIntervals(copies,95);

            System.out.println(
                    numCopies + " " +
                            StatAlgorithms.round2(totalTrafficStats.getFirst()) + " " +
                            //round2(totalTrafficStats.getFirst()-totalTrafficStats.getSecond()) + " " +
                            //round2(totalTrafficStats.getFirst()+totalTrafficStats.getSecond()) + " " +
                            StatAlgorithms.round2(dataTrafficStats.getFirst()) + " " +
                            //round2(dataTrafficStats.getFirst()-dataTrafficStats.getSecond()) + " " +
                            //round2(dataTrafficStats.getFirst()+dataTrafficStats.getSecond()) + " " +
                            StatAlgorithms.round2(syncTrafficStats.getFirst()) + " " +
                            //round2(syncTrafficStats.getFirst()-syncTrafficStats.getSecond()) + " " +
                            //round2(syncTrafficStats.getFirst()+syncTrafficStats.getSecond()) + " " +
                            //StatAlgorithms.
            (copyStats.getFirst()) //+ " " +
                            //round2(copyStats.getFirst()-copyStats.getSecond()) + " " +
                            //round2(copyStats.getFirst()+copyStats.getSecond())
            );
        }
    }

    private static double getSyncTraffic(ArrayList<Vertex> vertices, ListGraph graph,
                                         HashMap<TrafficDemand, Vertex> copyForTrafficDemand){

        HashMap<Vertex, Integer> occurance = new HashMap<>();

        vertices.forEach(vertex -> occurance.put(vertex, 0));

        for (TrafficDemand trafficDemand : copyForTrafficDemand.keySet()) {
            Vertex usedVertex = copyForTrafficDemand.get(trafficDemand);
            int temp = occurance.get(usedVertex);
            temp++;
            occurance.put(usedVertex,temp);
        }

        int numCopies = vertices.size();
        double syncTraffic = 0.0;

        for (int i = 0; i < numCopies; i++) {
            for (int j = 0; j < numCopies; j++) {

                if (i == j)
                    continue;

                int multiplier = occurance.get(vertices.get(i));

                try {
                    syncTraffic += (double) syncAlpha*multiplier*ShortestPath.dijsktra(
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


    private static Pair<Pair<HashMap<TrafficDemand, Vertex>, Double>, ArrayList<Vertex>>
                                        routeGetDataTraffic(ArrayList<Vertex> copies,
                                              HashMap<Vertex, HashMap<Vertex, Integer>> dist,
                                              TrafficStore trafficStore){

        HashMap<TrafficDemand, Vertex> copyForTrafficDemand = new HashMap<>();


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
                    copyForTrafficDemand.put(trafficDemand, targetVertex);
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
                        copyForTrafficDemand.put(trafficDemand, minDistVertices.getFirst());
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

        return new Pair<>(new Pair<>(copyForTrafficDemand, dataTraffic), usedVertices);
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
