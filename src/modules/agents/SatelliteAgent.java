package modules.agents;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.ManeuverAction;
import modules.actions.MeasurementAction;
import modules.environment.Environment;
import modules.actions.SimulationAction;
import modules.measurements.Measurement;
import modules.measurements.MeasurementRequest;
import modules.messages.MeasurementMessage;
import modules.messages.MeasurementRequestMessage;
import modules.messages.RelayMessage;
import modules.messages.filters.GndFilter;
import modules.orbitData.*;
import modules.planner.AbstractPlanner;
import modules.simulation.SimGroups;
import org.orekit.files.ccsds.OPMFile;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.*;

import java.util.*;
import java.util.logging.Level;

/**
 * Abstract Satellite Agent
 * Represent a generic satellite with varying degrees of autonomy, depending on the planner
 * assigned to the satellite. It operates as a states machine, where it senses its environment,
 * thinks about the newly perceived status of the world, and then performs an action based on
 * said observations.
 *
 * @author a.aguilar
 */
public abstract class SatelliteAgent extends AbstractAgent {
    /**
     * orekit satellite represented by this agent
     */
    protected Satellite sat;

    /**
     * satellite's current attitude
     */
    protected Attitude attitude;

    /**
     * ground point coverage, station overage, and cross-link access times for this satellite
     */
    protected HashMap<Satellite, TimeIntervalArray> accessesCL;
    protected HashMap<TopocentricFrame, TimeIntervalArray> accessGP;
    protected HashMap<Instrument, HashMap<CoverageDefinition, HashMap<TopocentricFrame, TimeIntervalArray>>> accessGPInst;
    HashMap<GndStation, TimeIntervalArray> accessGS;

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
     * Message Inboxes of different types. One for relay messages and one for measurement
     * request messages
     */
    protected ArrayList<Message> relayMessages;
    protected ArrayList<Message> requestMessages;
    protected ArrayList<Message> plannerMessages;

    /**
     * Creates an instance of a satellite agent. Requires a planner to already be created
     * and this must have the same orekit satellite assignment to this agent.
     * @param cons : constellation to who this sat belongs to
     * @param sat : orekit satellite to be represented by this agent
     * @param orbitData : coverage data of scenario
     * @param planner : planner chosen to be assigned to this satellite
     */
    public SatelliteAgent(Constellation cons, Satellite sat, OrbitData orbitData, Attitude attitude,
                          AbstractPlanner planner, SimGroups myGroups, Level loggerLevel){
        this.setName(sat.getName());
        this.sat = sat;
        this.attitude = attitude;

        this.accessesCL = new HashMap<>( orbitData.getAccessesCL().get(cons).get(sat) );
        this.accessGP = new HashMap<>();
        for(CoverageDefinition covDef : orbitData.getCovDefs()){
            accessGP.putAll( orbitData.getAccessesGP().get(covDef).get(sat) );
        }
        this.accessGPInst = new HashMap<>();
        for(Instrument ins : sat.getPayload()){
            accessGPInst.put(ins, new HashMap<>());
            for(CoverageDefinition covDef : orbitData.getCovDefs()){
                accessGPInst.get(ins).put(covDef, new HashMap<>(orbitData.getAccessesGPIns().get(covDef).get(sat).get(ins)));
                }
        }
        this.accessGS = new HashMap<>( orbitData.getAccessesGS().get(sat) );
        this.planner = planner;
        this.plan = new LinkedList<>();
        this.myGroups = myGroups;
        getLogger().setLevel(loggerLevel);

        // initializes inboxes
        relayMessages = new ArrayList<>();
        requestMessages = new ArrayList<>();
        plannerMessages = new ArrayList<>();
    }

    /**
     * Triggered when launching spacecraft agent. Assigns a satellite role to this agent and initializes planner
     */
    @Override
    protected void activate(){
        // request role as a satellite agent
        requestRole(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SATELLITE);

        // assign oneself to planner
        this.planner.setParentAgent(this);
    }

