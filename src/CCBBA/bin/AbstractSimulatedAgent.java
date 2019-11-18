package CCBBA.bin;

import CCBBA.CCBBASimulation;
import madkit.kernel.AbstractAgent;

import java.awt.*;
import java.util.Vector;

public class AbstractSimulatedAgent extends AbstractAgent {
    /**
     * Agent's Scenario
     */
    protected Scenario environment;

    /**
     * Properties
     */
    protected Dimension location = new Dimension();                 // current location
    protected double speed;                                         // displacement speed of agent
    protected Vector<String> sensors = new Vector<>();              // list of all sensors
    protected Vector<Subtask> J = new Vector<>();                   // list of all subtasks
    protected double miu;                                           // Travel cost
    protected int M;                                                // planning horizon
    protected int O_kq;                                             // max iterations in constraint violations
    protected int W_solo_max;                                       // max permissions to bid solo
    protected int W_any_max;                                        // max permissions to bid on any
    protected IterationLists localResults;                          // list of iteration results
    protected int zeta = 0;                                         // iteration counter
    protected double C_merge;                                       // Merging cost
    protected double C_split;                                       // Splitting cost
    protected double resources;                                     // Initial resources for agent
    protected double resourcesRemaining;                            // Current resources for agent
    protected double t_0; //    private long t_0;                   // start time
    protected Vector<IterationResults> receivedResults;             // list of received results
    protected boolean alive = true;                                 // alive indicator

    /**
     * Activator - constructor
     */
    protected void activate() {
        getLogger().info("Activating agent");

        // Request Role
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);

