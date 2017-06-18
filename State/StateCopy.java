package statefulsharding.State;

import statefulsharding.State.StateVariable;
import statefulsharding.graph.Vertex;

/**
 * Created by root on 6/11/17.
 */
public class StateCopy {

    private StateVariable state;
    private int copyNumber;
    private Vertex vertex;

    public StateCopy(StateVariable state, int copyNumber){
        this.state=state;
        this.copyNumber=copyNumber;
    }

    public String getLabel() {
        return state.getLabel();
    }

    public int getCopies(){
        return state.getCopies();
    }

    public int getCopyNumber(){
        return copyNumber;
    }

    public StateVariable getState(){
        return state;
    }

    public String getStateCopyString(){
        String stateCopyString = getLabel() + copyNumber;
        return stateCopyString;
    }

    public void setVertex(Vertex vertex){
        this.vertex = vertex;
    }

    public Vertex getVertex(){
        return vertex;
    }


    public boolean equals(Object other){
        boolean result=false;

        if (other instanceof StateCopy){
            StateCopy that = (StateCopy) other;
            result = (this.getLabel().equals(that.getLabel()) && this.getCopyNumber()==that.getCopyNumber());
        }
        return result;
    }



}
