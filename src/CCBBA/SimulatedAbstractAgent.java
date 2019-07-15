package CCBBA;

import madkit.kernel.AbstractAgent;

import java.awt.*;
import java.util.Vector;

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
    protected int O_kq = 10;                                        //
    protected Vector<IterationResults> results;                     // list of results
    protected Vector<Subtask> bundle;
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
        doTasks();
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


        while(consensus){
            //Phase 1
            if(zeta == 0){
                // Set results to 0
                localResults = new IterationResults(J, O_kq);
            }
            else{
                // Import results from previous iteration
                localResults = new IterationResults(results.get(zeta - 1));
            }

            // Generate Bundle
            while(bundle.size() < M){
                for(int i = 0; i < J.size(); i++){
                    // Calculate possible bid for every subtask to be added to the bundle
                    Subtask j = J.get(i);

                    if(bundle.contains(j)){

                        continue;
                    }

                }
            }


            results.add(localResults);

            //Phase 2
        }

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

        //boolean req1;   // Subtask requires sensor contained in this agent?
        boolean req2;   // Is subtask complete?

        for(int i = 0; i < V.size(); i++){
            Vector<Subtask> J_i = V.get(i).getJ();
            for(int j = 0; j < J_i.size(); j++) {
                /*
                if(sensors.contains(J_i.get(j).getMain_task())) req1 = true;
                else req1 = false;

                if(J_i.get(j).getComplete()) req2 = true;
                else req2 = false;
                */
                if (!J_i.get(j).getComplete()) {
                    J_available.add(J_i.get(j));
                }
            }
        }

        return J_available;
    }

    protected Vector<Vector<Subtask>> getPossiblePaths(Vector<Subtask> oldBundle, Subtask j){
        // Calculates all possible permutations of paths
        Vector<Vector<Subtask>> possiblePaths = new Vector<>();

        // Adds j to temporary bundle
        Vector<Subtask> temp_bundle = new Vector<>();
        for(int i = 0; i < oldBundle.size(); i++) {
            temp_bundle.add(oldBundle.get(i));
        }
        temp_bundle.add(j);

        Vector<Integer> count = new Vector<>();
        Vector<Subtask> combination = new Vector<>();
        combination.setSize(temp_bundle.size());
        count.setSize(temp_bundle.size());
        for(int i = 0; i < count.size(); i++){
            count.setElementAt(1,i);
        }

        generateCombinations(combination, temp_bundle, possiblePaths, count, 0);


        return possiblePaths;
    }

    protected int getMax(Vector<SubtaskBid> bid_list, Vector<Integer> h_aj){
        // obtains maximum value of a vector of type double
        double max = Double.NEGATIVE_INFINITY;
        int max_i = Integer.MIN_VALUE;

        for(int i = 0; i < bid_list.size(); i++){
            if(max < bid_list.get(i).getC()*h_aj.get(i)){
                max = bid_list.get(i).getC();
                max_i = i;
            }
        }

        return max_i;
    }

    protected int coalitionTest(SubtaskBid bid, Subtask j, Vector<Vector> iteration_results, int i_task){
        double new_bid = 0.0;
        double coalition_bid = 0.0;
        Vector<Double> y = iteration_results.get(0);
        Vector<SimulatedAbstractAgent> z = iteration_results.get(1);
        int[][] D = j.getParentTask().getD();
        int D_jq = 0;
        int D_jk = 0;

        for(int i = 0; i < y.size(); i++){
            if((z.get(i) == z.get(i_task)) && (D_jq == 1) ){
                // add to coalition_bid
                coalition_bid = coalition_bid + y.get(i);
            }
            if((z.get(i) == this) && (D_jk == 1) ){
                // add to new_bid
                new_bid = new_bid + y.get(i);
            }
        }
        new_bid = new_bid + bid.getC();

        if(new_bid > coalition_bid){
            return 1;
        }
        else{ return 0; }
    }

    protected int mutexTest(SubtaskBid bid, SimulatedAbstractAgent agent){
        double new_bid = 0.0;
        double coalition_bid = 0.0;

        if(new_bid > coalition_bid){
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