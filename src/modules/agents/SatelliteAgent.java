package modules.agents;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import modules.environment.Environment;
import modules.actions.SimulationAction;
import modules.measurements.Measurement;
import modules.measurements.MeasurementRequest;
import modules.planner.AbstractPlanner;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.*;

import java.util.*;

public abstract class SatelliteAgent extends AbstractAgent {
    /**
     * orekit satellite represented by this agent
     */
    protected Satellite sat;

    /**
     * ground point coverage, station overage, and cross-link access times for this satellite
     */
    protected HashMap<Satellite, TimeIntervalArray> accessesCL;
    protected HashMap<TopocentricFrame, TimeIntervalArray> accessGP;
    protected HashMap<Instrument, HashMap<TopocentricFrame, TimeIntervalArray>> accessGPInst;
    HashMap<GndStation, TimeIntervalArray> accessGS;

    /**
     * list of measurements requests received by this satellite at the current simulation time
     */
    protected ArrayList<MeasurementRequest> requestsReceived;

    /**
     * list of measurements performed by spacecraft pending to be downloaded to a the next visible ground station
     * or comms relay satellite
     */
    protected ArrayList<Measurement> measurementsPendingDownload;

    /**
     * overall list of measurements performed by this spacecraft
     */
    protected ArrayList<Measurement> measurementsDone;


    /**
     * Planner used by satellite to determine future actions. Regulates communications, measurements, and maneuvers
     */
    protected AbstractPlanner planner;

    /**
     * Names of groups and roles within simulation community
     */
    protected SimGroups myGroups;

    /**
     * current plan to be performed
     */
    protected LinkedList<SimulationAction> plan;

    /**
     * Environment in which this agent exists in.
     */
    protected Environment environment;

    /**
     * Addresses of all satellite and ground station agents present in the simulation
     */
    protected HashMap<Satellite, AgentAddress> satAddresses;
    protected HashMap<GndStation, AgentAddress> gndAddresses;

    /**
     * Creates an instance of a satellite agent. Requires a planner to already be created
     * and this must have the same orekit satellite assignment to this agent.
     * @param cons
     * @param sat
     * @param orbitData
     * @param planner
     */
    public SatelliteAgent(Constellation cons, Satellite sat, OrbitData orbitData, AbstractPlanner planner, SimGroups myGroups){
        this.setName(sat.getName());
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
        this.plan = new LinkedList<>();
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
    abstract public void sense() throws Exception;

    /**
     * Gives new information from messages or measurements to planner and crates/modifies plan if needed
     */
    abstract public void think() throws Exception;

    /**
     * Performs attitude maneuvers or sends messages to other satellites or ground stations if specified by plan
     */
    abstract public void execute() throws Exception;

    /**
     * Returns the next access with the target satellite
     * @param sat : target satellite
     * @return Array with the first element being the start date and the second being the end date
     */
    public ArrayList<AbsoluteDate> getNextAccess(Satellite sat){
        ArrayList<AbsoluteDate> access = new ArrayList<>();

        AbsoluteDate currDate = environment.getCurrentDate();

        for(int i = 0; i < accessesCL.get(sat).getRiseSetTimes().size(); i+=2){
            RiseSetTime riseTime = accessesCL.get(sat).getRiseSetTimes().get(i);
            RiseSetTime setTime = accessesCL.get(sat).getRiseSetTimes().get(i+1);

            AbsoluteDate riseDate = environment.getStartDate().shiftedBy(riseTime.getTime());
            AbsoluteDate setDate = environment.getStartDate().shiftedBy(setTime.getTime());

            if(setDate.compareTo(currDate) < 0) continue;

            access.add(riseDate); access.add(setDate);
            return access;
        }

        return new ArrayList<>();
    }

    /**
     * Returns the next access with the target ground station
     * @param gndStation : target ground station
     * @return Array with the first element being the start date and the second being the end date
     */
    public ArrayList<AbsoluteDate> getNextAccess(GndStation gndStation){
        ArrayList<AbsoluteDate> access = new ArrayList<>();

        AbsoluteDate currDate = environment.getCurrentDate();

        for(int i = 0; i < accessGS.get(gndStation).getRiseSetTimes().size(); i+=2){
            RiseSetTime riseTime = accessGS.get(gndStation).getRiseSetTimes().get(i);
            RiseSetTime setTime = accessGS.get(gndStation).getRiseSetTimes().get(i+1);

            AbsoluteDate riseDate = environment.getStartDate().shiftedBy(riseTime.getTime());
            AbsoluteDate setDate = environment.getStartDate().shiftedBy(setTime.getTime());

            if(setDate.compareTo(currDate) < 0) continue;

            access.add(riseDate); access.add(setDate);
            return access;
        }

        return new ArrayList<>();
    }

    /**
     * Returns the next access with the target address
     * @param address : target agent address
     * @return Array with the first element being the start date and the second being the end date
     */
    public ArrayList<AbsoluteDate> getNextAccess(AgentAddress address){
        for(Satellite target : satAddresses.keySet()){
            if(satAddresses.get(target).equals(address)) return getNextAccess(target);
        }
        for(GndStation target : gndAddresses.keySet()){
            if(gndAddresses.get(target).equals(address)) return getNextAccess(target);
        }
        return null;
    }

    public void registerAddresses(HashMap<Satellite, AgentAddress> satAdd, HashMap<GndStation, AgentAddress> gndAdd){
        this.satAddresses = new HashMap<>(satAdd);
        this.gndAddresses = new HashMap<>(gndAdd);
    }
    public String getName(){
        return sat.getName();
    }
    public Satellite getSat(){return sat; }
    public AgentAddress getMyAddress(){return this.satAddresses.get(this.sat);}
    public HashMap<Satellite, AgentAddress> getSatAddresses(){ return this.satAddresses; }
    public LinkedList<SimulationAction> getPlan(){ return this.plan; }
}
