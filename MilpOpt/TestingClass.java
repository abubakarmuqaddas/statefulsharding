package statefulsharding.MilpOpt;

import statefulsharding.State.GenerateStates;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;


public class TestingClass {

    public static void main(String[] args){

        String mainfile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/" +
                "analysis/";

        for(int size=3 ; size<=9 ; size++){
            for(int traffic=1 ; traffic<=10 ; traffic++){
                for(int numStates = 1 ; numStates <=7; numStates++){
                     for(int dependency = 1 ; dependency<=10; dependency++){

                        String stateFilename = mainfile + "StateDependencies/" + "StateDependencies_"
                                + numStates + "_" + dependency + ".txt";

                        int numLines=0;
                        try {
                            LineNumberReader lnr = new LineNumberReader(new FileReader(new File(stateFilename)));
                            lnr.skip(Long.MAX_VALUE);
                            numLines=lnr.getLineNumber();
                            lnr.close();
                        }
                        catch(IOException e){//
                        }

                        System.out.println(size + " " + traffic + " " + numStates + " " + dependency);

                        String combFileName = mainfile + "Size_TfcNo_NumStates_DependencyNo/"
                                + size + "_" +  traffic +"_" + numStates+"_" + dependency +".txt";

                        try {
                            FileWriter partfw = new FileWriter(combFileName, true);
                            BufferedWriter partbw = new BufferedWriter(partfw);
                            PrintWriter partout = new PrintWriter(partbw);

                            for(int run = 1 ; run<=10 ; run++) {

                                for (int currentSize = 1; currentSize <= size * size; currentSize++) {
                                    int randomNum = ThreadLocalRandom.current().nextInt(0, numLines);
                                    System.out.print(randomNum + ",");
                                    partout.print(randomNum + ",");
                                }

                                System.out.println();
                                partout.println();
                            }

                            partout.flush();

                        }
                        catch (IOException e){
                            /*  */
                        }



                    }
                }
            }
        }

        


    }
}
