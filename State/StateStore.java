package statefulsharding.State;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by root on 6/11/17.
 */
public class StateStore {

    Set<StateVariable> stateVariables;
    HashMap<StateVariable, LinkedList<StateCopy>> stateCopies;

    public StateStore() {
        stateVariables = new HashSet<>();
        stateCopies = new HashMap<>();
    }

    public void addStateVariable(StateVariable stateVariable) {
        stateVariables.add(stateVariable);
        stateCopies.putIfAbsent(stateVariable, new LinkedList<>());
        for(int i=1 ; i<=stateVariable.getCopies() ; i++){
            stateCopies.get(stateVariable).add(new StateCopy(stateVariable, i));
        }
    }

    public void addStateCopy(StateVariable stateVariable, StateCopy stateCopy) {
        stateVariables.add(stateVariable);
        stateCopies.putIfAbsent(stateVariable, new LinkedList<>());
        stateCopies.get(stateVariable).add(stateCopy);
    }

    public Set<StateVariable> getStateVariables() {
        return stateVariables;
    }

    public StateVariable getStateVariable(String string) {
        for (StateVariable stateVariable : stateVariables) {
            if(stateVariable.getLabel() == string){
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
            if(stateVariable.getLabel() == stateString){
                if(stateCopies.get(stateVariable)!=null) {
                    for (StateCopy copy : stateCopies.get(stateVariable))
                        if (copy.getCopyNumber() == copyNumber)
                            return copy;
                }
            }
        }

        return stateCopy;
    }






}