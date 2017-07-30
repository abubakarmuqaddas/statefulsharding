package statefulsharding.heuristic;

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

public class EvaluateTrafficHeuristicTabuSearch {

    public static void main(String[] args){

        LinkedList<String> result = new LinkedList<>();


        /*
        int startSize = 3;
        int finalSize = 11;
        */

        //double p=0;
        double p=0.5;


        int startSize = 3;
        int finalSize = 6;


        int startTraffic = 1;
        int endTraffic = 100;

        int startCopies = 1;
        int endCopies = 3;

        int startPartitionRuns = 1;
        int endPartitionRuns = 10;

        double alphaStart = 0.0;
        double alphaEnd = 1.0;
        double alphaInterval = 0.25;
        int tabuRunStart = 1;
        int tabuRunFinish = 500;

        /* Size, trafficNo, */
        HashMap<Integer, HashMap<Integer, Double>> TrafficShortestPath = new HashMap<>();
        /* Size, CopyNum*/
        HashMap<Integer, HashMap<Integer, ArrayList<Double>>>
                TrafficPartition = new HashMap<>();

        //for(int size = startSize ; size<=finalSize ; size++) {


        /**
           Generate files:
         */

        //try {

            //FileWriter logfw = new FileWriter("Tabu_WS_randPerm_p_"+p+".txt", true);
            //BufferedWriter logbw = new BufferedWriter(logfw);
            //PrintWriter logout = new PrintWriter(logbw);

            for (double alpha = alphaStart; alpha <= alphaEnd; alpha = alpha + alphaInterval) {

                System.out.println("Alpha: " + alpha);

                //for (int size = startSize; size <= finalSize; size += 12) {
                for (int size = startSize; size <= finalSize; size++) {

                    TrafficShortestPath.put(size, new HashMap<>());
                    TrafficPartition.put(size, new HashMap<>());


                    ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, Integer.MAX_VALUE,
                            ManhattanGraphGen.mType.UNWRAPPED, false, true);
                    ListGraph graph = manhattanGraphGen.getManhattanGraph();





                //Watts Strogatz

                /*

                String graphLocation = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                        "topologies_traffic/Traffic/WS_graph_" + p +
                        "/WS_graph" + size + "_" + p + "_8.csv";
                ListGraph graph = LoadGraph.GraphParserJ(graphLocation, Integer.MAX_VALUE, true);


                */



                    HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                            ShortestPath.FloydWarshall(graph, false, null);


                    String trafficFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                            "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" + size +
                            ".csv";




                    for (int traffic = startTraffic; traffic <= endTraffic; traffic++) {

                        TrafficStore trafficStore = new TrafficStore();


                    TrafficGenerator.fromFileLinebyLine(
                            graph,
                            trafficStore,
                            traffic,
                            1,
                            false,
                            trafficFile
                    );
                    /*

                    TrafficGenerator.fromFileLinebyLine(graph, trafficStore, traffic, 1, false,
                            "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                                    "topologies_traffic/Traffic/WS_Traffic/WS_Traffic" + size +
                                    ".csv");

                                    */



                        TrafficHeuristic trafficHeuristicSP = new TrafficHeuristic(graph,
                                trafficStore,
                                1,
                                shortestpath,
                                true,
                                false);

                        TrafficShortestPath.get(size).put(traffic, trafficHeuristicSP.getTotalTraffic());

                        for (int numCopies = startCopies; numCopies <= endCopies; numCopies++) {


                            System.out.println("Size: " + size + ", Traffic: " + traffic
                                    + ", Copy: " + numCopies);


                            TrafficPartition.get(size).putIfAbsent(numCopies, new ArrayList<>());

                            for (int partitionNum = startPartitionRuns; partitionNum <= endPartitionRuns; partitionNum++) {

                                //System.out.println("Partition num: " + partitionNum);

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

                                TrafficHeuristic trafficHeuristicPart = new TrafficHeuristic(graph,
                                        trafficStore,
                                        numCopies,
                                        fixedcopies,
                                        sortedVertices,
                                        true,
                                        false);

                                if (numCopies != 1) {

                                    checkedVertices.add(new ArrayList<>(sortedVertices));
                                    double bestTraffic = trafficHeuristicPart.getTotalTraffic() +
                                            alpha * getSyncTraffic(numCopies, sortedVertices, graph);
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

                                        double dataTraffic = 0.0;
                                        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                                            Vertex source = trafficDemand.getSource();
                                            Vertex destination = trafficDemand.getDestination();

                                            int minDist = Integer.MAX_VALUE;

                                            for (Vertex vertex : sortedVertices) {
                                                if ((dist.get(source).get(vertex) + dist.get(vertex).get(destination)) < minDist) {
                                                    minDist = dist.get(source).get(vertex) + dist.get(vertex).get(destination);
                                                }
                                            }

                                            dataTraffic += minDist;
                                        }

                                        double syncTraffic = getSyncTraffic(numCopies, sortedVertices, graph);
                                        double currentTraffic = dataTraffic + alpha * syncTraffic;

                                        if (currentTraffic <= bestTraffic) {
                                        /*
                                        System.out.println("Traffic: "+ traffic + ", Copy: " + numCopies +
                                                ", DataTraffic: " + dataTraffic + ", SyncTraffic: "
                                                + syncTraffic + ", TotalTraffic: " + currentTraffic);
                                        */

                                            bestTraffic = currentTraffic;
                                            bestSoln = new ArrayList<>(sortedVertices);
                                        } else {
                                            sortedVertices.set(targetVertexNo, targetVertex);
                                        }
                                    }
                                    TrafficPartition.get(size).get(numCopies).add(bestTraffic);
                                } else {
                                    TrafficPartition.get(size).get(numCopies).add(trafficHeuristicPart.getTotalTraffic());
                                }


                            }

                        }
                        trafficStore.clear();
                    }

