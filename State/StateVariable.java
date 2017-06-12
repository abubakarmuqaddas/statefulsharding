package statefulsharding.State;

/**
 * Created by build on 1/2/17.
 */
public class StateVariable {

    private String label;
    private int copies;

    public StateVariable(String label, int copies){
        this.label=label;
        this.copies=copies;
    }

    public StateVariable(String label){
        this.label=label;
    }

    public String getLabel(){
        return label;
    }

    public int getCopies(){
        return copies;
    }

    public boolean equals(Object other){
        boolean result=false;

        if (other instanceof StateVariable){
            StateVariable that = (StateVariable) other;
            result = (this.getLabel().equals(that.getLabel()));
        }
        return result;
    }

}
