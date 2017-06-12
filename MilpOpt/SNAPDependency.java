package statefulsharding.MilpOpt;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import statefulsharding.State.StateVariable;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by root on 5/22/17.
 */
public class SNAPDependency {

    private ListGraph graph;
    private TrafficStore trafficStore;
    private IloCplex cplex;
    private HashMap<TrafficDemand, HashMap<Edge, IloNumVar>> Flows;
    private HashMap<TrafficDemand, HashMap<StateVariable, HashMap<Edge, IloNumVar>>> PTracker;
    private HashMap<Vertex, HashMap<StateVariable, IloNumVar>> Placement;
    private double objectiveValue;
    private boolean objectiveValueFixed = false;
    private Set<StateVariable> states;
    private HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies;

    public SNAPDependency(ListGraph graph,
                          TrafficStore trafficStore,
                          boolean outputRequired,
                          boolean fixConstraints,
                          Set<StateVariable> states,
                          HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies){
        this.graph = graph;
        this.trafficStore = trafficStore;
        this.states = states;
        this.dependencies = dependencies;
        Flows = new HashMap<>();
        Placement = new HashMap<>();
        PTracker = new HashMap<>();

        try {
            this.cplex = new IloCplex();
        }
        catch(IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
        buildModel(outputRequired, fixConstraints);
    }

    private void buildModel(boolean outputRequired, boolean fixConstraints){
        try{

            if(!outputRequired){
                cplex.setOut(null);
            }

            Flows.clear();
            cplex.clearModel();
            Placement.clear();

            HashMap<TrafficDemand, IloLinearNumExpr> SourceIncoming = new HashMap<>();
            HashMap<TrafficDemand, IloLinearNumExpr> SourceOutgoing = new HashMap<>();
            HashMap<TrafficDemand, IloLinearNumExpr> DestinationIncoming = new HashMap<>();
            HashMap<TrafficDemand, IloLinearNumExpr> DestinationOutgoing = new HashMap<>();
            HashMap<TrafficDemand, HashMap<Vertex, IloLinearNumExpr>> IncomingFlows =
                    new HashMap<>();
            HashMap<TrafficDemand, HashMap<Vertex, IloLinearNumExpr>> OutgoingFlows =
                    new HashMap<>();
            HashMap<TrafficDemand, HashMap<StateVariable, HashMap<Vertex, IloLinearNumExpr>>> TrackIncoming = new HashMap<>();
            HashMap<TrafficDemand, HashMap<StateVariable, HashMap<Vertex, IloLinearNumExpr>>> TrackOutgoing = new HashMap<>();

            /**
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

            /**
             *
             * Defining Flow Tracker
             *
             * P_{state_uvij}
             *
             * HashMap<TrafficDemand, HashMap<StateVariable, HashMap<Edge, IloNumVar>>> PTracker
             *
             */



            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                PTracker.put(trafficDemand, new HashMap<>());
                for (StateVariable state: dependencies.get(trafficDemand)) {
                    PTracker.get(trafficDemand).put(state,new HashMap<>());
                    for (Edge edge : graph.getallEdges()) {
                        PTracker.get(trafficDemand).get(state)
                                .put(edge, cplex.boolVar("Puvij_" + state.getLabel() +"_" +
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
             * P_{state_n}
             *
             * private HashMap<Vertex, HashMap<StateVariable, IloNumVar>> Placement;
             *
             */

            for (Vertex vertex : graph.getVertices()){
                Placement.put(vertex, new HashMap<>());
                for (StateVariable state: states) {
                    Placement.get(vertex).put(state,
                            cplex.boolVar("P_" + state.getLabel() +"_" + vertex.getLabel()));
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
                    SourceIncoming.putIfAbsent(trafficDemand, cplex.linearNumExpr());
                    SourceOutgoing.putIfAbsent(trafficDemand, cplex.linearNumExpr());

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




            /**
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
                    DestinationIncoming.putIfAbsent(trafficDemand, cplex.linearNumExpr());
                    DestinationOutgoing.putIfAbsent(trafficDemand, cplex.linearNumExpr());

                    for (Vertex vertex : graph.getSuccessors(trafficDemand.getDestination())) {
                        if (!vertex.equals(trafficDemand.getDestination())) {
                            DestinationOutgoing
                                    .get(trafficDemand)
                                    .addTerm(1.0, Flows
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

            /**
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

            /**
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

                IncomingFlows.putIfAbsent(trafficDemand, new HashMap<>());
                OutgoingFlows.putIfAbsent(trafficDemand, new HashMap<>());

                for(Vertex n : graph.getVertices()) {

                    IncomingFlows.get(trafficDemand).put(n, cplex.linearNumExpr());
                    for (Vertex i : graph.getPredecessors(n)){
                        IncomingFlows
                                .get(trafficDemand)
                                .get(n)
                                .addTerm(1.0, Flows.get(trafficDemand).get(graph.getEdge(i,n)));
                    }

                    OutgoingFlows.get(trafficDemand).put(n, cplex.linearNumExpr());
                    for (Vertex j : graph.getSuccessors(n)){
                        OutgoingFlows
                                .get(trafficDemand)
                                .get(n)
                                .addTerm(1.0, Flows.get(trafficDemand).get(graph.getEdge(n, j)));
                    }

                    if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())){
                        cplex.addEq(IncomingFlows.get(trafficDemand).get(n),
                                OutgoingFlows.get(trafficDemand).get(n));

                    }
                }
            }

            /**
             *
             * Each copy is at only 1 switch
             * \sum_n P_{state_n} = 1
             * \forall c
             *
             */

            IloLinearNumExpr temp2;
            for (StateVariable state: states){
                temp2 = cplex.linearNumExpr();
                for (Vertex vertex: graph.getVertices()){
                    temp2.addTerm(1.0,Placement.get(vertex).get(state));
                }
                cplex.addEq(temp2,1.0);
            }

            /**
             *
             * All flows must cross the state
             *
             * \sum_i R_{uvin} \ge P_{state_n}
             * \forall n \ne u,v
             * \forall u,v
             * \forall state \in S_{uv}
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                for(Vertex n : graph.getVertices()) {
                    if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())) {
                        for (StateVariable state: dependencies.get(trafficDemand)) {
                            cplex.addGe(IncomingFlows.get(trafficDemand).get(n), Placement.get(n).get(state));
                        }
                    }
                }
            }

            // Initialize Pcuvij

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                TrackIncoming.put(trafficDemand, new HashMap<>());
                TrackOutgoing.put(trafficDemand, new HashMap<>());

                for (StateVariable state: dependencies.get(trafficDemand)){
                    TrackIncoming.get(trafficDemand).put(state, new HashMap<>());
                    TrackOutgoing.get(trafficDemand).put(state, new HashMap<>());

                    for(Vertex n : graph.getVertices()) {
                        TrackIncoming.get(trafficDemand).get(state).put(n,cplex.linearNumExpr());
                        TrackOutgoing.get(trafficDemand).get(state).put(n,cplex.linearNumExpr());

                        for (Vertex i : graph.getPredecessors(n)){
                            TrackIncoming
                                    .get(trafficDemand)
                                    .get(state)
                                    .get(n)
                                    .addTerm(1.0, PTracker.get(trafficDemand).get(state).get(graph.getEdge(i,n)));
                        }

                        for (Vertex j : graph.getSuccessors(n)){
                            TrackOutgoing
                                    .get(trafficDemand)
                                    .get(state)
                                    .get(n)
                                    .addTerm(1.0, PTracker.get(trafficDemand).get(state).get(graph.getEdge(n,j)));
                        }
                    }
                }
            }

            /**
             *
             * Upper limit of P_{state_uvij}
             *
             * P_{state_uvij} \le R_{uvij}
             *
             * \forall state \in S_{uv}
             * \forall u,v
             * \forall i,j
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (StateVariable state: dependencies.get(trafficDemand)){
                    for (Edge edge : Flows.get(trafficDemand).keySet()) {
                        cplex.addGe(Flows.get(trafficDemand).get(edge),
                                PTracker.get(trafficDemand).get(state).get(edge));
                    }
                }
            }

            /**
             *
             * Ensure that flow leaving a switch containing the state does
             * not pass another switch containing state
             *
             * P_{state_u} \le \sum_j R_{uvuj} - \sum_i R_{uviu}
             * \forall u \ne v
             * \forall state \in S_{uv}
             *
             * P_{state_u} \le \sum_j R_{uvuj}
             * \forall u=v
             * \forall state \in S_{uv}
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (StateVariable state : dependencies.get(trafficDemand)) {
                    IloLinearNumExpr sourceTemp = cplex.linearNumExpr();
                    for (Vertex vertex : graph.getSuccessors(trafficDemand.getSource())) {
                        if (!vertex.equals(trafficDemand.getSource()) ||
                                trafficDemand.getSource().equals(trafficDemand.getDestination())){
                            sourceTemp.addTerm(1.0, Flows
                                    .get(trafficDemand)
                                    .get(graph.getEdge(trafficDemand.getSource(), vertex)));
                        }
                    }
                    if (!trafficDemand.getSource().equals(trafficDemand.getDestination())) {
                        for (Vertex vertex : graph.getPredecessors(trafficDemand.getSource())) {
                            sourceTemp.addTerm(-1.0, Flows
                                    .get(trafficDemand)
                                    .get(graph.getEdge(vertex, trafficDemand.getSource())));
                        }
                    }
                    cplex.addGe(sourceTemp,Placement.get(trafficDemand.getSource()).get(state));
                }
            }

            /**
             *
             * Tracking at an intermediate node if flow has passed the state
             *
             * P_{state_n}+ \sum_i P_{state_uvin} = \sum_j P_{state_uvnj}
             * \forall n \ne u,v
             * \forall u,v
             * \forall state
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                for (StateVariable state: dependencies.get(trafficDemand)) {
                    for(Vertex n : graph.getVertices()) {
                        if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())){
                            cplex.addEq(cplex.sum(Placement.get(n).get(state),
                                    TrackIncoming.get(trafficDemand).get(state).get(n)),
                                    TrackOutgoing.get(trafficDemand).get(state).get(n));
                        }

                        if (n.equals(trafficDemand.getDestination())){
                            cplex.addEq(cplex.sum(Placement.get(n).get(state),
                                    TrackIncoming.get(trafficDemand).get(state).get(n)),
                                    1.0);
                        }
                    }
                }
            }

            /**
             * StateVariable dependencies:
             *
             * if flow has to pass ``s'', then ``t''
             * i.e. u --> s --> t --> v, then:
             *
             * P_{s_n} + \sum_i P_{s_uvin} \ge P_{tn}
             *
             * \forall n,u,v
             * s,t \in S_{uv}
             *
             */

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){

                LinkedList<StateVariable> reqStates = dependencies.get(trafficDemand);

                if(reqStates.size()>1) {
                    for (int i = 0; i < reqStates.size() - 1; i++) {
                        for (Vertex n : graph.getVertices()) {
                            if (!n.equals(trafficDemand.getSource()) && !n.equals(trafficDemand.getDestination())) {
                                cplex.addGe(cplex.sum(Placement.get(n).get(reqStates.get(i)),
                                        TrackIncoming.get(trafficDemand).get(reqStates.get(i)).get(n)),
                                        Placement.get(n).get(reqStates.get(i + 1)));
                            }
                        }
                    }
                }
            }

            if (fixConstraints)
                FixVariables();

            //cplex.exportModel("test_shortestpath.lp");

        }
        catch (IloException e){
            System.out.println("Caught IloException " + e + " while building the model");
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
            System.err.println("Concert exception caught:"  +  e);
        }
        return false;
    }


    private void setObjective(){
        try {

            IloLinearNumExpr objective = cplex.linearNumExpr();

            for (TrafficDemand trafficDemand : Flows.keySet()) {
                for (Edge edge : Flows.get(trafficDemand).keySet()) {
                    objective.addTerm(trafficDemand.getDemand(), Flows
                            .get(trafficDemand)
                            .get(edge));
                }
            }

            cplex.addMinimize(objective);
        }
        catch  (IloException e)  {
            System.err.println("Concert exception caught:"  +  e);
        }
    }

    private void FixVariables() {

        try {
            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()) {
                if (trafficDemand.getSource().getLabel() == 0 && trafficDemand.getDestination().getLabel() == 5) {
                    for (Edge edge : Flows.get(trafficDemand).keySet()) {
                        if (edge.getSource().getLabel() == 0 && edge.getDestination().getLabel() == 3) {
                            cplex.addEq(Flows.get(trafficDemand).get(edge), 1.0);
                        }
                        else if (edge.getSource().getLabel() == 3 && edge.getDestination().getLabel() == 4) {
                            cplex.addEq(Flows.get(trafficDemand).get(edge), 1.0);
                        }
                        else if (edge.getSource().getLabel() == 4 && edge.getDestination().getLabel() == 5) {
                            cplex.addEq(Flows.get(trafficDemand).get(edge), 1.0);
                        }
                        else {
                            cplex.addEq(Flows.get(trafficDemand).get(edge), 0.0);
                        }
                    }
                }
            }

            for(Vertex n : graph.getVertices()) {
                for (StateVariable state : states) {
                    if (state.getLabel() == "s" && n.equals(graph.getVertex(3)))
                        cplex.addEq(Placement.get(n).get(state), 1.0);
                    else if (state.getLabel() == "t" && n.equals(graph.getVertex(4)))
                        cplex.addEq(Placement.get(n).get(state), 1.0);
                    else
                        cplex.addEq(Placement.get(n).get(state), 0.0);
                }
            }


        }
        catch (IloException e) {
            System.out.println("Caught IloException error" + e);
        }
    }

    public void printSolution(){

        try {

            System.out.println("####################################################################");
            System.out.println("###########       SNAP Dependency: Model Solved     ################");
            System.out.println("####################################################################");

            for (TrafficDemand trafficDemand : Flows.keySet()) {
                for (Edge edge : Flows.get(trafficDemand).keySet()) {
                    if (cplex.getValue(Flows.get(trafficDemand).get(edge)) > 0) {
                        System.out.println("R_" + trafficDemand.getSource().getLabel()
                                + "_" + trafficDemand.getDestination().getLabel()
                                + "_" + edge.getSource().getLabel()
                                + "_" + edge.getDestination().getLabel() + " = " +
                                cplex.getValue(Flows.get(trafficDemand).get(edge)));
                    }
                }
                System.out.println();
            }

            System.out.println("obj = " + getObjectiveValue());
            System.out.println();

            for (Vertex vertex : graph.getVertices()) {
                for (StateVariable state : Placement.get(vertex).keySet()) {
                    if (cplex.getValue(Placement.get(vertex).get(state)) > 0) {
                        System.out.println("P_" + state.getLabel() + "_" + vertex.getLabel() + " " +
                                cplex.getValue(Placement.get(vertex).get(state)));
                    }
                }
            }

        }

        catch (IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
    }

    public double getObjectiveValue(){
        if (!objectiveValueFixed){
            try {
                for (TrafficDemand trafficDemand : Flows.keySet()) {
                    for (Edge edge : Flows.get(trafficDemand).keySet()) {
                        if (cplex.getValue(Flows.get(trafficDemand).get(edge)) > 0) {
                            if (!edge.getSource().equals(edge.getDestination())) {
                                objectiveValue = objectiveValue + trafficDemand.getDemand()*
                                        cplex.getValue(Flows.get(trafficDemand).get(edge));
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

    public void clear(){

        try {

            for (TrafficDemand trafficDemand : trafficStore.getTrafficDemands()){
                for (Edge edge : Flows.get(trafficDemand).keySet()){
                    Flows.get(trafficDemand).put(edge,null);
                }
            }

            Flows.clear();
            cplex.clearModel();
            objectiveValueFixed = false;
            objectiveValue=0;
        }

        catch (IloException e){
            System.out.println("Exception " + e +" caught while clearing the model");
        }

    }

}
