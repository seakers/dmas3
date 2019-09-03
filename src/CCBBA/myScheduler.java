package CCBBA;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import madkit.kernel.Scheduler;
import madkit.simulation.activator.GenericBehaviorActivator;

public class myScheduler extends Scheduler {

    protected GenericBehaviorActivator<AbstractAgent> agents;
    protected GenericBehaviorActivator<AbstractAgent> viewers;
    protected String planner = "CCBBA";

    @Override
    protected void activate() {

        // 1 : request my role
        requestRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.SCH_ROLE);

        // 2 : initialize the activators
        if(planner == "CCBBA") {
            agents = new GenericBehaviorActivator<>(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK, "phaseOne");
            addActivator(agents);
            agents = new GenericBehaviorActivator<>(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_THINK, "phaseTwo");
            addActivator(agents);
            agents = new GenericBehaviorActivator<>(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.AGENT_DO, "doTasks");
            addActivator(agents);
        }

        // 3 : Start the simulation
        setSimulationState(SimulationState.RUNNING);
    }
}