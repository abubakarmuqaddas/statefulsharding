package statefulsharding.heuristic;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.*;
import statefulsharding.graph.algorithms.Centrality;
import statefulsharding.graph.algorithms.ShortestPath;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;

public class TrafficHeuristic{

    private ListGraph graph;
    private TrafficStore trafficStore;
    private int numCopies;
    private LinkedList<Vertex> copies;
    private LinkedHashMap<TrafficDemand,Path> routingSolution;
    private LinkedHashMap<Vertex,Double> centrality;
    private hType heuristicType;
    private boolean multipleStates;
    private boolean rearrangeStates;

    public enum hType{
        shortestpath, betweennesstfc, flowtfc, flowtfcalternate, betweenness, residualflow, random, fixedcopies
    }

    public TrafficHeuristic(ListGraph graph, TrafficStore trafficStore, int numCopies, hType type,
                            boolean multipleStates, boolean rearrangeStates){

        this.graph = graph;
        this.trafficStore = trafficStore;
        this.numCopies = numCopies;
        this.multipleStates = multipleStates;
        this.rearrangeStates = rearrangeStates;
        routingSolution = new LinkedHashMap<>();
        copies = new LinkedList<>();


        if(type.toString().toLowerCase().equals(hType.betweennesstfc.toString())){
            this.heuristicType=hType.betweennesstfc;
        }
        else if(type.toString().toLowerCase().equals(hType.flowtfc.toString())) {
            this.heuristicType = hType.flowtfc;
        }
        else if(type.toString().toLowerCase().equals(hType.flowtfcalternate.toString())) {
            this.heuristicType = hType.flowtfcalternate;
        }
        else if(type.toString().toLowerCase().equals(hType.betweenness.toString())) {
            this.heuristicType = hType.betweenness;
        }
        else if(type.toString().toLowerCase().equals(hType.residualflow.toString())) {
            this.heuristicType = hType.residualflow;
        }
        else if(type.toString().toLowerCase().equals(hType.random.toString())) {
            this.heuristicType = hType.random;
        }
        else if(type.toString().toLowerCase().equals(hType.shortestpath.toString())) {
            this.heuristicType = hType.shortestpath;
        }

        if (this.heuristicType==hType.random){
            computeCopiesRandom();
            routeTraffic();

        }
        else if(this.heuristicType==hType.shortestpath)
            routeTraffic();
        else{
            computeCopies();
            routeTraffic();
        }



    }

    public TrafficHeuristic(ListGraph graph, TrafficStore trafficStore, int numCopies, hType type,
                            ArrayList<Vertex> sortedVertices, boolean multipleStates, boolean rearrangeStates){

        this.graph = graph;
        this.trafficStore = trafficStore;
        this.numCopies = numCopies;
        this.copies = new LinkedList<>();
        this.multipleStates = multipleStates;
        this.rearrangeStates = rearrangeStates;
        routingSolution = new LinkedHashMap<>();

        if(type.toString().toLowerCase().equals(hType.fixedcopies.toString())){
            this.heuristicType=hType.fixedcopies;
        }

        int index;

        for (index = 1 ; index<=numCopies ; index++){
            Vertex copy = sortedVertices.get(sortedVertices.size()-index);
            copies.add(copy);
        }

        if (this.heuristicType==hType.fixedcopies){
            if (numCopies>1 && rearrangeStates && !multipleStates)
                resolveCopies(sortedVertices,index);
            routeTraffic();
        }

    }

    private void computeCopies(){

        LinkedHashMap<Vertex,Double> centrality = null;

        if(heuristicType==hType.betweennesstfc){
            centrality = Centrality.BetweennessTfc(graph,trafficStore);
        }
        else if(heuristicType==hType.flowtfc){
            centrality = Centrality.FlowTfc(graph,trafficStore);
        }
        else if(heuristicType==hType.flowtfcalternate){
            centrality = Centrality.FlowTfcAlternate(graph,trafficStore);
        }
        else if(heuristicType==hType.betweenness){
            centrality = Centrality.BetweennessEndPoints(graph);
        }
        else if(heuristicType==hType.residualflow){
            centrality = Centrality.ResidualFlow(graph,trafficStore);
        }

        this.centrality=centrality;

        ArrayList<Vertex> sortedVertices = new ArrayList<>(centrality.keySet());

        int index;
        for (index = 1 ; index<=numCopies ; index++){
            Vertex copy = sortedVertices.get(centrality.size()-index);
            copies.add(copy);
        }

        if (copies.size()>1 && rearrangeStates && !multipleStates)
            resolveCopies(sortedVertices,index);

    }

    private void computeCopiesRandom(){

        int[] range = IntStream.rangeClosed(0, graph.getNumVertices()-1).toArray();
        Random RandomNumberGen = new Random();
        ArrayList<Vertex> sortedVertices = new ArrayList<>();

        for (int i = range.length - 1; i > 0; i--){
            int index = RandomNumberGen.nextInt(i + 1);
            // Simple swap
            int a = range[index];
            range[index] = range[i];
            range[i] = a;
        }

        for (int i=0 ; i<graph.getNumVertices() ; i++){
            sortedVertices.add(graph.getVertex(range[i]));
        }

        int index;
        for (index = 1 ; index<=numCopies ; index++){
            Vertex copy = sortedVertices.get(graph.getNumVertices()-index);
            copies.add(copy);
        }

        if (copies.size()>1 && rearrangeStates && !multipleStates)
            resolveCopies(sortedVertices,index);

    }

