package modules.messages;

import madkit.kernel.AgentAddress;
import org.orekit.time.AbsoluteDate;

public class DelayTrackingMessage extends DMASMessage{
    private final DMASMessage originalMessage;
    public DelayTrackingMessage(AbsoluteDate transmissionDate, AgentAddress originalSender, AgentAddress intendedReceiver, DMASMessage originalMessage) {
        super(transmissionDate, originalSender, intendedReceiver);
        this.originalMessage = originalMessage;
    }
}
