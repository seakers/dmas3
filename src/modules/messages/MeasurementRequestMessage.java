package modules.messages;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.measurements.MeasurementRequest;

import java.util.ArrayList;
import java.util.LinkedList;

public class MeasurementRequestMessage extends Message {
    private MeasurementRequest request;
    private ArrayList<AgentAddress> receivers;

    public MeasurementRequestMessage(MeasurementRequest request){
        this.request = request;
        this.receivers = new ArrayList<>();
    }

    public MeasurementRequest getRequest(){return this.request;}
    public void addReceiver(AgentAddress address) { this.receivers.add(address); }
    public boolean receivedBy(AgentAddress address){ return this.receivers.contains(address); }
}
