package modules.messages;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import org.orekit.time.AbsoluteDate;

public abstract class DMASMessage extends Message {
    protected AbsoluteDate sendDate;
    protected AbsoluteDate receptionDate;
    protected final AgentAddress originalSender;
    protected final AgentAddress intendedReceiver;

    public void setSendDate(AbsoluteDate transmissionDate){
        this.sendDate = transmissionDate;
    }
    public void setReceptionDate(AbsoluteDate receptionDate){
        this.receptionDate = receptionDate;
    }
    public AbsoluteDate getSendDate(){ return sendDate; }

    public DMASMessage(AbsoluteDate transmissionDate, AgentAddress originalSender, AgentAddress intendedReceiver){
        this.sendDate = transmissionDate;
        this.originalSender = originalSender;
        this.intendedReceiver = intendedReceiver;
    }

    public AgentAddress getIntendedReceiver(){return intendedReceiver;}
    public AgentAddress getOriginalSender(){return originalSender;}
}
