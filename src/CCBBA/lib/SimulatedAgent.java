package CCBBA.lib;

import madkit.kernel.AbstractAgent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.logging.Level;

public class SimulatedAgent extends AbstractAgent {
    private JSONObject inputAgentData;
    private Level loggerLevel;

    private Scenario environment;
    private String name;
    private ArrayList<String> sensorList;
    private ArrayList<Double> position;
    private ArrayList<Double> velocity;
    private double speed;
    private IterationResults localResults;
    public SimulatedAgent(JSONObject inputAgentData, JSONObject inputData) throws Exception {
        // Load Agent data
        this.inputAgentData = inputAgentData;

        // Set up logger level
        setUpLogger(inputData);

        // Check input formatting
        checkInputFormat(inputAgentData, inputData);

        // Unpack input data
        unpackInput(inputAgentData);
    }

    @Override
    protected void activate() {
        // Request Role
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK);
        getLogger().config("Got assigned to " + getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP) + " role");

        // Initite local results
        this.localResults = new IterationResults(this);
    }

    @SuppressWarnings("unused")
    public void phaseOne(){

    }

    @SuppressWarnings("unused")
    public void phaseTwo(){

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
        else if(inputAgentData.get("Mass") == null){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " mass not contained in input file.");
        }
        else if(worldType.equals("3D_Grid") || worldType.equals("2D_Grid")) {
            if (inputAgentData.get("Speed") == null) {
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " speed not contained in input file.");
            }
            else if(inputAgentData.get("Velocity") != null){
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + "'s velocity does not match world type selected.");
            }
        }
        else if(inputAgentData.get("Velocity") == null){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " velocity not contained in input file.");
        }
    }

    private void unpackInput(JSONObject inputAgentData) {
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

        int x = 1;
    }
}
