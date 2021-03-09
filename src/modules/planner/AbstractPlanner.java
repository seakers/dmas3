package modules.planner;

import madkit.kernel.Message;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public abstract class AbstractPlanner {
    /**
     * Types of accepted planners
     */
    public static final String NONE = "none";
    public static final String TIME = "time";
    public static final String CCBBA = "CCBBA";
    public static final String RELAY = "relay";
    public static final String[] PLANNERS = {NONE, TIME, CCBBA, RELAY};

    /**
     * Planning horizon in seconds
     */
    protected final double planningHorizon;

    /**
     * threshold of new measurement requests required to trigger a plan reschedule
     */
    protected final int requestThreshold;

    /**
     * List of actions to be given to an agent to be performed
     */
    protected LinkedList<SimulationAction> plan;

    public AbstractPlanner(double planningHorizon, int requestThreshold){
        this.planningHorizon = planningHorizon;
        this.requestThreshold = requestThreshold;
    }

    /**
     * Creates an initial plan at the beginning of the simulation
     * @return a linked list of actions for the agent
     */
    public abstract LinkedList<SimulationAction> initPlan();

    /**
     * Creates, modifies, or maintains the plan to be performed by an agent
     * @param messageMap : map of different types of messages received by an agent
     * @param agent : agent set to perform the plan
     * @return a linked list of actions to be performed by the agent
     * @throws Exception
     */
    public abstract LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<Message>> messageMap,
                                                          SatelliteAgent agent) throws Exception;


    /**
     * 
     * @param request
     * @param performance
     * @return
     */
    public abstract double calcUtility(MeasurementRequest request,
                                       HashMap<Requirement, RequirementPerformance> performance);
}
