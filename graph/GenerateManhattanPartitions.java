package statefulsharding.graph;

import statefulsharding.graph.algorithms.Partitioning;
import statefulsharding.randomgraphgen.ManhattanGraphGen;

import java.util.HashMap;


public class GenerateManhattanPartitions {

    public static void main(String[] args) {

        for (int size = 5; size <= 10; size++) {

            ManhattanGraphGen m = new ManhattanGraphGen(size, Integer.MAX_VALUE,
                    ManhattanGraphGen.mType.UNWRAPPED, false, true);

            ListGraph graph = m.getManhattanGraph();

            for (int numCopies = 1; numCopies <= 16; numCopies++) {

                for (int partitionRun = 1; partitionRun <= 100; partitionRun++) {

                    HashMap<Vertex, ListGraph> partitions =
                            Partitioning.EvolutionaryPartition(
                                    graph,
                                    3,
                                    50,
                                    "random",
                                    "betweenness",
                                    null,
                                    false,
                                    true,
                                    "../Dropbox/PhD_Work/Stateful_SDN/snapsharding/analysis/" +
                                            "MANHATTAN-UNWRAPPED-Partitions/" +
                                            "MANHATTAN-UNWRAPPED-Partitions_Size_" + size +
                                            "_NumCopies_" + numCopies + "_PartitionRun_" + partitionRun
                            );


                }


            }


        }


    }
}
