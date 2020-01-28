package CCBBA.lib;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Scheduler;
import madkit.simulation.activator.GenericBehaviorActivator;

public class Planner extends Scheduler {
    protected GenericBehaviorActivator<AbstractAgent> agents;
    private String planner;

    /**
     * Constructor
     */
    public Planner(String planner){
        this.planner = planner;
    }

    /**
     * Planners
     */
    @Override
    protected void activate() {
        // 1 : request my role
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCH_ROLE);

        // 2 : execute planners
        if(this.planner == "CCBBA") { //Consensus Constraint-Based Bundle Algorithm
            //thinking phase
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1, "thinkingPhaseOne");
            addActivator(agents);
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK2, "thinkingPhaseTwo");
            addActivator(agents);
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_CONS, "consensusPhase");
            addActivator(agents);

            //doing phase
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO, "doingPhase");
            addActivator(agents);
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE, "dying");
            addActivator(agents);

            //waiting phase
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_WAIT_DO, "waitOnTaskDoing");
            addActivator(agents);

            //dying phase
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_WAIT_DIE, "die");
            addActivator(agents);
        }
        else if(this.planner == "DEBUG_TASK"){  //Does not execute agents, just creates environment
            //thinking phase
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK1, "phaseOne");
            addActivator(agents);

            //dying phase
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE, "dying");
            addActivator(agents);
        }

        // 3 : collect results
        agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.RESULTS_ROLE, "checkResults");
        addActivator(agents);

        // 4 : start the simulation
        setSimulationState(SimulationState.RUNNING);
    }
}
