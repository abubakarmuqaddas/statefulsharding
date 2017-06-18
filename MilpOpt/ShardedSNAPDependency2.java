package statefulsharding.MilpOpt;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import statefulsharding.State.StateCopy;
import statefulsharding.State.StateStore;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by root on 5/23/17.
 */
public class ShardedSNAPDependency2 {

    private ListGraph graph;
    private TrafficStore trafficStore;
    private IloCplex cplex;
    private OptimizationOptions optimizationOptions;
    private HashMap<TrafficDemand, List<List<StateCopy>>> combinations;
    private HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies;
    private StateStore stateStore;
    private HashMap<TrafficDemand, HashMap<List<StateCopy>, HashMap<Edge, IloNumVar>>> Flows;
    private HashMap<Vertex, HashMap<StateCopy, IloNumVar>> Placement;
    private HashMap<TrafficDemand, HashMap<List<StateCopy>, IloNumVar>> X;
    private HashMap<TrafficDemand, HashMap<List<StateCopy>,
            HashMap<StateCopy, HashMap<Edge, IloNumVar>>>> PTracker;
    private HashMap<TrafficDemand, HashMap<List<StateCopy>,
            HashMap<StateCopy, HashMap<Vertex, IloNumVar>>>> Y;

    public ShardedSNAPDependency2(ListGraph graph, TrafficStore trafficStore,
                                  HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies,
                                  OptimizationOptions options,
                                  StateStore stateStore){

        this.graph = graph;
        this.trafficStore = trafficStore;
        this.optimizationOptions = options;
        this.dependencies = dependencies;
        this.stateStore = stateStore;
        Flows = new HashMap<>();
        Placement = new HashMap<>();
        combinations = new HashMap<>();
        PTracker = new HashMap<>();
        X = new HashMap<>();
        Y = new HashMap<>();

        try {
            this.cplex = new IloCplex();
        }
        catch(IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
        buildModel(optimizationOptions.isVerbose(), optimizationOptions.isFixConstraints());

    }

    private void buildModel(boolean outputRequired, boolean fixConstraints){

        try {

            /**
             * Defining temporary variables
             */

            HashMap<TrafficDemand, IloLinearNumExpr> SourceIncoming = new HashMap<>();
            HashMap<TrafficDemand, IloLinearNumExpr> SourceOutgoing = new HashMap<>();
            HashMap<TrafficDemand, IloLinearNumExpr> DestinationIncoming = new HashMap<>();
            HashMap<TrafficDemand, IloLinearNumExpr> DestinationOutgoing = new HashMap<>();

            HashMap<TrafficDemand, HashMap<List<StateCopy>, HashMap<Vertex, IloLinearNumExpr>>> IncomingFlows =
                    new HashMap<>();
            HashMap<TrafficDemand, HashMap<List<StateCopy>, HashMap<Vertex, IloLinearNumExpr>>> OutgoingFlows =
                    new HashMap<>();

            HashMap<TrafficDemand, HashMap<List<StateCopy>, HashMap<StateCopy,
                    HashMap<Vertex, IloLinearNumExpr>>>> TrackIncoming = new HashMap<>();
            HashMap<TrafficDemand, HashMap<List<StateCopy>, HashMap<StateCopy,
                    HashMap<Vertex, IloLinearNumExpr>>>> TrackOutgoing = new HashMap<>();


            /**
             * Create all combinations of state copies for each traffic demand
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {

                combinations.put(trafficDemand, new LinkedList<>());

                if (dependencies.get(trafficDemand).size() == 1) {
                    StateVariable currentState = dependencies.get(trafficDemand).getFirst();
                    for (int i = 1; i <= currentState.getCopies(); i++) {
                        LinkedList<StateCopy> currentCombination = new LinkedList<>();

                        StateCopy tempStateCopy = stateStore.getStateCopy(currentState, i);
                        currentCombination.add(tempStateCopy );
                        combinations.get(trafficDemand).add(currentCombination);
                    }
                }
                else {

                    Object[][] sets = new Object[dependencies.get(trafficDemand).size()][];
                    int j = 0;
                    for (StateVariable state : dependencies.get(trafficDemand)) {

                        Integer[] set = new Integer[state.getCopies()];

                        for (int i = 0; i < state.getCopies(); i++) {
                            set[i] = i + 1;
                        }

                        sets[j] = set;
                        j++;
                    }

                    getCopyCombinations(sets, 0, new Object[0], combinations, trafficDemand);
                }
            }

            if (!outputRequired) {
                cplex.setOut(null);
            }

            /**
             * Defining Routing Variable
             * R_{cuvij}
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                Flows.put(trafficDemand, new HashMap<>());
                for (List<StateCopy> stateCopies : combinations.get(trafficDemand)) {
                    Flows.get(trafficDemand).put(stateCopies, new HashMap<>());
                    for (Edge edge : graph.getallEdges()) {
                        Flows.get(trafficDemand).get(stateCopies)
                                .put(edge, cplex.boolVar("Rcuvij_" +
                                        StateCopyCombinationToString(stateCopies)+ "_" +
                                        trafficDemand.getSource().getLabel() + "_" +
                                        trafficDemand.getDestination().getLabel() + "_" +
                                        edge.getSource().getLabel() + "_" +
                                        edge.getDestination().getLabel()));
                    }
                }
            }

            /**
             *
             * Defining StateVariable Placement variable
             *
             * P_{S_fn}
             *
             */


            for (Vertex vertex : graph.getVertices()){
                Placement.put(vertex, new HashMap<>());
                for (StateVariable stateVariable : stateStore.getStateVariables())
                    for(StateCopy stateCopy : stateStore.getStateCopies(stateVariable))
                        Placement.get(vertex)
                                .put(stateCopy,cplex.boolVar("P_" + stateCopy.getStateCopyString()
                                + "_" + vertex.getLabel()));
            }

            /**
             *
             * Defining X_{cuv}
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                X.put(trafficDemand, new HashMap<>());
                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                    X.get(trafficDemand).put(stateCopyCombination,
                            cplex.boolVar("X_u_" + trafficDemand.getSource().getLabel() +
                                    "_v_" + trafficDemand.getSource().getLabel() + "_" +
                                    StateCopyCombinationToString(stateCopyCombination)));
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
                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                    PTracker.get(trafficDemand).put(stateCopyCombination, new HashMap<>());
                    for(StateCopy stateCopy : stateCopyCombination){
                        PTracker.get(trafficDemand).get(stateCopyCombination).put(stateCopy, new HashMap<>());
                        for (Edge edge : graph.getallEdges()) {
                            PTracker.get(trafficDemand).get(stateCopyCombination).get(stateCopy)
                                    .put(edge, cplex.boolVar("Pcuvij_" +
                                            StateCopyCombinationToString(stateCopyCombination) + "_(" +
                                            stateCopy.getStateCopyString() + ")_" +
                                            trafficDemand.getSource().getLabel() + "_" +
                                            trafficDemand.getDestination().getLabel() + "_" +
                                            edge.getSource().getLabel() + "_" +
                                            edge.getDestination().getLabel()));
                        }
                    }
                }
            }

            /**
             *
             * Defining Y_{cuvn}
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                Y.put(trafficDemand, new HashMap<>());
                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                    Y.get(trafficDemand).put(stateCopyCombination, new HashMap<>());
                    for (StateCopy stateCopy : stateCopyCombination) {
                        Y.get(trafficDemand).get(stateCopyCombination).put(stateCopy, new HashMap<>());
                        for (Vertex vertex : graph.getVertices()) {
                            Y
                                    .get(trafficDemand)
                                    .get(stateCopyCombination)
                                    .get(stateCopy)
                                    .put(vertex, cplex.boolVar(
                                    "Ycuvn_" + StateCopyCombinationToString(stateCopyCombination) + "_("
                                            + stateCopy.getStateCopyString() + ")_"
                                            + trafficDemand.getSource().getLabel() + "_"
                                            + trafficDemand.getDestination().getLabel() + "_"
                                            + vertex.getLabel())
                                    );
                        }
                    }
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

                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                    for (Vertex vertex : graph.getSuccessors(trafficDemand.getSource())) {
                        if (!vertex.equals(trafficDemand.getSource()) ||
                                trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                            SourceOutgoing
                                    .get(trafficDemand)
                                    .addTerm(1.0, Flows
                                            .get(trafficDemand)
                                            .get(stateCopyCombination)
                                            .get(graph.getEdge(trafficDemand.getSource(), vertex)));
                        }
                    }


                    for (Vertex vertex : graph.getPredecessors(trafficDemand.getSource())) {
                        if (!vertex.equals(trafficDemand.getSource()) ||
                                trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                            SourceIncoming
                                    .get(trafficDemand)
                                    .addTerm(1.0, Flows
                                            .get(trafficDemand)
                                            .get(stateCopyCombination)
                                            .get(graph.getEdge(vertex, trafficDemand.getSource())));
                        }
                    }

                }

                if (trafficDemand.getSource().equals(trafficDemand.getDestination()))
                    cplex.addEq(SourceOutgoing.get(trafficDemand),1);

                else
                    cplex.addEq(
                            SourceOutgoing.get(trafficDemand),
                            cplex.sum(
                                    SourceIncoming.get(trafficDemand),
                                    cplex.constant(1)
                            )
                    );

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

                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                    if (!trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                        for (Vertex vertex : graph.getSuccessors(trafficDemand.getDestination())) {
                            if (!vertex.equals(trafficDemand.getDestination()) ||
                                    trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                                DestinationOutgoing
                                        .get(trafficDemand)
                                        .addTerm(1.0, Flows
                                                .get(trafficDemand)
                                                .get(stateCopyCombination)
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
                                            .get(stateCopyCombination)
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
                    for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                        edgeTraffic.addTerm(trafficDemand.getDemand(),
                                Flows
                                .get(trafficDemand)
                                .get(stateCopyCombination)
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

                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {

                    IncomingFlows.get(trafficDemand).putIfAbsent(stateCopyCombination, new HashMap<>());
                    OutgoingFlows.get(trafficDemand).putIfAbsent(stateCopyCombination, new HashMap<>());

                    for(Vertex n : graph.getVertices()) {
                        IncomingFlows.get(trafficDemand).get(stateCopyCombination).put(n,cplex.linearNumExpr());
                        for (Vertex i : graph.getPredecessors(n)){
                            IncomingFlows
                                    .get(trafficDemand)
                                    .get(stateCopyCombination)
                                    .get(n)
                                    .addTerm(1.0, Flows.get(trafficDemand)
                                                         .get(stateCopyCombination)
                                                         .get(graph.getEdge(i,n)));
                        }

                        OutgoingFlows.get(trafficDemand).get(stateCopyCombination).put(n,cplex.linearNumExpr());
                        for (Vertex j : graph.getSuccessors(n)){
                            OutgoingFlows
                                    .get(trafficDemand)
                                    .get(stateCopyCombination)
                                    .get(n)
                                    .addTerm(1.0, Flows.get(trafficDemand)
                                                        .get(stateCopyCombination)
                                                        .get(graph.getEdge(n,j)));
                        }

                        if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())){
                            cplex.addEq(IncomingFlows.get(trafficDemand).get(stateCopyCombination).get(n),
                                    OutgoingFlows.get(trafficDemand).get(stateCopyCombination).get(n));

                        }
                    }
                }
            }


            /**
             *
             * Copies cannot be placed in the same switch
             * \sum_f P_{S_fn} \le 1
             * \forall n
             *
             */



            IloLinearNumExpr temp1;
            for(StateVariable stateVariable : stateStore.getStateVariables()) {
                for (Vertex vertex : graph.getVertices()) {
                    temp1 = cplex.linearNumExpr();
                    for(StateCopy stateCopy : stateStore.getStateCopies(stateVariable)){
                        temp1.addTerm(1.0, Placement.get(vertex).get(stateCopy));
                    }
                    cplex.addLe(temp1, 1.0);
                }
            }


            /**
             *
             * Each copy is at only 1 switch
             * \sum_n P_{fn} = 1
             * \forall c
             *
             */



            IloLinearNumExpr temp2;
            for(StateVariable stateVariable : stateStore.getStateVariables()) {
                for(StateCopy stateCopy : stateStore.getStateCopies(stateVariable)){
                    temp2 = cplex.linearNumExpr();
                    for (Vertex vertex : graph.getVertices()) {
                        temp2.addTerm(1.0, Placement.get(vertex).get(stateCopy));
                    }
                    cplex.addEq(temp2, 1.0);
                }
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
                for (List<StateCopy> stateCopyCombination : Flows.get(trafficDemand).keySet()) {
                    IloLinearNumExpr temp = cplex.linearNumExpr();
                    for (Vertex vertex : graph.getSuccessors(trafficDemand.getSource())) {

                        temp.addTerm(1.0, Flows
                                .get(trafficDemand)
                                .get(stateCopyCombination)
                                .get(graph.getEdge(trafficDemand.getSource(), vertex)));

                        /*
                        X.get(trafficDemand).get(stateCopyCombination).addTerm(1.0, Flows
                                .get(trafficDemand)
                                .get(stateCopyCombination)
                                .get(graph.getEdge(trafficDemand.getSource(), vertex)));
                                */

                    }
                    if (!trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                        for (Vertex vertex : graph.getPredecessors(trafficDemand.getSource())) {
                            temp.addTerm(-1.0, Flows
                                    .get(trafficDemand)
                                    .get(stateCopyCombination)
                                    .get(graph.getEdge(vertex, trafficDemand.getSource())));
                                    /*
                            X.get(trafficDemand).get(stateCopyCombination).addTerm(-1.0, Flows
                                    .get(trafficDemand)
                                    .get(stateCopyCombination)
                                    .get(graph.getEdge(vertex, trafficDemand.getSource())));
                                    */
                        }
                    }

                    cplex.addEq(temp, X.get(trafficDemand).get(stateCopyCombination));
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
                        for (List<StateCopy> stateCopyCombination : Flows.get(trafficDemand).keySet()) {
                            for(StateCopy stateCopy : stateCopyCombination) {
                                cplex.addGe(
                                        IncomingFlows.get(trafficDemand).get(stateCopyCombination).get(n),
                                        cplex.sum(
                                                Placement.get(n).get(stateCopy),
                                                X.get(trafficDemand).get(stateCopyCombination),
                                                cplex.constant(-1.0)
                                        )
                                );
                            }
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
                for (List<StateCopy> stateCopies : Flows.get(trafficDemand).keySet()){
                    for(StateCopy stateCopy : stateCopies) {
                        for (Edge edge : Flows.get(trafficDemand).get(stateCopies).keySet()) {
                            cplex.addGe(Flows.get(trafficDemand).get(stateCopies).get(edge),
                                    PTracker.get(trafficDemand).get(stateCopies).get(stateCopy).get(edge));
                        }


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
             * TODO: Need to implement this!
             *
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (List<StateCopy> stateCopies: Flows.get(trafficDemand).keySet()) {
                    for(StateCopy stateCopy : stateCopies) {
                        IloLinearNumExpr sourceTemp = cplex.linearNumExpr();
                        for (Vertex vertex : graph.getSuccessors(trafficDemand.getSource())) {
                            if (!vertex.equals(trafficDemand.getSource()) ||
                                    trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                                sourceTemp.addTerm(1.0, Flows
                                        .get(trafficDemand)
                                        .get(stateCopies)
                                        .get(graph.getEdge(trafficDemand.getSource(), vertex)));
                            }
                        }
                        if (!trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                            for (Vertex vertex : graph.getPredecessors(trafficDemand.getSource())) {
                                sourceTemp.addTerm(-1.0, Flows
                                        .get(trafficDemand)
                                        .get(stateCopies)
                                        .get(graph.getEdge(vertex, trafficDemand.getSource())));
                            }
                        }

                        cplex.addGe(sourceTemp, Y.get(trafficDemand)
                                        .get(stateCopies)
                                        .get(stateCopy)
                                        .get(trafficDemand.getSource())
                        );
                    }
                }
            }




            /**
             * Initialize Pcuvij
             */


            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                TrackIncoming.put(trafficDemand, new HashMap<>());
                TrackOutgoing.put(trafficDemand, new HashMap<>());

                for (List<StateCopy> stateCopies : Flows.get(trafficDemand).keySet()){
                    TrackIncoming.get(trafficDemand).put(stateCopies, new HashMap<>());
                    TrackOutgoing.get(trafficDemand).put(stateCopies, new HashMap<>());
                    for(StateCopy stateCopy : stateCopies) {
                        TrackIncoming.get(trafficDemand).get(stateCopies).put(stateCopy, new HashMap<>());
                        TrackOutgoing.get(trafficDemand).get(stateCopies).put(stateCopy, new HashMap<>());

                        for (Vertex n : graph.getVertices()) {
                            TrackIncoming.get(trafficDemand)
                                    .get(stateCopies)
                                    .get(stateCopy)
                                    .put(n, cplex.linearNumExpr());
                            TrackOutgoing.get(trafficDemand).
                                    get(stateCopies)
                                    .get(stateCopy)
                                    .put(n, cplex.linearNumExpr());

                            for (Vertex i : graph.getPredecessors(n)) {
                                TrackIncoming
                                        .get(trafficDemand)
                                        .get(stateCopies)
                                        .get(stateCopy)
                                        .get(n)
                                        .addTerm(
                                                1.0,
                                                PTracker.get(trafficDemand)
                                                        .get(stateCopies)
                                                        .get(stateCopy)
                                                        .get(graph.getEdge(i, n))
                                        );
                            }

                            for (Vertex j : graph.getSuccessors(n)) {
                                TrackOutgoing
                                        .get(trafficDemand)
                                        .get(stateCopies)
                                        .get(stateCopy)
                                        .get(n)
                                        .addTerm(
                                                1.0,
                                                PTracker.get(trafficDemand)
                                                        .get(stateCopies)
                                                        .get(stateCopy)
                                                        .get(graph.getEdge(n, j))
                                        );
                            }
                        }
                    }
                }
            }

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                for (List<StateCopy> stateCopies : combinations.get(trafficDemand)) {
                    for(StateCopy stateCopy : stateCopies) {
                        for (Vertex n : graph.getVertices()) {

                            /**
                             *
                             * Y_{cuvn} = P_{cn} AND X_{cuv}
                             *
                             * Y_{cuvn} \ge P_{cn} + X_{cuv} - 1
                             * Y_{cuvn} \le P_{cn}
                             * Y_{cuvn} \le X_{cuv}
                             *
                             */



                            cplex.addGe(
                                    Y.get(trafficDemand).get(stateCopies).get(stateCopy).get(n),
                                    cplex.sum(Placement.get(n).get(stateCopy),
                                    X.get(trafficDemand).get(stateCopies),
                                    cplex.constant(-1.0))
                            );

                            cplex.addLe(
                                    Y.get(trafficDemand).get(stateCopies).get(stateCopy).get(n),
                                    Placement.get(n).get(stateCopy)
                            );
                            cplex.addLe(
                                    Y.get(trafficDemand).get(stateCopies).get(stateCopy).get(n),
                                    X.get(trafficDemand).get(stateCopies)
                            );





                            if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())) {

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



                                cplex.addEq(
                                        cplex.sum(
                                                Y.get(trafficDemand)
                                                        .get(stateCopies)
                                                        .get(stateCopy)
                                                        .get(n),
                                        TrackIncoming
                                                .get(trafficDemand)
                                                .get(stateCopies)
                                                .get(stateCopy)
                                                .get(n)),
                                        TrackOutgoing
                                                .get(trafficDemand)
                                                .get(stateCopies)
                                                .get(stateCopy)
                                                .get(n)
                                );


                            }

                            if (n.equals(trafficDemand.getDestination())) {

                                /**
                                 *
                                 * Tracking at the destination if flow has passed the state
                                 *
                                 * Y_{cuvv}+\sum_i P_{cuviv}=X_{cuv} \quad \forall c \quad \forall u,v
                                 * \forall u,v
                                 * \forall c
                                 *
                                 */



                                cplex.addEq(
                                        cplex.sum(
                                                Y.get(trafficDemand)
                                                        .get(stateCopies)
                                                        .get(stateCopy)
                                                        .get(n),
                                        TrackIncoming
                                                .get(trafficDemand)
                                                .get(stateCopies)
                                                .get(stateCopy).get(n)
                                        ),
                                        X.get(trafficDemand).get(stateCopies));

                            }
                        }
                    }
                }
            }



            /**
             *
             * Flows must pass states as defined in the state sequence
             *
             * P_{s_fn} + \sum_i P_{s_fuvin} \ge P_{t_gn} + X_cuv -1
             * \forall f \in F_s
             * \forall g \in F_t
             * \forall s_f,t_g \in c
             * \forall c \in C_{u,v}
             * \forall n
             * \forall u,v
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (List<StateCopy> stateCopies : combinations.get(trafficDemand)) {
                    for(int i=0 ; i<=stateCopies.size()-2 ; i++){
                        for(Vertex vertex : graph.getVertices()){
                            cplex.addGe(
                                    cplex.sum(
                                            Placement.get(vertex).get(stateCopies.get(i)),
                                            TrackIncoming.get(trafficDemand)
                                                    .get(stateCopies)
                                                    .get(stateCopies.get(i))
                                                    .get(vertex)
                                    ),
                                    cplex.sum(
                                        Placement.get(vertex).get(stateCopies.get(i+1)),
                                        X.get(trafficDemand).get(stateCopies),
                                        cplex.constant(-1.0)
                                    )
                            );
                        }
                    }
                }
            }



            /**
             * Flows at self edges = 0
             *  if src != dst
             */



            for(TrafficDemand trafficDemand : Flows.keySet()){
                for(List<StateCopy> stateCopies : Flows.get(trafficDemand).keySet()){
                    for(Edge edge : Flows.get(trafficDemand).get(stateCopies).keySet()){
                        if(edge.getSource().equals(edge.getDestination())
                                && (!trafficDemand.getSource().equals(edge.getDestination()))){
                            cplex.addEq(Flows.get(trafficDemand).get(stateCopies).get(edge), 0.0);
                        }
                    }
                }
            }



            /**
             * PAuvij>=PBuvij
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for(List<StateCopy> stateCopies : PTracker.get(trafficDemand).keySet()){
                    for(int i=0 ; i<stateCopies.size()-1 ; i++){
                        for(Edge edge : graph.getallEdges()){
                            cplex.addGe(
                                    PTracker.get(trafficDemand).get(stateCopies).get(stateCopies.get(i)).get(edge),
                                    PTracker.get(trafficDemand).get(stateCopies).get(stateCopies.get(i+1)).get(edge)
                            );
                        }
                    }
                }
            }

            if (fixConstraints)
                FixVariables();

            cplex.exportModel("test_depShardedSNAP.lp");

        }
        catch(IloException e){
            System.out.println("Exception " + e +" caught while building the model");
        }
    }

    private void setObjective(){
        try {
            IloLinearNumExpr objective = cplex.linearNumExpr();

            for (TrafficDemand trafficDemand : Flows.keySet()) {
                for (List<StateCopy> stateCopyCombination : Flows.get(trafficDemand).keySet()) {
                    for (Edge edge : Flows.get(trafficDemand).get(stateCopyCombination).keySet()) {
                        objective.addTerm(trafficDemand.getDemand(), Flows
                                .get(trafficDemand)
                                .get(stateCopyCombination)
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

    public boolean optimize(){
        try{
            return cplex.solve();
        }
        catch  (IloException e)  {
            System.out.println("Exception " + e + " caught while optimizing the model");
        }
        return false;
    }

    public double getObjectiveValue(){
        double objectiveValue = 0.0;
        try {

            for (TrafficDemand trafficDemand : Flows.keySet()) {
                for (List<StateCopy> stateCopies: Flows.get(trafficDemand).keySet()) {
                    for (Edge edge : Flows.get(trafficDemand).get(stateCopies).keySet()) {
                        if (cplex.getValue(Flows.get(trafficDemand).get(stateCopies).get(edge)) > 0) {
                            if (!edge.getSource().equals(edge.getDestination())) {
                                objectiveValue += trafficDemand.getDemand()*
                                        cplex.getValue(Flows.get(trafficDemand).get(stateCopies).get(edge));
                            }
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

    public void printSolution(){

        try {

            System.out.println("####################################################################");
            System.out.println("#      Sharded SNAP Optimization with dependencies                 #");
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

            for (TrafficDemand trafficDemand : Flows.keySet()) {
                for (List<StateCopy> stateCopies: Flows.get(trafficDemand).keySet()) {
                    for (Edge edge : Flows.get(trafficDemand).get(stateCopies).keySet()) {
                        if (cplex.getValue(Flows.get(trafficDemand).get(stateCopies).get(edge)) > 0) {
                            System.out.println("R_" + StateCopyCombinationToString(stateCopies)
                                    + "_" + trafficDemand.getSource().getLabel()
                                    + "_" + trafficDemand.getDestination().getLabel()
                                    + "_" + edge.getSource().getLabel()
                                    + "_" + edge.getDestination().getLabel() + " = " +
                                    cplex.getValue(Flows.get(trafficDemand).get(stateCopies).get(edge)));
                        }
                    }
                }
                System.out.println();
            }



            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                    for(StateCopy stateCopy : stateCopyCombination){
                        for (Edge edge : graph.getallEdges()) {
                            if(cplex.getValue(PTracker
                                    .get(trafficDemand)
                                    .get(stateCopyCombination)
                                    .get(stateCopy)
                                    .get(edge))>0)
                                System.out.println("Pcuvij_" +
                                        trafficDemand.getSource().getLabel() + "_" +
                                        trafficDemand.getDestination().getLabel() + "_" +
                                        StateCopyCombinationToString(stateCopyCombination) + "_(" +
                                        stateCopy.getStateCopyString() + ")_" +
                                        edge.getSource().getLabel() + "_" +
                                        edge.getDestination().getLabel() + " = 1.0");
                        }
                    }
                }
            }

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                    if(cplex.getValue(X.get(trafficDemand).get(stateCopyCombination))>0) {
                        System.out.println("X_u_" + trafficDemand.getSource().getLabel() +
                                "_v_" + trafficDemand.getSource().getLabel() + "_" +
                                StateCopyCombinationToString(stateCopyCombination) + " = 1.0");
                    }
                }
            }


            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (List<StateCopy> stateCopyCombination : combinations.get(trafficDemand)) {
                    for (StateCopy stateCopy : stateCopyCombination) {
                        for (Vertex vertex : graph.getVertices()) {
                            if(cplex.getValue(Y.get(trafficDemand).get(stateCopyCombination)
                                    .get(stateCopy).get(vertex))>0)
                                System.out.println(
                                        "Ycuvn_" + trafficDemand.getSource().getLabel() + "_" +
                                                + trafficDemand.getDestination().getLabel() + "_"
                                                + StateCopyCombinationToString(stateCopyCombination) + "_(" +
                                                stateCopy.getStateCopyString() + ")_"
                                                 + vertex.getLabel()
                                );
                        }
                    }
                }
            }


        }
        catch (IloException e){
            System.err.println("Concert exception caught while optimizing:"  +  e);
        }
    }

    private void getCopyCombinations(Object[][] sets, int n, Object[] prefix,
                                   HashMap<TrafficDemand, List<List<StateCopy>>> combinations,
                                   TrafficDemand trafficDemand){
        if(n >= sets.length){
            LinkedList<StateCopy> stateCopies = new LinkedList<>();
            int i=0;
            for(Object o: prefix){
                StateVariable currentState = dependencies.get(trafficDemand).get(i);
                StateCopy tempStateCopy = stateStore.getStateCopy(currentState,(Integer)o);
                stateCopies.add(tempStateCopy);
                i++;
            }
            combinations.get(trafficDemand).add(stateCopies);
            return;
        }
        for(Object o : sets[n]){
            Object[] newPrefix = Arrays.copyOfRange(prefix,0,prefix.length+1);
            newPrefix[newPrefix.length-1] = o;
            getCopyCombinations(sets, n+1, newPrefix, combinations, trafficDemand);
        }
    }

    private String StateCopyCombinationToString(List<StateCopy> stateCopyCombination){

        String sccString = "";

        for (StateCopy stateCopy : stateCopyCombination) {
            sccString = sccString + stateCopy.getLabel() + stateCopy.getCopyNumber() + "_";
        }


        return sccString.substring(0, sccString.length()-1);



    }

    private List<StateCopy> getListStateCopy(LinkedList<String> states, LinkedList<Integer> copyNumbers){

        boolean test;

        for(TrafficDemand trafficDemand: combinations.keySet()){
            for(List<StateCopy> stateCopies : combinations.get(trafficDemand)){
                if(stateCopies.size()==copyNumbers.size()){
                    test = true;
                    for(int i=0 ; i<stateCopies.size() ; i++){
                        if(!(stateCopies.get(i).getState().equals(stateStore.getStateVariable(states.get(i)))
                                &&
                                stateCopies.get(i).getCopyNumber() == copyNumbers.get(i))){
                            test = false;
                            break;
                        }
                    }
                    if(test)
                        return stateCopies;
                }
            }
        }
        return null;

    }

    private void FixVariables() {

        try {


            for (Vertex vertex : graph.getVertices()) {
                for (StateVariable stateVariable : stateStore.getStateVariables()) {
                    for (StateCopy stateCopy : stateStore.getStateCopies(stateVariable)) {

                        if(stateCopy.getState().getLabel().equals("a") &&
                                stateCopy.getCopyNumber()==1 &&
                                vertex.getLabel()==5){
                            cplex.addEq(Placement.get(vertex).get(stateCopy),1);
                        }

                        else if(stateCopy.getState().getLabel().equals("a") &&
                                stateCopy.getCopyNumber()==2 &&
                                vertex.getLabel()==10){
                            cplex.addEq(Placement.get(vertex).get(stateCopy),1);
                        }

                        else if(stateCopy.getState().getLabel().equals("b") &&
                                stateCopy.getCopyNumber()==1 &&
                                vertex.getLabel()==5){
                            cplex.addEq(Placement.get(vertex).get(stateCopy),1);
                        }
                        else if(stateCopy.getState().getLabel().equals("b") &&
                                stateCopy.getCopyNumber()==2 &&
                                vertex.getLabel()==8){
                            cplex.addEq(Placement.get(vertex).get(stateCopy),1);
                        }
                        else if(stateCopy.getState().getLabel().equals("c") &&
                                stateCopy.getCopyNumber()==1 &&
                                vertex.getLabel()==5){
                            cplex.addEq(Placement.get(vertex).get(stateCopy),1);
                        }

                        else{
                            cplex.addEq(Placement.get(vertex).get(stateCopy),0);
                        }
                    }
                }
            }



            LinkedList<String> StringAB = new LinkedList<>(); StringAB.add("A"); StringAB.add("B");
            LinkedList<Integer> int21 = new LinkedList<>(); int21.add(2); int21.add(1);

            //LinkedList<Integer> int12 = new LinkedList<>(); int12.add(1); int11.add(2);
            //LinkedList<Integer> int21 = new LinkedList<>(); int21.add(2); int11.add(1);
            //LinkedList<Integer> int22 = new LinkedList<>(); int22.add(2); int11.add(2);

            List<StateCopy> A2B1 = getListStateCopy(StringAB, int21);


            /*
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (List<StateCopy> stateCopies : Flows.get(trafficDemand).keySet()) {
                    for (Edge edge : Flows.get(trafficDemand).get(stateCopies).keySet()) {
                        if (edge.getSource().getLabel() == 8 && edge.getDestination().getLabel() == 7
                                && stateCopies.equals(A2B1))
                            cplex.addEq(Flows.get(trafficDemand).get(stateCopies).get(edge), 1.0);

                        else if(edge.getSource().getLabel() == 7 && edge.getDestination().getLabel() == 6
                                && stateCopies.equals(A2B1))
                            cplex.addEq(Flows.get(trafficDemand).get(stateCopies).get(edge), 1.0);

                        else
                            cplex.addEq(Flows.get(trafficDemand).get(stateCopies).get(edge), 0.0);
                    }
                }
            }
            */






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


