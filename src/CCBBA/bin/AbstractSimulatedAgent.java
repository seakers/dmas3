package CCBBA.bin;

import CCBBA.CCBBASimulation;
import CCBBA.scenarios.debugger.DebuggerScenario;
import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import org.moeaframework.util.tree.Abs;

import java.awt.*;
import java.util.List;
import java.util.Vector;

import static java.lang.Math.pow;
import static java.lang.Math.random;

public class AbstractSimulatedAgent extends AbstractAgent {
    /**
     * Agent's Scenario
     */
    protected Scenario environment;

    /**
     * Properties
     */
    protected Dimension location = new Dimension();                 // current location
    protected Dimension initialPosition = new Dimension();          // initial location
    protected double speed;                                         // displacement speed of agent
    protected Vector<String> sensors = new Vector<>();              // list of all sensors
    protected double miu;                                           // Travel cost
    protected int M;                                                // planning horizon
    protected int O_kq;                                             // max iterations in constraint violations
    protected int O_all;                                            // max number of bids before tie-break is implemented
    protected int W_solo_max;                                       // max permissions to bid solo
    protected int W_any_max;                                        // max permissions to bid on any
    protected IterationLists localResults;                          // list of iteration results
    protected int zeta = 0;                                         // iteration counter
    protected double C_merge;                                       // Merging cost
    protected double C_split;                                       // Splitting cost
    protected double resources;                                     // Initial resources for agent
    protected double resourcesRemaining;                            // Current resources for agent
    protected double t_0; //    private long t_0;                   // start time
    protected Vector<IterationLists> receivedResults;               // list of received results
    private boolean alive = true;                                   // alive indicator
    private int convCounter = 0;                                    // Convergence Counter
    private int convIndicator = 10;                                 // Convergence Indicator
    protected Vector<Integer> doingIterations = new Vector<>();     // Iterations in which plans were agreed

    /**
     * Activator - constructor
     */
    protected void activate() {
        getLogger().info("Activating agent");

        // Request Role
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);

