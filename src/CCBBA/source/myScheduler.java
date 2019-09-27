package CCBBA;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Message;
import madkit.kernel.Scheduler;
import madkit.simulation.activator.GenericBehaviorActivator;
import java.util.List;

public class myScheduler extends Scheduler {
    protected GenericBehaviorActivator<AbstractAgent> agents;
    private String planner;

    /**
     * Constructor
     */
    public myScheduler(String planner){
        this.planner = planner;
    }

    /**
     * Planners
     */
    @Override
    protected void activate() {
        // 1 : request my role
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.SCH_ROLE);

        // 2 : execute planners
        if(this.planner == "CCBBA") { //Consensus Constraint-Based Bundle Algorithm
            //thinking phase
            agents = new GenericBehaviorActivator<>(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK, "phaseOne");
            addActivator(agents);
            agents = new GenericBehaviorActivator<>(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK, "phaseTwo");
            addActivator(agents);

            //doing phase
            agents = new GenericBehaviorActivator<>(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DO, "doTasks");
            addActivator(agents);

            //dying phase
            agents = new GenericBehaviorActivator<>(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DIE, "end");
            addActivator(agents);
        }
        else if(this.planner == "DEBUG_TASK"){  //Does not execute agents, just creates environment
            //thinking phase
            agents = new GenericBehaviorActivator<>(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK, "phaseOne");
            addActivator(agents);

            //dying phase
            agents = new GenericBehaviorActivator<>(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK, "end");
            addActivator(agents);
        }

        // 3 : collect results
        agents = new GenericBehaviorActivator<>(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE, "checkResults");
        addActivator(agents);

        // 4 : start the simulation
        setSimulationState(SimulationState.RUNNING);
    }
}