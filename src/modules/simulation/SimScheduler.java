package modules.simulation;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Scheduler;
import madkit.simulation.activator.GenericBehaviorActivator;
import modules.environment.Environment;
import org.orekit.time.AbsoluteDate;

public class SimScheduler extends Scheduler {

    private SimGroups myGroups;
    private Environment environment;
    protected GenericBehaviorActivator<AbstractAgent> agents;

    public SimScheduler(SimGroups myGroups, AbsoluteDate startDate, AbsoluteDate endDate){
        super(endDate.durationFrom(startDate));
        this.myGroups = myGroups;
    }

    @Override
    protected void activate() {
        // 0 : request my role
        requestRole(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCHEDULER);

        // 1 : make ground stations perform their duties
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.GNDSTAT, "sense");
        addActivator(agents);
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.GNDSTAT, "think");
        addActivator(agents);
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.GNDSTAT, "execute");
        addActivator(agents);

        // 2 : make agents listen/sense
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SATELLITE, "sense");
        addActivator(agents);

        // 3 : make agents update/create plans
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SATELLITE, "think");
        addActivator(agents);

        // 4 : let agents execute plans
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SATELLITE, "execute");
        addActivator(agents);

        // 5 : update sim time
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.ENVIRONMENT, "tic");
        addActivator(agents);
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCHEDULER, "tic");
        addActivator(agents);

        // 6 : start the simulation
        setSimulationState(SimulationState.RUNNING);
    }

    public void tic() throws Exception {
        this.setGVT( environment.getGVT() );

        boolean endSim = environment.getStartDate().shiftedBy(environment.getGVT()).compareTo(environment.getEndDate()) >= 0;

        if(endSim){
            environment.printResults();
            setSimulationState(SimulationState.SHUTDOWN);
        }
    }
}
