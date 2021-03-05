package modules.actions;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import org.orekit.time.AbsoluteDate;

public class MessageAction extends SimulationAction {
    private final Message message;
    private final AgentAddress target;

    public MessageAction(AbstractAgent agent, AgentAddress target, Message message, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, startDate, endDate);

        this.message = message;
        this.target = target;
    }

    public Message getMessage() { return message; }
    public AgentAddress getTarget() { return target; }
}
