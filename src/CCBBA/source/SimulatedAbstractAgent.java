package CCBBA.source;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import CCBBA.CCBBASimulation;

import java.awt.*;
import java.io.IOException;
import java.util.Vector;
import java.util.List;

public class SimulatedAbstractAgent extends AbstractAgent {


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
    protected Vector<IterationResults> results;                     // list of results
    protected Vector<Subtask> bundle = new Vector<>();              // bundle of chosen subtasks
    protected Vector<Subtask> path = new Vector<>();                // path chosen
    protected Vector<Dimension> X_path = new Vector<>();            // path locations
    protected String bidStrategy;                                   // bidding strategy indicator

    private List<AgentAddress> list_agents;                         // list of agents in planning phase
    private IterationResults localResults;                          // list of iteration results
    private int zeta = 0;                                           // iteration counter
    private int convergenceCounter;
    private int convergenceIndicator;
    protected double C_merge;
    protected double C_split;
    protected double resources;

    /**
     * initialize my role and fields
     */
    @Override
    protected void activate() {
        // Request Role
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK);

        this.location = getInitialPosition();
        this.sensors = getSensorList();
        this.resources = getResources();
        this.speed = setSpeed();
        this.miu = setMiu();
        this.M = getM();
        this.O_kq = getO_kq();
        this.W_solo_max = getW_solo_max();
        this.W_any_max = getW_any_max();
        this.results = new Vector<>();
        this.bundle = new Vector<>();
        this.bidStrategy = "OBS";
        this.zeta = 0;
        this.convergenceCounter = 0;
        this.convergenceIndicator = getConvergenceIndicator();
        this.C_merge = getC_merge();
        this.C_split = getC_split();
    }

    /**
     * Main Sim functions
     */
    @SuppressWarnings("unused")
    public void phaseOne(){
        //Obtain list of planning agents
        this.list_agents = getAgentsWithRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK, false);

        // Phase 1 - Create bid for individual spacecraft
        if(this.zeta == 0) {
            getLogger().info("Creating plan...");
        }
        //getLogger().info("Phase 1...");

        // Get incomplete subtasks
        J = getIncompleteSubtasks();

        //Phase 1 - Task Selection
        // -Initialize results
        if(this.zeta == 0){
            // Set results to 0
            localResults = new IterationResults(this.J, this.W_solo_max, this.W_any_max, this.M, this.C_merge, this.C_split, this.resources, this);
        }
        else{
            // Import results from previous iteration
            localResults = new IterationResults(results.get(zeta - 1), true, this);

            // Checks for new coalitions
            Vector<Vector<SimulatedAbstractAgent>> oldCoalition;
            Vector<Vector<SimulatedAbstractAgent>> newCoalition;
            oldCoalition = results.get(zeta-1).getOmega();
            newCoalition = localResults.getOmega();

            int i_j = isEqual(oldCoalition, newCoalition);
            if( (i_j > -1)&&(!bundle.isEmpty())&&(i_j < bundle.size()) ){ // if not equal, release task from bundle
                int i_remove = localResults.getJ().indexOf( this.bundle.get(i_j) );
                localResults.resetResults(localResults, i_remove, bundle);
                removeFromBundle(i_j);
                localResults.updateResults(this.bundle, this.path, this.X_path);
            }

        }

        // -Generate Bundle
        bundle = localResults.getBundle();
        path = localResults.getPath();
        while( (bundle.size() < M)&&(localResults.getH().contains(1)) ){
            Vector<SubtaskBid> bidList = new Vector<>();
            Subtask j_chosen = null;

            // Calculate bid for every subtask
            for(int i = 0; i < this.J.size(); i++){
                Subtask j = this.J.get(i);

                // Check if subtask can be bid on
//                if(canBid(j, i, localResults) && (localResults.getH().get(i) == 1)) { // task can be bid on
                if(canBid(j, i, localResults) ){ // task can be bid
                    // Calculate bid for subtask
                    SubtaskBid localBid = new SubtaskBid();
                    localBid.calcBidForSubtask(j, this);

                    bidList.add(localBid);

                    // Coalition & Mutex Tests
                    Vector<Integer> h = localResults.getH();
                    h.setElementAt( coalitionTest(localBid, localResults, j, i), i);
                    if(h.get(i) == 1){
                        h.setElementAt( mutexTest(localBid, localResults, j, i), i);
                    }
                    localResults.setH( h );
                }
                else{ // task CANNOT be bid on
                    // Give a bid of 0;
                    SubtaskBid localBid = new SubtaskBid();
                    bidList.add(localBid);

                    Vector<Integer> h = localResults.getH();
                    h.setElementAt( 0, i);
                    localResults.setH( h );
                }
            }

            // Choose max bid
            double currentMax = 0.0;
            int i_max = 0;
            SubtaskBid maxBid = new SubtaskBid();

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
            for(int i = 0; i < bundle.size(); i ++){
                if(j_chosen == bundle.get(i)){
                    Vector<Integer> h = localResults.getH();
                    h.setElementAt( 0, i);
                    localResults.setH( h );
                    bidExists = true;
                }
            }

            // Update results
            if(!bidExists){
                if(localResults.getY().get(i_max) < maxBid.getC()) {
                    this.bundle.add(j_chosen);
                    this.path.add(maxBid.getI_opt(), j_chosen);
                    this.X_path.add(maxBid.getX_aj());
                }
                localResults.updateResults(maxBid, i_max, this, zeta);
            }
        }


        //prepare results for broadcast
        myMessage myResults = new myMessage(localResults, this.getName());
    }

    @SuppressWarnings("unused")
    public void phaseTwo() {
        //Phase 2 - Consensus
        //getLogger().info("Phase 2...");

        //Receive results
        List<Message> receivedMessages = nextMessages(null);
        Vector<IterationResults> receivedResults = new Vector<>();
        for(int i = 0; i < receivedMessages.size(); i++){
            myMessage message = (myMessage) receivedMessages.get(i);
            receivedResults.add(message.myResults);
        }

        // Rule-Based Check
        // check consistency:
        boolean consistent = true;
        for(int i = 0; i < receivedResults.size(); i++){ // for every received result
            // compare local results to each received result
            for (int i_j = 0; i_j < localResults.getY().size(); i_j++){
                double myY = localResults.getY().get(i_j);
                double itsY = receivedResults.get(i).getY().get(i_j);
                if( myY != itsY ){
                    //getLogger().info("Inconsistencies in plan found !!");
                    consistent = false;
                    convergenceCounter = 0;
                    break;
                }
            }
            if(!consistent){ break; }
            else {
                // no inconsistancy found
                //getLogger().info("NO inconsistencies in plan found. Checking convergence...");
                convergenceCounter++;
                if(convergenceCounter >= convergenceIndicator){
                    getLogger().info("Plan Converged");
                    requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DO);
                    break;
                }
            }
        }

        if(!consistent) { // No consensus reached
            // getLogger().info("Fixing inconsistencies...");

            // Compare bids with other results
            for (int i = 0; i < receivedResults.size(); i++) { //for each received result
                for (int i_j = 0; i_j < localResults.getY().size(); i_j++) { // for each subtask
                    // Load my results
                    double myY = localResults.getY().get(i_j);
                    String myZ;
                    if(localResults.getZ().get(i_j) == null){
                        myZ = "";
                    }
                    else{ myZ = localResults.getZ().get(i_j).getName(); }
                    double myTz = localResults.getTz().get(i_j);
                    String me = this.getName();
                    int myS = localResults.getS().get(i_j);

                    // Load received results
                    double itsY = receivedResults.get(i).getY().get(i_j);
                    String itsZ;
                    if(receivedResults.get(i).getZ().get(i_j) == null){
                        itsZ = "";
                    }
                    else{ itsZ = receivedResults.get(i).getZ().get(i_j).getName(); }
                    double itsTz = receivedResults.get(i).getTz().get(i_j);
                    myMessage m = (myMessage) receivedMessages.get(i);
                    String it = m.senderName;
                    int itsS = receivedResults.get(i).getS().get(i_j);

                    // Comparing bids. See Ref 40 Table 1
                    if( itsZ == it ){
                        if( myZ == me ){
                            if( itsY > myY ){
                                // update
                                localResults.updateResults(receivedResults.get(i), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                        }
                        else if( myZ == it){
                            // update
                            localResults.updateResults(receivedResults.get(i), i_j, bundle);
                            removeFromBundle(localResults.getJ(), i_j);
                        }
                        else if( (myZ != me)&&(myZ != it)&&(myZ != "") ){
                            if( (itsS > myS)||(itsY > myY) ){
                                // update
                                localResults.updateResults(receivedResults.get(i), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                        }
                        else if( myZ == "" ){
                            // update
                            localResults.updateResults(receivedResults.get(i), i_j, bundle);
                            removeFromBundle(localResults.getJ(), i_j);
                        }
                    }
                    else if( itsZ == me ){
                        if( myZ == me ){
                            // leave
                            localResults.leaveResults(receivedResults.get(i), i_j);
                        }
                        else if( myZ == it){
                            // reset
                            localResults.resetResults(receivedResults.get(0), i_j, bundle);
                            removeFromBundle(localResults.getJ(), i_j);
                        }
                        else if( (myZ != me)&&(myZ != it)&&(myZ != "") ){
                            if(itsS > myS){
                                // reset
                                localResults.resetResults(receivedResults.get(0), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                        }
                        else if( myZ == "" ){
                            // leave
                            localResults.leaveResults(receivedResults.get(i), i_j);
                        }
                    }
                    else if( (itsZ != it)&&( itsZ != me)&&(itsZ != "") ){
                        if( myZ == me ){
                            if( (itsS > myS)&&(itsY > myY) ){
                                // update
                                localResults.updateResults(receivedResults.get(i), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                        }
                        else if( myZ == it){
                            if( itsS > myS ){
                                //update
                                localResults.updateResults(receivedResults.get(i), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                            else{
                                // reset
                                localResults.resetResults(receivedResults.get(0), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                        }
                        else if( myZ == itsZ ){
                            if(itsS > myS){
                                // update
                                localResults.updateResults(receivedResults.get(i), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                        }
                        else if( (myZ != me)&&(myZ != it)&&(myZ != itsZ)&&(myZ != "") ){
                            if( (itsS > myS)&&( itsY > myY ) ){
                                // update
                                localResults.updateResults(receivedResults.get(i), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                        }
                        else if( myZ == "" ){
                            // leave
                            localResults.leaveResults(receivedResults.get(i), i_j);
                        }
                    }
                    else if( itsZ == "") {
                        if (myZ == me) {
                            // leave
                            localResults.leaveResults(receivedResults.get(i), i_j);
                        } else if (myZ == it) {
                            // update
                            localResults.updateResults(receivedResults.get(i), i_j, bundle);
                            removeFromBundle(localResults.getJ(), i_j);
                        } else if ((myZ != me) && (myZ != it) && (myZ != "")) {
                            if (itsS > myS) {
                                // update
                                localResults.updateResults(receivedResults.get(i), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                        } else if (myZ == "") {
                            // leave
                            localResults.leaveResults(receivedResults.get(i), i_j);
                        }
                    }
                }
            }

            // Check for constraints
            // -Coalition constraints
            if(bidStrategy == "PBS"){
                for(int i = 0; i < bundle.size(); i++){
                    Vector<Integer> w_solo = localResults.getW_any();
                    Vector<Integer> w_any  = localResults.getW_solo();
                    Vector<SimulatedAbstractAgent> z = localResults.getZ();
                    Subtask j = bundle.get(i);
                    Task parentTask = j.getParentTask();
                    int i_task = parentTask.getJ().indexOf(j);
                    int[][] D = parentTask.getD();
                    int i_av = localResults.getJ().indexOf(j);

                    // Count number of requirements and number of completed requirements
                    int N_req = 0;
                    int n_sat = 0;
                    for(int k = 0; k < parentTask.getJ().size(); k++){
                        if(i_task == k){ continue;}
                        if( D[i_task][k] == 1){ N_req = N_req + 1; }
                        if( (z.get(i_av - i_task + k) != null )&&(D[i_task][k] == 1) ){ n_sat = n_sat + 1; }
                    }

                    if(N_req != n_sat){ //if not all dependencies are met
                        //release task
                        int i_j = localResults.getJ().indexOf(j);
                        localResults.resetResults(receivedResults.get(0), i_j, bundle);
                        removeFromBundle(localResults.getJ(), i_j);
                    }

                }
            }
            else if(bidStrategy == "OBS"){
                for(int i = 0; i < bundle.size(); i++){
                    Vector<Integer> v = localResults.getV();
                    Vector<Integer> w_solo = localResults.getW_any();
                    Vector<Integer> w_any  = localResults.getW_solo();
                    Vector<SimulatedAbstractAgent> z = localResults.getZ();
                    Subtask j = bundle.get(i);
                    Task parentTask = j.getParentTask();
                    int i_task = parentTask.getJ().indexOf(j);
                    int i_j = this.J.indexOf(bundle.get(i));
                    int[][] D = parentTask.getD();
                    int i_av = this.J.indexOf(j);

                    // Count number of requirements and number of completed requirements
                    int N_req = 0;
                    int n_sat = 0;
                    for(int k = 0; k < parentTask.getJ().size(); k++){
                        if(i_task == k){ continue;}
                        if( D[i_task][k] == 1){ N_req = N_req + 1; }
                        if( (z.get(i_av - i_task + k) != null )&&(D[i_task][k] == 1) ){ n_sat = n_sat + 1; }
                    }

                    if(N_req != n_sat){ //if not all dependencies are met, v_i++
                        v.setElementAt( v.get(i_j)+1 , i_j);
                        localResults.setV( v );
                    }

                    if(v.get(i_j) >= O_kq){ // if task has held on to task for too long, release task
                        localResults.resetResults(receivedResults.get(0), i_j, bundle);
                        removeFromBundle(localResults.getJ(), i_j);
                        w_solo.setElementAt( w_solo.get(i_j)-1,i_j);
                        w_any.setElementAt( w_any.get(i_j)-1,i_j);
                    }
                }
            }

            // Check for time constraints
            for(int i = 0; i < bundle.size(); i++){
                boolean taskReleased = false;
                Subtask j = bundle.get(i);
                Task parenTask = j.getParentTask();
                int[][] D = parenTask.getD();

                Vector<Integer> tempViolations = tempSat(j , localResults);

                int i_q = localResults.getJ().indexOf(j);
                int i_o = i_q - parenTask.getJ().indexOf(j);
                for(int i_v = 0; i_v < tempViolations.size(); i_v++){  // if time constraint violations exist
                    //compare each time violation
                    int i_u = tempViolations.get(i_v);
                    if( (D[parenTask.getJ().indexOf(j)][i_u - i_o] == 1)&&(D[i_u - i_o][parenTask.getJ().indexOf(j)] != 1) ){
                        //release task
                        localResults.resetResults(receivedResults.get(0), i_q, bundle);
                        removeFromBundle(localResults.getJ(), i_q);
                        taskReleased = true;
                        break;
                    }
                    else if( (D[parenTask.getJ().indexOf(j)][i_u - i_o] == 1)&&(D[i_u - i_o][parenTask.getJ().indexOf(j)] == 1) ){
                        double tz_q = localResults.getTz().get( localResults.getJ().indexOf(j) );
                        double tz_u = localResults.getTz().get( i_u );
                        double t_start = parenTask.getTC().get(0);
                        if( tz_q - t_start <= tz_u - t_start){
                            // release task
                            localResults.resetResults(receivedResults.get(0), i_q, bundle);
                            removeFromBundle(localResults.getJ(), i_q);
                            taskReleased = true;
                            break;
                        }

                    }
                }

                if(taskReleased){
                    if(bidStrategy == "OBS") {
                        Vector<Integer> w_any = localResults.getW_any();
                        Vector<Integer> w_solo = localResults.getW_solo();
                        w_any.setElementAt(w_any.get(i_q) - 1, i_q);
                        w_solo.setElementAt(w_any.get(i_q) - 1, i_q);
                        localResults.setW_any(w_any);
                        localResults.setW_solo(w_solo);
                    }
                }
            }
        }

        localResults.updateResults(this.bundle, this.path, this.X_path);
        updateResultsList(localResults);
        zeta++;

        //Broadcast results
        myMessage myResults = new myMessage(this.localResults, this.getName());
        for(int i = 0; i < list_agents.size(); i++) sendMessage(list_agents.get(i), myResults);
    }

    @SuppressWarnings("unused")
    private void doTasks() throws IOException {
        getLogger().info("Doing Tasks...");
        /*
        for all items in the bundle
            if resources are left
                //move to task
                start to move
                while agent is not at task
                    if agent is not there and resources are not zero
                        move one step
                        reduce resources
                    end if
                end while

                //do task
                while coalition partners are not there
                    wait
                end while
                mark task as done
                reduce resources
            else
                do nothing
                die
            end if
        end for

        if tasks are available
            make new plan
        else
            die
        end if
         */

        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DIE);
    }

    @Override
    protected void end(){
        getLogger().info("Tasks completed. Killing agent, goodbye!");

        AgentAddress resultsAddress = getAgentWithRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE);
        myMessage myDeath = new myMessage(this.localResults, this.getName());
        sendMessage(resultsAddress, myDeath);
    }

    /**
     * Misc tools and functions
     */

    protected Vector<Subtask> getIncompleteSubtasks(){
        //Looks for tasks from environment and checks for completion
        Vector<Task> V = environment.getTasks();
        Vector<Subtask> J_available = new Vector<>();

        for(int i = 0; i < V.size(); i++){
            Vector<Subtask> J_i = V.get(i).getJ();
            for(int j = 0; j < J_i.size(); j++) {
                if (!J_i.get(j).getComplete()) {
                    J_available.add(J_i.get(j));
                }
            }
        }

        return J_available;
    }

    protected boolean canBid(Subtask j, int i_av, IterationResults results){
        // checks if agent contains sensor for subtask
        if(!this.sensors.contains(j.getMain_task())){
            return false;
        }

        //check if pessimistic or optimistic strategy
        // if w_solo(i_j) = 0 & w_any(i_j) = 0, then PBS. Else OBS.
        Vector<Integer> w_solo = results.getW_any();
        Vector<Integer> w_any  = results.getW_solo();
        Vector<SimulatedAbstractAgent> z = results.getZ();
        Task parentTask = j.getParentTask();
        int i_task = parentTask.getJ().indexOf(j);
        int[][] D = parentTask.getD();

        // Count number of requirements and number of completed requirements
        int N_req = 0;
        int n_sat = 0;
        for(int k = 0; k < parentTask.getJ().size(); k++){
            if(i_task == k){ continue;}
            if( D[i_task][k] == 1){ N_req++; }
            if( (z.get(i_av - i_task + k) != null )&&(D[i_task][k] == 1) ){ n_sat++; }
        }

        if(!isOptimistic(j, i_av, results)){
            // Agent has spent all possible tries biding on this task with dependencies
            // Pessimistic Bidding Strategy to be used
            bidStrategy = "PBS";
            return (n_sat == N_req);
        }
        else{
            // Agent has NOT spent all possible tries biding on this task with dependencies
            // Optimistic Bidding Strategy to be used
            bidStrategy = "OBS";
            return ( (w_any.get(i_av) > 0)&&(n_sat > 0) )||( w_solo.get(i_av) > 0 )||(n_sat == N_req);
        }
    }

    protected boolean isOptimistic(Subtask j, int i_av, IterationResults results){
        //check if pessimistic or optimistic strategy
        // if w_solo(i_j) = 0 & w_any(i_j) = 0, then PBS. Else OBS.
        Vector<Integer> w_solo = results.getW_any();
        Vector<Integer> w_any  = results.getW_solo();
        Vector<SimulatedAbstractAgent> z = results.getZ();
        Task parentTask = j.getParentTask();
        int i_task = parentTask.getJ().indexOf(j);
        int[][] D = parentTask.getD();

        // Count number of requirements and number of completed requirements
        int N_req = 0;
        int n_sat = 0;
        for(int k = 0; k < parentTask.getJ().size(); k++){
            if(i_task == k){ continue;}
            if( D[i_task][k] == 1){ N_req = N_req + 1; }
            if( (z.get(i_av - i_task + k) != null )&&(D[i_task][k] == 1) ){ n_sat = n_sat + 1; }
        }

        if( (w_solo.get(i_av) == 0)&&(w_any.get(i_av) == 0) ){
            // Agent has spent all possible tries biding on this task with dependencies
            return false;
        }
        else{
            // Agent has NOT spent all possible tries biding on this task with dependencies
            return true;
        }
    }

    protected int coalitionTest(SubtaskBid bid, IterationResults localResults, Subtask j, int i_subtask){
        Task parentTask = j.getParentTask();
        Vector<Subtask> J_parent = parentTask.getJ();
        Vector<Double> y = localResults.getY();
        Vector<SimulatedAbstractAgent> z = localResults.getZ();
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
                if ((z.get(i) == this) && (D[j_index][q_index] == 1)) {
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

    protected int mutexTest(SubtaskBid bid, IterationResults localResults, Subtask j, int i_subtask){
        Task parentTask = j.getParentTask();
        Vector<Subtask> J_parent = parentTask.getJ();
        Vector<Double> y = localResults.getY();
        Vector<SimulatedAbstractAgent> z = localResults.getZ();
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

    private void removeFromBundle(Vector<Subtask> J, int i_j){
        //remove current and subsequent subtasks from bundle
        Subtask j = J.get(i_j);

        if(bundle.contains(j)) { // if bundle contains updated or reseted subtask
            Vector<Subtask> deletedTasks = new Vector<>();
            for (int i_bundle = bundle.indexOf(j); i_bundle < bundle.size(); ) {
                deletedTasks.add(bundle.get(i_bundle));
                bundle.remove(i_bundle);
            }

            //remove current and subsequent subtasks from path
            for (int i = 0; i < deletedTasks.size(); i++) {
                int i_path = path.indexOf(deletedTasks.get(i));
                path.remove(i_path);
                X_path.remove(i_path);
            }
        }
    }

    private void removeFromBundle(int i_j){
        //remove current and subsequent subtasks from bundle
        Subtask j = this.bundle.get(i_j);

        if(this.bundle.contains(j)) { // if bundle contains updated or reseted subtask
            Vector<Subtask> deletedTasks = new Vector<>();
            for (int i_bundle = bundle.indexOf(j); i_bundle < bundle.size(); ) {
                deletedTasks.add(bundle.get(i_bundle));
                bundle.remove(i_bundle);
            }

            //remove current and subsequent subtasks from path
            for (int i = 0; i < deletedTasks.size(); i++) {
                int i_path = path.indexOf(deletedTasks.get(i));
                path.remove(i_path);
                X_path.remove(i_path);
            }
        }
    }

    public Vector<Integer> tempSat(Subtask k_q, IterationResults results){
        double[][] T = k_q.getParentTask().getT();
        Vector<Subtask> J_parent = k_q.getParentTask().getJ();
        Vector<Subtask> J_results = this.J;
        Vector<Double> tz = results.getTz();
        Vector<SimulatedAbstractAgent> z = results.getZ();

        Vector<Integer> violationIndexes = new Vector<>();

        for(int k = 0; k < J_parent.size(); k++){
            Subtask k_u = J_parent.get(k);
            int i_q = J_results.indexOf(k_q);  // index of subtask q
            int i_u = J_results.indexOf(k_u);  // index of subtask u
            boolean req1 = true;
            boolean req2 = true;

            if( ( k != J_parent.indexOf(k_q) )&&( z.get(i_u) != null ) ){ // if not the same subtask and other subtask has a winner
                req1 = tz.get(i_q) <=  tz.get(i_u) + T[J_parent.indexOf(k_q)][k];
                req2 = tz.get(i_u) <=  tz.get(i_q) + T[k][J_parent.indexOf(k_q)];
            }

            if( !(req1 && req2) ){
                violationIndexes.add( i_u );
            }
        }

        return violationIndexes;
    }

    private void updateResultsList(IterationResults newResults){
        IterationResults updatedResults = new IterationResults(newResults, false, this);
        this.results.add(updatedResults);
    }

    private int isEqual(Vector<Vector<SimulatedAbstractAgent>> oldCoalition, Vector<Vector<SimulatedAbstractAgent>> newCoalition){
        for(int i = 0; i < this.M; i++){
            if(oldCoalition.get(i).size() != newCoalition.get(i).size()){ //if different sizes, they are not equal
                return i;
            }
            else{ // if equal sizes, then compare element by element
                for(int j = 0; j < oldCoalition.get(i).size(); j++){
                    if( !newCoalition.get(i).contains( oldCoalition.get(i).get(j) )){ // if new does not contain element from old, then they are not equal
                        return i;
                    }
                }
            }
        }
        return -1;
        // -1 if equal, positive integer for index of bundle where error was
    }

    /**
     * Properties Getters and Setters
     */
    public double getMiu(){ return this.miu; }
    public double getSpeed(){ return this.speed; }
    public Vector<Subtask> getPath(){ return this.path; }
    public Vector<Subtask> getBundle(){ return this.bundle; }
    public IterationResults getLocalResults(){ return this.localResults; }
    public Vector<Subtask> getJ(){ return this.J; }
    public int getZeta(){ return this.zeta; }
    public Vector<String> getSensors(){ return this.sensors; }
    protected double readResources(){ return this.resources; }

    /**
     * Abstract Agent Settings
     */
    protected Vector<String> getSensorList(){
        Vector<String> sensor_list = new Vector<>();
        sensor_list.add("IR");
        return sensor_list;
    }

    protected Dimension getInitialPosition(){
        Dimension position = new Dimension(0,0);
        return position;
    }

    protected int getM(){
        int M_agent = 1;
        return M_agent;
    }

    private double setSpeed(){
        double speed = 1.0;
        return speed;
    }

    protected double setMiu(){
        double miu = 1.0;
        return miu;
    }

    public int getO_kq(){
        int O_kq = 10;
        return O_kq;
    }

    public int getW_solo_max(){
        int w_solo_max = 10;
        return w_solo_max;
    }

    public int getW_any_max(){
        int w_any_max = 10;
        return w_any_max;
    }

    private int getConvergenceIndicator(){
        int convergenceIndicator = 5;
        return convergenceIndicator;
    }

    protected double getC_merge(){
        return 0;
    }
    protected double getC_split(){
        return 0;
    }
    protected double getResources(){
        return 0;
    }
}