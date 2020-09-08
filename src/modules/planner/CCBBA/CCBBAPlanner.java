package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import modules.environment.Environment;
import modules.environment.Task;
import modules.planner.plans.BroadcastPlan;
import modules.planner.plans.Plan;
import modules.simulation.SimGroups;
import modules.spacecraft.Spacecraft;
import modules.environment.Subtask;
import modules.planner.Planner;
import modules.planner.messages.*;
import modules.spacecraft.maneuvers.Maneuver;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class CCBBAPlanner extends Planner {
    private Plan plan;
    private String planner;
    private CCBBASettings settings;
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
        getLogger().info("Constructing bundle...");

        while((bundle.size() < settings.M) && (iterationResults.subtasksAvailable()) && alive){
            // Calculate bids for all available subtasks
            ArrayList<Bid> bidList = iterationResults.calcBidList(this);

            // Select Maximum
            Bid maxBid = getMaxBid(bidList);

            // Add max bid to bundle and path
            addToPath(maxBid);
        }


        // broadcast results
        AbsoluteDate currentDate = environment.getCurrentDate();
        double timestep = environment.getTimeStep();
        CCBBAResultsMessage resultsMessage = new CCBBAResultsMessage(this.iterationResults,
                                                                        parentSpacecraft.getPV(currentDate),
                                                                        parentSpacecraft.getPVEarth(currentDate));
        this.plan = new BroadcastPlan(resultsMessage,currentDate,currentDate.shiftedBy(timestep));

        sendPlanToParentAgent(new PlannerMessage(this.plan));

        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK1);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK2);
    }

    public void phaseTwo(){
        // compare results

        // evaluate constraints

        // evaluate convergence

        // if converged, then move to done

        // else, readjust plan
        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK2);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK1);
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

    private Bid getMaxBid(ArrayList<Bid> bidList){
        Bid winningBid = null;
        double maxBid = 0.0;

        for(Bid bid : bidList){
            if(bid.getScore() > maxBid){
                winningBid = bid;
                maxBid = bid.getScore();
            }
        }

        return winningBid;
    }

    private void addToPath(Bid bid){
        int x = 1;
    }

    public ArrayList<Subtask> getBundle(){return this.bundle;}
    public ArrayList<Subtask> getPath(){return this.path;}
    public double getTimeStep(){return this.environment.getTimeStep();}
    public IterationDatum getIterationDatum(Subtask j){return this.iterationResults.getIterationDatum(j);}
}
