package modules.agents;

import madkit.kernel.Agent;
import modules.actions.SimulationActions;
import modules.environment.Environment;
import modules.measurement.MeasurementRequest;
import modules.simulation.OrbitData;
import modules.simulation.SimGroups;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class GndStatAgent extends Agent {
    /**
     * Coverage data
     */
    private OrbitData orbitData;

    /**
     * Names of groups and roles within simulation community
     */
    private SimGroups myGroups;

    /**
     * current plan to be performed
     */
    private Queue<SimulationActions> plan;

    /**
     * Environment in which this agent exists in
     */
    private Environment environment;

    /**
     * List of measurement requests given chronologically
     */
    private Queue<MeasurementRequest> requests;


    public GndStatAgent(OrbitData orbitData, SimGroups myGroups){
        this.orbitData = orbitData;
        this.myGroups = myGroups;
    }

    @Override
    protected void activate(){
        requestRole(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SATELLITE);
        this.requests = environment.getOrderedRequests();
        this.plan = this.initPlan();
    }

    @Override
    protected void live(){ }

    protected Queue<SimulationActions> initPlan(){
        Queue<SimulationActions> plan = new LinkedList<>();

        return plan;
    }
}
