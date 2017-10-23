package statefulsharding;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficGenerator;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.LoadGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.graph.algorithms.ShortestPath;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.heuristic.TrafficHeuristic;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.IntStream;

import static statefulsharding.heuristic.TrafficHeuristic.hType.fixedcopies;


public class TestingClass {

    public static void main(String[] args) {

        for(int size=10 ; size<=10 ; size++) {

            ManhattanGraphGen m = new ManhattanGraphGen(size, Integer.MAX_VALUE,
                    ManhattanGraphGen.mType.UNWRAPPED, false, true);

            ListGraph graph = m.getManhattanGraph();

            for(int numCopies = 10 ; numCopies<=10 ; numCopies++) {

                for(int partitionRun = 10 ; partitionRun<=10 ; partitionRun++) {

                    System.out.println("Size: " + size + ", NumCopy: " + numCopies + ", Partition: " + partitionRun);

                    String PartitionFile = "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/" +
                                            "MANHATTAN-UNWRAPPED-Partitions/" +
                                            "MANHATTAN-UNWRAPPED-Partitions_Size_" + size +
                                            "_NumCopies_" + numCopies + "_PartitionRun_" + partitionRun;

                    ArrayList<Vertex> copies = Partitioning.getCopies(graph,PartitionFile,false);

                    for (Vertex copy : copies) {
                        System.out.println(copy.getLabel());
                    }


                }


            }


        }







    }





}