    /**
     * ask planner for initial plan to be done at t = 0
     */
    public void initPlanner(){
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

    /**
     * Satellite agent property getters
     */
    public String getName(){
        return sat.getName();
    }
    public Satellite getSat(){return sat; }
    public AgentAddress getMyAddress(){return this.satAddresses.get(this.sat);}
    public HashMap<Satellite, AgentAddress> getSatAddresses(){ return this.satAddresses; }
    public LinkedList<SimulationAction> getPlan(){ return this.plan; }
    public AgentAddress getTargetAddress(Satellite sat){
        return satAddresses.get(sat);
    }
    public AgentAddress getTargetAddress(GndStation gnd){
        return gndAddresses.get(gnd);
    }

    /**
     * Returns the target satellite from a given agent address
     * @param address : agent address to be searched
     * @return orekit sat object if agent address matches an existing sat. Returns null otherwise
     */
    public Satellite getTargetSatFromAddress(AgentAddress address){
        for(Satellite sat : satAddresses.keySet()){
            if(satAddresses.get(sat).equals(address)) return sat;
        }
        return null;
    }

    /**
     * Returns the target ground station from a given agent address
     * @param address : agent address to be searched
     * @return orekit ground station object if agent address matches an existing sat. Returns
     * null otherwise
     */
    public GndStation getTargetGndFromAddress(AgentAddress address){
        for(GndStation gnd : gndAddresses.keySet()){
            if(gndAddresses.get(gnd).equals(address)) return gnd;
        }
        return null;
    }

    /**
     * Reads incoming messages and selects those coming from ground stations. Packages them to
     * the requestMessages property to later be given to to the planner
     * @throws Exception : Throws an exception if a ground station sent a message of a type it
     * is not meant to handle
     */
    protected void readGndStationMessages() throws Exception {
        List<Message> gndMessages = nextMessages(new GndFilter());
        for (Message message : gndMessages) {
            if (MeasurementRequestMessage.class.equals(message.getClass())) {
                MeasurementRequestMessage reqMessage = (MeasurementRequestMessage) message;

                // check if this task announcement has already been made by this satellite
                if (reqMessage.receivedBy(this.getMyAddress())) continue;

                // if not, add to received messages
                reqMessage.addReceiver(this.getMyAddress());
                requestMessages.add(reqMessage);
            } else {
                throw new Exception("Received message of type "
                        + message.getClass().toString() + " from ground station. " +
                        "Message handling of this type is not yet supported");
            }
        }
    }

    /**
     * Logs messages sent to terminal
     * @param targetAddress : address of target agent
     * @param message : message being sent to target
     */
    protected void logMessageSent(AgentAddress targetAddress, Message message){
        String targetName;
        if(this.getTargetSatFromAddress(targetAddress) == null){
            GndStation targetGnd = this.getTargetGndFromAddress(targetAddress);
            targetName = targetGnd.getBaseFrame().getName();
        }
        else{
            Satellite targetSat = this.getTargetSatFromAddress(targetAddress);
            targetName = targetSat.getName();
        }

        String messageType = "N/A";
        if (RelayMessage.class.equals(message.getClass())) {
            messageType = "Relay";
        }
        else if (MeasurementRequestMessage.class.equals(message.getClass())) {
            messageType = "Measurement Request";
        }
        else if (MeasurementMessage.class.equals(message.getClass())) {
            messageType = "Measurement Result";
        }

        getLogger().fine("\tSending  " + targetName + " message of type " + messageType + "...");
    }

    /**
     * Logs measurement action being performed to terminal
     * @param action : measurement action to be done
     */
    protected void logMeasurementMade(MeasurementAction action){
        String targetName = action.getTarget().getName();
        String insName = action.getInstrument().getName();

        getLogger().fine("\tSensing point " + targetName + " with instrument "
                + insName + " at "  + environment.getCurrentDate().toString() + "...");
    }

    /**
     * Logs maneuver action being performed to terminal
     * @param action : maneuver action to be done
     */
    protected void logAttitudeMade(ManeuverAction action){
        double th0 = Math.toDegrees( action.getInitialRollAngle() );
        double thf = Math.toDegrees( action.getInitialRollAngle() );

        getLogger().fine("\tPerforming step of maneuver from " + th0 + " [deg] to " + thf + " [deg]");
    }

    /**
     * Reads incoming messages and selects those coming from satellites. Packages them to
     * the requestMessages, relayMessages, and plannerMessages properties to later be given
     * to the planner.
     * @throws Exception : Throws an exception if a satellite receives a message of a type it
     * is not meant to handle
     */
    abstract protected void readSatelliteMessages() throws Exception;

    /**
     * Empties out processed messages from inbox
     */
    protected void emptyMessages(){
        relayMessages = new ArrayList<>();
        requestMessages = new ArrayList<>();
        plannerMessages = new ArrayList<>();
    }

    public AbsoluteDate getCurrentDate(){
        return environment.getCurrentDate();
    }

    public AbsoluteDate getStartDate(){
        return environment.getStartDate();
    }

    public  AbsoluteDate getEndDate(){
        return environment.getEndDate();
    }

    public GndStation getNextGndAccess(){
        return getNextGndAccess(environment.getCurrentDate());
    }

    private GndStation getNextGndAccess(AbsoluteDate currDate){
        HashMap<GndStation, AbsoluteDate> nextAccessTimes = new HashMap<>();
        double t_curr = currDate.durationFrom( environment.getStartDate() );

        for(GndStation gnd : accessGS.keySet()){
            RiseSetTime earliest = null;
            for(RiseSetTime t : accessGS.get(gnd).getRiseSetTimes()){
                if(!t.isRise()) continue;

                if(t_curr < t.getTime()){
                    earliest = t;
                    break;
                }
            }
            nextAccessTimes.put(gnd, environment.getStartDate().shiftedBy(earliest.getTime()));
        }

        GndStation gndEarliest = null;
        for(GndStation gnd : nextAccessTimes.keySet()){
            if(gndEarliest == null) {
                gndEarliest = gnd;
                continue;
            }

            AbsoluteDate dateEarliest = nextAccessTimes.get(gndEarliest);
            AbsoluteDate date = nextAccessTimes.get(gnd);
            if(dateEarliest.compareTo(date) > 0) dateEarliest = date;
        }

        return gndEarliest;
    }

    public ArrayList<GPAccess> orderGPAccesses(){
        ArrayList<GPAccess> unordered = new ArrayList<>();
        ArrayList<GPAccess> ordered = new ArrayList<>();

        for(Instrument ins : accessGPInst.keySet()){
            for(CoverageDefinition covDef : accessGPInst.get(ins).keySet()) {
                for (TopocentricFrame point : accessGPInst.get(ins).get(covDef).keySet()) {
                    double t_0 = 0.0;
                    double t_f = 0.0;

                    for (RiseSetTime setTime : accessGPInst.get(ins).get(covDef).get(point).getRiseSetTimes()) {
                        if (setTime.isRise()) {
                            t_0 = setTime.getTime();
                        } else {
                            t_f = setTime.getTime();

                            AbsoluteDate startDate = this.getStartDate().shiftedBy(t_0);
                            AbsoluteDate endDate = this.getStartDate().shiftedBy(t_f);

                            unordered.add(new GPAccess(this.sat, covDef, point, ins, startDate, endDate));
                        }
                    }
                }
            }
        }

        for(GPAccess acc : unordered){
            if(ordered.size() == 0){
                ordered.add(acc);
                continue;
            }

            int i = 0;
            for(GPAccess accOrd : ordered){
                if(acc.getStartDate().compareTo(accOrd.getStartDate()) <= 0) break;
                i++;
            }
            ordered.add(i,acc);
        }

        return ordered;
    }

    public ArrayList<GndAccess> orderGndAccesses(){
        ArrayList<GndAccess> unordered = new ArrayList<>();
        ArrayList<GndAccess> ordered = new ArrayList<>();

        for(GndStation gnd : accessGS.keySet()){
            double t_0 = 0.0;
            double t_f = 0.0;

            for(RiseSetTime setTime : accessGS.get(gnd).getRiseSetTimes()){
                if(setTime.isRise()) {
                    t_0 = setTime.getTime();
                }
                else {
                    t_f = setTime.getTime();

                    AbsoluteDate startDate = this.getStartDate().shiftedBy(t_0);
                    AbsoluteDate endDate = this.getStartDate().shiftedBy(t_f);

                    unordered.add(new GndAccess(this.sat, gnd, startDate, endDate));
                }
            }
        }

        for(GndAccess acc : unordered){
            if(ordered.size() == 0){
                ordered.add(acc);
                continue;
            }

            int i = 0;
            for(GndAccess accOrd : ordered){
                if(acc.getStartDate().compareTo(accOrd.getStartDate()) <= 0) break;
                i++;
            }
            ordered.add(i,acc);
        }

        return ordered;
    }

    public ArrayList<CLAccess> orderCLAccesses(Satellite sender, Satellite receiver) throws Exception {
        return environment.getOrbitData().orderCLAccesses(sender,receiver);
    }

    public boolean isCommsSat(Satellite sat){
        return environment.getOrbitData().isCommsSat(sat);
    }
}
