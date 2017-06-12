package statefulsharding.MilpOpt;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;
import statefulsharding.graph.Edge;
import statefulsharding.graph.ListGraph;
import statefulsharding.graph.Vertex;

import java.util.HashMap;


public class ShortestPathOptimization {

    private ListGraph graph;
    private TrafficStore trafficStore;
    public IloCplex cplex;
    public HashMap<TrafficDemand,HashMap<Edge,IloNumVar>> Flows;
    private double objectiveValue;
    private boolean objectiveValueFixed = false;

    public ShortestPathOptimization(ListGraph graph, TrafficStore trafficStore,
                                    boolean outputRequired, boolean fixConstraints){
        this.graph = graph;
        this.trafficStore = trafficStore;
        Flows = new HashMap<>();

        try {
            this.cplex = new IloCplex();
        }
        catch(IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
        buildModel(outputRequired,fixConstraints);
    }

    public void buildModel(boolean outputRequired, boolean fixConstraints){
        try{

            if(!outputRequired){
                cplex.setOut(null);
            }

            Flows.clear();
            cplex.clearModel();
            HashMap<TrafficDemand,IloLinearNumExpr> SourceIncoming = new HashMap<>();
            HashMap<TrafficDemand,IloLinearNumExpr> SourceOutgoing = new HashMap<>();
            HashMap<TrafficDemand,IloLinearNumExpr> DestinationIncoming = new HashMap<>();
            HashMap<TrafficDemand,IloLinearNumExpr> DestinationOutgoing = new HashMap<>();
            HashMap<TrafficDemand,HashMap<Vertex,IloLinearNumExpr>> IncomingFlows =
                    new HashMap<>();
            HashMap<TrafficDemand,HashMap<Vertex,IloLinearNumExpr>> OutgoingFlows =
                    new HashMap<>();

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
             * Calling the objective function to be minimized
             */

            setObjective();

            /////////////////////////////// CONSTRAINTS //////////////////////////////////

            /**
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

            if (fixConstraints)
                FixVariables();

            cplex.exportModel("test_shortestpath.lp");

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
                if (trafficDemand.getSource().getLabel() == 0 && trafficDemand.getDestination().getLabel() == 1) {
                    for (Edge edge : Flows.get(trafficDemand).keySet()) {
                        if (edge.getSource().getLabel() == 0 && edge.getDestination().getLabel() == 1) {
                            cplex.addEq(Flows.get(trafficDemand).get(edge), 1.0);
                        }
                        else if (edge.getSource().getLabel() == 1 && edge.getDestination().getLabel() == 2) {
                            cplex.addEq(Flows.get(trafficDemand).get(edge), 1.0);
                        }
                        else if (edge.getSource().getLabel() == 2 && edge.getDestination().getLabel() == 1) {
                            cplex.addEq(Flows.get(trafficDemand).get(edge), 1.0);
                        }
                        else {
                            cplex.addEq(Flows.get(trafficDemand).get(edge), 0.0);
                        }
                    }
                }
            }
        }
        catch (IloException e) {
            System.out.println("Caught IloException error" + e);
        }
    }

    public void printSolution(){

        try {

            double obj = cplex.getObjValue();

            System.out.println("####################################################################");
            System.out.println("######      Shortest Path Optimization: Model Solved     ###########");
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

        }

        catch (IloException e){
            System.err.println("Concert exception caught:"  +  e);
        }
    }

    public double getObjectiveValue(){
        if (!objectiveValueFixed){
            try {
                objectiveValue = cplex.getObjValue();
                for (TrafficDemand trafficDemand : Flows.keySet()) {
                    for (Edge edge : Flows.get(trafficDemand).keySet()) {
                        if (cplex.getValue(Flows.get(trafficDemand).get(edge)) > 0) {
                            if (edge.getSource().equals(edge.getDestination())) {
                                objectiveValue -= cplex.getValue(Flows.get(trafficDemand).get(edge));
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


