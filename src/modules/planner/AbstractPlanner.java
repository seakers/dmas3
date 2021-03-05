package modules.planner;

import madkit.kernel.Message;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

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
     * List of actions to be given to an agent to be performed
     */
    private LinkedList<SimulationAction> plan;

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
}
