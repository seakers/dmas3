package CCBBA;

import madkit.kernel.AbstractAgent;

public class AgentSimulation extends AbstractAgent {

    // Organizational constants
    public static final String MY_COMMUNITY = "simu";
    public static final String SIMU_GROUP = "simu";
    public static final String AGENT_THINK = "agent_planner";
    public static final String AGENT_DO = "agent_execute";
    public static final String ENV_ROLE = "environment";
    public static final String SCH_ROLE = "scheduler";
    public static final String VIEWER_ROLE = "viewer";

    @Override
    protected void activate() {
        // 1 : create the simulation group
        createGroup(MY_COMMUNITY, SIMU_GROUP);

        // 2 : create the environment
        Scenario environment = new Scenario("random", 3);
        //Scenario environment = new Scenario("appendix b", 5);
        launchAgent(environment);

        // 3 : launch some simulated agents
        for (int i = 0; i < 1; i++) {
            launchAgent(new SimulatedAgent02());
            launchAgent(new SimulatedAgent01());
        }

        // 4 : create the scheduler
        myScheduler scheduler = new myScheduler();
        launchAgent(scheduler, false);

        /*
        // 5 : create the viewer
        Viewer viewer = new Viewer();
        launchAgent(viewer, true);
        */
    }

    public static void main(String[] args) {
        executeThisAgent(1, false); // no gui for me
    }
}
