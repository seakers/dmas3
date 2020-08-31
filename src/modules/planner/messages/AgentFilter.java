package modules.planner.messages;

import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.simulation.SimGroups;

public class AgentFilter implements MessageFilter {
    @Override
    public boolean accept(Message message) {
        return message.getSender().getRole().equals(SimGroups.AGENT);
    }
}
