package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Message;
import modules.environment.*;
import modules.planner.plans.BroadcastPlan;
import modules.planner.plans.Plan;
import modules.simulation.SimGroups;
import modules.spacecraft.Spacecraft;
import modules.planner.Planner;
import modules.planner.messages.*;
import modules.spacecraft.maneuvers.Maneuver;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class CCBBAPlanner extends Planner {
    private Plan plan;
    private String planner;
    private CCBBASettings settings;
    private int convergenceCounter;
    private ArrayList<Subtask> bundle;
    private ArrayList<Subtask> overallBundle;
    private ArrayList<Subtask> path;
    private ArrayList<Subtask> overallPath;
    private ArrayList<ArrayList<AbstractAgent>> omega;
    private ArrayList<ArrayList<Spacecraft>> overallOmega;
    private ArrayList<Maneuver> maneuvers;
    private ArrayList<Maneuver> overallManeuvers;
    private IterationResults iterationResults;
    private ArrayList<HashMap<Subtask, IterationDatum>> receivedResults;
    private Environment environment;

    public CCBBAPlanner(Spacecraft parentSpacecraft){
        this.parentSpacecraft = parentSpacecraft;
    }

    @Override
    public void activate(){
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.PLANNER);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK1);

        this.planner = "CCBBA";
        this.settings = new CCBBASettings();
        this.convergenceCounter = 0;
        this.bundle = new ArrayList<>();
        this.overallBundle = new ArrayList<>();
        this.path = new ArrayList<>();
        this.overallPath = new ArrayList<>();
        this.omega = new ArrayList<ArrayList<AbstractAgent>>();
        for(int i = 0; i < settings.M; i++){
            this.omega.add(new ArrayList<AbstractAgent>());
        }
        this.overallOmega = new ArrayList<>();
        this.maneuvers = new ArrayList<>();
        this.overallManeuvers = new ArrayList<>();
        this.iterationResults = new IterationResults(parentSpacecraft, environment.getEnvironmentSubtasks(), settings);
        this.receivedResults = new ArrayList<>();
    }

    @Override
    public void planDone() {
        // send plan to parent agent
        int x = 1;
    }


    public void phaseOne() throws Exception {
        // generate bundle
        getLogger().info("Starting phase one");

        // Check for life status
        var myRoles = getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);
        boolean alive = !(myRoles.contains(SimGroups.AGENT_DIE));

        // Save previous iteration's results
        IterationResults prevResults = iterationResults.copy();

        // reset availability for all subtasks
        this.iterationResults.resetTaskAvailability();

        // check for new coalition members
        checkNewCoalitionMembers();

        // construct bundle
        getLogger().info("TP1 - Constructing bundle...");

        while((bundle.size() < settings.M) && (iterationResults.subtasksAvailable()) && alive){
            // Calculate bids for all available subtasks
            ArrayList<Bid> bidList = iterationResults.calcBidList(this, this.parentSpacecraft);

            // Select Maximum
            Bid maxBid = getMaxBid(bidList, prevResults);

            // Add max bid to bundle and path
            Subtask j_chosen = maxBid.getJ();
            IterationDatum datum_chosen = iterationResults.getIterationDatum(j_chosen);
            if( maxBid.getC() > 0 && maxBid.getC() > datum_chosen.getY()){
                // Add task to bundle
                this.bundle.add(j_chosen);

                // Add new path
                this.path = new ArrayList<>(maxBid.getWinnerPath());

                // Add new maneuvers
                this.maneuvers = new ArrayList<>(maxBid.getManeuvers());

                // update iteration results
                this.iterationResults.updateResults(maxBid,parentSpacecraft);
            }
            else if(j_chosen != null){
                datum_chosen.setH(0);
            }
        }

        // check for new coalition members
        checkNewCoalitionMembers();


        // broadcast results
        getLogger().info("TP1 - Sending new results to spacecraft for broadcast...");
        AbsoluteDate currentDate = environment.getCurrentDate();
        double timestep = environment.getTimeStep();
        CCBBAResultsMessage resultsMessage = new CCBBAResultsMessage(this.iterationResults,
                                                                        parentSpacecraft.getPVEarth(currentDate));
        this.plan = new BroadcastPlan(resultsMessage,currentDate,currentDate.shiftedBy(timestep));
        sendPlanToParentAgent(new PlannerMessage(this.plan));

        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK1);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK2);
    }

    public void phaseTwo() throws Exception {
        getLogger().info("Starting phase two");
        // halt results broadcasting
        this.plan = null;

        // read messages from spacecraft
        List<Message>  receivedMessages = nextMessages(null);
        if(receivedMessages.size() > 0){
            getLogger().info("TP2 - New messages found! Comparing results...");

            // Save previous iteration's results
            IterationResults prevResults = iterationResults.copy();

            // Unpack messages
            ArrayList<IterationResults> receivedResults = new ArrayList<>();
            for(Message receivedMessage : receivedMessages){
                IterationResults results = ((CCBBAResultsMessage) receivedMessage).getResults();
                receivedResults.add(results);
            }

            // compare results
            compareResults(receivedResults);

            // evaluate constraints
            constraintEval();

            // evaluate convergence
            boolean changesMade = checkForChanges(prevResults);
            if(changesMade){
                // changes were made, reconsider bids
                getLogger().fine("Changes were made. Reconsidering bids");
                convergenceCounter = 0;
                leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK2);
                requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK1);
            }
            else{
                // no changes were made, check convergence
                getLogger().fine("No changes were made. Checking convergence");
                convergenceCounter++;

                // check if convergence counter reached
                if(convergenceCounter < settings.convIndicator){
                    // readjust plan
                    leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK2);
                    requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK1);
                }
                else{
                    // if converged, then move to done
                    convergenceCounter = 0;

                    // broadcast results
                    getLogger().info("TP2 - Sending converged results to spacecraft for broadcast...");
                    AbsoluteDate currentDate = environment.getCurrentDate();
                    double timestep = environment.getTimeStep();
                    CCBBAResultsMessage resultsMessage = new CCBBAResultsMessage(this.iterationResults,
                            parentSpacecraft.getPVEarth(currentDate));
                    this.plan = new BroadcastPlan(resultsMessage,currentDate,currentDate.shiftedBy(timestep));
                    sendPlanToParentAgent(new PlannerMessage(this.plan));

                    leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK2);
                    requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_DONE);
                }

            }
        }
        else{
            getLogger().info("TP2 - No messages found. Waiting for them...");
        }
    }

    private boolean checkForChanges(IterationResults prevResults) {
        return iterationResults.checkForChanges(prevResults);
    }

    private void compareResults(ArrayList<IterationResults> receivedResults){
        getLogger().info("Comparing results...");
        for(IterationResults result : receivedResults) {
            for(Subtask j : result.getResults().keySet()){
                // Unpack received Results
                IterationDatum itsDatum = result.getIterationDatum(j);
                double itsY = itsDatum.getY();
                AbstractAgent itsZ;
                itsZ = itsDatum.getZ();
                AbsoluteDate itsTz = itsDatum.getTz();
                AbstractAgent it = result.getParentAgent();
                AbsoluteDate itsS = itsDatum.getS();
                boolean itsCompletion = itsDatum.getSubtask().getCompletion();

                // Unpack my results
                if(!iterationResults.containsKey(j)){
                    // if its results are from an unseen subtask, add the datum to your results
                    iterationResults.put(itsDatum);
                    continue;
                }
                IterationDatum myDatum = iterationResults.getIterationDatum(j);
                double myY = myDatum.getY();
                AbstractAgent myZ = myDatum.getZ();
                AbsoluteDate myTz = myDatum.getTz();
                AbstractAgent me = parentSpacecraft;
                AbsoluteDate myS = myDatum.getS();
                boolean myCompletion = myDatum.getSubtask().getCompletion();

                // Comparing bids. See Ref 40 Table 1
                if (itsZ == null) {
                    if (myZ == null) {
                        // leave
                        iterationResults.leaveResults(itsDatum);
                    }else if (myZ == it) {
                        // update
                        iterationResults.updateResults(itsDatum);
                    } else if ((myZ != me) && (myZ != it) && (myZ != null)) {
                        if (itsS.compareTo(myS) < 0) {
                            // update
                            iterationResults.updateResults(itsDatum);
                        }
                    } else if (myZ == me) {
                        // leave
                        iterationResults.leaveResults(itsDatum);
                    }
                }
                else if (itsZ == me) {
                    if (myZ == null) {
                        // leave
                        iterationResults.leaveResults(itsDatum);
                    } else if (myZ == it) {
                        // reset
                        iterationResults.resetResults(itsDatum);
                    } else if ((myZ != me) && (myZ != it) && (myZ != null)) {
                        if (itsS.compareTo(myS) < 0) {
                            // reset
                            iterationResults.resetResults(itsDatum);
                        }
                    } else if (myZ == me) {
                        // leave
                        iterationResults.leaveResults(itsDatum);
                    }
                } else if ((itsZ != it) && (itsZ != me) && (itsZ != null)) {
                    if (myZ == me) {
                        if ((itsCompletion) && (itsCompletion != myCompletion)) {
                            // update
                            iterationResults.updateResults(itsDatum);
                        } else if ((itsS.compareTo(myS) < 0) && (itsY > myY)) {
                            // update
                            iterationResults.updateResults(itsDatum);
                            iterationResults.getIterationDatum(itsDatum).decreaseW_all();
                        }
                    } else if (myZ == it) {
                        if (itsS.compareTo(myS) < 0) {
                            //update
                            iterationResults.updateResults(itsDatum);
                        } else {
                            // reset
                            iterationResults.resetResults(itsDatum);
                        }
                    } else if (myZ == itsZ) {
                        if (itsS.compareTo(myS) < 0) {
                            // update
                            iterationResults.updateResults(itsDatum);
                        }
                    } else if ((myZ != me) && (myZ != it) && (myZ != itsZ) && (myZ != null)) {
                        if ((itsS.compareTo(myS) < 0) && (itsY > myY)) {
                            // update
                            iterationResults.updateResults(itsDatum);
                        }
                    } else if (myZ == null) {
                        // leave
                        iterationResults.leaveResults(itsDatum);
                    }
                } else if (itsZ.equals(it)) {
                    if (myZ == null) {
                        // update
                        iterationResults.updateResults(itsDatum);
                    }
                    else if (myZ == it) {
                        // update
                        iterationResults.updateResults(itsDatum);
                    } else if ((myZ != me) && (myZ != it) && (myZ != null)) {
                        if ((itsS.compareTo(myS) < 0) || (itsY > myY)) {
                            // update
                            iterationResults.updateResults(itsDatum);
                        }
                    } else if (myZ.equals(me)) {
                        if ((itsCompletion) && (itsCompletion != myCompletion)) {
                            // update
                            iterationResults.updateResults(itsDatum);
                        } else if (itsY > myY) {
                            // update
                            iterationResults.updateResults(itsDatum);
                            iterationResults.getIterationDatum(itsDatum).decreaseW_all();
                        }
                    }
                }

            }
        }
    }

    private void constraintEval() throws Exception {
        getLogger().info("Checking constraints...");
        for (Subtask j : this.bundle) {
            // create list of new coalition members
            ArrayList<ArrayList<AbstractAgent>> newOmega = getNewCoalitionMembers(j);
            ArrayList<ArrayList<AbstractAgent>> oldOmega = this.omega;

            boolean mutexSat = mutexSat(j);
            boolean timeSat = timeSat(j);
            boolean depSat = depSat(j);
            boolean coalitionSat = coalitionSat(j, oldOmega, newOmega);

            if (!mutexSat || !timeSat || !depSat || !coalitionSat) {
                // subtask does not satisfy all constraints, release task
                IterationDatum resetDatum = iterationResults.getIterationDatum(j);
                iterationResults.getIterationDatum(j).decreaseW_all();
                iterationResults.resetResults(resetDatum);
                String constraintsFailed = this.parentSpacecraft.getName() + " FAILED ";

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

                constraintsFailed += "on subtask " + j.toString();
                getLogger().fine(constraintsFailed);
                break;
            } else {
                getLogger().fine("Constraint check for bunde task #" + (bundle.indexOf(j) + 1) + " passed");
            }
        }
    }

    private boolean coalitionSat(Subtask j, ArrayList<ArrayList<AbstractAgent>> oldOmega, ArrayList<ArrayList<AbstractAgent>> newOmega) {
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
                    for (AbstractAgent listMember : oldOmega.get(i_b)) {
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

    private boolean depSat(Subtask j) {
        Task parentTask = j.getParentTask();
        Dependencies dep = parentTask.getDependencies();

        int n_req = 0;
        int n_sat = 0;

        for(Subtask q : parentTask.getSubtasks()){
            IterationDatum datum = iterationResults.getIterationDatum(q);
            if(j == q) continue;
            if(dep.depends(j,q)) {
                n_req++;
                if(datum.getZ() != null) n_sat++;
            }
        }

        IterationDatum datum = iterationResults.getIterationDatum(j);
        if (iterationResults.isOptimistic(j)) { // task has optimistic bidding strategy
            if (datum.getV() == 0) {
                if ((n_sat == 0) && (n_req > 0)) {
                    // agent must be the first to win a bid for this tasks
                    datum.decreaseW_solo();
                } else if ((n_req > n_sat) && (n_req > 0)) {
                    // agent bids on a task without all of its requirements met for the first time
                    datum.decreaseW_any();
                }
            }

            if ((n_req != n_sat) && (n_req > 0)) { //if not all dependencies are met, v_i++
                datum.increaseV();
            } else if ((n_req == n_sat) && (n_req > 0)) { // if all dependencies are met, v_i = 0
                datum.resetV();
            }

            if (datum.getV() > this.settings.O_kq) { // if task has held on to task for too long
                // release task
                datum.decreaseW_solo();
                datum.decreaseW_any();
                return false;
            }
        }
        else { // task has pessimistic bidding strategy
            //if not all dependencies are met
            if (n_req > n_sat) {
                //release task
                return false;
            }
        }
        return true;
    }

    private boolean mutexSat(Subtask j) {
        Task parentTask = j.getParentTask();
        Dependencies dep = parentTask.getDependencies();
        double y_bid = 0.0;
        double y_mutex = 0.0;

        for(Subtask q : parentTask.getSubtasks()){
            if(j != q && dep.mutuallyExclusive(j,q)) y_mutex += iterationResults.getIterationDatum(q).getY();
            else if(dep.depends(j,q)) y_bid += iterationResults.getIterationDatum(q).getY();
        }
        y_bid += iterationResults.getIterationDatum(j).getY();

        //if outbid by mutex, release task
        if (y_mutex > y_bid) {
            return false;
        } else if (y_mutex < y_bid) {
            return true;
        } else { // both coalition bid values are equal, compare costs
            double c_bid = 0.0;
            double c_mutex = 0.0;

            for(Subtask q : parentTask.getSubtasks()){
                if(j != q && dep.mutuallyExclusive(j,q)) c_mutex += iterationResults.getIterationDatum(q).getY();
                else if(dep.depends(j,q)) c_bid += iterationResults.getIterationDatum(q).getY();
            }
            c_bid += iterationResults.getIterationDatum(j).getY();

            if (c_mutex > c_bid) {
                // opposing coalition has higher costs
                return true;
            } else if (c_mutex < c_bid) {
                // your coalition has higher costs
                return false;
            } else {
                // if costs and bids are equal, the task highest on the list gets allocated
                int i_them = 0;
                int i_us = parentTask.getSubtasks().indexOf(j);

                for(Subtask q : parentTask.getSubtasks()){
                    if(j != q && dep.mutuallyExclusive(j,q)) i_them = parentTask.getSubtasks().indexOf(q);
                }
                return i_us > i_them;
            }
        }
    }

    private boolean timeSat(Subtask j) throws Exception {
        boolean taskReleased = false;
        Task parentTask = j.getParentTask();
        Dependencies dep = parentTask.getDependencies();

        // check for temporal constraints violations
        ArrayList<Subtask> tempViolations = tempSat(j);

        for(Subtask u : tempViolations){
            // if time constraint violations exist compare each time violation
            if(dep.depends(j,u) && ( dep.noDependency(u,j) || dep.noDependency(u,j) )){
                taskReleased = true;
                break;
            }
            else if(dep.depends(j,u) && dep.depends(u,j)){
                // if j and u are mutually dependent, check for latest arrival time
                AbsoluteDate tz_j = iterationResults.getIterationDatum(j).getTz();
                AbsoluteDate tz_u = iterationResults.getIterationDatum(u).getTz();
                AbsoluteDate t_start = parentSpacecraft.getStartDate();

                if( tz_j.durationFrom(t_start) <= tz_u.durationFrom(t_start) ){
                    // if u has a higher arrival time than j, release task
                    taskReleased = true;
                    break;
                }
                else if(u.getCompletion()){
                    // u already has been performed and j cannot meet time requirements, release task
                    taskReleased = true;
                    break;
                }

            }
        }

        if (taskReleased) {
            if (iterationResults.isOptimistic(j)) {
                iterationResults.getIterationDatum(j).decreaseW_any();
                iterationResults.getIterationDatum(j).decreaseW_solo();
            }
            return false;
        }

        return true;
    }

    private ArrayList<Subtask> tempSat(Subtask q) throws Exception {
        Task parentTask = q.getParentTask();
        Dependencies dep = parentTask.getDependencies();

        ArrayList<Subtask> violationSubtasks = new ArrayList<>();
        AbsoluteDate tz_q = iterationResults.getIterationDatum(q).getTz();
        for(Subtask u : parentTask.getSubtasks()){
            AbstractAgent winner_u = iterationResults.getIterationDatum(u).getZ();
            AbsoluteDate tz_u = iterationResults.getIterationDatum(u).getTz();
            boolean req1 = true;        // true if the times are between the maximum correlation time
            boolean req2 = true;        // true if the times are between the minimum correlation time

            if(q != u && winner_u != null){
                // if not the same subtask and other subtask has a winner, check time constraint satisfaction
                req1 = Math.abs(tz_u.durationFrom(tz_q) ) <= dep.Tmax(u,q) && Math.abs(tz_q.durationFrom(tz_u) ) <= dep.Tmax(q,u);
                req2 = Math.abs(tz_u.durationFrom(tz_q) ) >= dep.Tmin(u,q) && Math.abs(tz_q.durationFrom(tz_u) ) >= dep.Tmin(q,u);
            }
            if (!(req1 && req2)) {
                violationSubtasks.add(u);
            }
        }

        return violationSubtasks;
    }

    private void sendPlanToParentAgent(PlannerMessage message){
        sendMessageWithRole(this.parentAgentAddress, message, SimGroups.PLANNER);
    }

    private void checkNewCoalitionMembers(){
        for (Subtask j_b : bundle) {
            int i_b = bundle.indexOf(j_b);
            ArrayList<ArrayList<AbstractAgent>> newCoalitions = getNewCoalitionMembers(j_b);
            this.omega.set(i_b, newCoalitions.get(i_b));
        }
    }

    private ArrayList<ArrayList<AbstractAgent>> getNewCoalitionMembers(Subtask j) {
        ArrayList<ArrayList<AbstractAgent>> newOmega = new ArrayList<>();
        for (int i = 0; i < settings.M; i++) {
            ArrayList<AbstractAgent> tempCoal = new ArrayList<>();

            if (this.bundle.size() >= i + 1) {
                ArrayList<Subtask> coalTasks = j.getParentTask().getSubtasks();

                for(Subtask subtask : coalTasks){
                    IterationDatum datum = this.iterationResults.getIterationDatum(subtask);
                    AbstractAgent winner = datum.getZ();

                    if( (winner != this.parentSpacecraft) && (winner != null)
                            &&  (j.getParentTask() == subtask.getParentTask())){
                        // if the winner of the subtask is not me and not null and share the same parent task,
                        // then the winner Z is a coalition partner
                        tempCoal.add(winner);
                    }
                }
            }
            newOmega.add(tempCoal);
        }
        return newOmega;
    }

    private Bid getMaxBid(ArrayList<Bid> bidList, IterationResults prevResults){
        Bid winningBid = bidList.get(0);
        double maxBid = 0.0;

        for(Bid bid : bidList){
            Subtask j_bid = bid.getJ();
            IterationDatum datum = this.getIterationDatum(j_bid);

            double bidUtility = bid.getC();
            int h = datum.getH();

            if(h == 1){
                if(datum.getY() > bidUtility){
                    datum.setH(0);
                    h = 0;
                }
                else if(!pathAvailable(bid,prevResults)){
                    datum.setH(0);
                    h = 0;
                }
            }

            if( (h >= 0) && (bidUtility*h > maxBid) ){
                winningBid = bid;
                maxBid = bid.getScore();
            }
        }

        return winningBid;
    }

    private boolean pathAvailable(Bid bid, IterationResults prevResults){
        // checks if new proposed path does not have bids lower than those previously won
        ArrayList<Subtask> bidPath = bid.getPath();
        ArrayList<Double> pathUtility = bid.getWinningPathUtility().getUtilityList();

        for(Subtask j_path : bidPath){
            int i_p = bidPath.indexOf(j_path);
            double prevUtility = prevResults.getIterationDatum(j_path).getY();
            double newUtility = pathUtility.get(i_p);

            if(prevUtility > newUtility) return false;
        }
        return true;
    }

    public void releaseTaskFromBundle(IterationDatum itsDatum){
        Subtask j = itsDatum.getSubtask();

        if (bundle.contains(j)) {
            int counter = 0;
            int i_b = bundle.indexOf(j);
            int i_p = path.indexOf(j);

            if(i_b <= i_p) {
                for ( ; i_b < bundle.size(); ) {
                    Subtask j_b = bundle.get(i_b);
                    if (counter > 0) {
                        AbstractAgent updatedWinner = iterationResults.getIterationDatum(j_b).getZ();
                        if(updatedWinner == parentSpacecraft) {
                            iterationResults.resetResults(j_b);
                        }
                    }
                    this.omega.set(i_b, new ArrayList<>());

                    // remove subtask and all subsequent ones from bundle and path
                    this.maneuvers.remove(path.indexOf(j_b));
                    this.path.remove(path.indexOf(j_b));
                    this.bundle.remove(i_b);
                    counter++;
                }
            }
            else{
                for ( ; i_p < path.size(); ) {
                    Subtask j_p = path.get(i_p);
                    if (counter > 0) {
                        AbstractAgent updatedWinner = iterationResults.getIterationDatum(j_p).getZ();
                        if(updatedWinner == parentSpacecraft) {
                            iterationResults.resetResults(j_p);
                        }
                    }
                    i_b = bundle.indexOf(j_p);
                    this.omega.set(i_b, new ArrayList<>());

                    // remove subtask and all subsequent ones from bundle and path
                    this.path.remove(i_p);
                    this.maneuvers.remove(i_p);
                    this.bundle.remove(i_b);
                    counter++;
                }
            }
        }
        int x = 1;
    }

    public ArrayList<Subtask> getBundle(){return this.bundle;}
    public ArrayList<Subtask> getPath(){return this.path;}
    public double getTimeStep(){return this.environment.getTimeStep();}
    public IterationDatum getIterationDatum(Subtask j){return this.iterationResults.getIterationDatum(j);}
    public ArrayList<Subtask> getOverallBundle(){return this.overallBundle;}
    public ArrayList<Subtask> getOverallPath(){return this.overallPath;}
    public CCBBASettings getSettings(){return settings;}
    public IterationResults getIterationResults(){return iterationResults;}
}