                    Collection<Double> copy0 = TrafficShortestPath.get(size).values();
                    Collection<Double> copy1 = TrafficPartition.get(size).get(1);
                    Collection<Double> copy2 = TrafficPartition.get(size).get(2);
                    Collection<Double> copy3 = TrafficPartition.get(size).get(3);


                    Pair<Double, Double> copy0stats = interval(copy0);
                    Pair<Double, Double> copy1stats = interval(copy1);
                    Pair<Double, Double> copy2stats = interval(copy2);
                    Pair<Double, Double> copy3stats = interval(copy3);


                    //System.out.println("Stats for size 3");
                    /*size copy0m copy0l copy0u copy1 copy2 copy3*/

                    result.add(size + " " + alpha + " " +
                            round2(copy0stats.getFirst()) + " " +
                            round2(copy0stats.getFirst() - copy0stats.getSecond()) + " " +
                            round2(copy0stats.getFirst() + copy0stats.getSecond()) + " " +

                            round2(copy1stats.getFirst()) + " " +
                            round2(copy1stats.getFirst() - copy1stats.getSecond()) + " " +
                            round2(copy1stats.getFirst() + copy1stats.getSecond()) + " " +

                            round2(copy2stats.getFirst()) + " " +
                            round2(copy2stats.getFirst() - copy2stats.getSecond()) + " " +
                            round2(copy2stats.getFirst() + copy2stats.getSecond()) + " " +

                            round2(copy3stats.getFirst()) + " " +
                            round2(copy3stats.getFirst() - copy3stats.getSecond()) + " " +
                            round2(copy3stats.getFirst() + copy3stats.getSecond()) + " "
                    );

                    System.out.println(
                            size + " " + alpha + " " +
                                    round2(copy0stats.getFirst()) + " " +
                                    round2(copy0stats.getFirst() - copy0stats.getSecond()) + " " +
                                    round2(copy0stats.getFirst() + copy0stats.getSecond()) + " " +

                                    round2(copy1stats.getFirst()) + " " +
                                    round2(copy1stats.getFirst() - copy1stats.getSecond()) + " " +
                                    round2(copy1stats.getFirst() + copy1stats.getSecond()) + " " +

                                    round2(copy2stats.getFirst()) + " " +
                                    round2(copy2stats.getFirst() - copy2stats.getSecond()) + " " +
                                    round2(copy2stats.getFirst() + copy2stats.getSecond()) + " " +

                                    round2(copy3stats.getFirst()) + " " +
                                    round2(copy3stats.getFirst() - copy3stats.getSecond()) + " " +
                                    round2(copy3stats.getFirst() + copy3stats.getSecond()) + " "
                    );




            /*
            System.out.println(
                    size + " " +
                            round2(copy0stats.getFirst()) + " " +
                            round2(copy0stats.getFirst()-copy0stats.getSecond()) + " " +
                            round2(copy0stats.getFirst()+copy0stats.getSecond()) + " " +

                            round2(copy1stats.getFirst()) + " " +
                            round2(copy1stats.getFirst()-copy1stats.getSecond()) + " " +
                            round2(copy1stats.getFirst()+copy1stats.getSecond()) + " " +

                            round2(copy2stats.getFirst()) + " " +
                            round2(copy2stats.getFirst()-copy2stats.getSecond()) + " " +
                            round2(copy2stats.getFirst()+copy2stats.getSecond()) + " " +

                            round2(copy3stats.getFirst()) + " " +
                            round2(copy3stats.getFirst()-copy3stats.getSecond()) + " " +
                            round2(copy3stats.getFirst()+copy3stats.getSecond()) + " "
            );
            */


                }

            }

        //}
        //catch(IOException e){
        //    //
        //}

        for (String s : result) {
            System.out.println(s);
        }

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
