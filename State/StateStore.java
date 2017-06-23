package statefulsharding.State;

import statefulsharding.Traffic.TrafficDemand;
import statefulsharding.Traffic.TrafficStore;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by root on 6/11/17.
 */
public class StateStore {

    private Set<StateVariable> stateVariables;
    private HashMap<StateVariable, LinkedList<StateCopy>> stateCopies;
    private Set<String> stateVariableString;

    public StateStore() {
        stateVariables = new HashSet<>();
        stateCopies = new HashMap<>();
        stateVariableString = new HashSet<>();
    }

    public void addStateVariable(StateVariable stateVariable) {
        if(!stateVariables.contains(stateVariable)) {
            stateVariables.add(stateVariable);
            /*
            stateCopies.putIfAbsent(stateVariable, new LinkedList<>());
            for (int i = 1; i <= stateVariable.getCopies(); i++) {
                stateCopies.get(stateVariable).add(new StateCopy(stateVariable, i));
            }
            */
            stateVariableString.add(stateVariable.getLabel());
        }
    }

    public void addStateVariable(String string) {
        if(!checkStateVariable(string)) {
            StateVariable stateVariable = new StateVariable(string);
            stateVariables.add(stateVariable);
            /*
            stateCopies.putIfAbsent(stateVariable, new LinkedList<>());
            for (int i = 1; i <= stateVariable.getCopies(); i++) {
                stateCopies.get(stateVariable).add(new StateCopy(stateVariable, i));
            }
            */
            stateVariableString.add(stateVariable.getLabel());
        }
    }

    public void addStateVariable(String string, int numCopies) {
        if(!checkStateVariable(string)) {
            StateVariable stateVariable = new StateVariable(string, numCopies);
            stateVariables.add(stateVariable);
            /*
            stateVariables.add(stateVariable);
            stateCopies.putIfAbsent(stateVariable, new LinkedList<>());
            for (int i = 1; i <= stateVariable.getCopies(); i++) {
                stateCopies.get(stateVariable).add(new StateCopy(stateVariable, i));
            }
            */
            stateVariableString.add(stateVariable.getLabel());
        }
    }

    public void addStateCopy(StateVariable stateVariable, StateCopy stateCopy) {
        stateVariables.add(stateVariable);
        stateCopies.putIfAbsent(stateVariable, new LinkedList<>());
        stateCopies.get(stateVariable).add(stateCopy);
    }


    public void setStateCopies(String string, int numCopies){
        StateVariable stateVariable = getStateVariable(string);
        stateVariable.setCopies(numCopies);
        stateCopies.putIfAbsent(stateVariable, new LinkedList<>());
        for (int i = 1; i <= numCopies; i++) {
            stateCopies.get(stateVariable).add(new StateCopy(stateVariable, i));
        }
    }

    public Set<StateVariable> getStateVariables() {
        return stateVariables;
    }

    public StateVariable getStateVariable(String string) {
        for (StateVariable stateVariable : stateVariables) {
            if(stateVariable.getLabel().equals(string)){
                return stateVariable;
            }
        }
        return null;
    }

    public LinkedList<StateCopy> getStateCopies(StateVariable stateVariable) {
        return stateCopies.get(stateVariable);
    }

    public StateCopy getStateCopy(StateVariable stateVariable, Integer copyNumber) {

        StateCopy stateCopy = null;

        if(stateCopies.get(stateVariable)!=null) {
            for (StateCopy copy : stateCopies.get(stateVariable))
                if (copy.getCopyNumber() == copyNumber)
                    return copy;
        }

        return stateCopy;
    }

    public StateCopy getStateCopy(String stateString, Integer copyNumber) {

        StateCopy stateCopy = null;

        for(StateVariable stateVariable : stateVariables){
            if(stateVariable.getLabel().equals(stateString)){
                if(stateCopies.get(stateVariable)!=null) {
                    for (StateCopy copy : stateCopies.get(stateVariable))
                        if (copy.getCopyNumber() == copyNumber)
                            return copy;
                }
            }
        }

        return stateCopy;
    }

    public boolean checkStateVariable(String string){
        return stateVariableString.contains(string);
    }

    public int getNumStates(){
        return stateVariables.size();
    }

    public static void GenerateWriteStates(int maxDependencySize, int maxRun, String filename){

        for(int dependencySize =1 ; dependencySize<=maxDependencySize ; dependencySize++) {

            for (int run = 1; run <= maxRun; run++) {

                StateStore stateStore = new StateStore();
                LinkedList<LinkedList<StateVariable>> allDependencies =
                        GenerateStates.BinaryTreeGenerator(dependencySize, stateStore);


                filename = filename + dependencySize + "_" + run + ".txt";

                try {

                    FileWriter partfw = new FileWriter(filename, true);
                    BufferedWriter partbw = new BufferedWriter(partfw);
                    PrintWriter partout = new PrintWriter(partbw);

                    for (LinkedList<StateVariable> dependency : allDependencies) {
                        for (StateVariable stateVariable : dependency) {
                            System.out.print(stateVariable.getLabel() + " ");
                            partout.print(stateVariable.getLabel() + " ");
                        }
                        System.out.println();
                        partout.println();
                    }
                    partout.flush();

                }
                catch (IOException e) {
                    //
                }
            }
        }
    }

    public static LinkedList<LinkedList<StateVariable>> readStateDependency(String filename,
                                                                            StateStore stateStore){

        LinkedList<LinkedList<StateVariable>> allDependencies = new LinkedList<>();

        try{
            FileInputStream fstream = new FileInputStream(filename);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null)   {
                LinkedList<StateVariable> dependency = new LinkedList<>();

                String[] tokens = strLine.split(",");

                for(String string : tokens){
                    stateStore.addStateVariable(string);
                    StateVariable stateVariable = stateStore.getStateVariable(string);
                    dependency.add(stateVariable);
                }

                allDependencies.add(dependency);
            }
            in.close();
        }
        catch(FileNotFoundException ex){
            ex.printStackTrace();
        }
        catch(IOException ex) {
            ex.printStackTrace();;
        }


        return allDependencies;
    }

    public static HashMap<TrafficDemand, LinkedList<StateVariable>> assignStates2Traffic(
                                            TrafficStore trafficStore,
                                            LinkedList<LinkedList<StateVariable>> allDependencies,
                                            String filename,
                                            int assignmentLine){

        HashMap<TrafficDemand, LinkedList<StateVariable>> dependencies = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            String[] strNums;
            int i = 1;

            while ((line = br.readLine()) != null) {
                if (i==assignmentLine){

                    strNums = line.split(",");

                    LinkedList<TrafficDemand> trafficDemands = trafficStore.getTrafficDemands();

                    for(int j=0 ; j<trafficStore.getNumTrafficDemands() ; j++){
                        try {
                            dependencies.put(trafficDemands.get(j),
                                    allDependencies.get(
                                            Integer.parseInt(strNums[j])
                                    ));
                        }
                        catch(ArrayIndexOutOfBoundsException e){
                            int d=1;
                        }
                    }

                    break;
                }
                i++;
            }
        }
        catch(IOException e){
            System.out.println("FileNotFoundException " + e + ", Filename: " + filename);
        }

        return dependencies;
    }


}