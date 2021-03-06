package modules.agents;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MeasurementAction;
import modules.actions.MessageAction;
import modules.actions.NominalAction;
import modules.actions.UrgentAction;
import modules.measurements.Measurement;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.messages.MeasurementRequestMessage;
import modules.messages.RelayMessage;
import modules.messages.PlannerMessage;
import modules.messages.filters.SatFilter;
import modules.planner.AbstractPlanner;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;

import java.util.HashMap;
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

    public SensingSatellite(Constellation cons, Satellite sat, OrbitData orbitData,
                            AbstractPlanner planner, SimGroups myGroups, Level loggerLevel) {
        super(cons, sat, orbitData, planner, myGroups, loggerLevel);
    }

    /**
     * Reads messages from other satellites or ground stations. Performs measurements if specified by plan.
     */
    @Override
    public void sense() throws Exception {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am sensing...");

        // read and package incoming messages to be given to planner
            // -read messages from ground stations
            readGndStationMessages();

            // -read messages from satellites
            readSatelliteMessages();

        // make a measurement if stated in planner

            while(!plan.isEmpty()
                    && plan.getFirst().getClass().equals(MeasurementAction.class)
                    && environment.getCurrentDate().compareTo(plan.getFirst().getStartDate()) >= 0
                    && environment.getCurrentDate().compareTo(plan.getFirst().getEndDate()) <= 0){

                // -perform measurement
                MeasurementAction action = (MeasurementAction) plan.poll();
                assert action != null;

                Measurement measurement = performMeasurement(action);

                measurementsDone.add(measurement);
                measurementsPendingDownload.add(measurement);

                // -check if a new urgent task was detected
                requestsDetected = environment.checkDetection();
            }
    }

    public Measurement performMeasurement(MeasurementAction action){
        MeasurementRequest request = action.getRequest();
        Instrument instrument = action.getInstrument();

        // obtain measurement performance
        HashMap<Requirement, RequirementPerformance> performance = environment.calculatePerformance(this, instrument, request);

        // obtain measurement science/utility score
        double utility = planner.calcUtility(request, performance);

        return new Measurement(this, request, performance, environment.getCurrentDate(),utility);
    }

    /**
     * Gives new information from messages or measurements to planner and crates/modifies plan if needed
     */
    @Override
    public void think() throws Exception {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am thinking...");
    }

    /**
     * Performs attitude maneuvers or sends messages to other satellites or ground stations if specified by plan
     */
    @Override
    public void execute() throws Exception {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am executing...");
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
                RelayMessage relayMessage = (RelayMessage) message;
                relayMessages.add(relayMessage);

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
