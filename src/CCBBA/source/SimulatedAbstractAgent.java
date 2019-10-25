package CCBBA.source;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import CCBBA.CCBBASimulation;

import java.awt.*;
import java.io.IOException;
import java.util.Vector;
import java.util.List;

import static java.lang.Math.pow;
import static java.lang.StrictMath.sqrt;

public class SimulatedAbstractAgent extends AbstractAgent {
//    public class SimulatedAbstractAgent extends AbstractAgent {

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
    private Vector<Subtask> overallBundle;                          // bundle chosen throughout simulation
    protected Vector<Subtask> path = new Vector<>();                // path chosen
    private Vector<Subtask> overallPath = new Vector<>();           // path chosen throughout simulation
    protected Vector<Dimension> X_path = new Vector<>();            // path locations
    private IterationResults localResults;                          // list of iteration results
    private int zeta = 0;                                           // iteration counter
    protected double C_merge;                                       // Merging cost
    protected double C_split;                                       // Splitting cost
    protected double resources;                                     // Initial resources for agent
    private double resourcesRemaining;                              // Current resources for agent
//    private long t_0;
    private double t_0;                                             // start time
    private Vector<Integer> doingIterations = new Vector<>();       // Iterations in which plans were agreed
    private Vector<IterationResults> receivedResults;               // list of received results


