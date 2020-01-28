package CCBBA.lib;

import jmetal.encodings.variable.Int;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.logging.Level;

public class SimulatedAgent extends AbstractAgent {
    /**
     * Input parameters from file
     */
    private Level loggerLevel;

    /**
     * Properties
     */
    private Scenario environment;                           // world environment
    private String name;                                    // agent name
    private ArrayList<String> sensorList;                   // list of available sensors
    private ArrayList<Double> position;                     // agent position
    private ArrayList<Double> velocity;                     // agent velocity
    private double speed;                                   // agent speed
    private double mass;                                    // agent mass
    private ArrayList<IterationResults> localResults;       // list of local results
    private ArrayList<Task> worldTasks;                     // list of tasks in world environment
    private ArrayList<Subtask> worldSubtasks;               // list of subtasks in world environment
    private int zeta;                                       // iteration counter
    private int M;                                          // planning horizon
    private int O_kq;                                       // max iterations in constraint violation
    private AgentResources myResources;                     // agent resources
    private ArrayList<Subtask> bundle;                      // list of tasks in agent's plan
    private double t_0;                                     // start time


    public SimulatedAgent(JSONObject inputAgentData, JSONObject inputData) throws Exception {
        // Set up logger level
        setUpLogger(inputData);

        // Check input formatting
        checkInputFormat(inputAgentData, inputData);

        // Unpack input data
        unpackInput(inputAgentData);
    }

    @Override
    protected void activate() {
        getLogger().info("Initiating agent");

        // Request Role
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
        getLogger().config("Assigned to " + getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP) + " role");

        // Initiate local results
        this.z = 0;
        this.bundle = new ArrayList<>();
        this.t_0 = this.environment.getT_0();

        // Get world subtasks
        this.worldTasks = new ArrayList<>();
        this.worldSubtasks = new ArrayList<>();
        for(Task J : this.environment.getScenarioTasks()){
            this.worldTasks.add(J);
            this.worldSubtasks.addAll(J.getSubtaskList());
        }
        getLogger().info(this.worldTasks.size() + " Tasks found in world");
        getLogger().info(this.worldSubtasks.size() + " Subtasks found in world");

