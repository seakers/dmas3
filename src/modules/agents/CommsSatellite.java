package modules.agents;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MessageAction;
import modules.messages.RelayMessage;
import modules.messages.MeasurementRequestMessage;
import modules.messages.filters.GndFilter;
import modules.messages.filters.SatFilter;
import modules.orbitData.Attitude;
import modules.planner.AbstractPlanner;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Communications Satellite Agent
 * Represents a communications satellite in the simulation. It's duties involve relaying
 * information between satellites and informing satellites of newly requested measurements
 * by ground stations
 *
 * @author a.aguilar
 */
public class CommsSatellite extends SatelliteAgent {

    public CommsSatellite(Constellation cons, Satellite sat, OrbitData orbitData, Attitude attitude,
                          AbstractPlanner planner, SimGroups myGroups, Level loggerLevel) {
        super(cons, sat, orbitData, attitude, planner, myGroups, loggerLevel);
    }

    /**
     * Reads messages from other satellites or ground stations
     */
    @Override
    public void sense() throws Exception {
        getLogger().finest("\t Hello! This is " + this.getName() + ". I am sensing...");

        // read messages from ground stations
        readGndStationMessages();

        // read messages from satellites
        readSatelliteMessages();
    }

    /**
     * Gives new information from messages to planner and crates/modifies plan if needed
     */
    @Override
    public void think() throws Exception {
        getLogger().finest("\t Hello! This is " + this.getName() + ". I am thinking...");

        // package received messages and send to planner
        HashMap<String, ArrayList<Message>> messages = new HashMap<>();
        messages.put(MeasurementRequestMessage.class.toString(), requestMessages);
        messages.put(RelayMessage.class.toString(), relayMessages);

        // update plan
        this.plan = this.planner.makePlan(messages, this, environment.getCurrentDate());

        // empty planner message arrays
        emptyMessages();
    }

    /**
     * Sends messages to other satellites or ground stations if specified by plan
     */
    @Override
    public void execute() throws Exception {
        getLogger().finest("\t Hello! This is " + this.getName() + ". I am executing...");

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

            // send a copy of the message to environment for comms book-keeping
            sendMessage(envAddress,message);

            // log to terminal
            logMessageSent(targetAddress, message);
        }
    }

    /**
     * Reads incoming messages and selects those coming from satellites. Packages them to
     * the requestMessages and relayMessages properties to later be given to the planner.
     * @throws Exception : Throws an exception if a satellite receives a message of a type it
     * is not meant to handle
     */
    protected void readSatelliteMessages() throws Exception {
        List<Message> satMessages = nextMessages(new SatFilter());

        for(Message message : satMessages) {
            if (RelayMessage.class.equals(message.getClass())) {
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
