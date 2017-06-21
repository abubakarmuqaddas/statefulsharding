package statefulsharding.Traffic;

import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by build on 09/11/16.
 */
public class TrafficGenerator {

    public static void RandomAlltoAll(ListGraph graph, double load, double capacity, TrafficStore trafficStore){

        Random RandomNumberGen = new Random();

        for (Vertex source : graph.getVertices()) {
            for (Vertex destination : graph.getVertices()) {

                double demand=load*RandomNumberGen.nextInt((int)capacity);
                trafficStore.addTrafficDemand(new TrafficDemand(source,destination,demand));

            }
        }
    }

    public static void Deterministic(ListGraph graph, double demand, TrafficStore trafficStore,
                                     boolean avoidSelf){
        for (Vertex source : graph.getVertices()) {
            for (Vertex destination : graph.getVertices()) {
                if(!avoidSelf || !source.equals(destination)) {
                    trafficStore.addTrafficDemand(new TrafficDemand(source, destination, demand));
                }
            }
        }
    }

    public static void FisherYates(ListGraph graph, double demand, TrafficStore trafficStore){

        int[] range = IntStream.rangeClosed(0, graph.getNumVertices()-1).toArray();

        // System.out.println(Arrays.toString(range));

        Random RandomNumberGen = new Random();

        for (int i = range.length - 1; i > 0; i--){
            int index = RandomNumberGen.nextInt(i + 1);
            // Simple swap
            int a = range[index];
            range[index] = range[i];
            range[i] = a;
        }

        // System.out.println(Arrays.toString(range));

        for (int i=0 ; i<graph.getNumVertices() ; i++){
            trafficStore.addTrafficDemand(new TrafficDemand(graph.getVertex(i),graph.getVertex(range[i]),demand));
        }
    }

