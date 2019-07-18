package CCBBA;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;

import java.awt.*;
import java.util.Vector;
import java.util.logging.Level;
import java.util.List;

import static java.awt.desktop.UserSessionEvent.Reason.LOCK;

public class SimulatedAbstractAgent extends AbstractAgent {

    /**
     * Agent's Scenario
     */
    protected Scenario environment;
    double inf = Double.POSITIVE_INFINITY;

    /**
     * Properties
     */
    protected Dimension location = new Dimension();                 // current location
    protected double speed = 1.0;                                   // displacement speed of agent
    protected Vector<String> sensors = new Vector<>();              // list of all sensors
    protected Vector<Subtask> J = new Vector<>();                   // list of all subtasks
    protected double miu = 1.0;                                     // Travel cost
    protected int M;                                                // planning horizon
    protected int O_kq = 10;                                        // max iterations in constraint violations
    protected int W_solo_max = 10;                                  // max permissions to bid solo
    protected int W_any_max = 100;                                  // max permissions to bid on any
    protected Vector<IterationResults> results;                     // list of results
    protected Vector<Subtask> bundle = new Vector<>();              // bundle of chosen subtasks
    protected Vector<Subtask> path = new Vector<>();                // path chosen
    protected Vector<Dimension> X_path = new Vector<>();            // path locations
    protected long startTime;

    List<AgentAddress> list_agents;
    int zeta = 0;
    IterationResults localResults;

    /**
     * initialize my role and fields
     */
    @Override
    protected void activate() {
        // Request Role
        requestRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK);

        // Initiate position
        location = getInitialPosition();

        // Initiate Sensor Vector
        sensors = getSensorList();

        // Initiate Planning Horizon
        M = getM();

        // Initiate Bundle
        bundle = new Vector<>();


