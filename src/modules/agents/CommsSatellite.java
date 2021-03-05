package modules.agents;

import madkit.kernel.Message;
import modules.messages.RelayMessage;
import modules.messages.MeasurementRequestMessage;
import modules.messages.filters.GndFilter;
import modules.messages.filters.SatFilter;
import modules.planner.AbstractPlanner;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommsSatellite extends SatelliteAgent {

    ArrayList<Message> relayMessages;
    ArrayList<Message> requestMessages;

    public CommsSatellite(Constellation cons, Satellite sat, OrbitData orbitData, AbstractPlanner planner, SimGroups myGroups) {
        super(cons, sat, orbitData, planner, myGroups);

        relayMessages = new ArrayList<>();
        requestMessages = new ArrayList<>();
    }

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
                throw new Exception("Received message of type " + message.getClass().toString() + " not yet supported");
            }
        }

        // read messages from satellites
        List<Message> satMessages = nextMessages( new SatFilter(satAddresses) );
        for(Message message : satMessages){
            if (RelayMessage.class.equals(message.getClass())) {
                RelayMessage relayMessage = (RelayMessage) message;
                relayMessages.add(message);
            }
            else {
                throw new Exception("Received message of type " + message.getClass().toString() + " not yet supported");
            }
        }
    }

    @Override
    public void think() throws Exception {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am thinking...");

        // package received messages and send to planner
        HashMap<String, ArrayList<Message>> messages = new HashMap<>();
        messages.put(MeasurementRequestMessage.class.toString(), requestMessages);
        messages.put(RelayMessage.class.toString(), relayMessages);

        this.plan = this.planner.makePlan(messages, this);

    }

    @Override
    public void execute() throws Exception {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am executing...");
    }
}
