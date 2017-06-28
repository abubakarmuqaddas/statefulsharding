package statefulsharding.heuristic;

import statefulsharding.Pair;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static statefulsharding.heuristic.TrafficHeuristic.hType.fixedcopies;
import static statefulsharding.heuristic.TrafficHeuristic.hType.shortestpath;

public class EvaluateTrafficHeuristicPoster {

    public static void main(String[] args) {

        int startSize = 3;
        int finalSize = 15;

        int startTraffic = 1;
        int endTraffic = 10;

        int startCopies = 1;
        int endCopies = 4;

        int startPartitionRuns = 1;
        int endPartitionRuns = 10;

        /* Size, trafficNo, */
        HashMap<Integer, HashMap<Integer, Double>> TrafficShortestPath = new HashMap<>();
        /* Size, CopyNum*/
        HashMap<Integer, HashMap<Integer, ArrayList<Double>>>
                TrafficPartition = new HashMap<>();

        for(int size = startSize ; size<=finalSize ; size++) {

            TrafficShortestPath.put(size, new HashMap<>());
            TrafficPartition.put(size, new HashMap<>());

            ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, Integer.MAX_VALUE,
                    ManhattanGraphGen.mType.UNWRAPPED, false, true);
            ListGraph graph = manhattanGraphGen.getManhattanGraph();

            String trafficFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                    "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" + size +
                    ".csv";

            for(int traffic = startTraffic ; traffic<=endTraffic ; traffic++) {

                TrafficStore trafficStore = new TrafficStore();

                TrafficGenerator.fromFileLinebyLine(graph,
                                                    trafficStore,
                                                    traffic,
                                                    1,
                                                    true,
                                                    trafficFile);

                TrafficHeuristic trafficHeuristicSP = new TrafficHeuristic(graph,
                                                                            trafficStore,
                                                                            1,
                                                                            shortestpath,
                                                                            true,
                                                                            false);

                TrafficShortestPath.get(size).put(traffic,trafficHeuristicSP.getTotalTraffic());

                for(int numCopies = startCopies ; numCopies<=endCopies ; numCopies++){

                    //System.out.println("Size: " + size + ", Traffic: " + traffic
                    //        + ", Copy: " + numCopies);

                    TrafficPartition.get(size).putIfAbsent(numCopies, new ArrayList<>());

                    for(int partitionNum = startPartitionRuns ; partitionNum<=endPartitionRuns ; partitionNum++){

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


                        TrafficHeuristic trafficHeuristicPart = new TrafficHeuristic(graph,
                                trafficStore,
                                numCopies,
                                fixedcopies,
                                sortedVertices,
                                true,
                                false);

                        TrafficPartition.get(size).get(numCopies).add(trafficHeuristicPart.getTotalTraffic());
                    }
                }
                trafficStore.clear();
            }

            Collection<Double> copy0 = TrafficShortestPath.get(size).values();
            Collection<Double> copy1 = TrafficPartition.get(size).get(1);
            Collection<Double> copy2 = TrafficPartition.get(size).get(2);
            Collection<Double> copy3 = TrafficPartition.get(size).get(3);
            Collection<Double> copy4 = TrafficPartition.get(size).get(4);

            Pair<Double, Double> copy0stats = stdev(copy0);
            Pair<Double, Double> copy1stats = stdev(copy1);
            Pair<Double, Double> copy2stats = stdev(copy2);
            Pair<Double, Double> copy3stats = stdev(copy3);
            Pair<Double, Double> copy4stats = stdev(copy4);

            //System.out.println("Stats for size 3");
            /*size copy0m copy0l copy0u copy1 copy2 copy3*/
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
                            round2(copy3stats.getFirst()+copy3stats.getSecond()) + " " +

                            round2(copy4stats.getFirst()) + " " +
                            round2(copy4stats.getFirst()-copy4stats.getSecond()) + " " +
                            round2(copy4stats.getFirst()+copy4stats.getSecond())

            );
        }
    }

    /**
     *
     * @param values
     * @return <mean><1/2 interval>
     */

    private static Pair<Double, Double> stdev(Collection<Double> values){
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

        return new Pair<>(mean, (dev*1.96)/values.size());
    }

    private static double round2(double number){
        return Math.round((number*100.0))/100.0;
    }

}
