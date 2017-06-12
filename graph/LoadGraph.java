package statefulsharding.graph;

import java.io.*;

/**
 * Created by build on 08/11/16.
 */

public class LoadGraph {

    public static ListGraph LoadGraph(String fileName){

        ListGraph graph = new ListGraph();

        try{
            FileInputStream fstream = new FileInputStream(fileName);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                String[] tokens = strLine.split(" ");
                if (!graph.checkVertex(Integer.parseInt(tokens[0])))
                    graph.addVertex(Integer.parseInt(tokens[0]));
                if (!graph.checkVertex(Integer.parseInt(tokens[1])))
                    graph.addVertex(Integer.parseInt(tokens[1]));
                graph.insertEdge(Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1]),Integer.parseInt(tokens[2]));
            }
            in.close();
        }
        catch(FileNotFoundException ex){
            ex.printStackTrace();
        }
        catch(IOException ex) {
            ex.printStackTrace();;
        }
        return graph;
    }

    public static ListGraph GraphParserJ(String fileName, int capacity, boolean decrement){

        ListGraph graph = new ListGraph();

        try{
            FileInputStream fstream = new FileInputStream(fileName);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            int source;
            int destination;
            while ((strLine = br.readLine()) != null)   {
                String[] tokens = strLine.split(",");

                if(decrement) {
                    source = Integer.parseInt(tokens[0])-1;
                    destination = Integer.parseInt(tokens[1])-1;
                }
                else{
                    source = Integer.parseInt(tokens[0]);
                    destination = Integer.parseInt(tokens[1]);
                }

                if (!graph.checkVertex(source))
                    graph.addVertex(source);

                if (!graph.checkVertex(destination))
                    graph.addVertex(destination);

                graph.insertEdge(source,destination,capacity);

            }
            in.close();
        }
        catch(FileNotFoundException ex){
            ex.printStackTrace();
        }
        catch(IOException ex) {
            ex.printStackTrace();;
        }
        return graph;
    }
}