        this.zeta = 0;
        results = new Vector<>();
    }

    /**
     * Main Sim functions
     */
    public void phaseOne(){
        // Request role and obtain list of planning agents
        requestRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK);

        this.list_agents = getAgentsWithRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK, false);

        // Phase 1 - Create bid for individual spacecraft
        getLogger().info("Planning Tasks...");
        getLogger().info("Phase one...");

        // Get incomplete subtasks
        J = getIncompleteSubtasks();

        //Phase 1 - Task Selection
        // -Initialize results
        if(this.zeta == 0){
            // Set results to 0
            localResults = new IterationResults(J, O_kq);
        }
        else{
            // Import results from previous iteration
            localResults = new IterationResults(results.get(zeta - 1));
        }

        // -Generate Bundle
        while( (bundle.size() < M)&&(localResults.getH().contains(1)) ){
            Vector<SubtaskBid> bidList = new Vector<>();
            Subtask j_chosen = null;

            // Calculate bid for every subtask
            for(int i = 0; i < J.size(); i++){
                Subtask j = J.get(i);

                // Check if subtask can be bid on
                if(canBid(j, i, localResults)) { // task can be bid on
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
            double currentMax = Double.NEGATIVE_INFINITY;
            int i_max = 0;
            SubtaskBid maxBid = new SubtaskBid();

            for(int i = 0; i < bidList.size(); i++){
                double c = bidList.get(i).getC();
                int h = localResults.getH().get(i);
                if( c*h > currentMax ){
                    currentMax = c*h;
                    i_max = i;
                    maxBid = bidList.get(i);
                    j_chosen = this.J.get(i_max);
                }
            }

            // Update results
            this.bundle.add(j_chosen);
            this.path.add(maxBid.getI_opt(),j_chosen);
            this.X_path.add(maxBid.getX_aj());
            localResults.updateResults(maxBid, i_max, this, zeta);
        }

    }

    public void phaseTwo() {
        //Phase 2 - Consensus
        getLogger().info("Phase two...");

        //Broadcast results
        myMessage myResults = new myMessage(localResults, this.getName());
        for(int i = 0; i < list_agents.size(); i++) sendMessage(list_agents.get(i), myResults);

        //Receive results
        List<Message> receivedMessages = nextMessages(null);
        Vector<IterationResults> receivedResults = new Vector<>();
        for(int i = 0; i < receivedMessages.size(); i++){
            myMessage message = (myMessage) receivedMessages.get(i);
            receivedResults.add(message.myResults);
        }

        // Rule-Based Check
        // check consistency:
        boolean consistent = false;

            for(int i = 0; i < receivedResults.size(); i++){ // for every received result
                // compare local results to each received result
                for (int i_j = 0; i_j < localResults.getY().size(); i_j++){
                    if(localResults.getY().get(i_j) != receivedResults.get(i).getY().get(i_j)){
                        //
                        getLogger().info("Inconsistencies in plan found !!");
                        consistent = false;
                        break;
                    }
                }
                if(!consistent){ break; }
                else {
                    // no inconsistancy found
                    getLogger().info("NO inconsistencies in plan found. Checking convergence...");
                }
            }

            if(consistent == false) { // No consensus reached
            getLogger().info("Fixing inconsistencies...");

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

                    // Comparing results. See Ref 40 Table 1
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
                        else if( (myZ != me)&&(myZ != it) ){
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
                            localResults.leaveResults(receivedResults.get(i), i_j, bundle);
                        }
                        else if( myZ == it){
                            // reset
                            localResults.resetResults(receivedResults.get(i), i_j, bundle);
                            removeFromBundle(localResults.getJ(), i_j);
                        }
                        else if( (myZ != me)&&(myZ != it) ){
                            if(itsS > myS){
                                // reset
                                localResults.resetResults(receivedResults.get(i), i_j, bundle);
                                removeFromBundle(localResults.getJ(), i_j);
                            }
                        }
                        else if( myZ == "" ){
                            // leave
                            localResults.leaveResults(receivedResults.get(i), i_j);
                        }
                    }
                    else if( (itsZ != it)&&( itsZ != me) ){
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
                                localResults.resetResults(receivedResults.get(i), i_j, bundle);
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
                        else if( (myZ != me)&&(myZ != it)&&(myZ != itsZ) ){
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
                    else if( itsZ == ""){
                        if( myZ == me ){
                            // leave
                            localResults.leaveResults(receivedResults.get(i), i_j);
                        }
                        else if( myZ == it){
                            // update
                            localResults.updateResults(receivedResults.get(i), i_j, bundle);
                            removeFromBundle(localResults.getJ(), i_j);
                        }
                        else if( (myZ != me)&&(myZ != it) ){
                            if(itsS > myS){
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
                }
            }
        }

        zeta++;
        results.add(localResults);
    }

    @SuppressWarnings("unused")
    private void doTasks(){
        getLogger().info("Doing Tasks...");
        requestRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_DO);
        // set task from environment to COMPLETE
        // pop task from bundle list
        //popTask();
        requestRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK);
    }



    /**
     * Misc tools and functions
     */

    protected Vector<Subtask> getIncompleteSubtasks(){
        //Looks for tasks from environment and checks for completion
        Vector<Task> V = environment.getTasks();
        Vector<Subtask> J_available = new Vector<>();
        boolean req2;   // Is subtask complete?

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

        // checks if bid for the parent task already exists
        for(int i = 0; i < bundle.size(); i++){
            if(j.getParentTask() == bundle.get(i).getParentTask()){
                return false;
            }
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
            return (n_sat == N_req);
        }
        else{
            // Agent has NOT spent all possible tries biding on this task with dependencies
            // Optimistic Bidding Strategy to be used
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
            if( D[i_task][k] == 1){ N_req++; }
            if( (z.get(i_av - i_task + k) != null )&&(D[i_task][k] == 1) ){ n_sat++; }
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
        double c = bid.getC();
        int[][] D = j.getParentTask().getD();

        double new_bid = 0.0;
        double coalition_bid = 0.0;

        for(int i = 0; i < y.size(); i++){
            // Check if j and q are in the same task
            if( ( i > (i_subtask-J_parent.indexOf(j) ) )&&( i < ( i_subtask-J_parent.indexOf(j)+J_parent.size() ) ) ){
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
        new_bid = new_bid + c;

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
        double max_bid = 0.0;

        for(int i = 0; i < y.size(); i++){
            // Check if j and q are in the same task
            if( ( i > (i_subtask-J_parent.indexOf(j) ) )&&( i < ( i_subtask-J_parent.indexOf(j)+J_parent.size() ) ) ){
                int j_index = J_parent.indexOf(j);
                int q_index = i - (i_subtask - J_parent.indexOf(j));

                if(D[ j_index ][ q_index ] == 1){
                    if(i != i_subtask){
                        new_bid = new_bid + y.get(i);
                    }


                }

            }
        }
        new_bid = new_bid + c;

        if(new_bid > max_bid){
            return 1;
        }
        else{ return 0; }
    }

    protected Vector<Vector<SimulatedAbstractAgent>> updateCoalitionMates() {
        Vector<Vector<SimulatedAbstractAgent>> newCoalitions = new Vector<>();

        return newCoalitions;
    }

    protected void generateCombinations(Vector<Subtask> combination, Vector<Subtask> temp_bundle, Vector<Vector<Subtask>> possiblePaths, Vector<Integer> count, int level){
        if(level == combination.size()){
            Vector<Subtask> addedPath = new Vector<>();
            for(int i = 0; i < combination.size(); i++){
                addedPath.add(combination.get(i));
            }

            possiblePaths.add(addedPath);
            return;
        }
        for(int i = 0; i < temp_bundle.size(); i++){
            if(count.get(i)==0){
                continue;
            }
            combination.setElementAt(temp_bundle.get(i), level);
            count.setElementAt(count.get(i)-1 ,i);
            generateCombinations(combination, temp_bundle, possiblePaths, count, level+1);
            count.setElementAt(count.get(i)+1 ,i);
        }
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
        int M_agent = 2;
        return M_agent;
    }

    public double getMiu(){ return this.miu; }
    public double getSpeed(){ return this.speed; }
    public Vector<Subtask> getPath(){ return this.path; }
    public Vector<Subtask> getBundle(){ return this.bundle; }
}