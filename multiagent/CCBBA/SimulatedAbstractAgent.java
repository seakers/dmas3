package seakers.orekit.multiagent.CCBBA;

import com.sun.org.apache.xpath.internal.operations.Bool;
import jmetal.encodings.variable.Int;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import madkit.kernel.Message;
import java.lang.String;

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
    protected Dimension location = new Dimension();
    protected double speed = 1.0;
    protected Vector<String> sensors = new Vector<>();

    protected Vector<Vector<Double>> y_a = new Vector<>();          // winner bid list
    protected Vector<Vector<SimulatedAbstractAgent>> z_a = new Vector<>();// winner agents list
    protected Vector<Vector<Double>> tz_a = new Vector<>();         // arrival times list
    protected Vector<Vector<Double>> c_a = new Vector<>();          // self bid list
    protected Vector<Vector<Integer>> s_a = new Vector<>();         // iteration stamp vector
    //protected Vector<Vector<Integer>> h_a = new Vector<>();         // availability digit

    protected Vector<Subtask> bundle = new Vector<>();              // bundle of tasks chosen by agent
    protected Vector<Subtask> J = new Vector<>();                   // list of all subtasks
    protected int M;                                                // planning horizon
    protected Vector<Subtask> path = new Vector();                  // path list
    protected Vector<Dimension> X = new Vector<>();                 // location of realization list
    protected Vector<Double> t_a = new Vector<>();                  // Vector of execution times
    protected Vector<Vector<Integer>> h_a;                          // Availability vectors

    Vector<Vector<SimulatedAbstractAgent>> coalitionMates = new Vector<>(); // coalition mates matrix
    protected double miu = 1.0;                                     // Travel cost

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
        int zeta = 0;
        boolean consensus = false;
        while(consensus == false) {
            // Get available subtasks
            J = getAvailableSubtasks();

            // Initiate iteration results
            Vector<Vector> iteration_results = new Vector();
            Vector<Double> y_aj = new Vector<>();                   y_aj.setSize(J.size());
            Vector<SimulatedAbstractAgent> z_aj = new Vector<>();   z_aj.setSize(J.size());
            Vector<Double> tz_aj = new Vector<>();                  tz_aj.setSize(J.size());
            Vector<Double> c_aj = new Vector<>();                   c_aj.setSize(J.size());
            Vector<Integer> s_aj = new Vector<>();                  s_aj.setSize(J.size());
            Vector<Integer> h_aj = new Vector<>();                  h_aj.setSize(J.size());     // availability vector

            if (zeta == 0) {
                // Initialize all variables to 0
                for(int i = 0; i < J.size(); i++){
                    y_aj.setElementAt(0.0 ,i);
                    z_aj.setElementAt(null ,i);
                    tz_aj.setElementAt(0.0 ,i);
                    c_aj.setElementAt(0.0 ,i);
                    s_aj.setElementAt(0 ,i);
                    h_aj.setElementAt(1, i);
                }

                y_a = new Vector<>();   y_a.add(y_aj);
                z_a = new Vector<>();   z_a.add(z_aj);
                tz_a = new Vector<>();  tz_a.add(tz_aj);
                c_a = new Vector<>();   c_a.add(c_aj);
                s_a = new Vector<>();   s_a.add(s_aj);
                h_a = new Vector();     h_a.add(h_aj);
                path = new Vector();
                X = new Vector<>();

            } else {
                // load previous iteration's vectors
                y_a.add(y_a.get(zeta - 1));
                z_a.add(z_a.get(zeta - 1));
                tz_a.add(tz_a.get(zeta - 1));
                c_a.add(c_a.get(zeta - 1));
                s_a.add(s_a.get(zeta - 1));
                h_a.add(h_a.get(zeta - 1));

                y_aj = y_a.get(zeta - 1);
                z_aj = z_a.get(zeta - 1);
                tz_aj = tz_a.get(zeta - 1);
                c_aj = c_a.get(zeta - 1);
                s_aj = s_a.get(zeta - 1);
                h_aj = h_a.get(zeta - 1);
            }

            iteration_results.add(y_aj);
            iteration_results.add(z_aj);
            iteration_results.add(tz_aj);
            iteration_results.add(c_aj);
            iteration_results.add(s_aj);



            do{ // While the bundle is empty or smaller than M and tasks are available:
                Vector<SubtaskBid> bid_list = new Vector<>();   bid_list.setSize(J.size()); // list of bids for available tasks

                // Calculate bid for each task
                for(int j = 0; j < J.size(); j++){
                    Subtask j_j = J.get(j);
                    /*
                    // Calculate all possible paths when adding j_j to the bundle
                    Vector<Vector<Subtask>> possiblePaths = getPossiblePaths(bundle, j_j);

                    // Create bids for each path and obtain maximum bid for this subtask
                    SubtaskBid bid_j = new SubtaskBid();
                    //bid_j.calcBidForSubtask(possiblePaths, j_j,this);
                    bid_j.calcBidForSubtask(possiblePaths, j_j, j, this);
                    bid_list.setElementAt(bid_j, j);
                    */

                    SubtaskBid bid_j = new SubtaskBid();
                    bid_j.calcBidForSubtask(path, bundle, j_j, iteration_results, this);

                    // Coalition and mux tests
                    h_aj.setElementAt(coalitionTest(bid_j, j_j, iteration_results, j) , j);
                    if(h_aj.get(j) == 1){
                        h_aj.setElementAt(mutexTest(bid_j, this) , j);
                    }
                }

                // Find max allowable bid
                int i_max = getMax(bid_list, h_aj);
                SubtaskBid maxBid = bid_list.get(i_max);
                Subtask j_chosen = J.elementAt(i_max);

                // Add task to bundle and path
                bundle.add(j_chosen);
                path.add(maxBid.getI_opt(), j_chosen);
                X.add(maxBid.getI_opt(), maxBid.getX_aj());
                t_a.add(maxBid.getI_opt(), maxBid.getTStart());

                // Update lists
                /*
                Vector<Double> y_aj = y_a.get(zeta);
                Vector<SimulatedAbstractAgent> z_aj = z_a.get(zeta);
                Vector<Integer> s_aj = s_a.get(zeta);
                Vector<Double> tz_aj = tz_a.get(zeta);

                y_aj.setElementAt(maxBid.getC(),i_max);
                z_aj.setElementAt(this,i_max);
                s_aj.setElementAt(zeta,i_max);
                tz_aj.setElementAt(maxBid.getTStart(),i_max);

                y_a.setElementAt(y_aj, zeta);
                z_a.setElementAt(z_aj, zeta);
                s_a.setElementAt(s_aj, zeta);
                tz_a.setElementAt(tz_aj, zeta);
                */

                // Update Coalition Mate Matrix
                coalitionMates = updateCoalitionMates();

            }while ( (bundle.size() <= M) && (h_aj.contains(1)) );

            // Phase 2 - Consult with other agents
            getLogger().info("Sharing Tasks...");

            zeta++;
            if(zeta > 3) consensus = true; //<- DEBUGGING TOOL. REMOVE WHEN DONE
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

    protected Vector<Subtask> getAvailableSubtasks(){
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
    public Vector<Subtask> getPath(){ return path; }
}
