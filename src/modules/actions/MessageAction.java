package modules.actions;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import org.orekit.time.AbsoluteDate;

public class MessageAction extends SimulationAction {
    private final Message message;
    private final AgentAddress targetAddress;

    public MessageAction(AbstractAgent agent, Message message, AgentAddress targetAddress, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, startDate, endDate);

        this.message = message;
        this.targetAddress = targetAddress;
    }
}
