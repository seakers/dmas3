package CCBBA;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;

import java.awt.*;
import java.util.Vector;
import java.util.logging.Level;

public class SimulatedAbstractAgent extends Agent {

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
    protected Vector<Subtask> bundle;                               // bundle of chosen subtasks
    protected Vector<Subtask> path;                                 // path chosen

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
    }


    @SuppressWarnings("unused")
    protected void doSim(){
        thinkPlan();
        //doTasks();
    }

    /**
     * Main Sim functions
     */

    @SuppressWarnings("unused")
    private void thinkPlan(){
        requestRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK);

        // Phase 1 - Create bid for individual spacecraft
        getLogger().info("Planning Tasks...");

        // intialize values
        int zeta = 0;
        boolean consensus = false;
        results = new Vector<>();
        IterationResults localResults;

        // Get incomplete subtasks
        J = getIncompleteSubtasks();


        while(!consensus){
        /*
        //Phase 1 - Task Selection
            if(zeta == 0){
                // Set results to 0
                localResults = new IterationResults(J, O_kq);
            }
            else{
                // Import results from previous iteration
                localResults = new IterationResults(results.get(zeta - 1));
            }

            // Generate Bundle
            while( (bundle.size() < M)&&(localResults.getH().contains(1)) ){
                Vector<SubtaskBid> bidList = new Vector<>();

                for(int i = 0; i < J.size(); i++){
                    // Calculate possible bid for every subtask to be added to the bundle
                    Subtask j = J.get(i);
                    if(bundle.contains(j)){
                        // If subtask exists in bundle, skip bid for this subtask
                        continue;
                    }

                    // Check if bid can be placed on
                    if(canBid(j, i, localResults)) {
                        // task can be bid on

                        // Calculate bid for subtask
                        SubtaskBid localBid = new SubtaskBid();
                        //localBid.calcBidForSubtask();

                        bidList.add(localBid);

                        // Coalition & Mutex Tests
                        Vector<Integer> h = localResults.getH();
                        h.setElementAt( coalitionTest(localBid, localResults, j, i), i);
                        if(h.get(i) == 1){
                            h.setElementAt( mutexTest(localBid, localResults, j, i), i);
                        }
                        localResults.setH( h );
                    }
                    else{
                        // task cannot be bid on
                        // Give a bid of 0;
                        SubtaskBid localBid = new SubtaskBid();
                        bidList.add(localBid);

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
                    }
                }

                // Update results
                localResults.updateResults(maxBid, i_max);
                results.add(localResults);
            }
        */
        //Phase 2 - Consensus
            AgentAddress other = null;
            while (other == null) {
                // This way, I wait for another coming into play
                other = getAgentWithRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK);
                pause(1000);
            }

            getLogger().info("\n\tI found someone !!\n" + other + "\n\n");
            pause(1000);

            // sending the message to other: Success will be logged
            sendMessage(other, new Message());
            pause(1000);

            // waiting other's message: The reception will be logged
            waitNextMessage();
            pause(10000);



            zeta++;
            consensus = true;
        }


        // TEST OUTPUTS <- DELETE WHEN DONE --------------------
        System.out.println(results.get(results.size()).getY());
        System.out.println(results.get(results.size()).getZ());
        System.out.println(results.get(results.size()).getTz());
        // -----------------------------------------------------
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

    /*
    protected void generateCombinations(Vector<Integer> combination, Vector<Integer> temp_bundle, Vector<Vector<Integer>> possiblePaths, Vector<Integer> count, int level){
        if(level == combination.size()){
            Vector<Integer> addedPath = new Vector<>();
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

        /*
        // PERMUTATION TEST********************** GOES ON THINK FUNCTION
        Vector<Subtask> b = new Vector<>();
        Vector<Subtask> combination = new Vector<>();
        Vector<Integer> count = new Vector<>();
        Vector<Vector<Subtask>> paths = new Vector<>();

        b.setSize(3);
        combination.setSize(3);
        count.setSize(3);
        for(int i = 0; i < 3; i++){
            Subtask temp_subtask = new Subtask("IR", 1, j.getParentTask());
            b.setElementAt(temp_subtask,i);
            combination.setElementAt(null,i);
            count.setElementAt(1,i);
        }

        generateCombinations(combination, b, paths, count, 0);

        System.out.println(paths);
        // ************************************************
        */

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

    public double getMiu(){ return miu; }
    public double getSpeed(){ return speed; }
    //public Vector<Subtask> getPath(){ return path; }
}