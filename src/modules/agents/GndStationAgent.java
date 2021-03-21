package modules.agents;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MessageAction;
import modules.actions.SimulationAction;
import modules.environment.Environment;
import modules.measurements.Measurement;
import modules.measurements.MeasurementRequest;
import modules.messages.BookkeepingMessage;
import modules.messages.MeasurementMessage;
import modules.messages.MeasurementRequestMessage;
import modules.messages.filters.SatFilter;
import modules.orbitData.GndAccess;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;

import java.util.*;
import java.util.logging.Level;

/**
 * Ground Station Agent class
 * Represents a ground station during the simulation. It's duties involve accessing
 * satellites, announcing newly available urgent tasks, and registering measurement
 * downloads from satellites.
 *
 * @author a.aguilar
 */
public class GndStationAgent extends AbstractAgent {
    /**
     * Ground station assigned to this agent
     */
    private final GndStation gnd;

    /**
     * Coverage data
     */
    private final OrbitData orbitData;

    /**
     * Names of groups and roles within simulation community
     */
    private final SimGroups myGroups;

    /**
     * Stores the accesses of each satellite with this ground station
     */
    private final HashMap<Satellite, TimeIntervalArray> satAccesses;

    /**
     * current plan to be performed
     */
    private LinkedList<SimulationAction> plan;

    /**
     * Environment in which this agent exists in.
     */
    private Environment environment;

    /**
     * Addresses of all satellite and ground station agents present in the simulation
     */
    private HashMap<Satellite, AgentAddress> satAddresses;
    private HashMap<GndStation, AgentAddress> gndAddresses;
    protected AgentAddress envAddress;

    /**
     * Creates an instance of a ground station
     * @param gnd : ground station represented by this agent
     * @param orbitData : coverage data for everyone in the mission
     * @param myGroups : groups and communities in the simulation
     * @param loggerLevel : logger level
     */
    public GndStationAgent(GndStation gnd, OrbitData orbitData, SimGroups myGroups, Level loggerLevel){
        this.setName(gnd.getBaseFrame().getName());
        this.gnd = gnd;
        this.orbitData = orbitData;
        this.myGroups = myGroups;

        this.satAccesses = new HashMap<>();
        for(Satellite sat : orbitData.getAccessesGS().keySet()){
            TimeIntervalArray arr = orbitData.getAccessesGS().get(sat).get(gnd);
            this.satAccesses.put(sat, arr);
        }

        getLogger().setLevel(loggerLevel);
    }

    /**
     * Runs when agent is launched. Assigns role of ground station to this agent
     */
    @Override
    protected void activate(){
        requestRole(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.GNDSTAT);
        envAddress = getAgentWithRole(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.ENVIRONMENT);
    }

    /**
     * Reads messages from other satellites. Registers when these measurements were done and when they were received
     */
    public void sense() throws Exception {
        this.environment.registerMeasurements(readMeasurementMessages());
    }

    /**
     * Does nothing after initial broadcast plan has been made. Initial plan does not change throughout simulation.
     */
    public void think(){
        if(plan == null) this.plan = this.initPlan();
    }

    /**
     * Sends measurement request announcements according to initial plan.
     */
    public void execute(){
        // if the scheduled time is reached for next actions in plan, perform actions
        while(!plan.isEmpty()
        && environment.getCurrentDate().compareTo(plan.getFirst().getStartDate()) >= 0
        && environment.getCurrentDate().compareTo(plan.getFirst().getEndDate()) <= 0){

            // retrieve action and target
            MessageAction action = (MessageAction) plan.poll();
            assert action != null;
            AgentAddress targetAddress =  action.getTarget();

            // get all available tasks that can be announced
            MeasurementRequestMessage message = (MeasurementRequestMessage) action.getMessage();

            // send it to the target agent
            sendMessage(targetAddress,message);

            // send a copy of the message to sim scheduler for comms book-keeping
            AgentAddress envAddress = getAgentWithRole(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.ENVIRONMENT);
            BookkeepingMessage envMessage = new BookkeepingMessage(targetAddress, getCurrentDate(), message);
            sendMessage(envAddress,envMessage);
        }
    }