    /**
     * initialize my role and fields
     */
    @Override
    protected void activate() {
        getLogger().info("Activating agent");

        // Request Role
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);

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
        this.zeta = 0;
        this.C_merge = getC_merge();
        this.C_split = getC_split();
        this.t_0 = environment.getT_0();
        this.resourcesRemaining = this.resources;
        this.receivedResults = new Vector<>();
    }


    /**
     * Main Sim functions
     */
    @SuppressWarnings("unused")
    public void phaseOne(){
        // Phase 1 - Create bid for individual spacecraft
        if(this.zeta == 0) getLogger().info("Creating plan...");
        //getLogger().info("Phase 1...");

        // Get incomplete subtasks
        this.J = getSubtasks();

        // Empty mailbox
        emptyMailbox();

        //Phase 1 - Task Selection
        // -Initialize results
        if(this.zeta == 0){
            // Initialize results
            localResults = new IterationResults(this.J, this.W_solo_max, this.W_any_max, this.M, this.C_merge, this.C_split, this.resources, this);
        }
        else{
            // Import results from previous iteration
            localResults = new IterationResults(results.get(zeta - 1), true, this);
        }
        this.overallBundle = this.localResults.getOverallBundle();
        this.overallPath = this.localResults.getOverallPath();
        this.bundle = this.localResults.getBundle();
        this.path = this.localResults.getPath();

        // -Generate Bundle
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

                    // Check if agent has enough resources to execute task
                    double bundle_cost = 0.0;
                    for(Subtask subtask : this.bundle){ // count costs of bundle
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

        leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_WAIT_BUNDLES);
    }

    @SuppressWarnings("unused")
    public void phaseTwo() {
        //Phase 2 - Consensus
        //getLogger().info("Phase 2...");

        //Read received results
        List<Message> receivedMessages = nextMessages(null);

        for(int i = 0; i < receivedMessages.size(); i++){
            myMessage message = (myMessage) receivedMessages.get(i);
            receivedResults.add(message.myResults);
        }

        // Compare bids with other results
        // Rule-Based Check
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
//                myMessage m = (myMessage) receivedMessages.get(i);
//                String it = m.senderName;
                String it = receivedResults.get(i).getParentAgent().getName();
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
                        localResults.resetResults(i_j, bundle);
                        removeFromBundle(localResults.getJ(), i_j);
                    }
                    else if( (myZ != me)&&(myZ != it)&&(myZ != "") ){
                        if(itsS > myS){
                            // reset
                            localResults.resetResults(i_j, bundle);
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
                            localResults.resetResults(i_j, bundle);
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
        for (int i = 0; i < this.bundle.size(); i++) {
            if (isOptimistic(this.bundle.get(i))) { // task has optimistic bidding strategy
                Vector<Integer> v = localResults.getV();
                Vector<Integer> w_solo = localResults.getW_solo();
                Vector<Integer> w_any = localResults.getW_any();
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
                for (int k = 0; k < parentTask.getJ().size(); k++) {
                    if (i_task == k) {
                        continue;
                    }
                    if (D[i_task][k] >= 1) {
                        N_req++;
                    }
                    if ((z.get(i_av - i_task + k) != null) && (D[i_task][k] == 1)) {
                        n_sat++;
                    }
                }

                if ((N_req != n_sat) && (N_req > 0)) { //if not all dependencies are met, v_i++
                    v.setElementAt((v.get(i_j) + 1), i_j);
                    this.localResults.setV(v);
                }
                else if((N_req == n_sat)&&(N_req > 0)){ // if all dependencies are met, v_i = 0
                    v.setElementAt(0, i_j);
                    this.localResults.setV(v);
                }

                if (v.get(i_j) >= this.O_kq) { // if task has held on to task for too long, release task
                    this.localResults.resetResults(i_j, this.bundle);
                    removeFromBundle(this.localResults.getJ(), i_j);

                    w_solo.setElementAt((w_solo.get(i_j) - 1), i_j);
                    w_any.setElementAt((w_any.get(i_j) - 1), i_j);
                    v.setElementAt(0, i_j);
                    this.localResults.setW_solo(w_solo);
                    this.localResults.setW_any(w_any);
                    this.localResults.setV(v);
                }
            } else { // task has pessimistic bidding strategy
                Vector<Integer> w_solo = this.localResults.getW_any();
                Vector<Integer> w_any = this.localResults.getW_solo();
                Vector<SimulatedAbstractAgent> z = this.localResults.getZ();
                Subtask j = this.bundle.get(i);
                Task parentTask = j.getParentTask();
                int i_task = parentTask.getJ().indexOf(j);
                int[][] D = parentTask.getD();
                int i_av = this.localResults.getJ().indexOf(j);

                // Count number of requirements and number of completed requirements
                int N_req = 0;
                int n_sat = 0;
                for (int k = 0; k < parentTask.getJ().size(); k++) {
                    if (i_task == k) {
                        continue;
                    }
                    if (D[i_task][k] == 1) {
                        N_req++;
                    }
                    if ((z.get(i_av - i_task + k) != null) && (D[i_task][k] == 1)) {
                        n_sat++;
                    }
                }

                if (N_req > n_sat) { //if not all dependencies are met
                    //release task
                    int i_j = this.localResults.getJ().indexOf(j);
                    this.localResults.resetResults(i_j, bundle);
                    removeFromBundle(this.localResults.getJ(), i_j);
                }


            }
        }

        //-Mutex Constraints
        for (int i = 0; i < this.bundle.size(); i++) {
            Subtask j = this.bundle.get(i);
            Task parentTask = j.getParentTask();
            int i_task = parentTask.getJ().indexOf(j);
            int[][] D = parentTask.getD();
            int i_av = this.localResults.getJ().indexOf(j);
            int i_bid;

            double y_bid = 0.0;
            double y_mutex = 0.0;

            for (int i_j = 0; i_j < parentTask.getJ().size(); i_j++) {
                if( (i_j != i_task) && (D[i_task][i_j] <= 0) ){
                    i_bid = this.localResults.getJ().indexOf(parentTask.getJ().get(i_j));
                    y_mutex += this.localResults.getY().get(i_bid);
                } else if (D[i_task][i_j] >= 1) {
                    i_bid = this.localResults.getJ().indexOf(parentTask.getJ().get(i_j));
                    y_bid += this.localResults.getY().get(i_bid);
                }
            }
            int i_j = this.localResults.getJ().indexOf(j);
            y_bid += this.localResults.getY().get(i_j);

            if (y_mutex > y_bid) { //if outbid by mutex
                //release task
                this.localResults.resetResults(i_j, bundle);
                removeFromBundle(this.localResults.getJ(), i_j);
            }
        }

        //-Time constraints
        for (int i = 0; i < bundle.size(); i++) {
            boolean taskReleased = false;
            Subtask j = bundle.get(i);
            Task parenTask = j.getParentTask();
            int[][] D = parenTask.getD();

            Vector<Integer> tempViolations = tempSat(j, localResults);

            int i_q = localResults.getJ().indexOf(j);
            int i_o = i_q - parenTask.getJ().indexOf(j);
            for (int i_v = 0; i_v < tempViolations.size(); i_v++) {  // if time constraint violations exist
                //compare each time violation
                int i_u = tempViolations.get(i_v);
                if ((D[parenTask.getJ().indexOf(j)][i_u - i_o] == 1) && (D[i_u - i_o][parenTask.getJ().indexOf(j)] <= 0)) {
                    //release task
                    localResults.resetResults(i_q, bundle);
                    removeFromBundle(localResults.getJ(), i_q);
                    taskReleased = true;
                    break;
                } else if ((D[parenTask.getJ().indexOf(j)][i_u - i_o] == 1) && (D[i_u - i_o][parenTask.getJ().indexOf(j)] == 1)) {
                    double tz_q = localResults.getTz().get(localResults.getJ().indexOf(j));
                    double tz_u = localResults.getTz().get(i_u);
                    double t_start = t_0;
                    if (tz_q - t_start <= tz_u - t_start) {
                        // release task
                        localResults.resetResults(i_q, bundle);
                        removeFromBundle(localResults.getJ(), i_q);
                        taskReleased = true;
                        break;
                    }

                }
            }

            if ((taskReleased) && (isOptimistic(j))) {
                Vector<Integer> w_any = localResults.getW_any();
                Vector<Integer> w_solo = localResults.getW_solo();
                w_any.setElementAt(w_any.get(i_q) - 1, i_q);
                w_solo.setElementAt(w_any.get(i_q) - 1, i_q);
                localResults.setW_any(w_any);
                localResults.setW_solo(w_solo);
            }
        }

        //-Coalition Member Constraints
        // create list of new coalition members
        Vector<Vector<SimulatedAbstractAgent>> newOmega = new Vector<>();
        for(int i = 0; i < this.M; i++) {
            Vector<SimulatedAbstractAgent> tempCoal = new Vector<>();

            if( this.bundle.size() >= i+1 ) {
                for (int i_j = 0; i_j < this.localResults.getJ().size(); i_j++) {
                    if ((this.localResults.getZ().get(i_j) != this)
                            && (this.localResults.getZ().get(i_j) != null)
                            && (this.bundle.get(i).getParentTask() == this.J.get(i_j).getParentTask())) {
                        tempCoal.add(this.localResults.getZ().get(i_j));
                    }
                }
            }
            newOmega.add(tempCoal);
        }

        Vector<Vector<SimulatedAbstractAgent>> oldOmega = this.localResults.getOmega();

        // compare old list vs new list
        for (int i = 0; i < this.bundle.size(); i++){
            if (oldOmega.get(i).size() == 0) { // no coalition partners in original list
                if(newOmega.get(i).size() > 0){ // new coalition partners in new list
                    // release task
                    Subtask j = this.bundle.get(i);
                    int i_j = this.localResults.getJ().indexOf(j);
                    this.localResults.resetResults(i_j, bundle);
                    removeFromBundle(this.localResults.getJ(), i_j);
                    break;
                }
            }
            else{ // coalition partners exist in original list, compare lists
                if(newOmega.get(i).size() > 0){ // new list is not empty
                    // compare lists
                    if(oldOmega.get(i).size() != newOmega.get(i).size()){ // if different sizes, then lists are not the same
                        // release task
                        Subtask j = this.bundle.get(i);
                        int i_j = this.localResults.getJ().indexOf(j);
                        this.localResults.resetResults(i_j, bundle);
                        removeFromBundle(this.localResults.getJ(), i_j);
                        break;
                    }
                    else{ // compare element by element
                        boolean released = false;
                        for(SimulatedAbstractAgent listMember : oldOmega.get(i)){
                            if(!newOmega.get(i).contains(listMember)){
                                released = true;
                                break;
                            }
                        }
                        if(released){
                            // release task
                            Subtask j = this.bundle.get(i);
                            int i_j = this.localResults.getJ().indexOf(j);
                            this.localResults.resetResults(i_j, bundle);
                            removeFromBundle(this.localResults.getJ(), i_j);
                            break;
                        }
                    }
                }

            }
        }

        // Update results
        localResults.updateResults(this.bundle, this.path, this.X_path);
        updateResultsList(localResults);
        zeta++;
        receivedResults = new Vector<>();

        leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK2);
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_WAIT_COMP);
    }

    @SuppressWarnings("unused")
    public void compareResults(){
        // compares results with other agents

        //Read received results
        List<Message> receivedMessages = nextMessages(null);

        for (Message receivedMessage : receivedMessages) {
            myMessage message = (myMessage) receivedMessage;
            receivedResults.add(message.myResults);
        }

        // Check consistency
        boolean consistent = true;
        boolean coalViolation;
        for (IterationResults receivedResult : receivedResults) { // for every received result
            // compare local results to each received result
            for (int i_j = 0; i_j < localResults.getY().size(); i_j++) {
                double myY = localResults.getY().get(i_j);
                double itsY = receivedResult.getY().get(i_j);
                double myTz = localResults.getTz().get(i_j);
                double itsTz = receivedResult.getTz().get(i_j);
                int myS = localResults.getS().get(i_j);
                int itsS = receivedResult.getS().get(i_j);
                int myV = localResults.getV().get(i_j);
                int itsV = receivedResult.getV().get(i_j);

                coalViolation = ( (myV > 0) || (itsV > 0) );

                if ((myY != itsY) || (myTz != itsTz) || (myS != itsS) || coalViolation) {
                    // inconsistency found
                    consistent = false;
                    break;
                }
            }
            if (!consistent) {
                break;
            }
        }

        // empty received results list
        receivedResults = new Vector<>();

        if(consistent) {
            //-no inconsistencies found, check convergence
            leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_COMP);
            requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_WAIT_CONV);
        }
        else{
            //-no consensus reached, create new plan
            leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_COMP);
            requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);
        }

    }

    @SuppressWarnings("unused")
    private void doTasks(){
        //getLogger().info("Doing Tasks...");
        boolean alive = true;

        //empty mailbox
        emptyMailbox();

        // do tasks in bundle
        for(int i = 0; i < this.bundle.size(); i++){
            if(resourcesLeft()){ // agent still has resources left
                Subtask j = this.bundle.get(i);

                // move to task
                moveToTask(j);
                if(this.resourcesRemaining <= 0.0){ break; }

                //update time
                int i_j = this.localResults.getJ().indexOf(j);
                this.t_0 = this.localResults.getTz().get(i_j) + j.getParentTask().getT_d();

                // deduct task costs from resources
                this.resourcesRemaining -= this.localResults.getCost().get(i_j);
            }
            else{ // agent has no resources left
                alive = false;
                break;
            }
        }

        // set agreed tasks as completed
        for(Subtask j : this.localResults.getJ()){
            int i_j = this.localResults.getJ().indexOf(j);

            // if it has a winner, set subtask as complete
            if(this.localResults.getZ().get(i_j) != null) {
                j.getParentTask().setSubtaskComplete(j);
                this.localResults.getJ().setElementAt(j, i_j);
            }
        }

        // release tasks from bundle
        this.doingIterations.add(this.zeta);
        if(!this.bundle.isEmpty()) {
            removeFromBundle(localResults.getJ(), this.localResults.getJ().indexOf( this.bundle.get(0) ) );
        }

        // update results
        this.localResults.updateResults();
        updateResultsList(this.localResults);
        this.zeta++;

        // check for remaining tasks
        boolean tasksAvailable = tasksAvailable();
        if(alive) {
            // check agent status
            if ((tasksAvailable) && (this.resourcesRemaining > 0.0)) { // tasks are available and agent has resources
                getLogger().info("Tasks in bundle completed \n\t\t\t\t\t\t\t\tCreating a new plan...");
                leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DO);
                requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_WAIT_DO);
            }
            else {
                getLogger().info("No more tasks available. \n\t\t\t\t\t\t\t\tKilling agent, goodbye!");
                leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DO);
                requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DIE);
            }
        }
        else { // agent has no remaining resources
            getLogger().info("No remaining resources. Killing agent, goodbye!");
            leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DO);
            requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DIE);
        }
    }

    @SuppressWarnings("unused")
    protected void die(){
        // send results to results compiler
        AgentAddress resultsAddress = getAgentWithRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE);
        myMessage myDeath = new myMessage(this.localResults, this.getName());
        sendMessage(resultsAddress, myDeath);
    }

    /**
     * Wait functions
     */

    @SuppressWarnings("unused")
    public void waitOnBundles(){ // waits on other available agents to construct their bundles
        abstractWait(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_WAIT_BUNDLES, CCBBASimulation.AGENT_THINK2);
    }

    @SuppressWarnings("unused")
    public void waitOnComparisons(){ // waits on other agents to check their results and share their findings
        abstractWait(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_WAIT_COMP, CCBBASimulation.AGENT_COMP);
    }

    @SuppressWarnings("unused")
    public void waitOnConvergence(){ // waits on other agents to realize that their plans agree
        abstractWait(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_WAIT_CONV, CCBBASimulation.AGENT_DO);
    }

    @SuppressWarnings("unused")
    public void waitOnTaskDoing(){ // wait for other tasks to do their tasks
        abstractWait(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_WAIT_DO, CCBBASimulation.AGENT_THINK1);
    }

    /**
     * Misc tools and functions
     */
    private void emptyMailbox(){
        while(!isMessageBoxEmpty()){
            Message tempMessage = nextMessage();
        }
    }

    private void abstractWait(String community, String group, String roleWait, String roleActive){
        // inform agents that a goal has been reached on my end
        List<AgentAddress> agentsAlive = getAgentsWithRole(community, group, roleWait);
        List<AgentAddress> agentsDead = getAgentsWithRole(community, group, CCBBASimulation.AGENT_DIE);
        myMessage myResults = new myMessage(this.localResults, this.getName());


        if(!isMessageBoxEmpty()){
            // broadcast my results
            broadcastMessage(community, group, roleWait, myResults);
            broadcastMessage(community, group, roleActive, myResults);

            // start phase 2 of planner
            leaveRole(community, group, roleWait);
            requestRole(community, group, roleActive);
        }
        else if(agentsAlive != null){ // check other agent's response
            if( (agentsDead != null)&&((agentsAlive.size() + agentsDead.size()) == (environment.numAgents - 1)) ){
                // broadcast my results
                broadcastMessage(community, group, roleWait, myResults);
                broadcastMessage(community, group, roleActive, myResults);

                // exit wait phase and start active phase
                leaveRole(community, group, roleWait);
                requestRole(community, group, roleActive);
            }
            else if(agentsAlive.size() == (environment.numAgents - 1)){
                // broadcast my results
                broadcastMessage(community, group, roleWait, myResults);
                broadcastMessage(community, group, roleActive, myResults);

                // exit wait phase and start active phase
                leaveRole(community, group, roleWait);
                requestRole(community, group, roleActive);
            }
        }
        else if (agentsDead != null){
            if(agentsDead.size() == (environment.numAgents - 1)){
                // broadcast my results
                broadcastMessage(community, group, roleWait, myResults);
                broadcastMessage(community, group, roleActive, myResults);

                // exit wait phase and start active phase
                leaveRole(community, group, roleWait);
                requestRole(community, group, roleActive);
            }
        }
    }

    private boolean tasksAvailable(){
        boolean allCompleted = true;
        for(Subtask j : this.localResults.getJ()){
            if( !j.getComplete() ){
                allCompleted = false;
                break;
            }
        }

        if(allCompleted) {
            return false;
        }
        else{
            Vector<Task> V = new Vector<>();
            V = environment.getTasks();
            for (Task task : V) {
                if (!checkCompletion(task)) { // check if task's dependencies have been met
                    for (Subtask j : task.getJ()) {
                        if (!j.getComplete() && this.sensors.contains(j.getMain_task())) { // check if agent can resolve any of the subtasks available
                            // calculate bid
                            SubtaskBid localBid = new SubtaskBid();
                            localBid.calcBidForSubtask(j, this);

                            if ( this.resourcesRemaining >= localBid.getCost_aj() ) { // check if agent has enough resources to resolve available subtask
                                if(localBid.getC() > 0.0) { // check if bid produces a net gain
                                    int i_j = this.localResults.getJ().indexOf(j);
                                    if ((this.localResults.getW_any().get(i_j) > 0) && (this.localResults.getW_solo().get(i_j) > 0)) { // check if agent has exhausted attempts to bid on tasks
                                        return true;
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        // all tasks have been completed
        return false;
    }

    private boolean checkCompletion(Task V){ // checks if a task has all of its dependencies met
        Vector<Subtask> localJ = V.getJ();
        int[][] D = V.getD();
        int nullCounter = 0;
        for(int i = 0; i < localJ.size(); i++){
            Subtask j = localJ.get(i);
            int i_j = this.localResults.getJ().indexOf(j);

            // check if subtask has been bid on
            if (this.localResults.getZ().get(i_j) != null) {
                // subtask has a bid, check if dependencies are met
                for (int k = 0; k < localJ.size(); k++) {
                    int i_k = this.localResults.getJ().indexOf(localJ.get(k));
                    if ((D[i][k] >= 1) && (this.localResults.getZ().get(i_k) == null)) { // dependency not met
                        // task is available
                        return false;
                    }
                }
            } else nullCounter++;
        }
        if(nullCounter == localJ.size()) return false;

        return true;
    }

    private void moveToTask(Subtask j){
        // substract resources
        double delta_x;
        Dimension x_i;
        x_i = this.location;
        Dimension x_f = j.getParentTask().getLocation();
        delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);
//
        double distance = sqrt(delta_x);
//        this.resourcesRemaining = this.resourcesRemaining - distance*this.miu;

        // Move agent
        this.location = x_f;
    }

    private boolean resourcesLeft(){
        return this.resourcesRemaining > 0.0;
    }

    private Vector<Subtask> getSubtasks(){
        //Looks for tasks from environment and checks for completion
        Vector<Task> V = environment.getTasks();
        Vector<Subtask> J_available = new Vector<>();

        for (Task task : V) {
            Vector<Subtask> J_i = task.getJ();
            for (Subtask subtask : J_i) {
                J_available.add(subtask);
            }
        }

        return J_available;
    }

    private boolean canBid(Subtask j, int i_av, IterationResults results){
        if( !this.sensors.contains( j.getMain_task() ) ){ // checks if agent contains sensor for subtask
            return false;
        }
        else if(j.getParentTask().getStatus()){ // if task is completed, I can't bid
            return false;
        }
        else if(j.getComplete()){ // if task has been completed, I can't bid
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
            if( D[i_task][k] >= 1){ N_req++; }
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
            return ( (w_any.get(i_av) > 0)&&(n_sat > 0) )||( w_solo.get(i_av) > 0 )||(n_sat == N_req);
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

    private int coalitionTest(SubtaskBid bid, IterationResults localResults, Subtask j, int i_subtask){
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

    private int mutexTest(SubtaskBid bid, IterationResults localResults, Subtask j, int i_subtask){
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
            for (Subtask deletedTask : deletedTasks) {
                int i_path = path.indexOf(deletedTask);
                path.remove(i_path);
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

    private Vector<Integer> tempSat(Subtask k_q, IterationResults results){
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
    public Vector<Subtask> getOverallPath(){ return this.overallPath; }
    public Vector<Subtask> getBundle(){ return this.bundle; }
    public Vector<Subtask> getOverallBundle(){ return this.overallBundle; }
    public IterationResults getLocalResults(){ return this.localResults; }
    public Vector<Subtask> getJ(){ return this.J; }
    public int getZeta(){ return this.zeta; }
    public Vector<String> getSensors(){ return this.sensors; }
    protected double readResources(){ return this.resources; }
    public double getT_0() {
        return t_0;
    }
    public Vector<Integer> getDoingIterations(){ return this.doingIterations; }

    /**
     * Abstract Agent Settings
     */
    protected Vector<String> getSensorList(){
        Vector<String> sensor_list = new Vector<>();
        sensor_list.add("IR");
        sensor_list.add("MW");
        return sensor_list;
    }

    protected Dimension getInitialPosition(){
        return new Dimension(0,0);
    }

    protected int getM(){
        return 1;
    }

    private double setSpeed(){
        return 1.0;
    }

    protected double setMiu(){
        return 1.0;
    }

    public int getO_kq(){
        return 5;
    }

    public int getW_solo_max(){
        return 5;
    }

    public int getW_any_max(){
        return 10;
    }

    private int getConvergenceIndicator(){
        return 10;
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