        this.location = new Dimension(0,0);       // current location
        initialPosition = new Dimension(0, 0);    // initial position
        this.speed = 1;                                         // displacement speed of agent
        this.sensors = new Vector<>();                          // list of all sensors
        this.miu = 0;                                           // Travel cost
        this.M = 1;                                             // planning horizon
        this.O_kq = 2;                                          // max iterations in constraint violations
        this.O_all = 100;                                       // max number of bids before tie-break is implemented
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
    @SuppressWarnings("unused")
    public void phaseOne() {
        // Phase 1 - Create bid for individual spacecraft
        if (this.zeta == 0) getLogger().info("Creating plan...");

        // -Initialize results
        if(this.zeta == 0){
            // Initialize results
            localResults = new IterationLists(this.W_solo_max, this.W_any_max,
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
            for(int i = 0; i < localResults.getJ().size(); i++){
                Subtask j = localResults.getJ().get(i);

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
                    j_chosen = localResults.getJ().get(i_max);
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

        // Broadcast my results
        myMessage myResults = new myMessage( this.localResults, this );
        broadcastMessage(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1, myResults);
        broadcastMessage(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK2, myResults);
        broadcastMessage(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DO, myResults);

        // leave phase one and start phase 2
        leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK2);
    }

    @SuppressWarnings("unused")
    public void phaseTwo() {
        if(!isMessageBoxEmpty()){ // results received
            // Save current results
            IterationLists prevResults = new IterationLists( localResults, false, this);

            // unpack results
            List<Message> receivedMessages = nextMessages(null);

            for(int i = 0; i < receivedMessages.size(); i++){
                myMessage message = (myMessage) receivedMessages.get(i);
                receivedResults.add(message.myLists);
            }

            // compare results
            boolean changesMade = false;
            for(IterationLists result : this.receivedResults){
                for(int i_j = 0; i_j < result.getJ().size(); i_j++){
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
                    boolean myCompletion = localResults.getJ().get(i_j).getComplete();

                    // Load received results
                    double itsY = result.getY().get(i_j);
                    String itsZ;
                    if(result.getZ().get(i_j) == null){
                        itsZ = "";
                    }
                    else{ itsZ = result.getZ().get(i_j).getName(); }
                    double itsTz = result.getTz().get(i_j);
                    String it = result.getParentAgent().getName();
                    int itsS = result.getS().get(i_j);
                    boolean itsCompletion = result.getJ().get(i_j).getComplete();

                    // Comparing bids. See Ref 40 Table 1
                    if( itsZ == it ){
                        if( myZ == me ){
                            if( (itsCompletion) && (itsCompletion != myCompletion) ){
                                // update
                                localResults.updateResults(result, i_j);
                            }
                            else if( itsY > myY ) {
                                // update
                                localResults.updateResults(result, i_j);
                            }
                        }
                        else if( myZ == it){
                            // update
                            localResults.updateResults(result, i_j);
                        }
                        else if( (myZ != me)&&(myZ != it)&&(myZ != "") ){
                            if( (itsS > myS)||(itsY > myY) ){
                                // update
                                localResults.updateResults(result, i_j);
                            }
                        }
                        else if( myZ == "" ){
                            // update
                            localResults.updateResults(result, i_j);
                        }
                    }
                    else if( itsZ == me ){
                        if( myZ == me ){
                            // leave
                            localResults.leaveResults(result, i_j);
                        }
                        else if( myZ == it){
                            // reset
                            localResults.resetResults(i_j);
                        }
                        else if( (myZ != me)&&(myZ != it)&&(myZ != "") ){
                            if(itsS > myS){
                                // reset
                                localResults.resetResults(i_j);
                            }
                        }
                        else if( myZ == "" ){
                            // leave
                            localResults.leaveResults(result, i_j);
                        }
                    }
                    else if( (itsZ != it)&&( itsZ != me)&&(itsZ != "") ){
                        if( myZ == me ){
                            if( (itsCompletion) && (itsCompletion != myCompletion) ){
                                // update
                                localResults.updateResults(result, i_j);
                            }
                            else if( (itsS > myS)&&(itsY > myY) ){
                                // update
                                localResults.updateResults(result, i_j);
                            }
                        }
                        else if( myZ == it){
                            if( itsS > myS ){
                                //update
                                localResults.updateResults(result, i_j);
                            }
                            else{
                                // reset
                                localResults.resetResults(i_j);
                            }
                        }
                        else if( myZ == itsZ ){
                            if(itsS > myS){
                                // update
                                localResults.updateResults(result, i_j);
                            }
                        }
                        else if( (myZ != me)&&(myZ != it)&&(myZ != itsZ)&&(myZ != "") ){
                            if( (itsS > myS)&&( itsY > myY ) ){
                                // update
                                localResults.updateResults(result, i_j);
                            }
                        }
                        else if( myZ == "" ){
                            // leave
                            localResults.leaveResults(result, i_j);
                        }
                    }
                    else if( itsZ == "") {
                        if (myZ == me) {
                            // leave
                            localResults.leaveResults(result, i_j);
                        } else if (myZ == it) {
                            // update
                            localResults.updateResults(result, i_j);
                        } else if ((myZ != me) && (myZ != it) && (myZ != "")) {
                            if (itsS > myS) {
                                // update
                                localResults.updateResults(result, i_j);
                            }
                        } else if (myZ == "") {
                            // leave
                            localResults.leaveResults(result, i_j);
                        }
                    }
                }
            }

            // constrain checks
            for(Subtask j : localResults.getBundle()){
                // create list of new coalition members
                Vector<Vector<AbstractSimulatedAgent>> newOmega = getNewCoalitionMemebers(j);
                Vector<Vector<AbstractSimulatedAgent>> oldOmega = this.localResults.getOmega();

                if (!mutexSat(j) || !timeSat(j) || !depSat(j) || !coalitionSat(j, oldOmega, newOmega) ){
                    // subtask does not satisfy all constraints, release task
                    int i_j =  this.localResults.getJ().indexOf(j);
                    localResults.resetResults(i_j);
                    break;
                }
            }
            this.zeta++;

            if(checkForChanges(prevResults)){
                // changes were made, reconsider bids
                requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);
                this.convCounter = 0;
            }
            else{
                // no changes were made, check convergence
                this.convCounter++;
                if(convCounter >= convIndicator){
                    // convergence reached
                    this.convCounter = 0;
                    requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DO);
                }
                else {
                    requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);
                }
            }

            // empty received results and exit phase 2
            receivedResults = new Vector<>();
            leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK2);
        }

        // if no messages are received, wait for results to come in
    }

    @SuppressWarnings("unused")
    private void doTasks(){
        //getLogger().info("Doing Tasks...");
        boolean resourcesRem = true;

        // do tasks in bundle
        for(int i = 0; i < localResults.getBundle().size(); i++){
            if( alive ) { // agent still has resources left
                Subtask j = localResults.getBundle().get(i);

                // move to task
                moveToTask(j);
                if(this.resourcesRemaining <= 0.0){ break; }

                // deduct task costs from resources
                int i_j = this.localResults.getJ().indexOf(j);
                this.resourcesRemaining -= this.localResults.getCost().get(i_j);

                // set subtask as completed
                j.getParentTask().setSubtaskComplete(j);
                this.localResults.getJ().setElementAt(j, i_j);
            }
            else{ // agent has no resources left
                break;
            }
            resourcesRem = this.resourcesRemaining > 0.0;
        }

        // release tasks from bundle
        this.doingIterations.add(this.zeta);

        // update results with new overall bundle and path
        this.localResults.updateResults();

        // check for remaining tasks
        boolean tasksAvailable = tasksAvailable();
        var myRoles = getMyRoles(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP);
        leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DO);
        if(resourcesRem && this.alive) {
            // check agent status
            if (tasksAvailable) { // tasks are available and agent has resources
                getLogger().info("Tasks in bundle completed! " +
                        "\n\t\t\t\t\t\t\t\tResources still available. " +
                        "\n\t\t\t\t\t\t\t\tCreating a new plan...");
            }
            else {
                if(!myRoles.contains( CCBBASimulation.AGENT_DIE )){
                    getLogger().info("Tasks in bundle completed!" +
                            "\n\t\t\t\t\t\t\t\tNo more tasks available. " +
                            "\n\t\t\t\t\t\t\t\tKilling Agent.");
                }
                this.alive = false;
                requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DIE);
            }
        }
        else { // agent has no remaining resources
            if(!myRoles.contains( CCBBASimulation.AGENT_DIE )){
                getLogger().info("Tasks in bundle completed! " +
                        "\n\t\t\t\t\t\t\t\tNo remaining resources. " +
                        "\n\t\t\t\t\t\t\t\tKilling Agent.");
            }
            this.alive = false;
            requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DIE);
        }
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);
    }

    /**
     * Agent death function(s)
     */
    @SuppressWarnings("unused")
    protected void dying(){ // send results to results compiler
        List<AgentAddress> agentsDead = getAgentsWithRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DIE);
        if((agentsDead != null) && (agentsDead.size() == environment.numAgents - 1)){
            AgentAddress resultsAddress = getAgentWithRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE);
            myMessage myDeath = new myMessage(this.localResults,this);
            sendMessage(resultsAddress, myDeath);
        }
    }

    /**
     * Misc helper functions
     */
    private boolean tasksAvailable(){
        // -1 not enough resources available
        // 0 no tasks available
        // 1 tasks available
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
            Vector<Task> V = this.environment.getTasks();
            for (Task task : V) {
                if (!checkCompletion(task)) { // check if task's dependencies have been met
                    for (Subtask j : task.getJ()) {
                        int i_j = this.localResults.getJ().indexOf(j);
                        if(canBid(j, i_j, this.localResults)){ // if agent is allowed to bid
                            // Calculate bid for subtask
                            AbstractBid localBid = new AbstractBid();
                            localBid.calcBidForSubtask(j, this);

                            // Coalition & Mutex Tests
                            int h = coalitionTest(localBid, localResults, j, i_j);
                            if(h == 1){
                                h = mutexTest(localBid, localResults, j, i_j);
                            }

                            // Check if agent has enough resources to execute task
                            double bundle_cost = 0.0;
                            for(Subtask subtask : this.localResults.getBundle()){ // count costs of bundle
                                int i_b = localResults.getJ().indexOf(subtask);
                                bundle_cost += localResults.getCost().get(i_b);
                            }
                            if( (localBid.getCost_aj() + bundle_cost) > this.resourcesRemaining){
                                // agent does NOT have enough resources
                                h = 0;
                            }

                            if(h == 1){
                                return true;
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
                    if ((D[i][k] >= 1) && (this.localResults.getZ().get(i_k) == null)) { // check if dependency not met
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
        // update location
        double delta_x;
        Dimension x_i;
        x_i = this.location;
        Dimension x_f = j.getParentTask().getLocation();
        delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);
        this.location = x_f;

        // deduct resources
//        double distance = sqrt(delta_x);
//        this.resourcesRemaining = this.resourcesRemaining - distance*this.miu;
    }

    private boolean checkForChanges(IterationLists prevResults){
        return localResults.compareToList(prevResults);
    }

    private boolean mutexSat(Subtask j){
        Task parentTask = j.getParentTask();
        int i_task = parentTask.getJ().indexOf(j);
        int[][] D = parentTask.getD();
        int i_bid;

        double y_bid = 0.0;
        double y_mutex = 0.0;

        for (int i_j = 0; i_j < parentTask.getJ().size(); i_j++) {
            if( (i_j != i_task) && (D[i_task][i_j] < 0) ){
                i_bid = this.localResults.getJ().indexOf(parentTask.getJ().get(i_j));
                y_mutex += this.localResults.getY().get(i_bid);
            } else if (D[i_task][i_j] >= 1) {
                i_bid = this.localResults.getJ().indexOf(parentTask.getJ().get(i_j));
                y_bid += this.localResults.getY().get(i_bid);
            }
        }
        int i_av = this.localResults.getJ().indexOf(j);
        y_bid += this.localResults.getY().get(i_av);

        //if outbid by mutex, release task
        if (y_mutex > y_bid){
            return false;
        }
        else if(y_mutex < y_bid){
            return true;
        }
        else{ // both coalition bid values are equal, compare costs
            double c_bid = 0.0;
            double c_mutex = 0.0;

            for (int i_j = 0; i_j < parentTask.getJ().size(); i_j++) {
                if( (i_j != i_task) && (D[i_task][i_j] < 0) ){
                    i_bid = this.localResults.getJ().indexOf(parentTask.getJ().get(i_j));
                    c_mutex += this.localResults.getC().get(i_bid);
                } else if (D[i_task][i_j] >= 1) {
                    i_bid = this.localResults.getJ().indexOf(parentTask.getJ().get(i_j));
                    c_bid += this.localResults.getC().get(i_bid);
                }
            }
            c_bid += this.localResults.getY().get(i_av);

            if(c_mutex > c_bid){
                // opposing coalition has higher costs
                return true;
            }
            else if(c_mutex < c_bid){
                // your coalition has higher costs
                return false;
            }
            else {
                // if costs and bids are equal, the task highest on the list gets allocated
                int i_them = 0;
                int i_us = parentTask.getJ().indexOf(j);

                for (int i_j = 0; i_j < parentTask.getJ().size(); i_j++) {
                    if( (i_j != i_task) && (D[i_task][i_j] < 0) ){
                        i_them = i_j;
                        break;
                    }
                }
                return (i_us > i_them);
            }
        }
    }

    private boolean depSat(Subtask j){
        Vector<Integer> v = localResults.getV();
        Vector<Integer> w_solo = localResults.getW_solo();
        Vector<Integer> w_any = localResults.getW_any();
        Vector<AbstractSimulatedAgent> z = localResults.getZ();

        Task parentTask = j.getParentTask();
        int i_task = parentTask.getJ().indexOf(j);
        int i_j = localResults.getJ().indexOf(j);
        int[][] D = parentTask.getD();
        int i_av = localResults.getJ().indexOf(j);

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

        if ( isOptimistic(j) ) { // task has optimistic bidding strategy
            if(v.get(i_j) == 0) {
                if ( (n_sat == 0)  && (N_req > 0) ) {
                    // agent must be the first to win a bid for this tasks
                    w_solo.setElementAt((w_solo.get(i_j) - 1), i_j);
                    this.localResults.setW_solo(w_solo);
                }
                else if( (N_req > n_sat)  && (N_req > 0) ){
                    // agent bids on a task without all of its requirements met for the first time
                    w_any.setElementAt((w_any.get(i_j) - 1), i_j);
                    this.localResults.setW_solo(w_any);
                }
            }

            if ( (N_req != n_sat) && (N_req > 0) ) { //if not all dependencies are met, v_i++
                v.setElementAt((v.get(i_j) + 1), i_j);
                this.localResults.setV(v);
            }
            else if( (N_req == n_sat) && (N_req > 0)){ // if all dependencies are met, v_i = 0
                v.setElementAt(0, i_j);
                this.localResults.setV(v);
            }

            if (v.get(i_j) > this.O_kq) { // if task has held on to task for too long, release task
                w_solo.setElementAt((w_solo.get(i_j) - 1), i_j);
                w_any.setElementAt((w_any.get(i_j) - 1), i_j);
                this.localResults.setW_solo(w_solo);
                this.localResults.setW_any(w_any);

                return false;
            }
        }
        else { // task has pessimistic bidding strategy
            //if not all dependencies are met
            if( N_req > n_sat){
                //release task
                return false;
            }
        }
        return true;
    }

    private boolean coalitionSat(Subtask j, Vector<Vector<AbstractSimulatedAgent>> oldOmega, Vector<Vector<AbstractSimulatedAgent>> newOmega){
        // Check Coalition Member Constraints
        int i_b = localResults.getBundle().indexOf(j);

        if (oldOmega.get(i_b).size() == 0) { // no coalition partners in original list
            if(newOmega.get(i_b).size() > 0){ // new coalition partners in new list
                // release task
                return false;
            }
        }
        else{ // coalition partners exist in original list, compare lists
            if(newOmega.get(i_b).size() > 0){ // new list is not empty
                // compare lists
                if(oldOmega.get(i_b).size() != newOmega.get(i_b).size()){ // if different sizes, then lists are not the same
                    // release task
                    return false;
                }
                else{ // compare element by element
                    boolean released = false;
                    for(AbstractSimulatedAgent listMember : oldOmega.get(i_b)){
                        if(!newOmega.get(i_b).contains(listMember)){ // if new list does not contain member of old list, then lists are not the same
                            // release task
                            released = true;
                            break;
                        }
                    }
                    if(released){
                        // release task
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean timeSat(Subtask j){
        boolean taskReleased = false;
        Task parenTask = j.getParentTask();
        int[][] D = parenTask.getD();

        Vector<Integer> tempViolations = tempSat(j);

        int i_q = localResults.getJ().indexOf(j);
        int i_o = i_q - parenTask.getJ().indexOf(j);
        for (int i_v = 0; i_v < tempViolations.size(); i_v++) {  // if time constraint violations exist
            //compare each time violation
            int i_u = tempViolations.get(i_v);
            if ((D[parenTask.getJ().indexOf(j)][i_u - i_o] == 1) && (D[i_u - i_o][parenTask.getJ().indexOf(j)] <= 0)) {
                //release task
                taskReleased = true;
                break;
            } else if ((D[parenTask.getJ().indexOf(j)][i_u - i_o] == 1) && (D[i_u - i_o][parenTask.getJ().indexOf(j)] == 1)) {
                double tz_q = localResults.getTz().get(localResults.getJ().indexOf(j));
                double tz_u = localResults.getTz().get(i_u);
                double t_start = t_0;
                if (tz_q - t_start <= tz_u - t_start) {
                    // release task
                    taskReleased = true;
                    break;
                }

            }
        }

        if (taskReleased) {
            if(isOptimistic(j)) {
                Vector<Integer> w_any = localResults.getW_any();
                Vector<Integer> w_solo = localResults.getW_solo();
                w_any.setElementAt(w_any.get(i_q) - 1, i_q);
                w_solo.setElementAt(w_solo.get(i_q) - 1, i_q);
                localResults.setW_any(w_any);
                localResults.setW_solo(w_solo);
            }
            return false;
        }

        return true;
    }

    private Vector<Integer> tempSat(Subtask k_q){
        double[][] T = k_q.getParentTask().getT();
        Vector<Subtask> J_parent = k_q.getParentTask().getJ();
        Vector<Subtask> J_results = localResults.getJ();
        Vector<Double> tz = localResults.getTz();
        Vector<AbstractSimulatedAgent> z = localResults.getZ();

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

    private Vector<Vector<AbstractSimulatedAgent>> getNewCoalitionMemebers(Subtask j) {
        Vector<Vector<AbstractSimulatedAgent>> newOmega = new Vector<>();
        for(int i = 0; i < this.M; i++) {
            Vector<AbstractSimulatedAgent> tempCoal = new Vector<>();

            if( localResults.getBundle().size() >= i+1 ) {
                for (int i_o = 0; i_o < this.localResults.getJ().size(); i_o++) {
                    if ((this.localResults.getZ().get(i_o) != this)             // if winner at i_o is not me
                            && (this.localResults.getZ().get(i_o) != null)      // and if winner at i_o is not empty
                            && (j.getParentTask() == localResults.getJ().get(i_o).getParentTask())) // and subtasks share a task
                    {
                        // then winner at i_o is a coalition partner
                        tempCoal.add(this.localResults.getZ().get(i_o));
                    }
                }
            }
            newOmega.add(tempCoal);
        }
        return newOmega;
    }

    private boolean canBid(Subtask j, int i_av, IterationLists results){
        int i_j = results.getJ().indexOf(j);
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
        else if( results.getW_all().get(i_j) >= this.O_all ) {
            // agent has bid on task for multiple iterations and reached no consensus
            double wait = Math.random();
            if(wait <= 0.5){
                // randomly decide whether to wait or not to bid
                return false;
            }
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

        // check if dependent task is about to reach coalition violation timeout
        for(Subtask subtask : parentTask.getJ()){
            int i_q = results.getJ().indexOf(subtask);
            int i_jd = parentTask.getJ().indexOf(j);
            int i_qd = parentTask.getJ().indexOf(subtask);

            if( (results.getV().get(i_q) >= this.O_kq) && (D[i_jd][i_qd] >= 1) ){
                // if dependent subtask is about to be timed out, then don't bid
                return false;
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
    public Dimension getInitialPosition(){return this.initialPosition; }
    public double getSpeed() { return speed; }
    public Vector<String> getSensors() { return sensors; }
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
    public Vector<Integer> getDoingIterations(){ return this.doingIterations; }

    public void setEnvironment(Scenario environment) { this.environment = environment; }
    public void setLocation(Dimension location) { this.location = location; }
    public void setSpeed(double speed) { this.speed = speed; }
    public void setSensors(Vector<String> sensors) { this.sensors = sensors; }
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
    public void setReceivedResults(Vector<IterationLists> receivedResults) { this.receivedResults = receivedResults; }

}