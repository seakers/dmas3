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
        requestRole(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SCHEDULER);

        // 1 : make agents listen/sense
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SATELLITE, "sense");
        addActivator(agents);
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.GNDSTAT, "sense");
        addActivator(agents);

        // 2 : make agents update/create plans
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SATELLITE, "think");
        addActivator(agents);
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.GNDSTAT, "think");
        addActivator(agents);

        // 3 : let agents execute plans
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SATELLITE, "execute");
        addActivator(agents);
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.GNDSTAT, "execute");
        addActivator(agents);

        // 4 : update sim time
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.ENVIRONMENT, "tic");
        agents = new GenericBehaviorActivator<>(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SCHEDULER, "tic");
        addActivator(agents);

        // 5 : start the simulation
        setSimulationState(SimulationState.RUNNING);
    }

    public void tic(){
        this.setGVT( environment.getGVT() );
    }
}
