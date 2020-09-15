package modules.simulation;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Scheduler;
import madkit.simulation.activator.GenericBehaviorActivator;
import modules.environment.Environment;

import java.util.List;

public class SimScheduler extends Scheduler {
    private Environment environment;
    protected GenericBehaviorActivator<AbstractAgent> agents;

    @Override
    protected void activate() {
        // 1 : request my role
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCH_ROLE);
        agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCH_ROLE, "updateTime");
        addActivator(agents);

        // 2 : give instructions to agents
        agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_SENSE, "sense");
        addActivator(agents);
        agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK, "think");
        addActivator(agents);
        agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO, "execute");
        addActivator(agents);

        // 3 : give instructions to planners
            // 3.1 - Instructions for predetermined planner

            // 3.1 - Instructions for CCBBA planner
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK1, "phaseOne");
            addActivator(agents);
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_THINK2, "phaseTwo");
            addActivator(agents);
            agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.CCBBA_DONE, "planDone");
            addActivator(agents);

        // 4 : give instructions to environment
        agents = new GenericBehaviorActivator<>(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.ENV_ROLE, "tic");
        addActivator(agents);

        // 5 : start the simulation
        setSimulationState(SimulationState.RUNNING);
    }

    public void updateTime(){
        this.setGVT(environment.getGVT());
    }
}
