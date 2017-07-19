package statefulsharding.heuristic;

import statefulsharding.Pair;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static statefulsharding.heuristic.TrafficHeuristic.hType.fixedcopies;

public class EvaluateTrafficHeuristicTabuSearch {

    public static void main(String[] args){

        int startSize = 3;
        int finalSize = 10;

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


            ManhattanGraphGen manhattanGraphGen = new ManhattanGraphGen(size, Integer.MAX_VALUE,
                    ManhattanGraphGen.mType.UNWRAPPED, false, true);
            ListGraph graph = manhattanGraphGen.getManhattanGraph();


            HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                    ShortestPath.FloydWarshall(graph, false, null);

            String trafficFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                    "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" + size +
                    ".csv";

            int traffic = 1;

            TrafficStore trafficStore = new TrafficStore();

            int numCopies = 3;
            double alpha = 0.2;
            int tabuRunStart = 1;
            int tabuRunFinish = 1000;

            TrafficGenerator.fromFileLinebyLine(
                    graph,
                    trafficStore,
                    traffic,
                    1,
                    true,
                    trafficFile
            );

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


            checkedVertices.add(new ArrayList<>(sortedVertices));
            double bestTraffic = trafficHeuristicPart.getTotalTraffic() +
                    alpha * getSyncTraffic(numCopies, sortedVertices, graph);
            ArrayList<Vertex> bestSoln = null;


            for (int tabuRun = tabuRunStart; tabuRun <= tabuRunFinish; tabuRun++) {

                int targetVertexNo = 0;
                Vertex targetVertex = null;

                while (listContainsSol(checkedVertices, sortedVertices)) {
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
                    bestTraffic = currentTraffic;
                    bestSoln = new ArrayList<>(sortedVertices);
                } else {
                    sortedVertices.set(targetVertexNo, targetVertex);
                }


            }

        }



        //System.out.println("Final traffic: " + bestTraffic);


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
