package statefulsharding.graph.algorithms;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.heuristic.GeneratePartitionCopies;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.IntStream;

public class Partitioning {

    /**
     *
     * Implementation of:
     * Graph partitioning for network problems
     * Authors: K. Ruddell & A. Raith
     * 2013 Joint NZSA ORSNZ Conference, Hamilton, New Zealand
     *
     * @param graph: Input Graph
     * @param numParts: Number of partitions
     * @param numIters: Number of maximum iterations
     * @param initialMethod: Initial method to allocate partitions
     * @param insidePartitionMethod: Initial method to allocate partitions
     * @param trafficStore: Traffic Store object used to calculate f-measure centrality
     * @param writeGraph: Write the graph to file
     * @param writePartition: Write the partition to file
     * @param filename: Filename
     * @return Return the partition subGraphs with each leader as the Copy
     */

    public static HashMap<Vertex, ListGraph> EvolutionaryPartition(ListGraph graph, int numParts,
                                             int numIters, String initialMethod,
                                             String insidePartitionMethod, TrafficStore trafficStore,
                                             boolean writeGraph, boolean writePartition,
                                             String filename){

        LinkedHashMap<Vertex, Double> betCen = null;
        HashMap<Vertex, ListGraph> Partitions = new HashMap<>();
        ArrayList<Vertex> leaders = new ArrayList<>();
        ArrayList<Vertex> sortedVertices = null;

        if(numParts>1) {

            ListGraph[] subGraphs = new ListGraph[numParts];
            HashMap<Vertex, Vertex> affiliation = new HashMap<>();
            ArrayList<Vertex> previousLeaders;

            /*
            Initial Betweenness
            */

            if (initialMethod.toLowerCase().equals("betweenness")) {
                betCen = Centrality.BetweennessEndPoints(graph);
                sortedVertices = new ArrayList<>(betCen.keySet());
                for (int i = 1; i <= numParts; i++) {
                    leaders.add(sortedVertices.get(betCen.size() - i));
                }
            }
            else if (initialMethod.toLowerCase().equals("random")) {
                int[] range = IntStream.rangeClosed(0, graph.getNumVertices() - 1).toArray();
                Random RandomNumberGen = new Random();

                for (int i = range.length - 1; i > 0; i--) {
                    int index = RandomNumberGen.nextInt(i + 1);
                    // Simple swap
                    int a = range[index];
                    range[index] = range[i];
                    range[i] = a;
                }

                for (int i = 0; i < numParts; i++) {
                    leaders.add(graph.getVertex(range[i]));
                }

            }

            //Set<ArrayList<>>

            for (int i = 1; i <= numIters; i++) {
                /*
                System.out.println("Current Leaders: ");
                for (Vertex vertex : leaders) {
                    System.out.println(vertex.getLabel());
                }
                */

                affiliation = getAffiliation(graph, leaders, affiliation);

                for (int k = 0; k < numParts; k++) {
                    subGraphs[k] = new ListGraph();
                }

                for (int j = 0; j < numParts; j++) {
                    ListGraph currentSubGraph = subGraphs[j];

                    for (Vertex vertex : graph.getVertices()) {
                        if (affiliation.get(vertex).equals(leaders.get(j))) {
                            currentSubGraph.addVertex(vertex.getLabel());
                        }
                    }

                    for (Vertex src : currentSubGraph.getVertices()) {
                        for (Vertex dst : currentSubGraph.getVertices()) {
                            if (!src.equals(dst)) {
                                if (graph.checkEdge(src.getLabel(), dst.getLabel())) {
                                    if (!currentSubGraph.checkEdge(src, dst)) {
                                        currentSubGraph.insertDoubleEdge(src, dst,
                                                graph.getEdge(graph.getVertex(src.getLabel()),
                                                        graph.getVertex(dst.getLabel()))
                                                        .getCapacity());
                                    }
                                }

                            }
                        }
                    }
                }

                previousLeaders = new ArrayList<>(leaders);
                leaders.clear();

            /*
            New Leader Election
             */

                for (int j = 0; j < numParts; j++) {
                    ListGraph currentSubGraph = subGraphs[j];
                    if (insidePartitionMethod.toLowerCase().equals("betweenness"))
                        betCen = Centrality.BetweennessEndPoints(currentSubGraph);
                    if (insidePartitionMethod.toLowerCase().equals("fmeasure"))
                        betCen = Centrality.Fmeasure(currentSubGraph, trafficStore);
                    if (insidePartitionMethod.toLowerCase().equals("betweennesstfc"))
                        betCen = Centrality.BetweennessTfc(currentSubGraph, trafficStore);
                    sortedVertices = new ArrayList<>(betCen.keySet());
                    leaders.add(graph.getVertex(sortedVertices.get(betCen.size() - 1).getLabel()));
                }

            /*

                System.out.println();
                System.out.println("Previous Leaders: ");
                previousLeaders.forEach(vertex -> {
                    System.out.print(vertex.getLabel() + " ");
                });
                System.out.println();
                System.out.println("Current Leaders: ");
                leaders.forEach(vertex -> {
                    System.out.print(vertex.getLabel() + " ");
                });
                System.out.println();
                System.out.println();

            */
                if (previousLeaders.containsAll(leaders) && leaders.containsAll(previousLeaders)) {
                    //System.out.println(i + "th iteration!");
                    if (writeGraph)
                        writePartitionGraph(graph, subGraphs, numParts, leaders,
                                "analysis/partitioning/testGraph_" + initialMethod + "partition.dot");
                    break;
                }
                else {
                    previousLeaders.clear();
                }

                for (int k = numParts - 1; k > -1; k--) {
                    subGraphs[k].clear();
                }
                for (int k = numParts - 1; k > -1; k--) {
                    subGraphs[k] = null;
                }
            }

            for (int i = 0; i < numParts; i++) {
                Partitions.put(leaders.get(i), subGraphs[i]);
            }
        }

        else{
            if (insidePartitionMethod.toLowerCase().equals("betweenness"))
                betCen = Centrality.BetweennessEndPoints(graph);
            if (insidePartitionMethod.toLowerCase().equals("fmeasure"))
                betCen = Centrality.Fmeasure(graph, trafficStore);
            if (insidePartitionMethod.toLowerCase().equals("betweennesstfc"))
                betCen = Centrality.BetweennessTfc(graph, trafficStore);

            sortedVertices = new ArrayList<>(betCen.keySet());
            leaders.add(graph.getVertex(sortedVertices.get(betCen.size() - 1).getLabel()));
            Partitions.put(graph.getVertex(sortedVertices.get(betCen.size() - 1).getLabel()), graph);
        }

        if (writePartition)
            writePartitions(leaders, filename);

        return Partitions;
    }

