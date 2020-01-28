package CCBBA.lib;

import jmetal.encodings.variable.Int;
import madkit.kernel.AbstractAgent;
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
    private int z;                                          // iteration counter
    private int M;                                          // planning horizon
    private int O_kq;                                       // max iterations in constraint violation
    private AgentResources myResources;


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

    @SuppressWarnings("unused")
    public void thinkingPhaseOne(){
        // check for new tasks


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

    private void unpackInput(JSONObject inputAgentData) {
        getLogger().config("Configuring agent...");

        // -Agent name
        this.name = inputAgentData.get("Name").toString();

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
        this.myResources = new AgentResources(inputAgentData);

        int x = 1;
    }
}
