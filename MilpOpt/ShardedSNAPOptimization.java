package statefulsharding.MilpOpt;

import ilog.concert.*;
import ilog.cplex.*;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Path;
import statefulsharding.graph.Vertex;


import java.util.*;

/**
 * Class for performing Sharded SNAP Optimization
 */

public class ShardedSNAPOptimization {

    private ListGraph graph;
    private TrafficStore trafficStore;
    private int copies;
    private IloCplex cplex;
    private HashMap<TrafficDemand, HashMap<Integer, HashMap<Edge, IloNumVar>>> Flows;
    private HashMap<TrafficDemand, HashMap<Integer, HashMap<Edge, IloNumVar>>> PTracker;
    private HashMap<Vertex, HashMap<Integer, IloNumVar>> Placement;
    private HashMap<TrafficDemand, HashMap<Integer, IloLinearNumExpr>> X;
    private boolean objectiveValueFixed;
    private double objectiveValue;
    private OptimizationOptions options;

    public ShardedSNAPOptimization(ListGraph graph, TrafficStore trafficStore,
                                   OptimizationOptions options){
        this.graph = graph;
        this.trafficStore = trafficStore;
        this.copies = options.getCopies();
        Flows = new HashMap<>();
        Placement = new HashMap<>();
        PTracker = new HashMap<>();
        X = new HashMap<>();
        objectiveValueFixed = false;
        this.options = options;

        try {
            this.cplex = new IloCplex();
        }
        catch(IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
        buildModel(options.isVerbose(),options.isFixConstraints());
    }

    private void buildModel(boolean outputRequired, boolean fixConstraints){
        try {
            if(!outputRequired){
                cplex.setOut(null);
            }

            Flows.clear();
            Placement.clear();
            X.clear();
            cplex.clearModel();

            HashMap<TrafficDemand, IloLinearNumExpr> SourceIncoming = new HashMap<>();
            HashMap<TrafficDemand, IloLinearNumExpr> SourceOutgoing = new HashMap<>();
            HashMap<TrafficDemand, IloLinearNumExpr> DestinationIncoming = new HashMap<>();
            HashMap<TrafficDemand, IloLinearNumExpr> DestinationOutgoing = new HashMap<>();
            HashMap<TrafficDemand, HashMap<Integer,HashMap<Vertex,IloLinearNumExpr>>> IncomingFlows =
                    new HashMap<>();
            HashMap<TrafficDemand, HashMap<Integer,HashMap<Vertex,IloLinearNumExpr>>> OutgoingFlows =
                    new HashMap<>();
            HashMap<TrafficDemand, HashMap<Integer,HashMap<Vertex,IloLinearNumExpr>>> TrackIncoming = new HashMap<>();
            HashMap<TrafficDemand, HashMap<Integer,HashMap<Vertex,IloLinearNumExpr>>> TrackOutgoing = new HashMap<>();
            HashMap<TrafficDemand, HashMap<Integer, HashMap<Vertex, IloNumVar>>> Y = new HashMap<>();

            /**
             * Defining Routing Variable
             * R_{cuvij}
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                Flows.put(trafficDemand, new HashMap<>());
                for (int copy = 1; copy <= copies; copy++) {
                    Flows.get(trafficDemand).put(copy,new HashMap<>());
                    for (Edge edge : graph.getallEdges()) {
                        Flows.get(trafficDemand).get(copy)
                                .put(edge, cplex.boolVar("Rcuvij_" + copy +"_" +
                                        trafficDemand.getSource().getLabel() + "_" +
                                        trafficDemand.getDestination().getLabel() + "_" +
                                        edge.getSource().getLabel() + "_" +
                                        edge.getDestination().getLabel()));
                    }
                }
            }

            /**
             *
             * Defining Flow Tracker
             *
             * P_{cuvij}
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                PTracker.put(trafficDemand, new HashMap<>());
                for (int copy = 1; copy <= copies; copy++) {
                    PTracker.get(trafficDemand).put(copy,new HashMap<>());
                    for (Edge edge : graph.getallEdges()) {
                        PTracker.get(trafficDemand).get(copy)
                                .put(edge, cplex.boolVar("Pcuvij_" + copy +"_" +
                                        trafficDemand.getSource().getLabel() + "_" +
                                        trafficDemand.getDestination().getLabel() + "_" +
                                        edge.getSource().getLabel() + "_" +
                                        edge.getDestination().getLabel()));
                    }
                }
            }

            /**
             *
             * Defining Y_{cuvn}
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                Y.put(trafficDemand,new HashMap<>());
                for (int copy = 1; copy <= copies; copy++) {
                    Y.get(trafficDemand).put(copy,new HashMap<>());
                    for (Vertex vertex : graph.getVertices()) {
                        Y.get(trafficDemand).get(copy)
                                .put(vertex, cplex.boolVar("Ycuvn_" + copy + "_" + trafficDemand.getSource().getLabel()
                                        + "_" + trafficDemand.getDestination().getLabel() + "_"+  vertex.getLabel()));
                    }
                }
            }

            /**
             *
             * Defining X_{cuv}
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                X.put(trafficDemand, new HashMap<>());
                for (int copy = 1; copy <= copies; copy++) {
                    X.get(trafficDemand).put(copy,cplex.linearNumExpr());
                }
            }



            /**
             *
             * Defining StateVariable Placement variable
             *
             * P_{cn}
             *
             */

            for (Vertex vertex : graph.getVertices()){
                Placement.put(vertex, new HashMap<>());
                for (int copy = 1; copy <= copies; copy++) {
                    Placement.get(vertex).put(copy,cplex.boolVar("P_" + copy +"_" + vertex.getLabel()));
                }
            }

            /**
             * Calling the objective function to be minimized
             */

            setObjective();

            /////////////////////////////// CONSTRAINTS //////////////////////////////////

            /**
             *
             * Everything that exits the source - everything that enters = 1
             * \sum_c \left( \sum_j R_{cuvuj} - \sum_i R_{cuviu}\right) = 1
             * \forall (u \ne j \lor u = v)
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){

                SourceIncoming.putIfAbsent(trafficDemand,cplex.linearNumExpr());
                SourceOutgoing.putIfAbsent(trafficDemand,cplex.linearNumExpr());

                for (Integer copy : Flows.get(trafficDemand).keySet()){
                    for (Vertex vertex : graph.getSuccessors(trafficDemand.getSource())) {
                        if (!vertex.equals(trafficDemand.getSource()) ||
                                trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                            SourceOutgoing
                                    .get(trafficDemand)
                                    .addTerm(1.0,Flows
                                            .get(trafficDemand)
                                            .get(copy)
                                            .get(graph.getEdge(trafficDemand.getSource(), vertex)));
                        }
                    }
                    if (!trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                        for (Vertex vertex : graph.getPredecessors(trafficDemand.getSource())) {
                            if (!vertex.equals(trafficDemand.getSource()) ||
                                    trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                                SourceIncoming
                                        .get(trafficDemand)
                                        .addTerm(1.0, Flows
                                                .get(trafficDemand)
                                                .get(copy)
                                                .get(graph.getEdge(vertex, trafficDemand.getSource())));
                            }
                        }
                    }
                }

                if (trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                    cplex.addEq(SourceOutgoing.get(trafficDemand),1);
                }
                else{
                    cplex.addEq(SourceOutgoing.get(trafficDemand),
                            cplex.sum(
                                    SourceIncoming.get(trafficDemand),
                                    cplex.constant(1)));
                }
            }

            /**
             *
             * Everything that enters the destination - Everything that leaves = 1
             * \sum_c \left( \sum_j R_{cuviv} - \sum_i R_{cuvvj}\right) = 1
             * \forall (u \ne j \lor u = v)
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){

                DestinationIncoming.putIfAbsent(trafficDemand,cplex.linearNumExpr());
                DestinationOutgoing.putIfAbsent(trafficDemand,cplex.linearNumExpr());

                for (Integer copy : Flows.get(trafficDemand).keySet()){
                    if (!trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                        for (Vertex vertex : graph.getSuccessors(trafficDemand.getDestination())) {
                            if (!vertex.equals(trafficDemand.getDestination()) ||
                                    trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                                DestinationOutgoing
                                        .get(trafficDemand)
                                        .addTerm(1.0, Flows
                                                .get(trafficDemand)
                                                .get(copy)
                                                .get(graph.getEdge(trafficDemand.getDestination(), vertex)));
                            }
                        }
                    }

                    for (Vertex vertex : graph.getPredecessors(trafficDemand.getDestination())) {
                        if (!vertex.equals(trafficDemand.getDestination()) ||
                                trafficDemand.getSource().equals(trafficDemand.getDestination())) {

                            DestinationIncoming
                                    .get(trafficDemand)
                                    .addTerm(1.0, Flows
                                            .get(trafficDemand)
                                            .get(copy)
                                            .get(graph.getEdge(vertex, trafficDemand.getDestination())));
                        }
                    }

                }

                if (trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                    cplex.addEq(DestinationIncoming.get(trafficDemand),1);
                }
                else{
                    cplex.addEq(DestinationIncoming.get(trafficDemand),
                            cplex.sum(
                                    DestinationOutgoing.get(trafficDemand),
                                    cplex.constant(1)));
                }
            }

            /**
             *
             * Capacity Constraint per edge
             * \sum_c \sum_{u,v} R_{cuvij} d_{uv} \le c_{ij}
             * \forall i,j
             * \forall u \ne v
             *
             */

            for (Edge edge: graph.getallEdges()){
                if (edge.getSource().equals(edge.getDestination())){
                    continue;
                }
                IloLinearNumExpr edgeTraffic = cplex.linearNumExpr();
                for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                    for (Integer copy : Flows.get(trafficDemand).keySet()){
                        edgeTraffic.addTerm(trafficDemand.getDemand(),Flows
                                .get(trafficDemand)
                                .get(copy)
                                .get(edge));
                    }

                }
                cplex.addLe(edgeTraffic,edge.getCapacity());
            }

            /**
             *
             * Flow Conservation at the node
             * \sum_i R_{cuvin} = \sum_j R_{cuvnj}
             * \forall n \ne u,v
             * \forall c
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                IncomingFlows.putIfAbsent(trafficDemand, new HashMap<>());
                OutgoingFlows.putIfAbsent(trafficDemand, new HashMap<>());

                for (Integer copy : Flows.get(trafficDemand).keySet()){

                    IncomingFlows.get(trafficDemand).putIfAbsent(copy, new HashMap<>());
                    OutgoingFlows.get(trafficDemand).putIfAbsent(copy, new HashMap<>());

                    for(Vertex n : graph.getVertices()) {
                        IncomingFlows.get(trafficDemand).get(copy).put(n,cplex.linearNumExpr());
                        for (Vertex i : graph.getPredecessors(n)){
                            IncomingFlows
                                    .get(trafficDemand)
                                    .get(copy)
                                    .get(n)
                                    .addTerm(1.0,Flows.get(trafficDemand).get(copy).get(graph.getEdge(i,n)));
                        }

                        OutgoingFlows.get(trafficDemand).get(copy).put(n,cplex.linearNumExpr());
                        for (Vertex j : graph.getSuccessors(n)){
                            OutgoingFlows
                                    .get(trafficDemand)
                                    .get(copy)
                                    .get(n)
                                    .addTerm(1.0,Flows.get(trafficDemand).get(copy).get(graph.getEdge(n,j)));
                        }

                        if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())){
                            cplex.addEq(IncomingFlows.get(trafficDemand).get(copy).get(n),
                                    OutgoingFlows.get(trafficDemand).get(copy).get(n));

                        }
                    }
                }
            }

            /**
             *
             * Copies cannot be placed in the same switch
             * \sum_c P_{cn} \le 1
             * \forall n
             *
             */

            IloLinearNumExpr temp1;
            for (Vertex vertex: graph.getVertices()){
                temp1 = cplex.linearNumExpr();
                for (int copy = 1 ; copy<=copies ; copy++){
                    temp1.addTerm(1.0,Placement.get(vertex).get(copy));
                }
                cplex.addLe(temp1,1.0);
            }

            /**
             *
             * Each copy is at only 1 switch
             * \sum_n P_{cn} = 1
             * \forall c
             *
             */

            IloLinearNumExpr temp2;
            for (int copy = 1 ; copy<=copies ; copy++){
                temp2 = cplex.linearNumExpr();
                for (Vertex vertex: graph.getVertices()){
                    temp2.addTerm(1.0,Placement.get(vertex).get(copy));
                }
                cplex.addEq(temp2,1.0);
            }

            /**
             *
             * X_{cuv}: An indicator function to identify which copy corresponds to which flow
             *
             * \sum_j R_{cuvuj} = \sum_i R_{cuviu} + X_{cuv}
             * \forall u \ne v
             * \forall c
             *
             * \sum_j R_{cuvuj} = X_{cuv}
             * \forall u = v
             * \forall c
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (Integer copy : Flows.get(trafficDemand).keySet()) {
                    for (Vertex vertex : graph.getSuccessors(trafficDemand.getSource())) {
                        X.get(trafficDemand).get(copy).addTerm(1.0, Flows
                                .get(trafficDemand)
                                .get(copy)
                                .get(graph.getEdge(trafficDemand.getSource(), vertex)));
                    }
                    if (!trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                        for (Vertex vertex : graph.getPredecessors(trafficDemand.getSource())) {
                            X.get(trafficDemand).get(copy).addTerm(-1.0, Flows
                                    .get(trafficDemand)
                                    .get(copy)
                                    .get(graph.getEdge(vertex, trafficDemand.getSource())));
                        }
                    }
                }
            }

            /**
             *
             * All flows must cross the state
             *
             * \sum_i R_{cuvin} \ge P_{cn} + X_{cuv} -1
             * \forall n \ne u,v
             * \forall u,v
             * \forall c
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                for(Vertex n : graph.getVertices()) {
                    if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())) {
                        for (Integer copy : Flows.get(trafficDemand).keySet()) {
                            cplex.addGe(IncomingFlows.get(trafficDemand).get(copy).get(n),
                                    cplex.sum(Placement.get(n).get(copy), X.get(trafficDemand).get(copy), cplex.constant(-1.0)));
                        }
                    }
                }
            }

            /**
             *
             * Flows requiring different copies must not interfere unless
             * specified by isMultipleStates()
             *
             * \sum_{i} R_{cuvin} + P_{fn} \le 1
             * \forall u,v
             * \forall n
             * \forall c \ne f
             *
             */

            if (copies > 1 && !options.ismultipleStates()) {
                IloLinearNumExpr otherCopy;
                for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                    for (Vertex n : graph.getVertices()) {
                        for (Integer copy : Flows.get(trafficDemand).keySet()) {
                            for (Integer copy1 : Flows.get(trafficDemand).keySet()) {
                                otherCopy = cplex.linearNumExpr();
                                if (!copy1.equals(copy))
                                    for (Vertex i : graph.getPredecessors(n)){
                                        otherCopy.addTerm(1.0,
                                                Flows.get(trafficDemand).get(copy1).get(graph.getEdge(i,n)));
                                    }
                                cplex.addLe(cplex.sum(otherCopy, Placement.get(n).get(copy)), 1);
                            }
                        }
                    }
                }
            }

            // Initialize Pcuvij

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                TrackIncoming.put(trafficDemand,new HashMap<>());
                TrackOutgoing.put(trafficDemand,new HashMap<>());

                for (Integer copy : Flows.get(trafficDemand).keySet()){
                    TrackIncoming.get(trafficDemand).put(copy, new HashMap<>());
                    TrackOutgoing.get(trafficDemand).put(copy, new HashMap<>());

                    for(Vertex n : graph.getVertices()) {
                        TrackIncoming.get(trafficDemand).get(copy).put(n,cplex.linearNumExpr());
                        TrackOutgoing.get(trafficDemand).get(copy).put(n,cplex.linearNumExpr());

                        for (Vertex i : graph.getPredecessors(n)){
                            TrackIncoming
                                    .get(trafficDemand)
                                    .get(copy)
                                    .get(n)
                                    .addTerm(1.0,PTracker.get(trafficDemand).get(copy).get(graph.getEdge(i,n)));
                        }

                        for (Vertex j : graph.getSuccessors(n)){
                            TrackOutgoing
                                    .get(trafficDemand)
                                    .get(copy)
                                    .get(n)
                                    .addTerm(1.0,PTracker.get(trafficDemand).get(copy).get(graph.getEdge(n,j)));
                        }
                    }
                }
            }

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                for (Integer copy : Flows.get(trafficDemand).keySet()){
                    for(Vertex n : graph.getVertices()) {

                        /**
                         *
                         * Y_{cuvn} = P_{cn} AND X_{cuv}
                         *
                         * Y_{cuvn} \ge P_{cn} + X_{cuv} - 1
                         * Y_{cuvn} \le P_{cn}
                         * Y_{cuvn} \le X_{cuv}
                         *
                         */

                        cplex.addGe(Y.get(trafficDemand).get(copy).get(n),
                                cplex.sum(Placement.get(n).get(copy),X.get(trafficDemand).get(copy),
                                        cplex.constant(-1.0)));
                        cplex.addLe(Y.get(trafficDemand).get(copy).get(n),Placement.get(n).get(copy));
                        cplex.addLe(Y.get(trafficDemand).get(copy).get(n),X.get(trafficDemand).get(copy));


                        if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())){

                            /**
                             *
                             * Tracking at an intermediate node if flow has passed the state
                             *
                             * Y_{cuvn}+ \sum_i P_{cuvin} = \sum_j P_{cuvnj}
                             * \forall n \ne u,v
                             * \forall u,v
                             * \forall c
                             *
                             */

                            cplex.addEq(cplex.sum(Y.get(trafficDemand).get(copy).get(n),
                                    TrackIncoming.get(trafficDemand).get(copy).get(n)),
                                    TrackOutgoing.get(trafficDemand).get(copy).get(n));
                        }

