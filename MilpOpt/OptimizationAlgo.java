package statefulsharding.MilpOpt;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import ilog.concert.*;
import ilog.cplex.*;
import statefulsharding.graph.Vertex;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

/*
 * Static methods for optimizing algorithms
 */

public class OptimizationAlgo {

    public static boolean ShardedSNAP(ListGraph graph,
                                      TrafficStore trafficStore,
                                      boolean verbose,
                                      int copies,
                                      String fileProperty,
                                      boolean generateFiles,
                                      boolean multipleStates){

        try {

            FileWriter logfw = new FileWriter(fileProperty + "_Logfile.txt" ,true);
            BufferedWriter logbw = new BufferedWriter(logfw);
            PrintWriter logout = new PrintWriter(logbw);
            FileWriter solnRfw = new FileWriter(fileProperty + "_RoutingSolution.txt" ,true);
            BufferedWriter solnRbw = new BufferedWriter(solnRfw);
            PrintWriter solnRout = new PrintWriter(solnRbw); solnRout.println("c u v i j");
            FileWriter solnPfw = new FileWriter(fileProperty + "_PlacementSolution.txt" ,true);
            BufferedWriter solnPbw = new BufferedWriter(solnPfw);
            PrintWriter solnPout = new PrintWriter(solnPbw); solnPout.println("c n");

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

            logout.println(sdf.format(cal.getTime()) + " Defining Model");
            logout.flush();

            IloCplex cplex = new IloCplex();
            cplex.setParam(IloCplex.BooleanParam.MemoryEmphasis,true);
            cplex.setParam(IloCplex.Param.WorkDir,System.getProperty("user.home") + "/Desktop");

            HashMap<TrafficDemand,HashMap<Integer,HashMap<Edge,IloNumVar>>> Flows = new HashMap<>();
            HashMap<TrafficDemand,HashMap<Integer, HashMap<Edge,IloNumVar>>> PTracker = new HashMap<>();
            HashMap<Vertex,HashMap<Integer,IloNumVar>> Placement = new HashMap<>();
            HashMap<TrafficDemand,HashMap<Integer, IloLinearNumExpr>> X = new HashMap<>();

            if(!verbose){
                cplex.setOut(null);
            }

            HashMap<TrafficDemand,IloLinearNumExpr> SourceIncoming = new HashMap<>();
            HashMap<TrafficDemand,IloLinearNumExpr> SourceOutgoing = new HashMap<>();
            HashMap<TrafficDemand,IloLinearNumExpr> DestinationIncoming = new HashMap<>();
            HashMap<TrafficDemand,IloLinearNumExpr> DestinationOutgoing = new HashMap<>();
            HashMap<TrafficDemand,HashMap<Integer,HashMap<Vertex,IloLinearNumExpr>>> IncomingFlows =
                    new HashMap<>();
            HashMap<TrafficDemand,HashMap<Integer,HashMap<Vertex,IloLinearNumExpr>>> OutgoingFlows =
                    new HashMap<>();
            HashMap<TrafficDemand,HashMap<Integer,HashMap<Vertex,IloLinearNumExpr>>> TrackIncoming = new HashMap<>();
            HashMap<TrafficDemand,HashMap<Integer,HashMap<Vertex,IloLinearNumExpr>>> TrackOutgoing = new HashMap<>();
            HashMap<TrafficDemand, HashMap<Integer, HashMap<Vertex, IloNumVar>>> Y = new HashMap<>();

            /*
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

            /*
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

            /*
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

            /*
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



            /*
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

            /*
             * Calling the objective function to be minimized
             */

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

            /////////////////////////////// CONSTRAINTS //////////////////////////////////

            /*
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

            /*
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

            /*
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

            /*
             *
             * Flow Conservation at the node
             * \sum_i R_{cuvin} = \sum_j R_{cuvnj}
             * \forall n \ne u,v
             * \forall c
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                IncomingFlows.putIfAbsent(trafficDemand,new HashMap<>());
                OutgoingFlows.putIfAbsent(trafficDemand,new HashMap<>());

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

            /*
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

            /*
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

            /*
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

            /*
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

            /*
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

            if (copies > 1 && !multipleStates) {
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

                        /*
                         *
                         * Y_{cuvn} = P_{cn} AND X_{cuv}
                         *
                         * Y_{cuvn} \ge P_{cn} + X_{cuv} - 1
                         * Y_{cuvn} \le P_{cn}
                         * Y_{cuvn} \le X_{cuv}
                         *
                         */

                        cplex.addGe(Y.get(trafficDemand).get(copy).get(n),
                                cplex.sum(Placement.get(n).get(copy),X.get(trafficDemand).get(copy),cplex.constant(-1.0)));
                        cplex.addLe(Y.get(trafficDemand).get(copy).get(n),Placement.get(n).get(copy));
                        cplex.addLe(Y.get(trafficDemand).get(copy).get(n),X.get(trafficDemand).get(copy));


                        if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())){

                            /*
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

                            /*
                             *
                             * Tracking at the destination if flow has passed the state
                             *
                             * Y_{cuvv}+\sum_i P_{cuviv}=X_{cuv} \quad \forall c \quad \forall u,v
                             * \forall u,v
                             * \forall c
                             *
                             */

                            // (27)
                            // Y_{cuvn} + sum_i P_{cuvin} = X_{cuv}
                            // \forall u,v \forall n=v \forall c

                            cplex.addEq(cplex.sum(Y.get(trafficDemand).get(copy).get(n),
                                    TrackIncoming.get(trafficDemand).get(copy).get(n)),
                                    X.get(trafficDemand).get(copy));
                        }
                    }
                }
            }

            /*
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

            /*
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

            /*
             * For testing purposes, FixVariables() function
             * can be called. The testing variables can be fixed
             * in this function
             */

            cplex.exportModel(fileProperty + "_model.lp");

            logout.println(sdf.format(cal.getTime()) + " Optimizing");
            logout.flush();


            boolean result = cplex.solve();

            System.out.println("Objective Value: " + getObjectiveValue(cplex,Flows));
            logout.println(sdf.format(cal.getTime()) + " Objective Value: " + getObjectiveValue(cplex,Flows));
            logout.flush();

            /*
             * Clearing Values:
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                for (Integer copy : Flows.get(trafficDemand).keySet()){
                    for (Edge edge : Flows.get(trafficDemand).get(copy).keySet()){
                        if (cplex.getValue(Flows.get(trafficDemand).get(copy).get(edge)) > 0) {
                            solnRout.println(copy + " " +
                                                        trafficDemand.getSource().getLabel() + " " +
                                                        trafficDemand.getDestination().getLabel() + " " +
                                                        edge.getSource().getLabel() + " " +
                                                        edge.getDestination().getLabel());
                        }
                        Flows.get(trafficDemand).get(copy).put(edge,null);
                        PTracker.get(trafficDemand).get(copy).put(edge,null);
                    }
                    X.get(trafficDemand).put(copy,null);
                }
            }

            solnRout.flush();

            for (Vertex vertex : Placement.keySet()){
                for (Integer copy : Placement.get(vertex).keySet()){
                    if (cplex.getValue(Placement.get(vertex).get(copy)) > 0){
                        solnPout.println(copy + " " + vertex.getLabel());
                    }
                    Placement.get(vertex).put(copy,null);
                }
            }

            solnPout.flush();

            Flows.clear();
            PTracker.clear();
            X.clear();
            Placement.clear();
            cplex.clearModel();

            if(!generateFiles){
                File logFile = new File(fileProperty + "_Logfile.txt");
                File routingFile = new File(fileProperty + "_RoutingSolution.txt");
                File placementFile = new File(fileProperty + "_PlacementSolution.txt");
                File modelFile = new File(fileProperty + "_model.lp");
                logFile.delete();
                routingFile.delete();
                modelFile.delete();
                placementFile.delete();
            }

            return result;
        }
        catch(IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
        catch (IOException e) {
            System.err.println("IOException caught:"  +  e);
        }

        return false;

    }

    public static boolean ShortestPath(ListGraph graph,
                                       TrafficStore trafficStore,
                                       boolean verbose,
                                       String fileProperty,
                                       boolean generateFiles){

        try {

            FileWriter logfw = new FileWriter(fileProperty + "_Logfile.txt" ,true);
            BufferedWriter logbw = new BufferedWriter(logfw);
            PrintWriter logout = new PrintWriter(logbw);
            FileWriter solnRfw = new FileWriter(fileProperty + "_RoutingSolution.txt" ,true);
            BufferedWriter solnRbw = new BufferedWriter(solnRfw);
            PrintWriter solnRout = new PrintWriter(solnRbw); solnRout.println("u v i j");

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

            logout.println(sdf.format(cal.getTime()) + " Defining Model");
            logout.flush();

            IloCplex cplex = new IloCplex();
            HashMap<TrafficDemand,HashMap<Edge,IloNumVar>> Flows = new HashMap<>();

            if(!verbose){
                cplex.setOut(null);
            }

            HashMap<TrafficDemand,IloLinearNumExpr> SourceIncoming = new HashMap<>();
            HashMap<TrafficDemand,IloLinearNumExpr> SourceOutgoing = new HashMap<>();
            HashMap<TrafficDemand,IloLinearNumExpr> DestinationIncoming = new HashMap<>();
            HashMap<TrafficDemand,IloLinearNumExpr> DestinationOutgoing = new HashMap<>();
            HashMap<TrafficDemand,HashMap<Vertex,IloLinearNumExpr>> IncomingFlows =
                    new HashMap<>();
            HashMap<TrafficDemand,HashMap<Vertex,IloLinearNumExpr>> OutgoingFlows =
                    new HashMap<>();

            /*
             * Defining Routing Variable
             * R_{uvij}
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                Flows.put(trafficDemand, new HashMap<>());
                for (Edge edge : graph.getallEdges()) {
                    Flows.get(trafficDemand)
                            .put(edge, cplex.boolVar("Ruvij_" +
                                    trafficDemand.getSource().getLabel() + "_" +
                                    trafficDemand.getDestination().getLabel() + "_" +
                                    edge.getSource().getLabel() + "_" +
                                    edge.getDestination().getLabel()));
                }
            }

            /*
             * Calling the objective function to be minimized
             */

            IloLinearNumExpr objective = cplex.linearNumExpr();

            for (TrafficDemand trafficDemand : Flows.keySet()) {
                for (Edge edge : Flows.get(trafficDemand).keySet()) {
                    objective.addTerm(trafficDemand.getDemand(), Flows
                            .get(trafficDemand)
                            .get(edge));
                }
            }

            cplex.addMinimize(objective);

            /////////////////////////////// CONSTRAINTS //////////////////////////////////

            /*
             *
             * OutgoingFlows@Source - IncomingFlows@Source = 1
             * \sum_j R_{uvuj} - \sum_i R_{uviu} = 1
             * \forall (u \ne v)
             * j = i \ne u
             *
             * R_{uuuu} = R_{vvvv} = 1
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                if (trafficDemand.getSource().equals(trafficDemand.getDestination())){
                    cplex.addEq(Flows
                                    .get(trafficDemand)
                                    .get(graph.getEdge(trafficDemand.getSource(),trafficDemand.getDestination())),
                            1);
                }
                else {
                    SourceIncoming.putIfAbsent(trafficDemand,cplex.linearNumExpr());
                    SourceOutgoing.putIfAbsent(trafficDemand,cplex.linearNumExpr());

                    for (Vertex vertex : graph.getSuccessors(trafficDemand.getSource())) {
                        if (!vertex.equals(trafficDemand.getSource())) {
                            SourceOutgoing
                                    .get(trafficDemand)
                                    .addTerm(1.0,Flows
                                            .get(trafficDemand)
                                            .get(graph.getEdge(trafficDemand.getSource(), vertex)));
                        }
                    }

                    for (Vertex vertex : graph.getPredecessors(trafficDemand.getSource())) {
                        if (!vertex.equals(trafficDemand.getSource())) {
                            SourceIncoming
                                    .get(trafficDemand)
                                    .addTerm(1.0, Flows
                                            .get(trafficDemand)
                                            .get(graph.getEdge(vertex, trafficDemand.getSource())));
                        }
                    }

                    cplex.addEq(SourceOutgoing.get(trafficDemand),
                            cplex.sum(
                                    SourceIncoming.get(trafficDemand),
                                    cplex.constant(1)));
                }
            }




            /*
             *
             * Everything that enters the destination
             * \sum_i R_{uviv} - \sum_j R_{uvvj} = 1
             * \forall (u \ne v)
             * i = j \ne v
             *
             * R_{uuuu} = R_{vvvv} = 1
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                if (trafficDemand.getSource().equals(trafficDemand.getDestination()))
                    continue;
                else {
                    DestinationIncoming.putIfAbsent(trafficDemand,cplex.linearNumExpr());
                    DestinationOutgoing.putIfAbsent(trafficDemand,cplex.linearNumExpr());

                    for (Vertex vertex : graph.getSuccessors(trafficDemand.getDestination())) {
                        if (!vertex.equals(trafficDemand.getDestination())) {
                            DestinationOutgoing
                                    .get(trafficDemand)
                                    .addTerm(1.0,Flows
                                            .get(trafficDemand)
                                            .get(graph.getEdge(trafficDemand.getDestination(), vertex)));
                        }
                    }

                    for (Vertex vertex : graph.getPredecessors(trafficDemand.getDestination())) {
                        if (!vertex.equals(trafficDemand.getDestination())) {
                            DestinationIncoming
                                    .get(trafficDemand)
                                    .addTerm(1.0, Flows
                                            .get(trafficDemand)
                                            .get(graph.getEdge(vertex, trafficDemand.getDestination())));
                        }
                    }

                    cplex.addEq(DestinationIncoming.get(trafficDemand),
                            cplex.sum(
                                    DestinationOutgoing.get(trafficDemand),
                                    cplex.constant(1)));
                }
            }

            /*
             *
             * Capacity Constraint per edge
             * \sum_{u,v} R_{uvij} d_{uv} \le c_{ij}
             * \forall i,j
             * \forall u \ne v
             *
             */

            for (Edge edge: graph.getallEdges()){
                if (edge.getSource().equals(edge.getDestination()))
                    continue;
                IloLinearNumExpr edgeTraffic = cplex.linearNumExpr();
                for(TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                    edgeTraffic.addTerm(trafficDemand.getDemand(),Flows
                            .get(trafficDemand)
                            .get(edge));
                }
                cplex.addLe(edgeTraffic,edge.getCapacity());
            }

            /*
             *
             * Flow Conservation at the node
             * \sum_i R_{uvin} = \sum_j R_{uvnj}
             * \forall n \ne u,v
             * \forall c
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                if (trafficDemand.getSource().equals(trafficDemand.getDestination()))
                    continue;

                IncomingFlows.putIfAbsent(trafficDemand,new HashMap<>());
                OutgoingFlows.putIfAbsent(trafficDemand,new HashMap<>());

                for(Vertex n : graph.getVertices()) {

                    IncomingFlows.get(trafficDemand).put(n,cplex.linearNumExpr());
                    for (Vertex i : graph.getPredecessors(n)){
                        IncomingFlows
                                .get(trafficDemand)
                                .get(n)
                                .addTerm(1.0,Flows.get(trafficDemand).get(graph.getEdge(i,n)));
                    }

                    OutgoingFlows.get(trafficDemand).put(n,cplex.linearNumExpr());
                    for (Vertex j : graph.getSuccessors(n)){
                        OutgoingFlows
                                .get(trafficDemand)
                                .get(n)
                                .addTerm(1.0,Flows.get(trafficDemand).get(graph.getEdge(n,j)));
                    }

                    if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())){
                        cplex.addEq(IncomingFlows.get(trafficDemand).get(n),
                                OutgoingFlows.get(trafficDemand).get(n));

                    }
                }
            }

            cplex.exportModel(fileProperty + "_model.lp");

            logout.println(sdf.format(cal.getTime()) + " Optimizing");
            logout.flush();
            boolean result = cplex.solve();

            System.out.println("Objective Value: " + getObjectiveValueSP(cplex,Flows));
            logout.println(sdf.format(cal.getTime()) + " Objective Value: " + getObjectiveValueSP(cplex,Flows));
            logout.flush();

            /*
             * Clearing Values:
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                for (Edge edge : Flows.get(trafficDemand).keySet()){
                    if (cplex.getValue(Flows.get(trafficDemand).get(edge)) > 0) {
                        solnRout.println(trafficDemand.getSource().getLabel() + " " +
                                trafficDemand.getDestination().getLabel() + " " +
                                edge.getSource().getLabel() + " " +
                                edge.getDestination().getLabel());
                    }
                    Flows.get(trafficDemand).put(edge,null);
                }
            }
            solnRout.flush();
            Flows.clear();
            cplex.clearModel();

            if(!generateFiles){
                File logFile = new File(fileProperty + "_Logfile.txt");
                File routingFile = new File(fileProperty + "_RoutingSolution.txt");
                File modelFile = new File(fileProperty + "_model.lp");
                logFile.delete();
                routingFile.delete();
                modelFile.delete();
            }

            return result;
        }
        catch(IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
        catch (IOException e) {
            System.err.println("IOException caught:"  +  e);
        }

        return false;

    }

    private static double getObjectiveValue(IloCplex cplex,
                                           HashMap<TrafficDemand,HashMap<Integer,HashMap<Edge,IloNumVar>>> Flows){

        try {
            double objectiveValue = cplex.getObjValue();
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
            return objectiveValue;
        }
        catch (IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
        return 0.0;
    }

    private static double getObjectiveValueSP(IloCplex cplex,
                                              HashMap<TrafficDemand,HashMap<Edge,IloNumVar>> Flows){

        try {
            double objectiveValue = cplex.getObjValue();
            for (TrafficDemand trafficDemand : Flows.keySet()) {

                    for (Edge edge : Flows.get(trafficDemand).keySet()) {
                        if (cplex.getValue(Flows.get(trafficDemand).get(edge)) > 0) {
                            if (edge.getSource().equals(edge.getDestination())) {
                                objectiveValue -= cplex.getValue(Flows.get(trafficDemand).get(edge));
                            }
                        }
                    }

            }
            return objectiveValue;
        }
        catch (IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
        return 0.0;
    }

}
