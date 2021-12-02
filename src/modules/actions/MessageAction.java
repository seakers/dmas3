package modules.actions;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.messages.DMASMessage;
import org.orekit.time.AbsoluteDate;

public class MessageAction extends SimulationAction {
    private final DMASMessage message;
    private final AgentAddress target;

    public MessageAction(AbstractAgent agent, AgentAddress target, DMASMessage message, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, startDate, endDate);

        this.message = message;
        this.target = target;
    }

    public DMASMessage getMessage() { return message; }
    public AgentAddress getTarget() { return target; }
}
