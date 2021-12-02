package modules.messages;

import madkit.kernel.AgentAddress;
import modules.measurements.MeasurementRequest;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class MeasurementRequestMessage extends DMASMessage {
    private MeasurementRequest request;
    private AgentAddress firstReceiver;
    private ArrayList<AgentAddress> receivers;

    public MeasurementRequestMessage(MeasurementRequest request, AgentAddress originalSender, AgentAddress intendedReceiver, AbsoluteDate sendDate){
        super(sendDate, originalSender, intendedReceiver);
        this.request = request;
        this.firstReceiver = intendedReceiver;
        this.receivers = new ArrayList<>();
    }

    public MeasurementRequest getRequest(){return this.request;}
    public void addReceiver(AgentAddress address) { this.receivers.add(address); }
    public boolean receivedBy(AgentAddress address){ return this.receivers.contains(address); }
    public AgentAddress getFirstReceiver(){return firstReceiver;}

    public boolean equals(MeasurementRequestMessage message) {
        int x = 1;

        return message.getRequest().equals(request)
                && message.getFirstReceiver().equals(firstReceiver);
    }

    public MeasurementRequestMessage copy(AgentAddress target){
        return new MeasurementRequestMessage(request, originalSender,target, sendDate);
    }
}