    public static HashMap<Vertex, Vertex> getAffiliation (ListGraph graph, ArrayList<Vertex> leaders,
                                                          HashMap<Vertex, Vertex> affiliation){
        for(Vertex vertex : graph.getVertices()){

            if (leaders.contains(vertex)) {
                affiliation.put(vertex, vertex);
                continue;
            }

            int min = Integer.MAX_VALUE;
            Vertex candidateLeader=null;

            for (Vertex leader : leaders){
                int pathSize = ShortestPath.dijsktra(graph,vertex,leader).getSize();
                if (ShortestPath.dijsktra(graph,vertex,leader).getSize()<min){
                    min = pathSize;
                    candidateLeader = leader;
                }
            }

            affiliation.put(vertex,candidateLeader);
        }
        return affiliation;
    }

    public static void writePartitionGraph(ListGraph graph, ListGraph[] subGraphs, int numParts,
                                           ArrayList<Vertex> leaders, String filename){

        ArrayList<String> colors1 = new ArrayList<>(Arrays.asList("peru", "blue", "darkgreen",
                                                                    "red", "darkslategray","bisque4"));
        ArrayList<String> colors2 = new ArrayList<>(Arrays.asList("wheat", "lightblue", "seagreen1",
                                                                    "rosybrown1","darkseagreen","bisque"));

        try {
            FileWriter partfw = new FileWriter(filename, true);
            BufferedWriter partbw = new BufferedWriter(partfw);
            PrintWriter partout = new PrintWriter(partbw);
            ArrayList<Integer> leadersInteger = new ArrayList<>();

            partout.println("strict graph G {");
            partout.println("\t" + "ratio=1;");

            for (int i=0 ; i<leaders.size() ; i++){
                partout.println("\t" + leaders.get(i).getLabel()
                                    + " [color=" + colors1.get(i) + ",style = filled];");
                partout.flush();
                leadersInteger.add(leaders.get(i).getLabel());
            }

            for (int i=0 ; i<numParts ; i++){
                ListGraph currentSubGraph = subGraphs[i];
                for (Vertex vertex : currentSubGraph.getVertices()){
                    if (!leadersInteger.contains(vertex.getLabel())) {
                        partout.println("\t" + vertex.getLabel()
                                + " [color=" + colors2.get(i) + ",style = filled];");
                        partout.flush();
                    }
                }
            }
            for (Edge edge : graph.getallEdges()){
                if(!edge.getSource().equals(edge.getDestination())) {
                    partout.println("\t" + edge.getSource().getLabel() + " -- "
                            + edge.getDestination().getLabel() + ";");
                    partout.flush();
                }
            }

            partout.println("}");
            partout.flush();
        }
        catch (IOException e){
            System.out.println("Caught Exception " + e + " while writing graph partition");
        }
    }

