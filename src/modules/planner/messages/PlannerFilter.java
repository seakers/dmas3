package modules.planner.messages;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.MessageFilter;

public class PlannerFilter implements MessageFilter {
    private AgentAddress plannerAddress;
    public PlannerFilter(AgentAddress plannerAddress){
        this.plannerAddress = plannerAddress;
    }

    @Override
    public boolean accept(Message message) {
        return message.getSender().equals(plannerAddress);
    }
}
