package modules.planner;

import modules.actions.SimulationActions;

import java.util.Queue;

public abstract class AbstractPlanner {
    public static final String NONE = "none";
    public static final String TIME = "time";
    public static final String CCBBA = "CCBBA";
    public static final String RELAY = "relay";
    public static final String[] PLANNERS = {NONE, TIME, CCBBA, RELAY};

    private Queue<SimulationActions> plan;

    public abstract Queue<SimulationActions> initPlan();
}
