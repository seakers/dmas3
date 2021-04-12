package modules.messages;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;

import java.util.LinkedList;

public class RelayMessage extends Message{
    private final Message message;
    private final AgentAddress initialSender;
    private final AgentAddress intendedReceiver;
    private LinkedList<AgentAddress> path;

    public RelayMessage(Message message, AgentAddress senderAddress, LinkedList<AgentAddress> path){
        this.message = message;
        this.initialSender = senderAddress;
        this.intendedReceiver = path.getLast();
        this.path = path;
    }

    public RelayMessage copy(){
        return new RelayMessage(message, initialSender, path);
    }
    public void popPath(){ path.poll(); }

    public Message getMessage() { return message; }
    public AgentAddress getInitialSender() { return initialSender; }
    public AgentAddress getIntendedReceiver() { return intendedReceiver; }
    public LinkedList<AgentAddress> getPath() { return path; }
    public AgentAddress getNextTarget(){ return path.getFirst(); }
    public Message getMessageToRelay(){
        AgentAddress nextReceiver = path.poll();
        if(intendedReceiver.equals(nextReceiver)){
            return this.message;
        }
        return this;
    }
}