        // Initiate iteration results
        this.localResults = new ArrayList<>();
        for(Subtask j : this.worldSubtasks){
            this.localResults.add( new IterationResults(j) );
        }
    }

    /**
     * Planner functions
     */
    @SuppressWarnings("unused")
    public void thinkingPhaseOne(){
        getLogger().info("Starting phase one...");

        // check for new tasks
        getAvailableSubtasks();

        // Empty mailbox
        emptyMailbox();

        // construct bundle
        boolean alive = true;
        var myRoles = getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);
        if(myRoles.contains( SimGroups.AGENT_DIE )){
            alive = false;
        }

        getLogger().info("Constructing bundle");
        while( (this.bundle.size() < this.M) && (checkForAvailability()) && alive ){
            getLogger().info("Calculating bids for every subtask");

            ArrayList<SubtaskBid> bidList = new ArrayList<SubtaskBid>();
            Subtask j_chosen = null;

            // Calculate bid for every subtask
            for(IterationResults result : this.localResults){
                Subtask j = result.getJ();

                // Chefk if subtask can be bid on
                if( canBid(result) ){

                }
            }
            break;
        }

        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2);
    }

    private boolean canBid(IterationResults result){
        Subtask j = result.getJ();
        if( !this.sensorList.contains( j.getMain_task() ) ){
            // check if agent contains sensor for subtask
            return false;
        }
        // MISSING COMPLETION CHECKS
        else if( this.bundle.contains(j) ){
            return false;
        }

        // check if bid for a subtask of the same task is in the bundle
        int i_q = j.getI_q();
        Task parentTask = j.getParentTask();
        int[][] D = parentTask.getD();

        // check if subtask in question is mutually exclusive with a bid already in the bundle
        for(Subtask bundleSubtask : this.bundle){
            if(bundleSubtask.getParentTask() == parentTask) {
                int i_b = bundleSubtask.getI_q();
                if (D[i_q][i_b] == -1) { // if subtask j has a mutually exclusive task in bundle, you cannot bid
                    return false;
                }
            }
        }

        // check if dependent task is about to reach coalition violation timeout
        for(Subtask subtask : parentTask.getSubtaskList()){
            int i_jd = j.getI_q();
            int i_qd = subtask.getI_q();

            if( (result.getV() >= this.O_kq) && (D[i_jd][i_qd] >= 1) ){
                // if dependent subtask is about to be timed out, then don't bid
                return false;
            }
        }

        // Count number of requirements and number of completed requirements
        int N_req = 0;
        int n_sat = 0;
        for(int k = 0; k < parentTask.getSubtaskList().size(); k++){
            if(i_q == k){ continue;}
            if( D[i_q][k] >= 0){ N_req++; }
            if( (z.get(i_av - i_task + k) != null )&&(D[i_task][k] == 1) ){
                n_sat++;
            }
        }

        return true;
    }

    @SuppressWarnings("unused")
    public void thinkingPhaseTwo(){

    }

    @SuppressWarnings("unused")
    public void consensusPhase(){

    }

    @SuppressWarnings("unused")
    public void doingPhase(){

    }

    /**
     * Misc and helping functions
     */

    private void setUpLogger(JSONObject inputData) throws Exception {
        if(inputData.get("Logger").toString().equals("OFF")){
            this.loggerLevel = Level.OFF;
        }
        else if(inputData.get("Logger").toString().equals("SEVERE")){
            this.loggerLevel = Level.SEVERE;
        }
        else if(inputData.get("Logger").toString().equals("WARNING")){
            this.loggerLevel = Level.WARNING;
        }
        else if(inputData.get("Logger").toString().equals("INFO")){
            this.loggerLevel = Level.INFO;
        }
        else if(inputData.get("Logger").toString().equals("CONFIG")){
            this.loggerLevel = Level.CONFIG;
        }
        else if(inputData.get("Logger").toString().equals("FINE")){
            this.loggerLevel = Level.FINE;
        }
        else if(inputData.get("Logger").toString().equals("FINER")){
            this.loggerLevel = Level.FINER;
        }
        else if(inputData.get("Logger").toString().equals("FINEST")){
            this.loggerLevel = Level.FINEST;
        }
        else if(inputData.get("Logger").toString().equals("ALL")){
            this.loggerLevel = Level.ALL;
        }
        else{
            throw new Exception("INPUT ERROR: Logger type not supported");
        }

        getLogger().setLevel(this.loggerLevel);
    }

    private void checkInputFormat(JSONObject inputAgentData, JSONObject inputData) throws Exception {
        String worldType = ( (JSONObject) ( (JSONObject) inputData.get("Scenario")).get("World")).get("Type").toString();

        if(inputAgentData.get("Name") == null){
            throw new NullPointerException("INPUT ERROR: Agent name not contained in input file");
        }
        else if(inputAgentData.get("SensorList") == null){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " sensor list not contained in input file.");
        }
        else if(inputAgentData.get("Position") == null){
            if(inputAgentData.get("Orbital Parameters") == null){
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " starting position or orbit not contained in input file.");
            }
            else{
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " starting position not contained in input file.");
            }
        }
        else if(worldType.equals("3D_Grid") || worldType.equals("2D_Grid")) {
            if (inputAgentData.get("Speed") == null) {
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " speed not contained in input file.");
            }
            else if(inputAgentData.get("Velocity") != null){
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + "'s velocity does not match world type selected.");
            }
        }

        if(inputAgentData.get("Mass") == null){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " mass not contained in input file.");
        }
        else if( inputAgentData.get("PlanningHorizon") == null ){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " planning horizon not contained in input file.");
        }
        else if( inputAgentData.get("MaxConstraintViolations") == null ){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " max number of constraint violation not contained in input file.");
        }
        else if(inputAgentData.get("Resources") == null){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " resource information not contained in input file.");
        }

    }

    private void unpackInput(JSONObject inputAgentData) throws Exception{
        getLogger().config("Configuring agent...");

        // -Sensor List
        this.sensorList = new ArrayList();
        JSONArray sensorListData = (JSONArray) inputAgentData.get("SensorList");
        for(int i = 0; i < sensorListData.size(); i++){
            this.sensorList.add(sensorListData.get(i).toString() );
        }

        // -Position
        this.position = new ArrayList<>();
        JSONArray positionData = (JSONArray) inputAgentData.get("Position");
        for (Object positionDatum : positionData) {
            this.position.add((double) positionDatum);
        }

        // -Speed or Velocity
        if (inputAgentData.get("Speed") != null) {
            this.speed = (double) inputAgentData.get("Speed");
        }
        else if(inputAgentData.get("Velocity") != null){
            this.velocity = new ArrayList<>();
            JSONArray velocityData = (JSONArray) inputAgentData.get("Velocity");
            for(Object velocityDatum : velocityData){
                this.velocity.add( (double) velocityDatum );
            }
        }

        // -Mass
        this.mass = (double) inputAgentData.get("Mass");

        // -Coalition Restrictions
        this.M = Integer.parseInt( inputAgentData.get("PlanningHorizon").toString() );
        this.O_kq = Integer.parseInt( inputAgentData.get("MaxConstraintViolations").toString() );

        // -Resources
        this.myResources = new AgentResources((JSONObject) inputAgentData.get("Resources"));
    }

    private void getAvailableSubtasks(){
        ArrayList<Task> J_new = this.environment.getScenarioTasks();

        for(Task J : J_new){
            if(!this.worldTasks.contains(J)){
                // if known list of tasks does not include a task, add it to the list and create new results
                getLogger().info("New task in world discovered");
                this.worldTasks.add(J);
                this.worldSubtasks.addAll(J.getSubtaskList());
                for(Subtask j : J.getSubtaskList()){
                    this.localResults.add( new IterationResults(j) );
                }
            }
        }
    }

    private void emptyMailbox(){
        while(!isMessageBoxEmpty()){
            Message tempMessage = nextMessage();
        }
    }

    private boolean checkForAvailability(){
        for(IterationResults result : this.localResults){
            if(result.getH() == 1){ return true; }
        }
        return false;
    }
}
