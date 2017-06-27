package statefulsharding.MilpOpt;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import statefulsharding.Pair;
import statefulsharding.State.StateCopy;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;
import statefulsharding.graph.algorithms.getNCombinations;
import statefulsharding.heuristic.StateSync;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by root on 5/23/17.
 */
public class StateSyncOptimization {

    private ListGraph graph;
    private IloCplex cplex;
    private StateStore stateStore;
    private HashMap<Vertex, HashMap<StateCopy, IloNumVar>> Placement;
    private HashMap<LinkedList<StateCopy>, HashMap<Edge, IloNumVar>> StateSyncFlows;

    public StateSyncOptimization(ListGraph graph, OptimizationOptions options, StateStore stateStore){

        this.graph = graph;
        this.stateStore = stateStore;
        Placement = new HashMap<>();
        StateSyncFlows = new HashMap<>();

        try {
            this.cplex = new IloCplex();
        }
        catch(IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
        buildModel(options.isVerbose(), options.isFixConstraints());

    }

    private void buildModel(boolean outputRequired, boolean fixConstraints){


        try{

            if (!outputRequired) {
                cplex.setOut(null);
            }

            /**
             *
             * Defining StateVariable Placement variable
             *
             * P_{S_fn}
             *
             */


            for (Vertex vertex : graph.getVertices()) {
                Placement.put(vertex, new HashMap<>());
                for (StateVariable stateVariable : stateStore.getStateVariables())
                    for (StateCopy stateCopy : stateStore.getStateCopies(stateVariable))
                        Placement.get(vertex)
                                .put(stateCopy, cplex.boolVar("P_" + stateCopy.getStateCopyString()
                                        + "_" + vertex.getLabel()));
            }

            /**
             *
             * Defining StateSync Flows
             *
             * R_{state_{c_1}state_{c_2}_ij}
             *
             */

            for (StateVariable stateVariable : stateStore.getStateVariables()){

                if(!(stateVariable.getCopies()>1))
                    continue;

                LinkedList<Integer> temp = new LinkedList<>();

                for(int i=1 ; i<=stateVariable.getCopies() ; i++){
                    temp.add(i);
                }

                LinkedList<LinkedList<Integer>> comb = getNCombinations.getCombinations(temp, 2);

                for(LinkedList<Integer> combLeftRight : comb){

                    /**
                     * Left to right c_a --> c_b
                     */

                    LinkedList<StateCopy> leftRight = new LinkedList<>();
                    leftRight.add(stateStore.getStateCopy(stateVariable, combLeftRight.get(0)));
                    leftRight.add(stateStore.getStateCopy(stateVariable, combLeftRight.get(1)));
                    StateSyncFlows.put(leftRight, new HashMap<>());

                    /**
                     * Right to left c_b --> c_a
                     */
                    LinkedList<StateCopy> rightLeft = new LinkedList<>();
                    rightLeft.add(stateStore.getStateCopy(stateVariable, combLeftRight.get(1)));
                    rightLeft.add(stateStore.getStateCopy(stateVariable, combLeftRight.get(0)));
                    StateSyncFlows.put(rightLeft, new HashMap<>());

                    for(Edge edge : graph.getallEdges()){
                        StateSyncFlows.get(leftRight).put(edge, cplex.boolVar("Rcij_" +
                                                        StateCopyCombinationToString(leftRight)+ "_" +
                                                        edge.getSource().getLabel() + "_" +
                                                        edge.getDestination().getLabel()));

                        StateSyncFlows.get(rightLeft).put(edge, cplex.boolVar("Rcij_" +
                                                        StateCopyCombinationToString(rightLeft)+ "_" +
                                                        edge.getSource().getLabel() + "_" +
                                                        edge.getDestination().getLabel()));
                    }
                }
            }

            setObjective();

            /////////////////////////////// CONSTRAINTS //////////////////////////////////

            /**
             * All constraints related to state synchronization flows!
             */

            HashMap<List<StateCopy>, HashMap<Vertex, IloLinearNumExpr>> outStateSyncFlows = new HashMap<>();
            HashMap<List<StateCopy>, HashMap<Vertex, IloLinearNumExpr>> inStateSyncFlows = new HashMap<>();

            /**
             *  Initialize all in and out state sync flows at all nodes
             *
             *  out: \sum_j R_{state_{c_1} state{c_2} nj}
             *  in:  \sum_j R_{state_{c_1} state{c_2} in}
             *
             */

            for(LinkedList<StateCopy> stateCopies : StateSyncFlows.keySet()){
                outStateSyncFlows.put(stateCopies, new HashMap<>());
                inStateSyncFlows.put(stateCopies, new HashMap<>());

                for(Vertex vertex : graph.getVertices()){
                    outStateSyncFlows.get(stateCopies).putIfAbsent(vertex, cplex.linearNumExpr());

                    for(Vertex successor : graph.getSuccessors(vertex)){

                        outStateSyncFlows
                                .get(stateCopies)
                                .get(vertex)
                                .addTerm(1.0, StateSyncFlows
                                        .get(stateCopies)
                                        .get(graph.getEdge(vertex, successor))
                                );
                    }

                    inStateSyncFlows.get(stateCopies).putIfAbsent(vertex, cplex.linearNumExpr());

                    for(Vertex predecessor : graph.getPredecessors(vertex)){
                        inStateSyncFlows
                                .get(stateCopies)
                                .get(vertex)
                                .addTerm(1.0, StateSyncFlows
                                        .get(stateCopies)
                                        .get(graph.getEdge(predecessor, vertex))
                                );
                    }
                }
            }

            /**
             *  \sum_j R_{state_{c_1} state{c_2} nj} \ge P_{state_{c_1}n}
             */

            int i=0;
            for(List<StateCopy> stateCopies : StateSyncFlows.keySet()) {
                for (Vertex vertex : graph.getVertices()) {

                    cplex.addGe(
                            outStateSyncFlows.get(stateCopies).get(vertex),
                            Placement.get(vertex).get(stateCopies.get(0)),
                            "stateSync1_" + i++
                    );

                }
            }

            /**
             *  P_{state_{c_2}n} + \sum_j R_{state_{c_1} state{c_2} nj} \le 1
             */

            i=0;
            for(List<StateCopy> stateCopies : StateSyncFlows.keySet()) {
                for (Vertex vertex : graph.getVertices()) {


                    /*
                    cplex.addLe(
                            cplex.sum(
                                    outStateSyncFlows.get(stateCopies).get(vertex),
                                    Placement.get(vertex).get(stateCopies.get(1))
                            ),
                            1.0,
                            "stateSync2_" + i++
                    );
                    */


                }
            }

            /**
             *  \sum_i R_{state_{c_1} state{c_2} in} \ge P_{state_{c_2}n}
             */

            i=0;
            for(List<StateCopy> stateCopies : StateSyncFlows.keySet()) {
                for (Vertex vertex : graph.getVertices()) {

                    cplex.addGe(
                            inStateSyncFlows.get(stateCopies).get(vertex),
                            Placement.get(vertex).get(stateCopies.get(1)),
                            "stateSync3_" + i++
                    );
                }
            }


            /**
             *  \sum_j R_{state_{c_1} state{c_2} nj} + P_{state_{c_2}n}
             *  \ge
             *  \sum_i R_{state_{c_1} state{c_2} in}
             */

            i=0;
            for(List<StateCopy> stateCopies : StateSyncFlows.keySet()) {
                for (Vertex vertex : graph.getVertices()) {


                    cplex.addGe(
                            cplex.sum(
                                    outStateSyncFlows.get(stateCopies).get(vertex),
                                    Placement.get(vertex).get(stateCopies.get(1))
                            ),
                            inStateSyncFlows.get(stateCopies).get(vertex),
                            "stateSync4_" + i++
                    );

                }
            }

            /**
             *  \sum_i R_{state_{c_1} state{c_2} in} \ge \sum_j R_{state_{c_1} state{c_2} nj}
             */

            i=0;
            for(List<StateCopy> stateCopies : StateSyncFlows.keySet()) {
                for (Vertex vertex : graph.getVertices()) {

                    cplex.addGe(
                            inStateSyncFlows.get(stateCopies).get(vertex),
                            outStateSyncFlows.get(stateCopies).get(vertex),
                            "stateSync5_" + i++
                    );

                }
            }

            if (fixConstraints)
                FixVariables();

            cplex.exportModel("test_StateSyncOpt.lp");

        }
        catch(IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }



    }

    public boolean optimize(){
        try{
            return cplex.solve();
        }
        catch  (IloException e)  {
            System.out.println("Exception " + e + " caught while optimizing the model");
        }
        return false;
    }

    private void setObjective(){
        try {
            IloLinearNumExpr objective = cplex.linearNumExpr();


            for (LinkedList<StateCopy> stateCopies : StateSyncFlows.keySet()) {
                for (Edge edge : StateSyncFlows.get(stateCopies).keySet()) {
                    objective.addTerm(1.0, StateSyncFlows
                            .get(stateCopies)
                            .get(edge));
                }
            }

            cplex.addMinimize(objective);
        }
        catch  (IloException e)  {
            System.err.println("Concert exception caught:"  +  e);
        }
    }

    public void printSolution(){

        try {

            System.out.println("####################################################################");
            System.out.println("#                  State Sync Optimization                         #");
            System.out.println("####################################################################");

            System.out.println("obj = " + getObjectiveValue());

            for (Vertex vertex : graph.getVertices()) {
                for (StateVariable stateVariable : stateStore.getStateVariables()) {
                    for (StateCopy stateCopy : stateStore.getStateCopies(stateVariable)) {
                        if (cplex.getValue(Placement.get(vertex).get(stateCopy)) > 0) {
                            System.out.println(
                                    "P_" + stateCopy.getStateCopyString()
                                            + "_" + vertex.getLabel() + " " +
                                            cplex.getValue(Placement.get(vertex).get(stateCopy))
                            );
                        }
                    }
                }
            }

            System.out.println();


            for (LinkedList<StateCopy> stateCopies: StateSyncFlows.keySet()) {
                for (Edge edge : StateSyncFlows.get(stateCopies).keySet()) {
                    if(cplex.getValue(StateSyncFlows.get(stateCopies).get(edge)) > 0) {
                        System.out.println("Rcij_" + StateCopyCombinationToString(stateCopies)
                                + "_" + edge.getSource().getLabel()
                                + "_" + edge.getDestination().getLabel() + " = " +
                                cplex.getValue(StateSyncFlows.get(stateCopies).get(edge)));
                    }
                }
            }
                System.out.println();


        }
        catch (IloException e){
            System.err.println("Concert exception caught while optimizing:"  +  e);
        }
    }

    public double getObjectiveValue(){
        double objectiveValue = 0.0;
        try {
            for (LinkedList<StateCopy> stateCopies: StateSyncFlows.keySet()) {
                for (Edge edge : StateSyncFlows.get(stateCopies).keySet()) {
                    if (cplex.getValue(StateSyncFlows.get(stateCopies).get(edge)) > 0) {
                        if (!edge.getSource().equals(edge.getDestination())) {
                            objectiveValue += 1.0*
                                    cplex.getValue(StateSyncFlows.get(stateCopies).get(edge));
                        }
                    }
                }
            }

        }
        catch (IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }

        return objectiveValue;
    }

    private String StateCopyCombinationToString(List<StateCopy> stateCopyCombination){

        String sccString = "";

        for (StateCopy stateCopy : stateCopyCombination) {
            sccString = sccString + stateCopy.getLabel() + stateCopy.getCopyNumber() + "_";
        }


        return sccString.substring(0, sccString.length()-1);



    }

    private void FixVariables() {

        try {


            for (Vertex vertex : graph.getVertices()) {
                for (StateVariable stateVariable : stateStore.getStateVariables()) {
                    for (StateCopy stateCopy : stateStore.getStateCopies(stateVariable)) {

                        if(stateCopy.getState().getLabel().equals("a") &&
                                stateCopy.getCopyNumber()==1 &&
                                vertex.getLabel()==0){
                            cplex.addEq(Placement.get(vertex).get(stateCopy),1);
                        }

                        else if(stateCopy.getState().getLabel().equals("a") &&
                                stateCopy.getCopyNumber()==2 &&
                                vertex.getLabel()==2){
                            cplex.addEq(Placement.get(vertex).get(stateCopy),1);
                        }

                        else if(stateCopy.getState().getLabel().equals("b") &&
                                stateCopy.getCopyNumber()==1 &&
                                vertex.getLabel()==6){
                            cplex.addEq(Placement.get(vertex).get(stateCopy),1);
                        }
                        else{
                            cplex.addEq(Placement.get(vertex).get(stateCopy),0);
                        }
                    }
                }
            }

            for (List<StateCopy> stateCopies : StateSyncFlows.keySet()) {
                for (Edge edge : StateSyncFlows.get(stateCopies).keySet()) {
                    
                    if (edge.getSource().getLabel() == 0 && edge.getDestination().getLabel() == 1
                            && stateCopies.get(0).getLabel().equals("a")
                            && stateCopies.get(0).getCopyNumber()==1
                            && stateCopies.get(1).getLabel().equals("a")
                            && stateCopies.get(1).getCopyNumber()==2)
                        cplex.addEq(StateSyncFlows.get(stateCopies).get(edge), 1.0);

                    else if(edge.getSource().getLabel() == 1 && edge.getDestination().getLabel() == 2
                            && stateCopies.get(0).getLabel().equals("a")
                            && stateCopies.get(0).getCopyNumber()==1
                            && stateCopies.get(1).getLabel().equals("a")
                            && stateCopies.get(1).getCopyNumber()==2)
                        cplex.addEq(StateSyncFlows.get(stateCopies).get(edge), 1.0);

                    else if(edge.getSource().getLabel() == 2 && edge.getDestination().getLabel() == 1
                            && stateCopies.get(0).getLabel().equals("a")
                            && stateCopies.get(0).getCopyNumber()==2
                            && stateCopies.get(1).getLabel().equals("a")
                            && stateCopies.get(1).getCopyNumber()==1)
                        cplex.addEq(StateSyncFlows.get(stateCopies).get(edge), 1.0);

                    else if(edge.getSource().getLabel() == 1 && edge.getDestination().getLabel() == 0
                            && stateCopies.get(0).getLabel().equals("a")
                            && stateCopies.get(0).getCopyNumber()==2
                            && stateCopies.get(1).getLabel().equals("a")
                            && stateCopies.get(1).getCopyNumber()==1)
                        cplex.addEq(StateSyncFlows.get(stateCopies).get(edge), 1.0);

                    else
                        cplex.addEq(StateSyncFlows.get(stateCopies).get(edge), 0.0);

                }
            }








            /*
            StateCopy A1 = stateStore.getStateCopy("A",1);
            StateCopy B1 = stateStore.getStateCopy("B",1);

            for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                    for (StateCopy stateCopy : stateCopyCombination) {
                        for (Edge edge : graph.getallEdges()) {
                            if(edge.getSource().getLabel()==1 && edge.getDestination().getLabel()==2) {
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(A1).get(edge), 1.0);
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(B1).get(edge), 0.0);
                                continue;
                            }
                            if(edge.getSource().getLabel()==2 && edge.getDestination().getLabel()==3) {
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(A1).get(edge), 1.0);
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(B1).get(edge), 0.0);
                                continue;
                            }
                            if(edge.getSource().getLabel()==3 && edge.getDestination().getLabel()==7) {
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(A1).get(edge), 1.0);
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(B1).get(edge), 1.0);
                                continue;
                            }
                            if(edge.getSource().getLabel()==7 && edge.getDestination().getLabel()==11) {
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(A1).get(edge), 1.0);
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(B1).get(edge), 1.0);
                                continue;
                            }
                            if(edge.getSource().getLabel()==0 && edge.getDestination().getLabel()==1) {
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(A1).get(edge), 0.0);
                                cplex.addEq(PTracker.get(trafficDemand).get(A1B1).get(B1).get(edge), 0.0);
                                continue;
                            }
                            cplex.addEq(PTracker
                                    .get(trafficDemand)
                                    .get(stateCopyCombination)
                                    .get(stateCopy)
                                    .get(edge), 0.0);
                            cplex.addEq(PTracker
                                    .get(trafficDemand)
                                    .get(stateCopyCombination)
                                    .get(stateCopy)
                                    .get(edge), 0.0);
                        }
                    }
                }
            }

            */


        }
        catch (IloException e) {
            System.out.println("Caught IloException error" + e);
        }



    }


}


