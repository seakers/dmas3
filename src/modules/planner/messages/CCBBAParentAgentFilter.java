package modules.planner.messages;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.MessageFilter;

public class CCBBAParentAgentFilter implements MessageFilter {
    private AgentAddress parentAgentAddress;
    public CCBBAParentAgentFilter(AgentAddress plannerAddress){
        this.parentAgentAddress = plannerAddress;
    }

    @Override
    public boolean accept(Message message) {
        return message.getSender().equals(parentAgentAddress);
    }
}
