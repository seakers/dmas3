package modules.messages;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;

import java.util.HashMap;

public class BookkeepingMessage extends DMASMessage {
    private final Message originalMessage;
    private final AgentAddress sender;
    private final AgentAddress receiver;

    public BookkeepingMessage(AgentAddress sender, AgentAddress receiver, AbsoluteDate sendDate, DMASMessage originalMessage) {
        super(sendDate, originalMessage.getOriginalSender(), originalMessage.getIntendedReceiver());
        this.originalMessage = originalMessage;
        this.sender = sender;
        this.receiver = receiver;
    }

    public String toString(HashMap<Satellite, AgentAddress> satAddresses, HashMap<GndStation, AgentAddress> gndAddresses, AbsoluteDate startDate) throws Exception {
        StringBuilder out = new StringBuilder();

        if(satAddresses.containsValue(this.sender) ){
            Satellite sender = null;
            for(Satellite sat : satAddresses.keySet()){
                if(satAddresses.get(sat).equals( this.sender )) {
                    sender = sat;
                    break;
                }
            }

            if(satAddresses.containsValue(this.receiver) ){
                Satellite receiver = null;
                for(Satellite sat : satAddresses.keySet()){
                    if(satAddresses.get(sat).equals( this.receiver )) {
                        receiver = sat;
                        break;
                    }
                }

                out.append(sender.getName() + "," + receiver.getName() + "," + messageType());
            }
            else if(gndAddresses.containsValue(this.receiver)){
                GndStation receiver = null;
                for(GndStation gnd : gndAddresses.keySet()){
                    if(gndAddresses.get(gnd).equals( this.receiver )) {
                        receiver = gnd;
                        break;
                    }
                }
                out.append(sender.getName() + "," + receiver.getBaseFrame().getName() + "," + messageType());
            }
            else{
                throw new Exception("Message was sent to an agentAddress not corresponding to the simulation satellites");
            }
        }
        else if(gndAddresses.containsValue(this.sender)){
            GndStation sender = null;
            for(GndStation gnd : gndAddresses.keySet()){
                if(gndAddresses.get(gnd).equals( this.sender )) {
                    sender = gnd;
                    break;
                }
            }

            if(satAddresses.containsValue(this.receiver) ){
                Satellite receiver = null;
                for(Satellite sat : satAddresses.keySet()){
                    if(satAddresses.get(sat).equals( this.receiver )) {
                        receiver = sat;
                        break;
                    }
                }

                out.append(sender.getBaseFrame().getName() + "," + receiver.getName() + "," + messageType());
            }
            else if(gndAddresses.containsValue(this.receiver)){
                GndStation receiver = null;
                for(GndStation gnd : gndAddresses.keySet()){
                    if(gndAddresses.get(gnd).equals( this.receiver )) {
                        receiver = gnd;
                        break;
                    }
                }
                out.append(sender.getBaseFrame().getName() + "," + receiver.getBaseFrame().getName() + "," + messageType());
            }
            else{
                throw new Exception("Message was sent to an agentAddress not corresponding to the simulation satellites");
            }
        }
        else{
            throw new Exception("Message was sent by an agentAddress not corresponding to the simulation satellites");
        }
        out.append("," + getSendDate() + "," + getSendDate().durationFrom(startDate));
        return out.toString();
    }

    private String messageType() throws Exception {
        if(originalMessage.getClass().equals(MeasurementMessage.class)){
            return "Measurement";
        }
        else if(originalMessage.getClass().equals(MeasurementRequestMessage.class)){
            return "Request";
        }
        else if(originalMessage.getClass().equals(PlannerMessage.class)){
            return "Planner";
        }
        else if(originalMessage.getClass().equals(RelayMessage.class)){
            return "Relay";
        }
        else{
            throw new Exception("Message type not yet supported of output");
        }
    }

    public Message getOriginalMessage() { return originalMessage; }
    public AbsoluteDate getSendDate(){ return sendDate; }
}
