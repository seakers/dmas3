package modules.messages;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import org.orekit.time.AbsoluteDate;

import java.util.LinkedList;

public class RelayMessage extends DMASMessage{
    private final DMASMessage message;
    private final AgentAddress initialSender;
    private final AgentAddress intendedReceiver;
    private LinkedList<AgentAddress> path;

    public RelayMessage(DMASMessage message, AgentAddress senderAddress, LinkedList<AgentAddress> path, AbsoluteDate sendDate){
        super(sendDate, message.getOriginalSender(), message.getIntendedReceiver());
        this.message = message;
        this.initialSender = senderAddress;
        this.intendedReceiver = path.getLast();
        this.path = path;
    }

    public RelayMessage copy(){
        return new RelayMessage(message, initialSender, path, sendDate);
    }
    public void popPath(){ path.poll(); }

    public Message getMessage() { return message; }
    public AgentAddress getInitialSender() { return initialSender; }
    public AgentAddress getIntendedReceiver() { return intendedReceiver; }
    public LinkedList<AgentAddress> getPath() { return path; }
    public AgentAddress getNextTarget(){ return path.getFirst(); }
    public DMASMessage getMessageToRelay(){
        AgentAddress nextReceiver = path.poll();
        if(intendedReceiver.equals(nextReceiver)){
            return this.message;
        }
        return this;
    }
}
