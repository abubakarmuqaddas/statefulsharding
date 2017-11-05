package statefulsharding.heuristic;

import statefulsharding.MapUtils;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.*;

/**
 * Evaluate Traffic Heuristic Local Search Realistic sync restructured
 */


public class NewTfcHeur {

    private static double syncAlpha = 0.05;

    public static void main(String[] args){

        Random rand = new Random(2500);

        int size = 5;
        int numCopies = 2;
        int numLocalSearchIterOuter = 10;
        int numLocalSearchIterInner = 100;

        ListGraph graph = ManhattanGraphGen.generateManhattanUnwrapped(size, Integer.MAX_VALUE, true);

        HashMap<Vertex, HashMap<Vertex, Integer>> dist =
                ShortestPath.FloydWarshall(graph, false, null);

        String trafficFile =
                "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                        "topologies_traffic/Traffic/Manhattan_Traffic/Manhattan_Unwrapped_Traffic" +
                        size +  ".csv";

        int traffic = 1;

        TrafficStore trafficStore = new TrafficStore();
        TrafficGenerator.fromFileLinebyLine(graph, trafficStore, traffic,1,false, trafficFile);

        int partitionNum = 1;
        String PartitionFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/" +
                "MANHATTAN-UNWRAPPED-Partitions/" +
                "MANHATTAN-UNWRAPPED-Partitions_Size_" + size +
                "_NumCopies_" + numCopies + "_PartitionRun_" + partitionNum;

        for(int outerLocalSearchIter = 1 ; outerLocalSearchIter<=numLocalSearchIterOuter ; outerLocalSearchIter++) {

            /*
                Initial computation of localSearch
             */

            ArrayList<Vertex> currentCopies = Partitioning.getCopies(graph, PartitionFile,false);
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

            for (int innerLocalSearchIter = 1; innerLocalSearchIter<=numLocalSearchIterInner; innerLocalSearchIter++){

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

                if(!nestedListContainsList(checkedCopies, currentCopies)){

                    ArrayList<Vertex> temp = new ArrayList<>(currentCopies);
                    checkedCopies.add(temp);

                    double dataTraffic = routeTraffic(currentCopies, dist, trafficStore);
                    ArrayList<Vertex> usedCopies = getUsage(trafficStore);
                    double syncTraffic = getSyncTraffic(usedCopies, graph, trafficStore);
                    double totalTraffic = dataTraffic + syncTraffic;

                    if(totalTraffic<bestTotalTraffic){

                        bestCopies.clear();
                        bestCopies.addAll(currentCopies);
                        bestUsedCopies.clear();
                        bestUsedCopies.addAll(usedCopies);

                        bestDataTraffic = dataTraffic;
                        bestSyncTraffic = syncTraffic;
                        bestTotalTraffic = totalTraffic;
                    }
                    else{
                        currentCopies.set(targetVertexNo, targetVertex);
                    }
                }
                else{
                    currentCopies.set(targetVertexNo, targetVertex);
                }
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

                    usage.put(targetVertex, usage.get(targetVertex)+1);
                }
                else{
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

    public static ArrayList<Vertex> getUsage(TrafficStore trafficStore){

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


}