    public static void writePartitions(ArrayList<Vertex> leaders, String filename){

        try {
            FileWriter partfw = new FileWriter(filename, true);
            BufferedWriter partbw = new BufferedWriter(partfw);
            PrintWriter partout = new PrintWriter(partbw);

            leaders.forEach(vertex -> {
                partout.println(vertex.getLabel());
            });

            partout.flush();
        }
        catch (IOException e){
            System.out.println("Caught Exception " + e + " while writing copies");
        }

    }

    public static ArrayList<Vertex> getCopies(ListGraph graph, String filename, boolean skipFirstLine){

        ArrayList<Vertex> copies = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            String line;
            String[] strNums;

            int vertexInt;
            int i=1;

            while ((line = br.readLine()) != null) {
                if(i==1 && skipFirstLine) {
                    i++;
                    continue;
                }

                strNums = line.split("\\s");

                vertexInt=Integer.parseInt(strNums[0]);

                copies.add(graph.getVertex(vertexInt));

                i++;
            }

            return copies;
        }
        catch(FileNotFoundException e){
            System.out.println("FileNotFoundException " + e + ", Filename: " + filename);
        }
        catch(IOException e){
            System.out.println("IOException " + e);
        }
        return null;
    }

    public static ListGraph[] getPartitions(ListGraph graph, String filename){

        HashMap<Vertex, Vertex> affiliation = new HashMap<>();

        ArrayList<Vertex> leaders = getCopies(graph,filename,false);
        affiliation = getAffiliation(graph,leaders,affiliation);

        int numParts = leaders.size();
        ListGraph[] subGraphs = new ListGraph[numParts];

        for (int k=0 ; k<numParts ; k++){
            subGraphs[k] = new ListGraph();
        }

        for (int j=0 ; j<numParts ; j++){
            ListGraph currentSubGraph = subGraphs[j];

            for (Vertex vertex : graph.getVertices()){
                if(affiliation.get(vertex).equals(leaders.get(j))){
                    currentSubGraph.addVertex(vertex.getLabel());
                }
            }

            for (Vertex src : currentSubGraph.getVertices()){
                for (Vertex dst : currentSubGraph.getVertices()){
                    if (!src.equals(dst)){
                        if (graph.checkEdge(src.getLabel(),dst.getLabel())){
                            if(!currentSubGraph.checkEdge(src,dst)) {
                                currentSubGraph.insertDoubleEdge(src, dst,
                                        graph.getEdge(graph.getVertex(src.getLabel()),
                                                graph.getVertex(dst.getLabel()))
                                                .getCapacity());
                            }
                        }

                    }
                }
            }
        }

        return subGraphs;
    }



}
