package modules.messages;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.measurements.MeasurementRequest;

import java.util.ArrayList;
import java.util.LinkedList;

public class MeasurementRequestMessage extends Message {
    private LinkedList<MeasurementRequest> availableRequests;
    private ArrayList<AgentAddress> receivers;

    public MeasurementRequestMessage(LinkedList<MeasurementRequest> availableRequests){
        this.availableRequests = new LinkedList<>( availableRequests );
        this.receivers = new ArrayList<>();
    }

    public LinkedList<MeasurementRequest> getAvailableRequests(){return this.availableRequests;}
    public void addReceiver(AgentAddress address) { this.receivers.add(address); }
    public boolean receivedBy(AgentAddress address){ return this.receivers.contains(address); }
}
