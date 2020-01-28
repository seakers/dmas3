package CCBBA.lib;

import madkit.kernel.AbstractAgent;

public class SimGroups extends AbstractAgent {

    /**
     * Organizational constants
     */
    public static final String MY_COMMUNITY = "simu";
    public static final String SIMU_GROUP = "simu";
    public static final String AGENT_THINK1 = "plan_construction";
    public static final String AGENT_THINK2 = "plan_sharing";
    public static final String AGENT_CONS = "plan_consensus";
    public static final String AGENT_DO = "plan_execute";
    public static final String AGENT_WAIT_DO = "wait_execute_plans";
    public static final String AGENT_DIE = "agent_die";
    public static final String AGENT_WAIT_DIE = "wait_agents_die";
    public static final String ENV_ROLE = "environment";
    public static final String SCH_ROLE = "scheduler";
    public static final String RESULTS_ROLE = "results";

}
