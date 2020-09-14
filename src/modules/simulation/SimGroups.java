package modules.simulation;

import madkit.kernel.AbstractAgent;

public class SimGroups {

    /**
     * Organizational constants
     */
    // Community and Groups
    public static final String MY_COMMUNITY = "simu";
    public static final String SIMU_GROUP = "simu";

    // Environment Roles
    public static final String ENV_ROLE = "environment";

    // Agent Roles
    public static final String AGENT = "agent_in_simulation";
    public static final String AGENT_SENSE = "agent_sense_environment";
    public static final String AGENT_THINK = "agent_make_plan";
    public static final String AGENT_DO = "agent_do_plan";
    public static final String AGENT_DIE = "agent_die";


    // Planner Roles
        // Scheduler Roles
        public static final String SCH_ROLE = "scheduler";

        // Results Collector Roles
        public static final String RESULTS_ROLE = "results";

        // Agent CCBBA Roles
        public static final String PLANNER = "agent_planner";
        public static final String PLANNER_DIE = "planner_die";
        public static final String CCBBA_THINK1 = "plan_construction";
        public static final String CCBBA_THINK2 = "plan_sharing";
        public static final String CCBBA_DONE = "plan_achieved";
}
