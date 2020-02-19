package CCBBA.lib;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
    private ArrayList<Double> initialPosition;              // initial agent position
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
    private AgentResources initialResources;                // initial agent resources
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
    private int convCounter;                                // convergence counter
    private int convIndicator;                              // convergence indicator


    public SimulatedAgent(JSONObject inputAgentData, JSONObject inputData) throws Exception {
        // Set up logger level
        setUpLogger(inputData);

        // Check input formatting
        checkInputFormat(inputAgentData, inputData);

        // Unpack input data
        JSONObject worldData = (JSONObject) ((JSONObject) inputData.get("Scenario")).get("World");
        unpackInput(inputAgentData, worldData);
    }

    @Override
    protected void activate() {
        getLogger().info("Initiating agent");

        // Request Role
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_EXIST);
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
        for (int i = 0; i < M; i++) {
            omega.add(new ArrayList<>());
        }
        this.overallOmega = new ArrayList<>();
        this.t_0 = this.environment.getT_0();
        this.convCounter = 0;

        // Get world subtasks
        this.worldTasks = new ArrayList<>();
        this.worldSubtasks = new ArrayList<>();
        for (Task J : this.environment.getScenarioTasks()) {
            this.worldTasks.add(J);
            this.worldSubtasks.addAll(J.getSubtaskList());
        }
        getLogger().info(this.worldTasks.size() + " Tasks found in world");
        getLogger().info(this.worldSubtasks.size() + " Subtasks found in world");

        // Initiate iteration results
        this.localResults = new IterationResults(this);
    }

    /**
     * Planner functions
     */
    @SuppressWarnings("unused")
    public void thinkingPhaseOne() throws Exception {
        getLogger().info("Starting phase one");

        if (zeta != 0) zeta += 1;

        // check for new tasks
        getAvailableSubtasks();

        // Check for life status
        var myRoles = getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);
        boolean alive = !(myRoles.contains(SimGroups.AGENT_DIE));

        // Check for new Coalition members
        getNewCoalitionMemebers();

        // Reset task availability indicators
        this.localResults.resetAvailability();
        getLogger().fine(this.name + " results before bundle construction: \n" + this.localResults.toString());

        // construct bundle
        getLogger().info("Constructing bundle...");
        while ((this.bundle.size() < this.M) && (this.localResults.checkAvailability()) && alive) {
            getLogger().fine("Calculating bids for bundle item number " + (this.bundle.size() + 1) + "...");

            // Calculate bid for every subtask
            ArrayList<SubtaskBid> bidList = this.localResults.calcBidList(this);
            Subtask j_chosen = null;

            // Choose max bid
            double currentMax = 0.0;
            int i_max = 0;
            SubtaskBid maxBid = new SubtaskBid(null);

            for (int i = 0; i < bidList.size(); i++) {
                Subtask j_bid = bidList.get(i).getJ_a();
                if (j_bid == null) {
                    continue;
                }

                double bidUtility = bidList.get(i).getC();
                int h = localResults.getIterationDatum(j_bid).getH();

                if ((bidUtility * h > currentMax)) {
                    currentMax = bidUtility * h;
                    i_max = i;
                    maxBid = bidList.get(i);
                    j_chosen = j_bid;
                }
            }

            // Check if bid already exists for that subtask in the bundle
            boolean bidExists = false;
            for (int i = 0; i < bundle.size(); i++) {
                if (j_chosen == bundle.get(i)) {
                    localResults.getIterationDatum(j_chosen).setH(0);
                    bidExists = true;
                }
            }

            // Update results
            if (!bidExists) {
                if (maxBid.getC() > 0 && localResults.getIterationDatum(j_chosen).getY() < maxBid.getC()) {
                    this.bundle.add(j_chosen);
                    this.path.add(maxBid.getI_opt(), j_chosen);
                    this.x_path.add(maxBid.getI_opt(), maxBid.getX());
                    localResults.updateResults(maxBid, this);
                    localResults.getIterationDatum(j_chosen).setH(0);
                }
            }
        }

        getLogger().info("Bundle constructed");
        logBundle();
        logPath();
        getLogger().fine(this.name + " results after bundle construction: \n" + this.localResults.toString());

        // Broadcast my results
        ResultsMessage myResults = new ResultsMessage(this.localResults, this);
        broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1, myResults);
        broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2, myResults);
        broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO, myResults);

        // leave phase one and start phase 2
        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2);

        // Check for new Coalition members
        getNewCoalitionMemebers();
    }

    @SuppressWarnings("unused")
    public void thinkingPhaseTwo() throws Exception {
        getLogger().info("Starting phase two");
        if (!isMessageBoxEmpty()) { // results received
            // Save current results
            IterationResults prevResults = new IterationResults(localResults, this);

            // unpack results
            List<Message> receivedMessages = nextMessages(null);
            this.receivedResults = new ArrayList<>();
            for (int i = 0; i < receivedMessages.size(); i++) {
                ResultsMessage message = (ResultsMessage) receivedMessages.get(i);
                receivedResults.add(message.getResults());
            }

            // compare results
            boolean changesMade = false;
            for (IterationResults result : this.receivedResults) {
                for (int i_j = 0; i_j < result.getResults().size(); i_j++) {
                    // Load received results
                    IterationDatum itsDatum = result.getIterationDatum(i_j);
                    double itsY = itsDatum.getY();
                    String itsZ;
                    if (itsDatum.getZ() == null) {
                        itsZ = "";
                    } else {
                        itsZ = itsDatum.getZ().getName();
                    }
                    double itsTz = itsDatum.getTz();
                    String it = result.getParentAgent().getName();
                    int itsS = itsDatum.getS();
                    boolean itsCompletion = itsDatum.getJ().getCompleteness();

                    // Load my results
                    IterationDatum myDatum = localResults.getIterationDatum(itsDatum.getJ());
                    double myY = myDatum.getY();
                    String myZ;
                    if (myDatum.getZ() == null) {
                        myZ = "";
                    } else {
                        myZ = myDatum.getZ().getName();
                    }
                    double myTz = myDatum.getTz();
                    String me = this.getName();
                    int myS = myDatum.getS();
                    boolean myCompletion = myDatum.getJ().getCompleteness();

                    // Comparing bids. See Ref 40 Table 1
                    if (itsZ.equals(it)) {
                        if (myZ.equals(me)) {
                            if ((itsCompletion) && (itsCompletion != myCompletion)) {
                                // update
                                localResults.updateResults(itsDatum);
                            } else if (itsY > myY) {
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        } else if (myZ == it) {
                            // update
                            localResults.updateResults(itsDatum);
                        } else if ((myZ != me) && (myZ != it) && (myZ != "")) {
                            if ((itsS > myS) || (itsY > myY)) {
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        } else if (myZ == "") {
                            // update
                            localResults.updateResults(itsDatum);
                        }
                    } else if (itsZ == me) {
                        if (myZ == me) {
                            // leave
                            localResults.leaveResults(itsDatum);
                        } else if (myZ == it) {
                            // reset
                            localResults.resetResults(itsDatum);
                        } else if ((myZ != me) && (myZ != it) && (myZ != "")) {
                            if (itsS > myS) {
                                // reset
                                localResults.resetResults(itsDatum);
                            }
                        } else if (myZ == "") {
                            // leave
                            localResults.leaveResults(itsDatum);
                        }
                    } else if ((itsZ != it) && (itsZ != me) && (itsZ != "")) {
                        if (myZ == me) {
                            if ((itsCompletion) && (itsCompletion != myCompletion)) {
                                // update
                                localResults.updateResults(itsDatum);
                            } else if ((itsS > myS) && (itsY > myY)) {
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        } else if (myZ == it) {
                            if (itsS > myS) {
                                //update
                                localResults.updateResults(itsDatum);
                            } else {
                                // reset
                                localResults.resetResults(itsDatum);
                            }
                        } else if (myZ == itsZ) {
                            if (itsS > myS) {
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        } else if ((myZ != me) && (myZ != it) && (myZ != itsZ) && (myZ != "")) {
                            if ((itsS > myS) && (itsY > myY)) {
                                // update
                                localResults.updateResults(itsDatum);
                            }
                        } else if (myZ == "") {
                            // leave
                            localResults.leaveResults(itsDatum);
                        }
                    } else if (itsZ == "") {
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
            logBundle();
            logPath();
            getLogger().fine(this.name + " results after comparison: \n" + this.localResults.toString());


            // constrain checks
            getLogger().info("Checking constraints...");
            for (Subtask j : this.bundle) {
                // create list of new coalition members
                ArrayList<ArrayList<SimulatedAgent>> newOmega = getNewCoalitionMemebers(j);
                ArrayList<ArrayList<SimulatedAgent>> oldOmega = this.omega;

                boolean mutexSat = mutexSat(j);
                boolean timeSat = timeSat(j);
                boolean depSat = depSat(j);
                boolean coalitionSat = coalitionSat(j, oldOmega, newOmega);

                if (!mutexSat || !timeSat || !depSat || !coalitionSat) {
                    // subtask does not satisfy all constraints, release task
                    localResults.resetResults(localResults.getIterationDatum(j));
                    String constraintsFailed = this.name + " FAILED ";

                    if (!mutexSat) {
                        constraintsFailed += "mutexSat, ";
                    }
                    if (!timeSat) {
                        constraintsFailed += "timeSat, ";
                    }
                    if (!depSat) {
                        constraintsFailed += "depSat, ";
                    }
                    if (!coalitionSat) {
                        constraintsFailed += "coalitionSat ";
                    }

                    constraintsFailed += "on subtask " + j.getName() + " #" + localResults.getIndexOf(j);
                    getLogger().fine(constraintsFailed);
                    break;
                } else {
                    getLogger().fine("Constraint check for bunde task #" + (bundle.indexOf(j) + 1) + " passed");
                }
            }
            this.zeta++;

            logBundle();
            logPath();
            getLogger().fine(this.name + " results after constraint check: \n" + this.localResults.toString());

            getLogger().info("Checking for changes");
            int i_dif = checkForChanges(prevResults);
            if (i_dif >= 0) {
                // changes were made, reconsider bids
                getLogger().fine("Changes were made. Reconsidering bids");
                getLogger().fine(this.localResults.comparisonToString(i_dif, prevResults));
                requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
                this.convCounter = 0;
            } else {
                // no changes were made, check convergence
                getLogger().fine("No changes were made. Checking convergence");
                this.convCounter++;

                if (convCounter < convIndicator) {
                    getLogger().fine("Convergence status: " + convCounter + "/" + convIndicator);
                    requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);

                } else {
                    // convergence reached
                    getLogger().config("Convergence reached. Plan determined!");
                    this.convCounter = 0;
                    requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO);

                    // Broadcast my results
                    ResultsMessage myResults = new ResultsMessage(this.localResults, this);
                    broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1, myResults);
                    broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2, myResults);
                    broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO, myResults);
                }
            }

            // empty received results and exit phase 2
            receivedResults = new ArrayList<>();
            leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2);
        } else {
            getLogger().info("No messages received. Waiting on other agents");
        }
        int x = 1;
    }

    @SuppressWarnings("unused")
    public void doingPhase() throws Exception {
        // check life status
        var myRoles = getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);
        boolean alive = !(myRoles.contains(SimGroups.AGENT_DIE));

        if(alive) {
            getLogger().info("Executing plan");
            // do all tasks in path
            int i_done = -1;
            for (int i = 0; i < this.path.size(); i++) {
                Subtask j = path.get(i);
                ArrayList<Double> x = x_path.get(i);

                completeTask(j, x);
                getLogger().fine("Performing task: " + j.getName() + " #" + (this.localResults.indexOf(j)+1) );

                myRoles = getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);
                if (myRoles.contains(SimGroups.AGENT_DIE)) {
                    alive = false;
                    break;
                }
                i_done = i;
            }

            if (i_done > -1) {
                // save to overall bundle, path, and omega
                if (i_done == bundle.size() - 1) {
                    getLogger().info("All tasks in bundle completed");
                } else {
                    getLogger().info("Not all tasks in bundle completed");
                }
                logRemainingBundle(i_done);

                getLogger().fine("Saving tasks to overall bundle and path");
                for (int i = 0; i < i_done + 1; i++) {
                    this.overallOmega.add(omega.get(i));
                    this.overallX_path.add(x_path.get(i));
                    this.overallPath.add(path.get(i));
                    this.overallBundle.add(bundle.get(i));
                }

                // release tasks from path and bundle
                getLogger().fine("Releasing all tasks from bundle");
                for (int i = i_done; i < this.path.size()-1; i++) {
                    // resetting results for incomplete tasks
                    localResults.resetResults(localResults.getIterationDatum(this.path.get(i)));
                }
                this.x_path = new ArrayList<>();
                this.path = new ArrayList<>();
                this.bundle = new ArrayList<>();
                this.omega = new ArrayList<>();
                for (int i = 0; i < M; i++) {
                    omega.add(new ArrayList<>());
                }
            } else {
                getLogger().fine(this.name + " did not perform any task");
            }

            logBundle();
            logPath();
            leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO);

            // check for remaining tasks
            getLogger().fine("Checking for remaining tasks...");
            boolean tasksAvailable = tasksAvailable();
            if (checkResources()) {
                if (tasksAvailable) {
                    // tasks are remaining and the agent is alive
                    getLogger().info("Resources still available. Creating new plan!");
                } else {
                    // no tasks are remaining
                    getLogger().config("No more tasks available. Killing agent");
                    requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE);
                }
            } else {
                // no sufficient resources left in agent
                getLogger().config("No more resources available. Killing agent");
                requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE);
            }
        }
        else{
            getLogger().info("Agent is dead and has no plan to perform");
        }
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
    }

    @SuppressWarnings("unused")
    protected void dying() throws Exception { // send results to results compiler
        List<AgentAddress> agentsDead = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE);
        List<AgentAddress> agentsEnvironment = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_EXIST);

        if ((agentsDead != null) && (agentsDead.size() == agentsEnvironment.size())) {
            getLogger().info("Sending results to results compiler");
            AgentAddress resultsAddress = getAgentWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.RESULTS_ROLE);
            SimResultsMessage myDeath = new SimResultsMessage(this);
            sendMessage(resultsAddress, myDeath);
