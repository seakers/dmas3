package CCBBA.lib;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Watcher;
import madkit.simulation.probe.PropertyProbe;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.logging.Level;

public class Scenario extends Watcher {
    /**
     * environment's description
     */
    private JSONObject scenarioData;
    private JSONArray scenarioTaskData;
    private JSONObject worldData;
    private ArrayList<Task> scenarioTasks = new ArrayList<>();
    private ArrayList<Subtask> scenarioSubtasks = new ArrayList<>();
    private Level loggerLevel;
    private double t_0;

    // 2D or 3D grid world
    private ArrayList<Double> bounds = new ArrayList<>();


    /**
     * constructor
     * @param inputData - data from input data
     * @throws Exception - detect any errors in input data
     */
    public Scenario(JSONObject inputData) throws Exception {
        // Load Scenario data
        this.scenarioData = (JSONObject) inputData.get("Scenario");

        // Set up logger level
        setUpLogger(inputData);
    }

    @Override
    protected void activate() {
        try {
            // Set up world
            // 1-Unpack world data
            this.worldData = (JSONObject) scenarioData.get("World");

            // 2-Check what type of world it is
            String worldType = this.worldData.get("Type").toString();
            if( worldType.equals("2D_Grid") ){
                // set up 2D world
                JSONArray boundsData = (JSONArray) this.worldData.get("Bounds");
                if(boundsData.size() != 2){
                    throw new Exception("INPUT ERROR: World bounds do not match world type");
                }
                double x_max = (double) boundsData.get(0);
                double y_max = (double) boundsData.get(1);

                bounds.add(x_max);
                bounds.add(y_max);
                getLogger().config("2D grid world configured with bounds (" + x_max + ", " + y_max + ")");
            }
            else if( worldType.toString().equals("3D_Grid") ){
                // set up 3D world
                JSONArray boundsData = (JSONArray) this.worldData.get("Bounds");
                if(boundsData.size() != 3){
                    throw new Exception("INPUT ERROR: World bounds do not match world type");
                }
                double x_max = (double) boundsData.get(0);
                double y_max = (double) boundsData.get(1);
                double z_max = (double) boundsData.get(2);

                bounds.add(x_max);
                bounds.add(y_max);
                bounds.add(z_max);
                getLogger().config("3D grid world configured with bounds (" + x_max + ", " + y_max + ", " + z_max +")");
            }
//            else if( worldType.toString().equals("3D_Earth") ){
//                // set up Orekit 3D world
//            }
            else{
                throw new Exception("INPUT ERROR: World type not supported.");
            }

            // Start time
            this.t_0 = (double) this.worldData.get("t_0");

            // Create tasks
            // 1-Unpack task list
            this.scenarioTaskData = (JSONArray) scenarioData.get("TaskList");

            // 2-Create the required instances of each task on the list
            for(int i = 0; i < scenarioTaskData.size(); i++){
                JSONObject taskData = (JSONObject) scenarioTaskData.get(i);
                int instances = Integer.parseInt( taskData.get("Instances").toString() );

                for(int j = 0; j < instances; j++){
                    scenarioTasks.add( new Task((JSONObject) scenarioTaskData.get(i), this.worldData) );
                    getLogger().config("Task created\n" + scenarioTasks.get(i).toString());
                }
            }

            for(Task J : this.scenarioTasks){
                this.scenarioSubtasks.addAll( J.getSubtaskList() );
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        // 1 : request my role so that the viewer can probe me
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.ENV_ROLE);
        getLogger().config("Assigned to " + getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP) + " role");

        // 2 : this probe is used to initialize the agents' environment field
        addProbe(new Scenario.AgentsProbe(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1, "environment"));
        addProbe(new Scenario.AgentsProbe(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2, "environment"));
        addProbe(new Scenario.AgentsProbe(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.RESULTS_ROLE, "environment"));
    }

    class AgentsProbe extends PropertyProbe<AbstractAgent, Scenario> {

        public AgentsProbe(String community, String group, String role, String fieldName) {
            super(community, group, role, fieldName);
        }

        @Override
        protected void adding(AbstractAgent agent) {
            super.adding(agent);
            setPropertyValue(agent, Scenario.this);
        }
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

    /**
     * Getters and Setters
     */
    public ArrayList<Task> getScenarioTasks(){ return this.scenarioTasks; }
    public ArrayList<Subtask> getScenarioSubtasks(){ return  this.scenarioSubtasks; }
    public double getT_0(){ return this.t_0; }
}


