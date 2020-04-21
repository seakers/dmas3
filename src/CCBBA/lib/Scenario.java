package CCBBA.lib;

import madkit.action.SchedulingAction;
import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.kernel.Watcher;
import madkit.message.SchedulingMessage;
import madkit.simulation.probe.PropertyProbe;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Scenario extends Watcher {
    /**
     * environment's description
     */
    private JSONObject simData;
    private JSONObject scenarioData;
    private JSONArray scenarioTaskData;
    private JSONObject worldData;
    private ArrayList<Task> scenarioTasks = new ArrayList<>();
    private ArrayList<Subtask> scenarioSubtasks = new ArrayList<>();
    private Level loggerLevel;
    private String worldType;
    private double t_0;
    private double del_t;
    private double GVT;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;

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
        this.simData = inputData;

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
            this.worldType = this.worldData.get("Type").toString();
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
            else if( worldType.toString().equals("3D_Earth") ){
                // set up Orekit 3D world tasks
                File orekitData = new File("./src/orekit-data");
                DataProvidersManager manager = DataProvidersManager.getInstance();
                try {
                    manager.addProvider(new DirectoryCrawler(orekitData));
                } catch (OrekitException e) {
                    e.printStackTrace();
                }
                // set up start and end dates
                JSONObject startDateData = (JSONObject) worldData.get("StartDate");
                JSONObject endDateData = (JSONObject) worldData.get("EndDate");

                if(startDateData == null){
                    throw new Exception("INPUT ERROR: start-date not included in input file.");
                }
                else if(endDateData == null){
                    throw new Exception("INPUT ERROR: end-date not included in input file.");
                }

                setStartDate(startDateData);
                setEndDate(endDateData);

                getLogger().config("3D Earth world configured");
            }
            else{
                throw new Exception("INPUT ERROR: World type not supported.");
            }

            // Start time
            if(startDate == null) {
                this.t_0 = (double) this.worldData.get("t_0");
            }
            else{
                this.t_0 = 0.0;
            }
            this.GVT = this.t_0;
            this.del_t = (double) simData.get("TimeStep");

            // Create tasks
            // 1-Unpack task list
            this.scenarioTaskData = (JSONArray) scenarioData.get("TaskList");

            int instances = 0;
            int totalIntances = 0;
            // 2-Create the required instances of each task on the list
            for(int i = 0; i < scenarioTaskData.size(); i++){
                JSONObject taskData = (JSONObject) scenarioTaskData.get(i);
                instances = Integer.parseInt( taskData.get("Instances").toString() );


                for(int j = 0; j < instances; j++){
                    Task newTask = new Task( taskData, this.worldData, j);
                    scenarioTasks.add( newTask );
                    getLogger().config("Task created\n" + scenarioTasks.get( totalIntances + j).toString());
                }
                totalIntances += instances - 1;
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
        addProbe(new Scenario.AgentsProbe(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCH_ROLE, "environment"));
    }

    private void updateTime() throws Exception{
        List<AgentAddress> agentsThinking1 = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
        List<AgentAddress> agentsThinking2 = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2);
        List<AgentAddress> agentsEnvironment = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_EXIST);

        if ( this.worldType.equals("3D_Earth") ) {
            if (isMessageBoxEmpty()) {
                if (agentsThinking1 == null && agentsThinking2 == null && agentsEnvironment != null) {
                    // if no agents are thinking or creating a plan, advance time
                    this.GVT += this.del_t;
                } else {
                    // if there is at least one agent thinking, freeze time
                    this.GVT += 0.0;
                }
            } else {
                // checks for messages sent from agents stuck in
                List<Message> receivedMessages = nextMessages(null);
                List<AgentAddress> messageAddress = new ArrayList<>();
                for (int i = 0; i < receivedMessages.size(); i++) {
                    AgentAddress address = receivedMessages.get(i).getSender();
                    if (!messageAddress.contains(address)) {
                        messageAddress.add(address);
                    }
                }

                if(agentsEnvironment != null && messageAddress.size() >= agentsEnvironment.size()){
                    // if all agents are out of filed of view from each other, then advance one time-step
                    this.GVT += this.del_t;
                }
                else{
                    // if there is at least one agent in field of view of another, freeze time
                    this.GVT += 0.0;
                }
            }
        }
        else if(this.worldType.equals("3D_World") || this.worldType.equals("2D_Grid")){
            if (agentsThinking1 == null && agentsThinking2 == null && agentsEnvironment != null) {
                // if no agents are thinking or creating a plan, advance time
                this.GVT += this.del_t;
            } else {
                // if there is at least one agent thinking, freeze time
                this.GVT += 0.0;
            }
        }
        else{
            throw new Exception("ERROR world type not supported");
        }

        if( Math.abs( this.GVT - endDate.durationFrom(startDate) ) <= 1e-3 ){
            // End time reached, terminate sim
            SchedulingMessage terminate = new SchedulingMessage(SchedulingAction.SHUTDOWN);
            sendMessage(getAgentWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCH_ROLE), terminate);
        }
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

    private void setStartDate(JSONObject startDateData) throws OrekitException {
        TimeScale utc = TimeScalesFactory.getUTC();
        int YY = Integer.parseInt( startDateData.get("Year").toString() );
        int MM = Integer.parseInt( startDateData.get("Month").toString() );
        int DD = Integer.parseInt( startDateData.get("Day").toString() );
        int hh = Integer.parseInt( startDateData.get("Hour").toString() );
        int mm = Integer.parseInt( startDateData.get("Minute").toString() );
        double ss = (double) startDateData.get("Second");

        startDate = new AbsoluteDate(YY, MM, DD, hh, mm, ss, utc);
    }

    private void setEndDate(JSONObject endDateData) throws OrekitException {
        TimeScale utc = TimeScalesFactory.getUTC();
        int YY = Integer.parseInt( endDateData.get("Year").toString() );
        int MM = Integer.parseInt( endDateData.get("Month").toString() );
        int DD = Integer.parseInt( endDateData.get("Day").toString() );
        int hh = Integer.parseInt( endDateData.get("Hour").toString() );
        int mm = Integer.parseInt( endDateData.get("Minute").toString() );
        double ss = (double) endDateData.get("Second");

        endDate = new AbsoluteDate(YY, MM, DD, hh, mm, ss, utc);
    }

    /**
     * Getters and Setters
     */
    public ArrayList<Task> getScenarioTasks(){ return this.scenarioTasks; }
    public ArrayList<Subtask> getScenarioSubtasks(){ return  this.scenarioSubtasks; }
    public double getT_0(){ return this.t_0; }
    public double getGVT(){ return this.GVT; }
    public String getWorldType(){ return this.worldType; }
    public AbsoluteDate getStartDate(){ return this.startDate; }
    public AbsoluteDate getEndDate(){ return this.endDate; }
}


