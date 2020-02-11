package CCBBA.lib;

import CCBBA.CCBBASimulation;
import CCBBA.bin.myMessage;
import jmetal.encodings.variable.Int;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.orekit.frames.ITRFVersion;

import java.util.ArrayList;
import java.util.List;
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
    private IterationResults localResults;                  // list of local results
    private ArrayList<Task> worldTasks;                     // list of tasks in world environment
    private ArrayList<Subtask> worldSubtasks;               // list of subtasks in world environment
    private int zeta;                                       // iteration counter
    private int M;                                          // planning horizon
    private int O_kq;                                       // max iterations in constraint violation
    private int w_solo;                                     // permission to bid solo on a task
    private int w_any;                                      // permission to bid on a task
    private AgentResources myResources;                     // agent resources
    private ArrayList<Subtask> bundle;                      // list of tasks in agent's plan
    private ArrayList<Subtask> overallBundle;               // list of tasks in agent's past plans
    private ArrayList<Subtask> path;                        // path taken to execute bundle
    private ArrayList<Subtask> overallPath;                 // path taken to execute past bundles
    private ArrayList<ArrayList<Double>> x_path;            // location of execution of each element in the bundle
    private ArrayList<ArrayList<Double>> overallX_path;     // location of execution of each element in previous bundles
    private ArrayList<ArrayList<SimulatedAgent>> omega;     // Coalition mate matrix of current bundle
    private ArrayList<ArrayList<SimulatedAgent>> overallOmega; // Coalition mate matrix of previous bundle
    private double t_0;                                     // start time
    private ArrayList<IterationResults> receivedResults;    // list of received results from other agents


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
        this.zeta = 0;
        this.bundle = new ArrayList<>();
        this.overallBundle = new ArrayList<>();
        this.path = new ArrayList<>();
        this.overallPath = new ArrayList<>();
        this.x_path = new ArrayList<>();
        this.overallX_path = new ArrayList<>();
        this.omega = new ArrayList<>();
        this.overallOmega = new ArrayList<>();
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
        this.localResults = new IterationResults( this );
    }

    /**
     * Planner functions
     */
    @SuppressWarnings("unused")
    public void thinkingPhaseOne() throws Exception {
        getLogger().info("Starting phase one");
        if(zeta != 0) zeta += 1;

        // check for new tasks
        getAvailableSubtasks();

        // Empty mailbox
        emptyMailbox();

        // Check for life status

        var myRoles = getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);
        boolean alive = !(myRoles.contains( SimGroups.AGENT_DIE ));

        // construct bundle
        getLogger().info("Constructing bundle...");
        while( (this.bundle.size() < this.M) && (this.localResults.checkAvailability()) && alive ){
            getLogger().fine("Calculating bids for bundle item number " + (this.bundle.size() + 1) + "...");

            // Calculate bid for every subtask
            ArrayList<SubtaskBid> bidList = this.localResults.calcBidList(this);
            Subtask j_chosen = null;

            // Choose max bid
            double currentMax = 0.0;
            int i_max = 0;
            SubtaskBid maxBid = new SubtaskBid(null);

            for(int i = 0; i < bidList.size(); i++){
                Subtask j_bid = bidList.get(i).getJ_a();
                if( j_bid == null){
                    continue;
                }

                double bidUtility = bidList.get(i).getC();
                int h = localResults.getIterationDatum(j_bid).getH();

                if( (bidUtility*h > currentMax) ){
                    currentMax = bidUtility*h;
                    i_max = i;
                    maxBid = bidList.get(i);
                    j_chosen = j_bid;
                }
            }

            // Check if bid already exists for that subtask in the bundle
            boolean bidExists = false;
            for(int i = 0; i < bundle.size(); i ++){
                if(j_chosen == bundle.get(i)){
                    localResults.getIterationDatum(j_chosen).setH(0);
                    bidExists = true;
                }
            }

            // Update results
            if(!bidExists){
                if( maxBid.getC() > 0 && localResults.getIterationDatum(j_chosen).getY() < maxBid.getC()) {
                    this.bundle.add(j_chosen);
                    this.path.add(maxBid.getI_opt(), j_chosen);
                    this.x_path.add(maxBid.getI_opt(), maxBid.getX());
                    localResults.updateResults(maxBid, this);
                    localResults.getIterationDatum(j_chosen).setH(0);
                }
            }
        }

        getLogger().info("Bundle constructed");
        StringBuilder bundleList = new StringBuilder();
        for(Subtask b : this.bundle){ bundleList.append(" " + b.getName()); }
        getLogger().fine(this.name + " bundle: " + bundleList);
        getLogger().fine(this.name + " results after bundle construction: \n" + this.localResults.toString() );

        // Broadcast my results
        ResultsMessage myResults = new ResultsMessage( this.localResults, this);
        broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1, myResults);
        broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2, myResults);
        broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO, myResults);

        // leave phase one and start phase 2
        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2);
    }

    @SuppressWarnings("unused")
    public void thinkingPhaseTwo() throws Exception {
        if( !isMessageBoxEmpty() ){ // results received
            // Save current results
            IterationResults prevResults = new IterationResults( localResults, this );

            // unpack results
            List<Message> receivedMessages = nextMessages(null);
            this.receivedResults = new ArrayList<>();
            for(int i = 0; i < receivedMessages.size(); i++){
                ResultsMessage message = (ResultsMessage) receivedMessages.get(i);
                receivedResults.add(message.getResults());
            }

            // compare results
            boolean changesMade = false;
            for(IterationResults result : this.receivedResults){
                for(int i_j = 0; i_j < result.getResults().size(); i_j++){
                    // Load received results
                    IterationDatum itsDatum = result.getIterationDatum(i_j);
                    double itsY = itsDatum.getY();
                    String itsZ;
                    if(itsDatum.getZ() == null){
                        itsZ = "";
                    }
                    else{ itsZ = itsDatum.getZ().getName(); }
                    double itsTz = itsDatum.getTz();
                    String it = result.getParentAgent().getName();
                    int itsS = itsDatum.getS();
                    boolean itsCompletion = itsDatum.getJ().getCompleteness();

                    // Load my results
                    IterationDatum myDatum = localResults.getIterationDatum( itsDatum.getJ() );
                    double myY = myDatum.getY();
                    String myZ;
                    if(myDatum.getZ() == null){
                        myZ = "";
                    }
                    else{ myZ = myDatum.getZ().getName(); }
                    double myTz = myDatum.getTz();
                    String me = this.getName();
                    int myS = myDatum.getS();
                    boolean myCompletion = myDatum.getJ().getCompleteness();

                    // Comparing bids. See Ref 40 Table 1
                    if(itsZ.equals(it)){
                        if(myZ.equals(me)){
                            if( (itsCompletion) && (itsCompletion != myCompletion) ){
                                // update
                                localResults.updateResults(itsDatum);
                            }
                            else if( itsY > myY ) {
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        }
                        else if( myZ == it){
                            // update
                            localResults.updateResults(itsDatum);
                        }
                        else if( (myZ != me)&&(myZ != it)&&(myZ != "") ){
                            if( (itsS > myS)||(itsY > myY) ){
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        }
                        else if( myZ == "" ){
                            // update
                            localResults.updateResults(itsDatum);
                        }
                    }
                    else if( itsZ == me ){
                        if( myZ == me ){
                            // leave
                            localResults.leaveResults(itsDatum);
                        }
                        else if( myZ == it){
                            // reset
                            localResults.resetResults(itsDatum);
                        }
                        else if( (myZ != me)&&(myZ != it)&&(myZ != "") ){
                            if(itsS > myS){
                                // reset
                                localResults.resetResults(itsDatum);
                            }
                        }
                        else if( myZ == "" ){
                            // leave
                            localResults.leaveResults(itsDatum);
                        }
                    }
                    else if( (itsZ != it)&&( itsZ != me)&&(itsZ != "") ){
                        if( myZ == me ){
                            if( (itsCompletion) && (itsCompletion != myCompletion) ){
                                // update
                                localResults.updateResults(itsDatum);
                            }
                            else if( (itsS > myS)&&(itsY > myY) ){
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        }
                        else if( myZ == it){
                            if( itsS > myS ){
                                //update
                                localResults.updateResults(itsDatum);
                            }
                            else{
                                // reset
                                localResults.resetResults(itsDatum);
                            }
                        }
                        else if( myZ == itsZ ){
                            if(itsS > myS){
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        }
                        else if( (myZ != me)&&(myZ != it)&&(myZ != itsZ)&&(myZ != "") ){
                            if( (itsS > myS)&&( itsY > myY ) ){
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        }
                        else if( myZ == "" ){
                            // leave
                            localResults.leaveResults(itsDatum);
                        }
                    }
                    else if( itsZ == "") {
                        if (myZ == me) {
                            // leave
                            localResults.leaveResults(itsDatum);
                        } else if (myZ == it) {
                            // update
                            localResults.updateResults(itsDatum);
                        } else if ((myZ != me) && (myZ != it) && (myZ != "")) {
                            if (itsS > myS) {
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        } else if (myZ == "") {
                            // leave
                            localResults.leaveResults(itsDatum);
                        }
                    }
                }
            }

            getLogger().info("Results compared");
            getLogger().fine(this.name + " results after comparison: \n" + this.localResults.toString() );

            int x = 1;

//            // constrain checks
//            for(Subtask j : localResults.getBundle()){
//                // create list of new coalition members
//                Vector<Vector<AbstractSimulatedAgent>> newOmega = getNewCoalitionMemebers(j);
//                Vector<Vector<AbstractSimulatedAgent>> oldOmega = this.localResults.getOmega();
//
//                if (!mutexSat(j) || !timeSat(j) || !depSat(j) || !coalitionSat(j, oldOmega, newOmega) ){
//                    // subtask does not satisfy all constraints, release task
//                    int i_j =  this.localResults.getJ().indexOf(j);
//                    localResults.resetResults(i_j);
//                    break;
//                }
//            }
//            this.zeta++;
//
//            if(checkForChanges(prevResults)){
//                // changes were made, reconsider bids
//                requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);
//                this.convCounter = 0;
//            }
//            else{
//                // no changes were made, check convergence
//                this.convCounter++;
//                if(convCounter >= convIndicator){
//                    // convergence reached
//                    this.convCounter = 0;
//                    requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DO);
//                }
//                else {
//                    requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);
//                }
//            }
//
//            // empty recieved results and exit phase 2
//            receivedResults = new Vector<>();
//            leaveRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK2);
        }
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
        else if( inputAgentData.get("MaxBidsSolo") == null ){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " max number of solo bids not contained in input file.");
        }
        else if( inputAgentData.get("MaxBidsAny") == null ){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " max number of any bids not contained in input file.");
        }
        else if(inputAgentData.get("Resources") == null){
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " resource information not contained in input file.");
        }

    }

    private void unpackInput(JSONObject inputAgentData) throws Exception{
        getLogger().config("Configuring agent...");

        // -Name
        this.name = inputAgentData.get("Name").toString();

        // -Sensor List
        this.sensorList = new ArrayList<>();
        JSONArray sensorListData = (JSONArray) inputAgentData.get("SensorList");
        for (Object sensorListDatum : sensorListData) {
            this.sensorList.add(sensorListDatum.toString());
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
        this.w_solo = Integer.parseInt( inputAgentData.get("MaxBidsSolo").toString() );
        this.w_any = Integer.parseInt( inputAgentData.get("MaxBidsAny").toString() );

        // -Resources
        this.myResources = new AgentResources((JSONObject) inputAgentData.get("Resources"));
    }

    private void getAvailableSubtasks(){
        this.worldTasks = this.environment.getScenarioTasks();
        this.worldSubtasks = this.environment.getScenarioSubtasks();

        if(this.worldSubtasks.size() > this.localResults.size()){
            // if new tasks have been added, create new results for them
            for(Subtask j : this.worldSubtasks){
                if(!this.localResults.contains(j)){
                    this.localResults.addResult(j, this);
                }
            }
        }
    }

    private void emptyMailbox(){
        while(!isMessageBoxEmpty()){
            Message tempMessage = nextMessage();
        }
    }

    /**
     * Getters and Setters
     */
    public ArrayList<String> getSensorList(){ return this.sensorList; }
    public ArrayList<Subtask> getBundle(){ return this.bundle; }
    public ArrayList<Subtask> getOverallBundle(){ return this.overallBundle; }
    public ArrayList<Subtask> getPath(){ return this.path; }
    public ArrayList<Subtask> getOverallPath(){ return this.overallPath; }
    public ArrayList<ArrayList<Double>> getX_path(){ return this.x_path; }
    public ArrayList<ArrayList<Double>> getOverallX_path(){ return this.overallX_path; }
    public int getMaxItersInViolation(){ return this.O_kq; }
    public ArrayList<Subtask> getWorldSubtasks(){ return this.worldSubtasks; }
    public int getW_solo(){ return this.w_solo; }
    public int getW_any(){ return this.w_any; }
    public IterationResults getLocalResults(){ return this.localResults; }
    public ArrayList<Double> getPosition(){ return this.position; }
    public double getT_0(){ return this.t_0; }
    public Scenario getEnvironment(){ return this.environment; }
    public double getSpeed(){ return this.speed; }
    public AgentResources getResources(){return this.myResources; }
    public ArrayList<ArrayList<SimulatedAgent>> getOmega(){ return this.omega; }
    public ArrayList<ArrayList<SimulatedAgent>> getOverallOmega(){ return this.overallOmega; }
    public int getIteration(){ return this.zeta; }
}