    public static void fromFile(ListGraph graph, TrafficStore trafficStore, String filename){

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            String line;
            String[] strNums;
            int i = 1;
            int source;
            int destination;
            double demand;

            while ((line = br.readLine()) != null) {
                // process the line.
                if (i!=1){
                    strNums = line.split("\\s");

                    source=Integer.parseInt(strNums[0]);
                    destination=Integer.parseInt(strNums[1]);
                    demand=Double.parseDouble(strNums[2]);

                    trafficStore.addTrafficDemand(
                            new TrafficDemand(
                            graph.getVertex(source),
                            graph.getVertex(destination),
                                    demand
                    ));
                }
                i++;
            }
        }
        catch(FileNotFoundException e){
            System.out.println("FileNotFoundException " + e + ", Filename: " + filename);
        }
        catch(IOException e){
            System.out.println("IOException " + e);
        }


    }

    public static void fromFileN(ListGraph graph, TrafficStore trafficStore, String filename, int numFlows){

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            String line;
            String[] strNums;
            int i = 0;

            LinkedList<Integer> source = new LinkedList<>();
            LinkedList<Integer> destination = new LinkedList<>();
            LinkedList<Integer> demand = new LinkedList<>();

            while ((line = br.readLine()) != null) {
                // process the line.
                if (i!=0){
                    strNums = line.split("\\s");

                    source.add(Integer.parseInt(strNums[0]));
                    destination.add(Integer.parseInt(strNums[1]));
                    demand.add(Integer.parseInt(strNums[2]));
                }
                i++;
            }

            int[] range = IntStream.rangeClosed(0, graph.getNumVertices()-1).toArray();
            Random RandomNumberGen = new Random();

            for (int j = range.length - 1; j > 0; j--){
                int index = RandomNumberGen.nextInt(j + 1);
                // Simple swap
                int a = range[index];
                range[index] = range[j];
                range[j] = a;
            }

            for(int h=0 ; h<numFlows ; h++){
                trafficStore.addTrafficDemand(
                        new TrafficDemand(
                                graph.getVertex(source.get(range[h])),
                                graph.getVertex(destination.get(range[h])),
                                demand.get(range[h])
                        ));
            }


        }
        catch(FileNotFoundException e){
            System.out.println("FileNotFoundException " + e + ", Filename: " + filename);
        }
        catch(IOException e){
            System.out.println("IOException " + e);
        }


    }

    public static void fromFileLinebyLine(ListGraph graph, TrafficStore trafficStore,int trafficLine,
                                          double demand, boolean decrement, String filename){

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            String line;
            String[] strNums;
            int i = 1;
            int destination;

            while ((line = br.readLine()) != null) {
                // process the line.
                if (i==trafficLine){

                    strNums = line.split(",");

                    for (int source=0 ; source<graph.getNumVertices() ; source++){

                        if(decrement)
                            destination = Integer.parseInt(strNums[source])-1;
                        else
                            destination = Integer.parseInt(strNums[source]);

                        trafficStore.addTrafficDemand(
                                new TrafficDemand(
                                        graph.getVertex(source),
                                        graph.getVertex(destination),
                                        demand));
                    }
                    break;
                }
                i++;
            }
        }
        catch(FileNotFoundException e){
            System.out.println("FileNotFoundException " + e + ", Filename: " + filename);
        }
        catch(IOException e){
            System.out.println("IOException " + e);
        }


    }

    public static void pairsfromfileFixDemand(ListGraph graph, TrafficStore trafficStore,
                                              String filename, double demand){

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            String line;
            String[] strNums;
            int i = 1;
            int source;
            int destination;

            while ((line = br.readLine()) != null) {
                // process the line.
                if (i!=1){
                    strNums = line.split("\\s");

                    source=Integer.parseInt(strNums[0]);
                    destination=Integer.parseInt(strNums[1]);

                    trafficStore.addTrafficDemand(
                            new TrafficDemand(
                                    graph.getVertex(source),
                                    graph.getVertex(destination),
                                    demand
                            ));
                }
                i++;
            }
        }
        catch(FileNotFoundException e){
            System.out.println("FileNotFoundException " + e + ", Filename: " + filename);
        }
        catch(IOException e){
            System.out.println("IOException " + e);
        }


    }

    public static void pairsfromFileGeometricDemand(ListGraph graph, TrafficStore trafficStore,
                                                    String filename, int mean){

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            String line;
            String[] strNums;
            int i = 1;
            int source;
            int destination;
            double demand;

            while ((line = br.readLine()) != null) {
                // process the line.
                if (i!=1){
                    strNums = line.split("\\s");

                    source=Integer.parseInt(strNums[0]);
                    destination=Integer.parseInt(strNums[1]);
                    demand=geometricDistDemand(mean);

                    trafficStore.addTrafficDemand(
                            new TrafficDemand(
                                    graph.getVertex(source),
                                    graph.getVertex(destination),
                                    demand
                            ));
                }
                i++;
            }
        }
        catch(FileNotFoundException e){
            System.out.println("FileNotFoundException " + e + ", Filename: " + filename);
        }
        catch(IOException e){
            System.out.println("IOException " + e);
        }


    }

    public static int geometricDistDemand(double mean){

        Random RandomNumberGen = new Random();

        return (int)Math.ceil(Math.log(1-RandomNumberGen.nextDouble())/Math.log(1-1/mean));

    }

    public static void writeTraffic(TrafficStore trafficStore, String filename){

        try{
            FileWriter tfcfw = new FileWriter(filename, true);
            BufferedWriter tfcbw = new BufferedWriter(tfcfw);
            PrintWriter tfcout = new PrintWriter(tfcbw);
            tfcout.println("u v d");

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                tfcout.println(trafficDemand.getSource().getLabel() +
                        " " + trafficDemand.getDestination().getLabel() +
                        " " + trafficDemand.getDemand());
            }
            tfcout.flush();
        }

        catch(IOException e){
            System.out.println("Cannot write Traffic");
        }


    }

    public static LinkedHashMap<TrafficDemand,Path> fromFileRouting(ListGraph graph, TrafficStore trafficStore,
                                                                    String filename){

        LinkedHashMap<TrafficDemand, Path> routingSolution = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            String line;
            String[] strNums;
            int i = 1;
            int source;
            int destination;
            int vertexA;
            int vertexB;

            while ((line = br.readLine()) != null) {
                // process the line.
                if (i!=1){
                    strNums = line.split(" ");

                    source = Integer.parseInt(strNums[0]);
                    destination = Integer.parseInt(strNums[1]);
                    vertexA = Integer.parseInt(strNums[2]);
                    vertexB = Integer.parseInt(strNums[3]);

                    TrafficDemand trafficDemand =
                            trafficStore.getTrafficDemand(graph.getVertex(source),graph.getVertex(destination));

                    routingSolution.putIfAbsent(trafficDemand, new Path());
                    Path path = routingSolution.get(trafficDemand);

                    path.add(graph.getEdge(graph.getVertex(vertexA),graph.getVertex(vertexB)));
                }
                i++;
            }

            return routingSolution;

        }
        catch(FileNotFoundException e){
            System.out.println("FileNotFoundException " + e + ", Filename: " + filename);
        }
        catch(IOException e){
            System.out.println("IOException " + e);
        }

        return null;

    }



}