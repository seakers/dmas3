package modules.messages.filters;

import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.simulation.SimGroups;

/**
 * Only accepts messages coming from satellites
 */
public class SatFilter implements MessageFilter {
    @Override
    public boolean accept(Message message) {
        return message.getSender().getRole().equals(SimGroups.SATELLITE);
    }
}
