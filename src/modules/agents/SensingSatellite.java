package modules.agents;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.*;
import modules.measurements.Measurement;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.messages.*;
import modules.messages.filters.SatFilter;
import modules.orbitData.Attitude;
import modules.planner.AbstractPlanner;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import org.orekit.frames.TopocentricFrame;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

/**
 * Sensing Satellite Agent
 * Represents a remote sensing satellite capable of performing scientific measurements of Earth.
 * It's duties involve making measurements of its ground track during its nominal operations as
 * well as reacting to newly requested or detected urgent measurements and changing its behavior
 * to maximize the performance of the constellation.
 *
 * @author a.aguilar
 */
public class SensingSatellite extends SatelliteAgent {

    public SensingSatellite(Constellation cons, Satellite sat, OrbitData orbitData, Attitude attitude,
                            AbstractPlanner planner, SimGroups myGroups, Level loggerLevel) {
        super(cons, sat, orbitData, attitude, planner, myGroups, loggerLevel);

        measurementsDone = new ArrayList<>();
        measurementsPendingDownload = new ArrayList<>();
    }

    /**
     * Reads messages from other satellites or ground stations. Performs measurements if specified by plan.
     */
    @Override
    public void sense() throws Exception {
        getLogger().finest("\t Hello! This is " + this.getName() + ". I am sensing...");

        // read and package incoming messages to be given to planner
            // -read messages from ground stations
            readGndStationMessages();

            // -read messages from satellites
            readSatelliteMessages();
    }

    /**
     * Gives new information from messages or measurements to planner and crates/modifies plan if needed
     */
    @Override
    public void think() throws Exception {
        getLogger().finest("\t Hello! This is " + this.getName() + ". I am thinking...");

        // package received messages and send to planner
        HashMap<String, ArrayList<Message>> messages = new HashMap<>();
        messages.put(MeasurementRequestMessage.class.toString(), requestMessages);
        messages.put(RelayMessage.class.toString(), relayMessages);
        messages.put(PlannerMessage.class.toString(), plannerMessages);

        // update plan
        this.plan = this.planner.makePlan(messages, this, environment.getCurrentDate());

        // empty planner message arrays
        emptyMessages();
    }

    /**
     * Performs attitude maneuvers or sends messages to other satellites or ground stations if specified by plan
     */
    @Override
    public void execute() throws Exception {
        getLogger().finest("\t Hello! This is " + this.getName() + ". I am executing...");

        // make a measurement if stated in planner
        while(!plan.isEmpty()){

            if( environment.getCurrentDate().compareTo(plan.getFirst().getStartDate()) < 0
                    || environment.getCurrentDate().compareTo(plan.getFirst().getEndDate()) > 0){
                throw new Exception("Attempting to perform action outside its agreed schedule");
            }

            if(plan.getFirst().getClass().equals(MeasurementAction.class)){
                // -perform measurement

                // retrieve action
                MeasurementAction action = (MeasurementAction) plan.poll();
                assert action != null;

                // perform measurement
                Measurement measurement = performMeasurement(action);

                // save measurements on sat memory
                measurementsPendingDownload.add(measurement);
                measurementsDone.add(measurement);

                // log to terminal
                logMeasurementMade(action);
            }
            else if(plan.getFirst().getClass().equals(MessageAction.class)){
                // -send message to target

                // retrieve action and target
                MessageAction action = (MessageAction) plan.poll();
                assert action != null;
                AgentAddress targetAddress =  action.getTarget();

                // get all available tasks that can be announced
                Message message = action.getMessage();

                // check if message being sent is a measurement
                if(message.getClass().equals(MeasurementMessage.class)){
                    if(this.measurementsPendingDownload.isEmpty()){
                        // if no measurements have been made before this download access, then skip
                        continue;
                    }
                    else if( ((MeasurementMessage) message).getMeasurements() == null ){
                        // else package measurements into message and clear pending download list
                        ((MeasurementMessage) message).setMeasurements(this.measurementsPendingDownload);
                        this.measurementsPendingDownload = new ArrayList<>();
                    }
                }

                // send it to the target agent
                sendMessage(targetAddress,message);

                // send a copy of the message to sim scheduler for comms book-keeping
                AgentAddress envAddress = getAgentWithRole(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.ENVIRONMENT);
                BookkeepingMessage envMessage = new BookkeepingMessage(targetAddress, message);
                sendMessage(envAddress,envMessage);

                // log to terminal
                logMessageSent(targetAddress, message);
            }
            else if(plan.getFirst().getClass().equals(ManeuverAction.class)){
                // -perform roll maneuver

                // retrieve action and target
                ManeuverAction action = (ManeuverAction) plan.poll();
                assert action != null;

                // perform maneuver up to the current simulation time
                this.attitude.updateAttitude(action, environment.getCurrentDate());

                // log to terminal
                logAttitudeMade(action);
            }
            else{
                throw new Exception("Scheduled action of type "
                        + plan.getFirst().getClass() + " not yet supported.");
            }
        }
    }

    /**
     * Reads and measurement action from the planner and creates a measurement with the specified
     * instrument at the current time
     * @param action : measurement action from planner
     * @return a new object of class measurement containing the request that the measurement is
     * satisfying and its performance
     * @throws Exception
     */
    public Measurement performMeasurement(MeasurementAction action) throws Exception {
        MeasurementRequest request = action.getRequest();
        Instrument instrument = action.getInstrument();
        TopocentricFrame target = action.getTarget();

        // obtain measurement performance
        HashMap<Requirement, RequirementPerformance> performance = environment.calculatePerformance(this, instrument, target, request);

        // obtain measurement science/utility score
        double utility = planner.calcUtility(request, performance);

        return new Measurement(this, request, performance, environment.getCurrentDate(),utility);
    }

    /**
     * Reads incoming messages and selects those coming from satellites. Packages them to
     * the requestMessages, relayMessages, and plannerMessages properties to later be given
     * to the planner.
     * @throws Exception : Throws an exception if a satellite receives a message of a type it
     * is not meant to handle
     */
    @Override
    protected void readSatelliteMessages() throws Exception {
        List<Message> satMessages = nextMessages(new SatFilter());

        for(Message message : satMessages) {
            if (PlannerMessage.class.equals(message.getClass())) {
                PlannerMessage plannerMessage = (PlannerMessage) message;
                plannerMessages.add(plannerMessage);

            } else if (RelayMessage.class.equals(message.getClass())) {
                RelayMessage relayMessage = (RelayMessage) message;
                relayMessages.add(relayMessage);

            } else if (MeasurementRequestMessage.class.equals(message.getClass())) {
                MeasurementRequestMessage reqMessage = (MeasurementRequestMessage) message;

                // check if this task announcement has already been made by this satellite
                if (reqMessage.receivedBy(this.getMyAddress())) continue;

                // if not, add to received messages
                reqMessage.addReceiver(this.getMyAddress());
                requestMessages.add(reqMessage);
            } else {
                throw new Exception("Received message of type "
                        + message.getClass().toString() + " not yet supported");
            }
        }
    }
}