                        if (n.equals(trafficDemand.getDestination())){

                            /**
                             *
                             * Tracking at the destination if flow has passed the state
                             *
                             * Y_{cuvv}+\sum_i P_{cuviv}=X_{cuv} \quad \forall c \quad \forall u,v
                             * \forall u,v
                             * \forall c
                             *
                             */

                            cplex.addEq(cplex.sum(Y.get(trafficDemand).get(copy).get(n),
                                    TrackIncoming.get(trafficDemand).get(copy).get(n)),
                                    X.get(trafficDemand).get(copy));
                        }
                    }
                }
            }

            /**
             *
             * Upper limit of P_{cuvij}
             *
             * P_{cuvij} \le R_{cuvij}
             *
             * \forall c
             * \forall u,v
             * \forall i,j
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (Integer copy : Flows.get(trafficDemand).keySet()){
                    for (Edge edge : Flows.get(trafficDemand).get(copy).keySet()) {
                        cplex.addGe(Flows.get(trafficDemand).get(copy).get(edge),
                                PTracker.get(trafficDemand).get(copy).get(edge));
                    }
                }
            }

            /**
             *
             * Ensure that flow leaving a switch containing the state does
             * not pass another switch containing state
             *
             * P_{cu} \le \sum_j R_{cuvuj} - \sum_i R_{cuviu}
             * \forall u \ne v
             * \forall c
             *
             * P_{cu} \le \sum_j R_{cuvuj}
             * \forall u=v
             * \forall c
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (Integer copy : Flows.get(trafficDemand).keySet()) {
                    IloLinearNumExpr sourceTemp = cplex.linearNumExpr();
                    for (Vertex vertex : graph.getSuccessors(trafficDemand.getSource())) {
                        if (!vertex.equals(trafficDemand.getSource()) ||
                                trafficDemand.getSource().equals(trafficDemand.getDestination())){
                            sourceTemp.addTerm(1.0, Flows
                                    .get(trafficDemand)
                                    .get(copy)
                                    .get(graph.getEdge(trafficDemand.getSource(), vertex)));
                        }
                    }
                    if (!trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                        for (Vertex vertex : graph.getPredecessors(trafficDemand.getSource())) {
                            sourceTemp.addTerm(-1.0, Flows
                                    .get(trafficDemand)
                                    .get(copy)
                                    .get(graph.getEdge(vertex, trafficDemand.getSource())));
                        }
                    }
                    cplex.addGe(sourceTemp,Placement.get(trafficDemand.getSource()).get(copy));
                }
            }

            /**
             * For testing purposes, FixVariables() function
             * can be called. The testing variables can be fixed
             * in this function
             */

            if (fixConstraints)
                FixVariables();

            //cplex.exportModel("test_shardedSNAP.lp");

        }
        catch (IloException e){
            System.out.println("Exception " + e +" caught while building the model");
        }

    }

    public boolean optimize(){
        try
        {
            boolean result = cplex.solve();
            getObjectiveValue();

            return result;
        }
        catch  (IloException e)  {
            System.out.println("Exception " + e + " caught while optimizing the model");
        }
        return false;
    }

    /**
     * For testing purposes, FixVariables() function
     * can be called. The testing variables can be fixed
     * in this function
     */

    private void FixVariables() {

        try {
            /*
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                if (trafficDemand.getSource().getLabel() == 0 && trafficDemand.getDestination().getLabel() == 1) {
                    for (Integer copy : Flows.get(trafficDemand).keySet()) {
                        for (Edge edge : Flows.get(trafficDemand).get(copy).keySet()) {
                            if (copy == 1) {
                                if (edge.getSource().getLabel() == 0 && edge.getDestination().getLabel() == 1) {
                                    cplex.addEq(Flows.get(trafficDemand).get(copy).get(edge), 1.0);
                                }
                                else if (edge.getSource().getLabel() == 1 && edge.getDestination().getLabel() == 2) {
                                    cplex.addEq(Flows.get(trafficDemand).get(copy).get(edge), 1.0);
                                }
                                else if (edge.getSource().getLabel() == 2 && edge.getDestination().getLabel() == 1) {
                                    cplex.addEq(Flows.get(trafficDemand).get(copy).get(edge), 1.0);
                                }
                                else {
                                    cplex.addEq(Flows.get(trafficDemand).get(copy).get(edge), 0.0);
                                }
                            }
                            if (copy == 2) {
                                cplex.addEq(Flows.get(trafficDemand).get(copy).get(edge), 0.0);
                            }
                        }
                    }
                }
            }
            */

            cplex.addEq(Placement.get(graph.getVertex(0)).get(1),1);
            cplex.addEq(Placement.get(graph.getVertex(3)).get(2),1);
            cplex.addEq(Placement.get(graph.getVertex(5)).get(3),1);

        }
        catch (IloException e) {
            System.out.println("Caught IloException error" + e);
        }
    }

    /**
     * Objective Function
     */

    private void setObjective(){
        try {
        IloLinearNumExpr objective = cplex.linearNumExpr();

            for (TrafficDemand trafficDemand : Flows.keySet()) {
                for (Integer copy : Flows.get(trafficDemand).keySet()) {
                    for (Edge edge : Flows.get(trafficDemand).get(copy).keySet()) {
                        objective.addTerm(trafficDemand.getDemand(), Flows
                                .get(trafficDemand)
                                .get(copy)
                                .get(edge));
                    }
                }
            }
            cplex.addMinimize(objective);
        }
         catch  (IloException e)  {
            System.err.println("Concert exception caught:"  +  e);
        }
    }

    /**
     * Solution can be printed
     */

    public void printSolution(){

        try {

            double obj = cplex.getObjValue();

            System.out.println("####################################################################");
            System.out.println("#      Sharded SNAP Optimization: Model Solved with " + copies + " copy(s)      #");
            System.out.println("####################################################################");


            for (TrafficDemand trafficDemand : Flows.keySet()) {
                for (Integer copy : Flows.get(trafficDemand).keySet()) {
                    for (Edge edge : Flows.get(trafficDemand).get(copy).keySet()) {
                        if (cplex.getValue(Flows.get(trafficDemand).get(copy).get(edge)) > 0) {
                            System.out.println("R_" + copy + "_" + trafficDemand.getSource().getLabel()
                                    + "_" + trafficDemand.getDestination().getLabel()
                                    + "_" + edge.getSource().getLabel()
                                    + "_" + edge.getDestination().getLabel() + " = " +
                                    cplex.getValue(Flows.get(trafficDemand).get(copy).get(edge)));
                        }
                    }
                }
                System.out.println();
            }

            System.out.println("obj = " + getObjectiveValue());

            for (Vertex vertex : graph.getVertices()) {
                for (Integer copy : Placement.get(vertex).keySet()) {
                    if (cplex.getValue(Placement.get(vertex).get(copy)) > 0) {
                        System.out.println("P_" + copy + "_" + vertex.getLabel() + " " +
                                cplex.getValue(Placement.get(vertex).get(copy)));
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
        if (!objectiveValueFixed){
            try {
                objectiveValue = cplex.getObjValue();
                for (TrafficDemand trafficDemand : Flows.keySet()) {
                    for (Integer copy : Flows.get(trafficDemand).keySet()) {
                        for (Edge edge : Flows.get(trafficDemand).get(copy).keySet()) {
                            if (cplex.getValue(Flows.get(trafficDemand).get(copy).get(edge)) > 0) {
                                if (edge.getSource().equals(edge.getDestination())) {
                                    objectiveValue -= cplex.getValue(Flows.get(trafficDemand).get(copy).get(edge));
                                }
                            }
                        }
                    }
                }
                objectiveValueFixed = true;
            }
            catch (IloException e){
                System.err.println("Concert exception caught:"  +  e);
            }
        }
        return objectiveValue;
    }

    public LinkedList<Vertex> getStateLocation(){

        LinkedList<Vertex> stateLocation = new LinkedList<>();

        try {
            for (Vertex vertex : graph.getVertices()) {
                for (Integer copy : Placement.get(vertex).keySet()) {
                    if (cplex.getValue(Placement.get(vertex).get(copy)) > 0)
                        stateLocation.add(vertex);
                }
            }

        }
        catch (IloException e){
            System.err.println("Concert exception caught in getStateLocation():"  +  e);
        }

        return stateLocation;
    }

    public void clear(){

        try {

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                for (Integer copy : Flows.get(trafficDemand).keySet()){
                    for (Edge edge : Flows.get(trafficDemand).get(copy).keySet()){
                        Flows.get(trafficDemand).get(copy).put(edge,null);
                        PTracker.get(trafficDemand).get(copy).put(edge,null);
                    }
                    X.get(trafficDemand).put(copy,null);
                }
            }

            Flows.clear();
            PTracker.clear();
            X.clear();

            for (Vertex vertex : Placement.keySet()){
                for (Integer copy : Placement.get(vertex).keySet()){
                    Placement.get(vertex).put(copy,null);
                }
            }

            Placement.clear();
            copies=0;
            cplex.clearModel();
            objectiveValueFixed = false;
            objectiveValue=0;
        }

        catch (IloException e){
            System.out.println("Exception " + e +" caught while clearing the model");
        }

    }

    public LinkedHashMap<TrafficDemand, Path> getLoopFreeRoutingSolution(){

        LinkedHashMap<TrafficDemand, Path> tempRoutingSolution = new LinkedHashMap<>();
        LinkedHashMap<TrafficDemand, Path> routingSolution = new LinkedHashMap<>();

        try {
            for (TrafficDemand trafficDemand : Flows.keySet()) {
                tempRoutingSolution.put(trafficDemand, new Path());
                for (Integer copy : Flows.get(trafficDemand).keySet()) {
                    for (Edge edge : Flows.get(trafficDemand).get(copy).keySet()) {
                        if (cplex.getValue(Flows.get(trafficDemand).get(copy).get(edge)) > 0) {
                            tempRoutingSolution.get(trafficDemand).add(edge);
                        }
                    }
                }
            }
        }
        catch (IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }

        int d=1;

        HashMap<Vertex, Integer> occurrencesSource = new HashMap<>();
        HashMap<Vertex, Integer> occurrencesDestination = new HashMap<>();
        boolean loop;

        for (TrafficDemand trafficDemand : Flows.keySet()){

            loop = false;

            graph.getVertices().forEach(vertex -> {
                occurrencesSource.put(vertex,0);
                occurrencesDestination.put(vertex,0);
            });

            for (Edge edge : tempRoutingSolution.get(trafficDemand).getEdges()){
                int temp1 = occurrencesSource.get(edge.getSource());
                int temp2 = occurrencesDestination.get(edge.getDestination());
                temp1++;
                temp2++;
                occurrencesSource.put(edge.getSource(),temp1);
                occurrencesDestination.put(edge.getDestination(),temp1);
                if(temp1>1 || temp2>1){
                    loop = true;
                    break;
                }
            }

            if(!loop){

                routingSolution.put(trafficDemand, new Path());

                Vertex current = trafficDemand.getSource();
                Vertex destination = trafficDemand.getDestination();

                Path correctPath = routingSolution.get(trafficDemand);
                Path unmodifiedPath = tempRoutingSolution.get(trafficDemand);

                while(!current.equals(destination)){
                    for (Edge edge : unmodifiedPath.getEdges()) {
                        if(edge.getSource().equals(current)){
                            correctPath.add(edge);
                            current = edge.getDestination();
                            unmodifiedPath.remove(edge);
                            break;
                        }
                    }
                }
            }
        }

        return routingSolution;
    }

}
