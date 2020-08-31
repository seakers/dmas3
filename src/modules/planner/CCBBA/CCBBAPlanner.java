package modules.planner.CCBBA;

import modules.environment.Environment;
import modules.planner.plans.BroadcastPlan;
import modules.planner.plans.Plan;
import modules.simulation.SimGroups;
import modules.spacecraft.Spacecraft;
import modules.environment.Subtask;
import modules.planner.Planner;
import modules.planner.messages.*;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;

public class CCBBAPlanner extends Planner {
    private Plan plan;
    private String planner;
    private CCBBASettings settings;
    private ArrayList<Subtask> bundle;
    private ArrayList<Subtask> overallBundle;
    private ArrayList<Subtask> path;
    private ArrayList<Subtask> overallPath;
    private ArrayList<ArrayList<Spacecraft>> omega;
    private ArrayList<ArrayList<Spacecraft>> overallOmega;
    private HashMap<Subtask, IterationDatum> iterationResults;
    private ArrayList<HashMap<Subtask, IterationDatum>> receivedResults;
    private Environment environment;

    public CCBBAPlanner(Spacecraft parentSpacecraft){
        this.parentSpacecraft = parentSpacecraft;
    }

    @Override
    public void activate(){
        this.planner = "CCBBA";
        this.settings = new CCBBASettings();
        this.bundle = new ArrayList<>();
        this.overallBundle = new ArrayList<>();
        this.path = new ArrayList<>();
        this.overallPath = new ArrayList<>();
        this.omega = new ArrayList<>();
        this.overallOmega = new ArrayList<>();
        this.iterationResults = new HashMap<>();
        for(Subtask subtask : environment.getEnvironmentSubtasks()){
            IterationDatum datum = new IterationDatum(subtask,this.settings);
            iterationResults.put(subtask, datum);
        }
        this.receivedResults = new ArrayList<>();

        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.PLANNER);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK1);
    }

    @Override
    public void planDone() {
        // send plan to parent agent

    }


    public void phaseOne() throws OrekitException {
        // generate bundle

        // broadcast results
        AbsoluteDate currentDate = environment.getCurrentDate();
        CCBBAResultsMessage resultsMessage = new CCBBAResultsMessage(this.iterationResults,
                                                                        parentSpacecraft.getPV(currentDate),
                                                                        parentSpacecraft.getPVEarth(currentDate));
        this.plan = new BroadcastPlan(resultsMessage);

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
}