    /**
     * Reads incoming messages from satellites and extract measurement information
     * @return measurements : array containing the received messages
     */
    private ArrayList<Measurement> readMeasurementMessages() throws Exception {
        ArrayList<Measurement> measurements = new ArrayList<>();
        List<Message> messages = nextMessages(new SatFilter());

        // read every message received from a satellite
        for(Message message : messages){
            if(message.getClass().equals(MeasurementMessage.class)) {
                MeasurementMessage measurementMessage = (MeasurementMessage) message;

                // obtain measurements information from message
                ArrayList<Measurement> receivedMeasurements = measurementMessage.getMeasurements();
                for (Measurement measurement : receivedMeasurements){
                    // set download date
                    measurement.setDownloadDate(environment.getCurrentDate());
                    measurement.setGndReceiver(gnd);

                    // add to list
                    measurements.add(measurement);
                }
            }
            else{
                throw new Exception("Message of type " + message.getClass()
                        + " not yet supported for Ground Stations");
            }
        }

        return measurements;
    }

    /**
     * Initiates final plan for ground station, which consists of sending messages to accessing satellites
     * announcing urgent measurement requests.
     * @return plan : linked list of actions to be performed during execute phase
     */
    private LinkedList<SimulationAction> initPlan(){
        LinkedList<SimulationAction> plan = new LinkedList<>();
        ArrayList<GndAccess> orderedAccesses = this.orderSatAccesses();

        for(GndAccess acc : orderedAccesses){
            LinkedList<MeasurementRequest> accessAnnouncements = environment.getAvailableRequests( acc.getStartDate(), acc.getEndDate() );

            for(MeasurementRequest req : accessAnnouncements) {
                AbsoluteDate startDate;
                AbsoluteDate endDate;

                if(req.getStartDate().compareTo(acc.getStartDate()) >= 0)
                    startDate = req.getStartDate();
                else startDate = acc.getStartDate();

                if(req.getEndDate().compareTo(acc.getEndDate()) <= 0)
                    endDate = req.getEndDate();
                else endDate = acc.getEndDate();

                AgentAddress target = satAddresses.get(acc.getSat());
                MeasurementRequestMessage announcement = new MeasurementRequestMessage(req);

                MessageAction ann = new MessageAction(this, target, announcement, startDate, endDate);
                plan.add(ann);
            }
        }

        return plan;
    }

    /**
     * Collects satellite access data and orders it in chronological order
     * @return ordered : array list containing ordered accesses with all satellites
     */
    private ArrayList<GndAccess> orderSatAccesses(){
        ArrayList<GndAccess> ordered = new ArrayList();
        HashMap<Satellite, ArrayList<GndAccess>> unordered = new HashMap<>();

        for(Satellite sat : satAccesses.keySet()){
            TimeIntervalArray arr = satAccesses.get(sat);
            double t_0 = 0.0;
            double t_f = 0.0;

            ArrayList<GndAccess> accesses = new ArrayList<>();
            for(int i = 0 ; i < arr.getRiseSetTimes().size(); i++){
                RiseSetTime setTime = arr.getRiseSetTimes().get(i);

                if(setTime.isRise()) {
                    t_0 = setTime.getTime();
                }
                else {
                    t_f = setTime.getTime();

                    AbsoluteDate startDate = orbitData.getStartDate().shiftedBy(t_0);
                    AbsoluteDate endDate = orbitData.getStartDate().shiftedBy(t_f);

                    GndAccess access = new GndAccess(sat, this.gnd, startDate, endDate);
                    accesses.add(access);
                }
            }

            unordered.put(sat, accesses);
        }


        for(Satellite sat : unordered.keySet()) {
            for(GndAccess acc : unordered.get(sat)){
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
        }

        return ordered;
    }

    /**
     * Saves addresses of all agents in the simulation for future use
     * @param satAdd : map of each satellite to an address
     * @param gndAdd : map of each ground station to an address
     */
    public void registerAddresses(HashMap<Satellite, AgentAddress> satAdd, HashMap<GndStation, AgentAddress> gndAdd){
        this.satAddresses = new HashMap<>(satAdd);
        this.gndAddresses = new HashMap<>(gndAdd);
    }

    public AbsoluteDate getCurrentDate(){
        return environment.getCurrentDate();
    }

    /**
     * @return gnd : ground station represented by this agent
     */
    public GndStation getGnd(){return gnd;}
}