//            // All agents have declared themselves as dead. Checking for consistent results
//            leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
//            leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
//
//            // send current results to other agents
//            AgentAddress agentsAddress = getAgentWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE);
//            ResultsMessage myResults = new ResultsMessage(this.localResults, this);
//            sendMessage(agentsAddress, myResults);
//
//            if(!isMessageBoxEmpty()){
//                // unpack results
//                List<Message> receivedMessages = nextMessages(null);
//                this.receivedResults = new ArrayList<>();
//                for (Message receivedMessage : receivedMessages) {
//                    ResultsMessage message = (ResultsMessage) receivedMessage;
//                    receivedResults.add(message.getResults());
//                }
//
//                int consistencyCounter = 0;
//                for(IterationResults receivedResult : receivedResults){
//                    int i_dif = checkForChanges(receivedResult);
//                    if (i_dif >= 0) {
//                        // changes were made, reconsider bids
//                        getLogger().fine("Changes were made");
//                        getLogger().fine(this.localResults.comparisonToString(i_dif, receivedResult));
//                        break;
//                    }
//                    else{
//                        // results were consistent with received result, check the rest
//                        consistencyCounter++;
//                    }
//                }
//                if(consistencyCounter == receivedResults.size()) {
//                    // all results are consistent, send to compiler
//                    getLogger().info("Sending results to results compiler");
//                    AgentAddress resultsAddress = getAgentWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.RESULTS_ROLE);
//                    SimResultsMessage myDeath = new SimResultsMessage(this);
//                    sendMessage(resultsAddress, myDeath);
//                }
//                else{
////                    requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1);
//                }
//            }
        }
    }

    /**
     * Misc and helping functions
     */

    private void setUpLogger(JSONObject inputData) throws Exception {
        if (inputData.get("Logger").toString().equals("OFF")) {
            this.loggerLevel = Level.OFF;
        } else if (inputData.get("Logger").toString().equals("SEVERE")) {
            this.loggerLevel = Level.SEVERE;
        } else if (inputData.get("Logger").toString().equals("WARNING")) {
            this.loggerLevel = Level.WARNING;
        } else if (inputData.get("Logger").toString().equals("INFO")) {
            this.loggerLevel = Level.INFO;
        } else if (inputData.get("Logger").toString().equals("CONFIG")) {
            this.loggerLevel = Level.CONFIG;
        } else if (inputData.get("Logger").toString().equals("FINE")) {
            this.loggerLevel = Level.FINE;
        } else if (inputData.get("Logger").toString().equals("FINER")) {
            this.loggerLevel = Level.FINER;
        } else if (inputData.get("Logger").toString().equals("FINEST")) {
            this.loggerLevel = Level.FINEST;
        } else if (inputData.get("Logger").toString().equals("ALL")) {
            this.loggerLevel = Level.ALL;
        } else {
            throw new Exception("INPUT ERROR: Logger type not supported");
        }

        getLogger().setLevel(this.loggerLevel);
    }

    private void checkInputFormat(JSONObject inputAgentData, JSONObject inputData) throws Exception {
        String worldType = ((JSONObject) ((JSONObject) inputData.get("Scenario")).get("World")).get("Type").toString();

        if (inputAgentData.get("Name") == null) {
            throw new NullPointerException("INPUT ERROR: Agent name not contained in input file");
        } else if (inputAgentData.get("SensorList") == null) {
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " sensor list not contained in input file.");
        } else if (inputAgentData.get("Position") == null) {
            if (inputAgentData.get("Orbital Parameters") == null) {
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " starting position or orbit not contained in input file.");
            } else {
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " starting position not contained in input file.");
            }
        } else if (worldType.equals("3D_Grid") || worldType.equals("2D_Grid")) {
            if (inputAgentData.get("Speed") == null) {
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " speed not contained in input file.");
            } else if (inputAgentData.get("Velocity") != null) {
                throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + "'s velocity does not match world type selected.");
            }
        }

        if (inputAgentData.get("Mass") == null) {
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " mass not contained in input file.");
        } else if (inputAgentData.get("PlanningHorizon") == null) {
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " planning horizon not contained in input file.");
        } else if (inputAgentData.get("MaxConstraintViolations") == null) {
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " max number of constraint violation not contained in input file.");
        } else if (inputAgentData.get("MaxBidsSolo") == null) {
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " max number of solo bids not contained in input file.");
        } else if (inputAgentData.get("MaxBidsAny") == null) {
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " max number of any bids not contained in input file.");
        } else if (inputAgentData.get("Resources") == null) {
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " resource information not contained in input file.");
        } else if (inputAgentData.get("ConvergenceIndicator") == null) {
            throw new NullPointerException("INPUT ERROR: " + inputAgentData.get("Name").toString() + " convergence indicator information not contained in input file.");
        }

    }

    private void unpackInput(JSONObject inputAgentData, JSONObject worldData) throws Exception {
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
        this.initialPosition = new ArrayList<>();
        if(inputAgentData.get("Position").getClass().equals(inputAgentData.get("Resources").getClass())){
            // random position
            JSONObject positionData = (JSONObject) inputAgentData.get("Position");
            String worldType = worldData.get("Type").toString();

            if(positionData.get("Dist").toString().equals("Linear")){
                // ---Linear distribution
                if( worldType.equals("2D_Grid") ){
                    // ---Task is in a 2D grid world
                    JSONArray boundsData = (JSONArray) worldData.get("Bounds");
                    double x_max = (double) boundsData.get(0);
                    double y_max = (double) boundsData.get(1);

                    double x = x_max * Math.random();
                    double y = y_max * Math.random();

                    this.position.add(x);
                    this.position.add(y);
                    this.position.add(0.0);
                    this.initialPosition.add(x);
                    this.initialPosition.add(y);
                    this.initialPosition.add(0.0);
                }
                else if(worldType.equals("3D_Grid")){
                    // ---Task is in a 3D grid world
                    JSONArray boundsData = (JSONArray) worldData.get("Bounds");
                    double x_max = (double) boundsData.get(0);
                    double y_max = (double) boundsData.get(1);
                    double z_max = (double) boundsData.get(2);

                    double x = x_max * Math.random();
                    double y = y_max * Math.random();
                    double z = z_max * Math.random();

                    this.position.add(x);
                    this.position.add(y);
                    this.position.add(z);
                    this.initialPosition.add(x);
                    this.initialPosition.add(y);
                    this.initialPosition.add(z);
                }
//                else if(worldType.equals("3D_Earth")){
//                    // ---Task is on earth's surface
//                    // IMPLEMENTATION NEEDED
//                }
                else{
                    throw new Exception("INPUT ERROR:" + this.name + " world not supported.");
                }

            }
//            else if(locationDist.equals("Normal")){
//                // ---Normal distribution
//                // NEEDS IMPLEMENTATION
//            }
            else{
                throw new Exception("INPUT ERROR:" + this.name + " location distribution not supported.");
            }
        }
        else{
            // predetermined position
            JSONArray positionData = (JSONArray) inputAgentData.get("Position");
            for (Object positionDatum : positionData) {
                this.position.add((double) positionDatum);
                this.initialPosition.add((double) positionDatum);
            }
        }



        // -Speed or Velocity
        if (inputAgentData.get("Speed") != null) {
            this.speed = (double) inputAgentData.get("Speed");
        } else if (inputAgentData.get("Velocity") != null) {
            this.velocity = new ArrayList<>();
            JSONArray velocityData = (JSONArray) inputAgentData.get("Velocity");
            for (Object velocityDatum : velocityData) {
                this.velocity.add((double) velocityDatum);
            }
        }

        // -Mass
        this.mass = (double) inputAgentData.get("Mass");

        // -Coalition Restrictions
        this.M = Integer.parseInt(inputAgentData.get("PlanningHorizon").toString());
        this.O_kq = Integer.parseInt(inputAgentData.get("MaxConstraintViolations").toString());
        this.w_solo = Integer.parseInt(inputAgentData.get("MaxBidsSolo").toString());
        this.w_any = Integer.parseInt(inputAgentData.get("MaxBidsAny").toString());

        // -Resources
        this.myResources = new AgentResources((JSONObject) inputAgentData.get("Resources"));
        this.initialResources = new AgentResources((JSONObject) inputAgentData.get("Resources"));

        // -Convergence Indicator
        this.convIndicator = Integer.parseInt(inputAgentData.get("ConvergenceIndicator").toString());
    }

    private void getAvailableSubtasks() {
        this.worldTasks = this.environment.getScenarioTasks();
        this.worldSubtasks = this.environment.getScenarioSubtasks();

        if (this.worldSubtasks.size() > this.localResults.size()) {
            // if new tasks have been added, create new results for them
            for (Subtask j : this.worldSubtasks) {
                if (!this.localResults.contains(j)) {
                    this.localResults.addResult(j, this);
                }
            }
        }
    }

    private void emptyMailbox() {
        while (!isMessageBoxEmpty()) {
            Message tempMessage = nextMessage();
        }
    }

    private void getNewCoalitionMemebers() {
        for (Subtask j_b : bundle) {
            int i_b = bundle.indexOf(j_b);
            ArrayList<ArrayList<SimulatedAgent>> newCoalitions = getNewCoalitionMemebers(j_b);
            this.omega.set(i_b, newCoalitions.get(i_b));
        }
    }

    public void releaseTaskFromBundle(IterationDatum itsDatum) throws Exception {
        if (bundle.contains(itsDatum.getJ())) {
            int counter = 0;
            for (int i_b = bundle.indexOf(itsDatum.getJ()); i_b < bundle.size(); ) {
                Subtask j_b = bundle.get(i_b);
                if(counter > 0){
                    this.localResults.resetResults(j_b);
                }
                this.omega.set(i_b, new ArrayList<>());

                // remove subtask and all subsequent ones from bundle and path
                this.x_path.remove(path.indexOf(j_b));
                this.path.remove(path.indexOf(j_b));
                this.bundle.remove(i_b);
                counter++;
            }
        }
    }

    private ArrayList<ArrayList<SimulatedAgent>> getNewCoalitionMemebers(Subtask j) {
        ArrayList<ArrayList<SimulatedAgent>> newOmega = new ArrayList<>();
        for (int i = 0; i < this.M; i++) {
            ArrayList<SimulatedAgent> tempCoal = new ArrayList<>();

            if (this.bundle.size() >= i + 1) {
                for (int i_o = 0; i_o < this.localResults.size(); i_o++) {
                    if ((this.localResults.getIterationDatum(i_o).getZ() != this)             // if winner at i_o is not me
                            && (this.localResults.getIterationDatum(i_o).getZ() != null)      // and if winner at i_o is not empty
                            && (this.bundle.get(i).getParentTask() == localResults.getIterationDatum(i_o).getJ().getParentTask())) // and subtasks share a task
                    {
                        // then winner at i_o is a coalition partner
                        tempCoal.add(this.localResults.getIterationDatum(i_o).getZ());
                    }
                }
            }

            newOmega.add(tempCoal);
        }
        return newOmega;
    }

    private boolean mutexSat(Subtask j) throws Exception {
        Task parentTask = j.getParentTask();
        int i_task = localResults.getIterationDatum(j).getI_q();
        int[][] D = parentTask.getD();

        double y_bid = 0.0;
        double y_mutex = 0.0;

        for (int i_j = 0; i_j < parentTask.getSubtaskList().size(); i_j++) {
            if ((i_j != i_task) && (D[i_task][i_j] < 0)) {
                y_mutex += localResults.getIterationDatum(parentTask.getSubtaskList().get(i_j)).getY();
            } else if (D[i_task][i_j] >= 1) {
                y_bid += localResults.getIterationDatum(parentTask.getSubtaskList().get(i_j)).getY();
            }
        }
        y_bid += localResults.getIterationDatum(j).getY();

        //if outbid by mutex, release task
        if (y_mutex > y_bid) {
            return false;
        } else if (y_mutex < y_bid) {
            return true;
        } else { // both coalition bid values are equal, compare costs
            double c_bid = 0.0;
            double c_mutex = 0.0;

            for (int i_j = 0; i_j < parentTask.getSubtaskList().size(); i_j++) {
                if ((i_j != i_task) && (D[i_task][i_j] < 0)) {
                    c_mutex += localResults.getIterationDatum(parentTask.getSubtaskList().get(i_j)).getCost();
                } else if (D[i_task][i_j] >= 1) {
                    c_bid += localResults.getIterationDatum(parentTask.getSubtaskList().get(i_j)).getCost();
                }
            }
            c_bid += localResults.getIterationDatum(j).getCost();

            if (c_mutex > c_bid) {
                // opposing coalition has higher costs
                return true;
            } else if (c_mutex < c_bid) {
                // your coalition has higher costs
                return false;
            } else {
                // if costs and bids are equal, the task highest on the list gets allocated
                int i_them = 0;
                int i_us = parentTask.getSubtaskList().indexOf(j);

                for (int i_j = 0; i_j < parentTask.getSubtaskList().size(); i_j++) {
                    if ((i_j != i_task) && (D[i_task][i_j] < 0)) {
                        i_them = i_j;
                        break;
                    }
                }
                return (i_us > i_them);
            }
        }
    }

    private boolean timeSat(Subtask j) throws Exception {
        boolean taskReleased = false;
        Task parenTask = j.getParentTask();
        int[][] D = parenTask.getD();
        ArrayList<Subtask> tempViolations = tempSat(j);

        for (Subtask j_u : tempViolations) { // if time constraint violations exist compare each time violation
            int i_j = localResults.getIterationDatum(j).getI_q();
            int i_u = localResults.getIterationDatum(j_u).getI_q();
            if ((D[i_j][i_u] >= 1) && (D[i_u][i_j] <= 0)) {
                // if j depends on u but u does not depend on j, release task
                taskReleased = true;
                break;
            } else if ((D[i_j][i_u] >= 1) && (D[i_u][i_j] >= 1)) {
                // if j and u are mutually dependent, check for latest arrival time
                double tz_j = localResults.getIterationDatum(j).getTz();
                double tz_u = localResults.getIterationDatum(j_u).getTz();
                double t_start = t_0;
                if (tz_j - t_start <= tz_u - t_start) {
                    // if u has a higher arrival time than j, release task
                    taskReleased = true;
                    break;
                }

            }
        }

        if (taskReleased) {
            if (localResults.isOptimistic(j)) {
                localResults.getIterationDatum(j).decreaseW_any();
                localResults.getIterationDatum(j).decreaseW_solo();
            }
            return false;
        }

        return true;
    }

    private ArrayList<Subtask> tempSat(Subtask j_q) throws Exception {
        double[][] T = j_q.getParentTask().getT();
        ArrayList<Subtask> J_parent = j_q.getParentTask().getSubtaskList();

        ArrayList<Subtask> violationSubtasks = new ArrayList<>();

        for (int u = 0; u < J_parent.size(); u++) {
            Subtask j_u = J_parent.get(u);
            double tz_q = localResults.getIterationDatum(j_q).getTz();
            double tz_u = localResults.getIterationDatum(J_parent.get(u)).getTz();
            boolean req1 = true;
            boolean req2 = true;

            if ((u != J_parent.indexOf(j_q)) && (localResults.getIterationDatum(j_u).getZ() != null)) {
                // if not the same subtask and other subtask has a winner
                req1 = tz_q <= tz_u + T[J_parent.indexOf(j_q)][u];
                req2 = tz_u <= tz_q + T[u][J_parent.indexOf(j_q)];
            }

            if (!(req1 && req2)) {
                violationSubtasks.add(j_u);
            }
        }

        return violationSubtasks;
    }

    private boolean depSat(Subtask j) throws Exception {
        IterationDatum datum = localResults.getIterationDatum(j);
        Task parentTask = j.getParentTask();
        int i_task = parentTask.getSubtaskList().indexOf(j);
        int[][] D = parentTask.getD();

        // Count number of requirements and number of completed requirements
        int N_req = 0;
        int n_sat = 0;
        for (int k = 0; k < parentTask.getSubtaskList().size(); k++) {
            if (i_task == k) {
                continue;
            }
            if (D[i_task][k] >= 1) {
                N_req++;
            }
            if (localResults.getIterationDatum(parentTask.getSubtaskList().get(k)).getZ() != null
                    && (D[i_task][k] == 1)) {
                n_sat++;
            }
        }

        if (localResults.isOptimistic(j)) { // task has optimistic bidding strategy
            if (datum.getV() == 0) {
                if ((n_sat == 0) && (N_req > 0)) {
                    // agent must be the first to win a bid for this tasks
                    datum.decreaseW_solo();
                } else if ((N_req > n_sat) && (N_req > 0)) {
                    // agent bids on a task without all of its requirements met for the first time
                    datum.decreaseW_any();
                }
            }

            if ((N_req != n_sat) && (N_req > 0)) { //if not all dependencies are met, v_i++
                datum.increaseV();
            } else if ((N_req == n_sat) && (N_req > 0)) { // if all dependencies are met, v_i = 0
                datum.resetV();
            }

            if (datum.getV() > this.O_kq) { // if task has held on to task for too long, release task
                datum.decreaseW_solo();
                datum.decreaseW_any();
                return false;
            }
        } else { // task has pessimistic bidding strategy
            //if not all dependencies are met
            if (N_req > n_sat) {
                //release task
                return false;
            }
        }
        return true;
    }

    private boolean coalitionSat(Subtask j, ArrayList<ArrayList<SimulatedAgent>> oldOmega, ArrayList<ArrayList<SimulatedAgent>> newOmega) {
        // Check Coalition Member Constraints
        int i_b = bundle.indexOf(j);

        if (oldOmega.get(i_b).size() == 0) { // no coalition partners in original list
            if (newOmega.get(i_b).size() > 0) { // new coalition partners in new list
                // release task
                return false;
            }
        } else { // coalition partners exist in original list, compare lists
            if (newOmega.get(i_b).size() > 0) { // new list is not empty
                // compare lists
                if (oldOmega.get(i_b).size() != newOmega.get(i_b).size()) { // if different sizes, then lists are not the same
                    // release task
                    return false;
                } else { // compare element by element
                    boolean released = false;
                    for (SimulatedAgent listMember : oldOmega.get(i_b)) {
                        if (!newOmega.get(i_b).contains(listMember)) { // if new list does not contain member of old list, then lists are not the same
                            // release task
                            released = true;
                            break;
                        }
                    }
                    if (released) {
                        // release task
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private int checkForChanges(IterationResults prevResults) throws Exception {
        return localResults.compareToList(prevResults);
    }

    private void logBundle() throws Exception {
        StringBuilder bundleList = new StringBuilder();
        bundleList.append("[");
        if (this.bundle.size() == 0) {
            bundleList.append("Empty");
        } else for (Subtask b : this.bundle) {
            bundleList.append(this.localResults.getIndexOf(b));
            if (this.bundle.indexOf(b) != this.bundle.size() - 1) {
                bundleList.append(", ");
            }
        }
        bundleList.append("]");
        getLogger().fine(this.name + " bundle:\t" + bundleList);
    }

    private void logPath() throws Exception {
        StringBuilder bundleList = new StringBuilder();
        bundleList.append("[");
        if (this.path.size() == 0) {
            bundleList.append("Empty");
        } else for (Subtask p : this.path) {
            bundleList.append(this.localResults.getIndexOf(p));
            if (this.path.indexOf(p) != this.path.size() - 1) {
                bundleList.append(", ");
            }
        }
        bundleList.append("]");
        getLogger().fine(this.name + " path:\t" + bundleList);
    }

    private void logRemainingBundle(int i_done) {
        StringBuilder bundleList = new StringBuilder();
        bundleList.append("[");
        if (this.bundle.size() == i_done+1) {
            bundleList.append("Empty");
        } else for (int i_b = i_done; i_b < path.size(); i_b++) {
            Subtask p = this.path.get(i_b);
            bundleList.append(p.getName());
            if (this.path.indexOf(p) != this.path.size() - 1) {
                bundleList.append(", ");
            }
        }
        bundleList.append("]");
        getLogger().fine(this.name + " remaining bundle: " + bundleList);
    }

    private void completeTask(Subtask j, ArrayList<Double> x_j) throws Exception {
        // check resources
        if (checkResources()) {
            moveToTastk(x_j);
            deductCosts(j);
            setToComplete(j);
        } else {
            requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE);
        }
    }

    private boolean checkResources() throws Exception {
        return this.myResources.checkResources(environment.getWorldType());
    }

    private void moveToTastk(ArrayList<Double> x_j) throws Exception {
        if (environment.getWorldType().equals("2D_Grid") || environment.getWorldType().equals("3D_Grid")) {
            this.position = new ArrayList<>();
            this.position.addAll(x_j);
        } else {
            throw new Exception("World type not supported.");
        }
    }

    private void deductCosts(Subtask j) throws Exception {
        if (this.myResources.getType().equals("Const")) {
            IterationDatum datum = this.localResults.getIterationDatum(j);
            this.myResources.deductCost(datum, environment.getWorldType());
        }
    }

    private void setToComplete(Subtask j) {
        j.setToComplete();
    }

    private boolean tasksAvailable() throws Exception {
        boolean allComplete = true;
        for (IterationDatum datum : localResults.getResults()) {
            if (!datum.getJ().getCompleteness()) {
                allComplete = false;
                break;
            }
        }
        if (allComplete) {
            return false;
        }

        for (IterationDatum datum : localResults.getResults()) {
            ArrayList<SubtaskBid> bidList = this.localResults.calcBidList(this);
            return (this.localResults.checkAvailability());
        }

        return true;
    }

    /**
     * Getters and Setters
     */
    public ArrayList<String> getSensorList() { return this.sensorList; }
    public ArrayList<Subtask> getBundle() { return this.bundle; }
    public ArrayList<Subtask> getOverallBundle() { return this.overallBundle; }
    public ArrayList<Subtask> getPath() { return this.path; }
    public ArrayList<Subtask> getOverallPath() { return this.overallPath; }
    public ArrayList<ArrayList<Double>> getX_path() { return this.x_path; }
    public ArrayList<ArrayList<Double>> getOverallX_path() { return this.overallX_path; }
    public int getMaxItersInViolation() { return this.O_kq; }
    public ArrayList<Subtask> getWorldSubtasks() { return this.worldSubtasks; }
    public int getW_solo() { return this.w_solo; }
    public int getW_any() { return this.w_any; }
    public IterationResults getLocalResults() { return this.localResults; }
    public ArrayList<Double> getPosition() { return this.position; }
    public double getT_0() { return this.t_0; }
    public Scenario getEnvironment() { return this.environment; }
    public double getSpeed() { return this.speed; }
    public AgentResources getResources() { return this.myResources; }
    public ArrayList<ArrayList<SimulatedAgent>> getOmega() { return this.omega; }
    public ArrayList<ArrayList<SimulatedAgent>> getOverallOmega() { return this.overallOmega; }
    public int getIteration() { return this.zeta; }
    public int getM(){ return this.M; }
    public ArrayList<Double> getInitialPosition(){ return this.initialPosition; }
    public int getZeta(){return this.zeta; }
    public AgentResources getInitialResources(){ return this.initialResources; }
}
