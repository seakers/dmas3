package modules.agents;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MessageAction;
import modules.messages.RelayMessage;
import modules.messages.MeasurementRequestMessage;
import modules.messages.filters.GndFilter;
import modules.messages.filters.SatFilter;
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

    /**
     * Message Inboxes of different types. One for relay messages and one for measurement
     * request messages
     */
    ArrayList<Message> relayMessages;
    ArrayList<Message> requestMessages;

    public CommsSatellite(Constellation cons, Satellite sat, OrbitData orbitData,
                          AbstractPlanner planner, SimGroups myGroups, Level loggerLevel) {
        super(cons, sat, orbitData, planner, myGroups, loggerLevel);

        // initializes inboxes
        relayMessages = new ArrayList<>();
        requestMessages = new ArrayList<>();
    }

    /**
     * Reads messages from other satellites or ground stations
     */
    @Override
    public void sense() throws Exception {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am sensing...");

        // read messages from ground stations
        List<Message> gndMessages = nextMessages( new GndFilter(gndAddresses) );
        for(Message message : gndMessages){
            if (MeasurementRequestMessage.class.equals(message.getClass())) {
                MeasurementRequestMessage reqMessage = (MeasurementRequestMessage) message;

                // check if this task announcement has already been made by this satellite
                if(reqMessage.receivedBy( this.getMyAddress() )) continue;

                // if not, add to received messages
                reqMessage.addReceiver( this.getMyAddress() );
                requestMessages.add(reqMessage);
            }
            else {
                throw new Exception("Received message of type "
                        + message.getClass().toString() + " not yet supported");
            }
        }

        // read messages from satellites
        List<Message> satMessages = nextMessages( new SatFilter(satAddresses) );
        for(Message message : satMessages){
            if (RelayMessage.class.equals(message.getClass())) {
                RelayMessage relayMessage = (RelayMessage) message;
                relayMessages.add(relayMessage);
            }
            else if (MeasurementRequestMessage.class.equals(message.getClass())) {
                MeasurementRequestMessage reqMessage = (MeasurementRequestMessage) message;

                // check if this task announcement has already been made by this satellite
                if(reqMessage.receivedBy( this.getMyAddress() )) continue;

                // if not, add to received messages
                reqMessage.addReceiver( this.getMyAddress() );
                requestMessages.add(reqMessage);
            }
            else {
                throw new Exception("Received message of type "
                        + message.getClass().toString() + " not yet supported");
            }
        }
    }

    /**
     * Gives new information from messages to planner and crates/modifies plan if needed
     */
    @Override
    public void think() throws Exception {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am thinking...");

        // package received messages and send to planner
        HashMap<String, ArrayList<Message>> messages = new HashMap<>();
        messages.put(MeasurementRequestMessage.class.toString(), requestMessages);
        messages.put(RelayMessage.class.toString(), relayMessages);

        // update plan
        this.plan = this.planner.makePlan(messages, this);

        // empty planner message arrays
        emptyMessages();
    }

    /**
     * Sends messages to other satellites or ground stations if specified by plan
     */
    @Override
    public void execute() throws Exception {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am executing...");

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

            // log to terminal
            logMessageSent(targetAddress, message);
        }

    }

    /**
     * Logs messages sent to terminal
     * @param targetAddress : address of target agent
     * @param message : message being sent to target
     */
    private void logMessageSent(AgentAddress targetAddress, Message message){
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

        getLogger().fine("\tSending  " + targetName + " message of type " + messageType + "...");
    }

    /**
     * Empties out processed messages from inbox
     */
    private void emptyMessages(){
        relayMessages = new ArrayList<>();
        requestMessages = new ArrayList<>();
    }
}
