package statefulsharding.MilpOpt;

/**
 * Options for the optimization algorithm
 */

public class OptimizationOptions {

    private int copies = 1;
    private boolean verbose = false;
    private boolean fixConstraints = false;
    private boolean multipleStates = false;
    private boolean generateFiles = false;


    /**
     * Constructor for Sharded Optimization
     * @param copies: number of copies required
     * @param verbose: Output of Optimization required
     * @param fixConstraints: Fix custom constraints
     * @param multipleStates: A flow can/cannot cross multiple state copies
     */

    public OptimizationOptions(int copies, boolean verbose, boolean fixConstraints,
                               boolean multipleStates){
        this.copies = copies;
        this.verbose = verbose;
        this.fixConstraints = fixConstraints;
        this.multipleStates = multipleStates;
    }

    /**
     * Constructor for Shortest Path
     * @param verbose: Output of Optimization required
     * @param fixConstraints: Fix custom constraints
     */

    public OptimizationOptions(boolean verbose, boolean fixConstraints){
        this.verbose = verbose;
        this.fixConstraints = fixConstraints;
    }

    public int getCopies(){
        return copies;
    }

    public void setCopies(int copies){
        this.copies=copies;
    }

    public boolean isVerbose(){
        return verbose;
    }

    public boolean isFixConstraints(){
        return fixConstraints;
    }

    public boolean ismultipleStates(){
        return multipleStates;
    }
}
