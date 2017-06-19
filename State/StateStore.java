package statefulsharding.State;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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

}