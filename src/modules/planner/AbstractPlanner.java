package modules.planner;

import madkit.kernel.Message;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public abstract class AbstractPlanner {
    public static final String NONE = "none";
    public static final String TIME = "time";
    public static final String CCBBA = "CCBBA";
    public static final String RELAY = "relay";
    public static final String[] PLANNERS = {NONE, TIME, CCBBA, RELAY};

    private LinkedList<SimulationAction> plan;

    public abstract LinkedList<SimulationAction> initPlan();
    public abstract LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<Message>> messages,
                                                          SatelliteAgent agent) throws Exception;
}
