package modules.agents;

import madkit.kernel.Agent;
import modules.environment.Environment;
import modules.actions.SimulationActions;
import modules.measurement.Measurement;
import modules.measurement.MeasurementRequest;
import modules.planner.AbstractPlanner;
import modules.simulation.OrbitData;
import modules.simulation.SimGroups;
import org.orekit.frames.TopocentricFrame;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;

public abstract class SatelliteAgent extends Agent {
    /**
     * orekit satellite represented by this agent
     */
    private Satellite sat;

    /**
     * ground point coverage, station overage, and cross-link access times for this satellite
     */
    private HashMap<Satellite, TimeIntervalArray> accessesCL;
    private HashMap<TopocentricFrame, TimeIntervalArray> accessGP;
    private HashMap<Instrument, HashMap<TopocentricFrame, TimeIntervalArray>> accessGPInst;
    HashMap<GndStation, TimeIntervalArray> accessGS;

    /**
     * list of measurements requests received by this satellite at the current simulation time
     */
    private ArrayList<MeasurementRequest> requestsReceived;

    /**
     * list of measurements performed by spacecraft pending to be downloaded to a the next visible ground station
     * or comms relay satellite
     */
    private ArrayList<Measurement> measurementsPendingDownload;

    /**
     * overall list of measurements performed by this spacecraft
     */
    private ArrayList<Measurement> measurementsDone;


    /**
     * Planner used by satellite to determine future actions. Regulates communications, measurements, and maneuvers
     */
    private AbstractPlanner planner;

    /**
     * Names of groups and roles within simulation community
     */
    private SimGroups myGroups;

    /**
     * current plan to be performed
     */
    private Queue<SimulationActions> plan;

    /**
     * Environment in which this agent exists in.
     */
    private Environment environment;

    /**
     * Creates an instance of a satellite agent. Requires a planner to already be created
     * and this must have the same orekit satellite assignment to this agent.
     * @param cons
     * @param sat
     * @param orbitData
     * @param planner
     */
    public SatelliteAgent(Constellation cons, Satellite sat, OrbitData orbitData, AbstractPlanner planner, SimGroups myGroups){
        this.sat = sat;
        this.accessesCL = new HashMap<>( orbitData.getAccessesCL().get(cons).get(sat) );
        this.accessGP = new HashMap<>();
        this.accessGPInst = new HashMap<>();
        for(CoverageDefinition covDef : orbitData.getCovDefs()){
            accessGP.putAll( orbitData.getAccessesGP().get(covDef).get(sat) );
            for(Instrument ins : sat.getPayload()){
                accessGPInst.put(ins, orbitData.getAccessesGPIns().get(covDef).get(sat).get(ins));
            }
        }
        this.accessGS = new HashMap<>( orbitData.getAccessesGS().get(sat) );
        this.planner = planner;
        this.plan = new PriorityQueue<>();
        this.myGroups = myGroups;
    }

    @Override
    protected void activate(){
        requestRole(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SATELLITE);
        this.plan = planner.initPlan();
    }

    /**
     * Reads messages from other satellites or ground stations. Performs measurements if specified by plan.
     */
    abstract public void sense();

    /**
     * Gives new information from messages or measurements to planner and crates/modifies plan if needed
     */
    abstract public void think();

    /**
     * Performs attitude maneuvers or sends messages to other satellites or ground stations if specified by plan
     */
    abstract public void execute();

    @Override
    protected void live() { }

    public String getName(){
        return sat.getName();
    }
}