        this.location = new Dimension(0,0);       // current location
        this.speed = 1;                                         // displacement speed of agent
        this.sensors = new Vector<>();                          // list of all sensors
        this.J = new Vector<>();                                // list of all subtasks
        this.miu = 0;                                           // Travel cost
        this.M = 1;                                             // planning horizon
        this.O_kq = 2;                                          // max iterations in constraint violations
        this.W_solo_max = 5;                                    // max permissions to bid solo
        this.W_any_max = 10;                                    // max permissions to bid on any
        this.zeta = 0;                                          // iteration counter
        this.C_merge = 0.0;                                     // Merging cost
        this.C_split = 0.0;                                     // Splitting cost
        this.resources = 0.0;                                   // Initial resources for agent
        this.resourcesRemaining = 0.0;                          // Current resources for agent
        this.t_0 = 0.0; //    private long t_0;                 // start time
    }

    /**
     * Planner functions
     */
    public void phaseOne() {
        // Phase 1 - Create bid for individual spacecraft
        if (this.zeta == 0) getLogger().info("Creating plan...");

        // -Initialize results
        if(this.zeta == 0){
            // Initialize results
            localResults = new IterationLists(this.J, this.W_solo_max, this.W_any_max,
                    this.M, this.C_merge, this.C_split, this.resources, this);
        }
        else{
            // Import results from previous iteration
            localResults = new IterationLists(this.localResults, true, this);
        }

        // -Generate Bundle
        while( (localResults.getBundle().size() < M) && ( localResults.getH().contains(1)) && this.alive ){
            Vector<AbstractBid> bidList = new Vector<>();
            Subtask j_chosen = null;

            // Calculate bid for every subtask
            for(int i = 0; i < this.J.size(); i++){
                Subtask j = this.J.get(i);

                // Check if subtask can be bid on
//                if(canBid(j, i, localResults) && (localResults.getH().get(i) == 1)) { // task can be bid on
                if(canBid(j, i, localResults) ){ // task can be bid
                    // Calculate bid for subtask
                    AbstractBid localBid = new AbstractBid();
                    localBid.calcBidForSubtask(j, this);

                    bidList.add(localBid);

                    // Coalition & Mutex Tests
                    Vector<Integer> h = localResults.getH();
                    h.setElementAt( coalitionTest(localBid, localResults, j, i), i);
                    if(h.get(i) == 1){
                        h.setElementAt( mutexTest(localBid, localResults, j, i), i);
                    }

                    // Check if agent has enough resources to execute task
                    double bundle_cost = 0.0;
                    for(Subtask subtask : localResults.getBundle()){ // count costs of bundle
                        int i_j = localResults.getJ().indexOf(subtask);
                        bundle_cost += localResults.getCost().get(i_j);
                    }
                    if( (localBid.getCost_aj() + bundle_cost) > this.resourcesRemaining){
                        // agent does NOT have enough resources
                        h.setElementAt( 0, i);
                    }

                    localResults.setH( h );
                }
                else{ // task CANNOT be bid on
                    // Give a bid of 0;
                    AbstractBid localBid = new AbstractBid();
                    bidList.add(localBid);

                    Vector<Integer> h = localResults.getH();
                    h.setElementAt( 0, i);
                    localResults.setH( h );
                }
            }

            // Choose max bid
            double currentMax = 0.0;
            int i_max = 0;
            AbstractBid maxBid = new AbstractBid();

            for(int i = 0; i < bidList.size(); i++){
                double c = bidList.get(i).getC();
                int h = localResults.getH().get(i);

                if( (c*h > currentMax) ){
                    currentMax = c*h;
                    i_max = i;
                    maxBid = bidList.get(i);
                    j_chosen = this.J.get(i_max);
                }
            }

            // Check if bid already exists for that subtask in the bundle
            boolean bidExists = false;
            for(int i = 0; i < localResults.getBundle().size(); i ++){
                if(j_chosen == localResults.getBundle().get(i)){
                    Vector<Integer> h = localResults.getH();
                    h.setElementAt( 0, i);
                    localResults.setH( h );
                    bidExists = true;
                }
            }

            // Update results
            if(!bidExists){
                if(localResults.getY().get(i_max) < maxBid.getC()) {
                    localResults.getBundle().add(j_chosen);
                    localResults.getPath().add(maxBid.getI_opt(), j_chosen);
                    Vector<Integer> h = localResults.getH();
                    h.setElementAt( 0, i_max);
                    localResults.setH( h );
                }
                localResults.updateResults(maxBid, i_max, this, zeta);
            }
        }

    }


    /**
     * Misc helper functions
     */
    private boolean canBid(Subtask j, int i_av, IterationLists results){
        if( !this.sensors.contains( j.getMain_task() ) ){ // checks if agent contains sensor for subtask
            return false;
        }
        else if(j.getParentTask().getStatus()){ // if task is completed, I can't bid
            return false;
        }
        else if(j.getComplete()){ // if task has been completed, I can't bid
            return false;
        }
        else if(results.getBundle().contains(j)){ // if subtask is already in bundle, I can't bid
            return false;
        }

        // check if bid for a subtask of the same task is in the bundle
        Task parentTask = j.getParentTask();
        int i_task = parentTask.getJ().indexOf(j);
        int[][] D = parentTask.getD();

        // check if subtask in question is mutually exclusive with a bid already in the bundle
        for(Subtask bundleSubtask : results.getBundle()){
            if(bundleSubtask.getParentTask() == parentTask) {
                int i_b = parentTask.getJ().indexOf(bundleSubtask);
                if (D[i_task][i_b] == -1) { // if subtask j has a mutually exclusive task in bundle, you cannot bid
                    return false;
                }
            }
        }

        //check if pessimistic or optimistic strategy -> if w_solo(i_j) = 0 & w_any(i_j) = 0, then PBS. Else OBS.
        Vector<Integer> w_any = results.getW_any();
        Vector<Integer> w_solo  = results.getW_solo();
        Vector<AbstractSimulatedAgent> z = results.getZ();

        // Count number of requirements and number of completed requirements
        int N_req = 0;
        int n_sat = 0;
        for(int k = 0; k < parentTask.getJ().size(); k++){
            if(i_task == k){ continue;}
            if( D[i_task][k] >= 0){ N_req++; }
            if( (z.get(i_av - i_task + k) != null )&&(D[i_task][k] == 1) ){ n_sat++; }
        }

        if(!isOptimistic(j)){
            // Agent has spent all possible tries biding on this task with dependencies
            // Pessimistic Bidding Strategy to be used
            return (n_sat == N_req);
        }
        else{
            // Agent has NOT spent all possible tries biding on this task with dependencies
            // Optimistic Bidding Strategy to be used
            return ( (w_any.get(i_av) > 0)&&(n_sat > 0) ) || ( w_solo.get(i_av) > 0 ) || (n_sat == N_req);
        }
    }

    private boolean isOptimistic(Subtask j){
        //check if pessimistic or optimistic strategy
        Task parentTask = j.getParentTask();
        int[][] D = parentTask.getD();
        int q = parentTask.getJ().indexOf(j);
        Vector<Subtask> dependentTasks = new Vector<>();

        for(int u = 0; u < parentTask.getJ().size(); u++){
            if( (D[u][q] >= 1) && (D[q][u] == 1) ){
                dependentTasks.add(parentTask.getJ().get(u));
            }
        }

        if(dependentTasks.size() > 0){ return true; }
        else{ return false; }
    }

    private int coalitionTest(AbstractBid bid, IterationLists localResults, Subtask j, int i_subtask){
        Task parentTask = j.getParentTask();
        Vector<Subtask> J_parent = parentTask.getJ();
        Vector<Double> y = localResults.getY();
        Vector<AbstractSimulatedAgent> z = localResults.getZ();
        int[][] D = j.getParentTask().getD();

        double new_bid = 0.0;
        double coalition_bid = 0.0;

        for(int i = 0; i < y.size(); i++){
            // Check if j and q are in the same task
            if((localResults.getJ().get(i).getParentTask() == parentTask) && (i != i_subtask)){
                //Check if bid outmatches coalition bid
                int j_index = J_parent.indexOf(j);
                int q_index = i - (i_subtask - J_parent.indexOf(j));

                if ((z.get(i) == z.get(i_subtask)) && ((D[ j_index ][ q_index ] == 0) || (D[ j_index ][ q_index ] == 1))) {
                    coalition_bid = coalition_bid + y.get(i);
                }
                if (D[j_index][q_index] == 1)
                    if (z.get(i) == this) {
                        new_bid = new_bid + y.get(i);
                    }
            }
        }
        new_bid = new_bid + bid.getC();;

        if(new_bid > coalition_bid){
            return 1;
        }
        else{ return 0; }
    }

    private int mutexTest(AbstractBid bid, IterationLists localResults, Subtask j, int i_subtask){
        Task parentTask = j.getParentTask();
        Vector<Subtask> J_parent = parentTask.getJ();
        Vector<Double> y = localResults.getY();
        Vector<AbstractSimulatedAgent> z = localResults.getZ();
        double c = bid.getC();
        int[][] D = j.getParentTask().getD();

        double new_bid = 0.0;
        for(int q = 0; q < J_parent.size(); q++) {
            //if q != j and D(j,q) == 1, then add y_q to new bid
            if( (J_parent.get(q) != j) && (D[J_parent.indexOf(j)][q] == 1) ){
                int i_q = localResults.getJ().indexOf(J_parent.get(q));
                new_bid = new_bid + y.get(i_q);
            }
        }
        new_bid = new_bid + c;

        Vector<Vector<Integer>> coalitionMembers = new Vector<>();
        for(int i_j = 0; i_j < J_parent.size(); i_j++){
            Vector<Integer> Jv = new Vector<>();
            for(int i_q = 0; i_q < J_parent.size(); i_q++){
                if( (D[i_j][i_q] == 1) ){
                    Jv.add(i_q);
                }
            }
            Jv.add(J_parent.indexOf(j));

            coalitionMembers.add(Jv);
        }

        double max_bid = 0.0;
        double y_coalition;
        Vector<Integer> Jv;
        for(int i_c = 0; i_c < coalitionMembers.size(); i_c++) {
            y_coalition = 0.0;
            Jv = coalitionMembers.get(i_c);

            for (int i = 0; i < Jv.size(); i++) {
                int i_v = Jv.get(i) + localResults.getJ().indexOf(j) - J_parent.indexOf(j);
                y_coalition = y_coalition + y.get(i_v);
            }
            int i_v = i_c + localResults.getJ().indexOf(j) - J_parent.indexOf(j);
            y_coalition = y_coalition + y.get(i_v);

            if (y_coalition > max_bid) {
                max_bid = y_coalition;
            }
        }

        if(new_bid > max_bid){ return 1; }
        else{ return 0; }
    }

    /**
     * Getters and Setters
     */
    public Scenario getEnvironment() { return environment;}
    public Dimension getLocation() { return location; }
    public double getSpeed() { return speed; }
    public Vector<String> getSensors() { return sensors; }
    public Vector<Subtask> getJ() { return J; }
    public double getMiu() { return miu; }
    public int getM() { return M; }
    public int getO_kq() { return O_kq; }
    public int getW_solo_max() { return W_solo_max; }
    public int getW_any_max() { return W_any_max; }
    public IterationLists getLocalResults() { return localResults; }
    public int getZeta() { return zeta; }
    public double getC_merge() { return C_merge; }
    public double getC_split() { return C_split; }
    public double getResources() { return resources; }
    public double getResourcesRemaining() { return resourcesRemaining; }
    public double getT_0() { return t_0; }

    public void setEnvironment(Scenario environment) { this.environment = environment; }
    public void setLocation(Dimension location) { this.location = location; }
    public void setSpeed(double speed) { this.speed = speed; }
    public void setSensors(Vector<String> sensors) { this.sensors = sensors; }
    public void setJ(Vector<Subtask> j) { J = j; }
    public void setMiu(double miu) { this.miu = miu; }
    public void setM(int m) { this.M = m; }
    public void setO_kq(int o_kq) { this.O_kq = o_kq; }
    public void setW_solo_max(int w_solo_max) { this.W_solo_max = w_solo_max; }
    public void setW_any_max(int w_any_max) { W_any_max = w_any_max; }
    public void setLocalResults(IterationLists localResults) { this.localResults = localResults; }
    public void setZeta(int zeta) { this.zeta = zeta; }
    public void setC_merge(double c_merge) { this.C_merge = c_merge; }
    public void setC_split(double c_split) { this.C_split = c_split; }
    public void setResources(double resources) { this.resources = resources; }
    public void setResourcesRemaining(double resourcesRemaining) { this.resourcesRemaining = resourcesRemaining; }
    public void setT_0(double t_0) { this.t_0 = t_0; }
    public void setReceivedResults(Vector<IterationResults> receivedResults) { this.receivedResults = receivedResults; }

}