    private void resolveCopies(ArrayList<Vertex> sortedVertices, int index){
        boolean interference = false;
        boolean fixed = false;
        while(!interference){
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                if (copies.contains(trafficDemand.getSource()) && copies.contains(trafficDemand.getDestination())
                        && !trafficDemand.getSource().equals(trafficDemand.getDestination())){
                    int d=1;
                    int srcIndex = sortedVertices.indexOf(trafficDemand.getSource());
                    int dstIndex = sortedVertices.indexOf(trafficDemand.getDestination());
                    int min = Math.min(srcIndex,dstIndex);
                    fixed=true;
                    copies.remove(sortedVertices.get(min));
                    copies.add(sortedVertices.get(sortedVertices.size()-index));
                    sortedVertices.remove(min);
                    break;
                }
            }

            if(fixed){
                fixed=false;
                continue;
            }

            interference = true;
        }

    }

    private void routeTraffic(){
        for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
            //System.out.println("Routing" + trafficDemand.getSource().getLabel() + " -> "
            // + trafficDemand.getDestination().getLabel());
            if(this.heuristicType==hType.shortestpath){
                Path path = ShortestPath.dijsktra(graph, trafficDemand.getSource(), trafficDemand.getDestination());
                if (path == null)
                    routingSolution.put(trafficDemand, null);
                else
                    routingSolution.put(trafficDemand, path);
            }
            else {
                Path path = RouteThroughState.route(graph, trafficDemand, copies, multipleStates);
                if (path == null)
                    routingSolution.put(trafficDemand, null);
                else
                    routingSolution.put(trafficDemand, path);
            }
        }
    }

    public LinkedHashMap<TrafficDemand,Path> getRoutingSolution(){
        return routingSolution;
    }

    public LinkedList<Vertex> getCopies(){
        return copies;
    }

    public double getTotalTraffic(){

        double obj=0;
        for(TrafficDemand trafficDemand : routingSolution.keySet()){
            if (routingSolution.get(trafficDemand)!=null){
                for (Edge edge : routingSolution.get(trafficDemand).getEdges()){
                    if (!edge.getSource().equals(edge.getDestination()))
                        obj=obj+trafficDemand.getDemand();
                }
            }
        }

        return obj;
    }

    public void writeSolution(String filename){

        try{

            if (this.heuristicType!=hType.shortestpath)
                filename = filename + "_copy_" + numCopies;

            FileWriter logfw = new FileWriter(filename + "_Logfile.txt" ,true);
            BufferedWriter logbw = new BufferedWriter(logfw);
            PrintWriter logout = new PrintWriter(logbw);
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            logout.println(sdf.format(cal.getTime()) + " Objective Value: " + getTotalTraffic());
            logout.flush();

            FileWriter solnRfw = new FileWriter(filename + "_RoutingSolution.txt" ,
                    true);
            BufferedWriter solnRbw = new BufferedWriter(solnRfw);
            PrintWriter solnRout = new PrintWriter(solnRbw); solnRout.println("u v i j");

            for (TrafficDemand trafficDemand : routingSolution.keySet()) {
                for (Edge edge : routingSolution.get(trafficDemand).getEdges()) {
                    solnRout.println(trafficDemand.getSource().getLabel() + " " +
                            trafficDemand.getDestination().getLabel() + " " +
                            edge.getSource().getLabel() + " " +
                            edge.getDestination().getLabel());
                }
            }
            solnRout.flush();

            if (this.heuristicType!=hType.shortestpath) {
                FileWriter solnPfw = new FileWriter(filename + "_PlacementSolution.txt",true);
                BufferedWriter solnPbw = new BufferedWriter(solnPfw);
                PrintWriter solnPout = new PrintWriter(solnPbw);
                solnPout.println("n");

                for (Vertex vertex : getCopies()) {
                    solnPout.println(vertex.getLabel());
                }
                solnPout.flush();
            }

        }
        catch (IOException e){
            System.out.println("Error " + e + "while writing solution in Traffic Heuristic");
        }


    }

    public LinkedHashMap<Vertex,Double> getCentrality(){
        return centrality;
    }

    public static double getTrafficFromDemands(LinkedHashMap<TrafficDemand, Path> routingSolution){

        double obj=0;
        for(TrafficDemand trafficDemand : routingSolution.keySet()){
            if (routingSolution.get(trafficDemand)!=null){
                for (Edge edge : routingSolution.get(trafficDemand).getEdges()){
                    if (!edge.getSource().equals(edge.getDestination()))
                        obj=obj+trafficDemand.getDemand();
                }
            }
        }

        return obj;
    }

    public static void writeSolution(LinkedHashMap<TrafficDemand, Path> routingSolution, String filename){

        try{

            FileWriter logfw = new FileWriter(filename + "_Logfile.txt" ,true);
            BufferedWriter logbw = new BufferedWriter(logfw);
            PrintWriter logout = new PrintWriter(logbw);
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            logout.println(sdf.format(cal.getTime()) + " Objective Value: " +
                    getTrafficFromDemands(routingSolution));
            logout.flush();

            FileWriter solnRfw = new FileWriter(filename + "_RoutingSolution.txt" ,
                    true);
            BufferedWriter solnRbw = new BufferedWriter(solnRfw);
            PrintWriter solnRout = new PrintWriter(solnRbw); solnRout.println("u v i j");

            for (TrafficDemand trafficDemand : routingSolution.keySet()) {
                for (Edge edge : routingSolution.get(trafficDemand).getEdges()) {
                    solnRout.println(trafficDemand.getSource().getLabel() + " " +
                            trafficDemand.getDestination().getLabel() + " " +
                            edge.getSource().getLabel() + " " +
                            edge.getDestination().getLabel());
                }
            }
            solnRout.flush();


        }
        catch (IOException e){
            System.out.println("Error " + e + "while writing solution in Traffic Heuristic");
        }

    }



}
