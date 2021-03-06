package modules.messages.filters;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.simulation.SimGroups;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Only accepts messages coming from satellites
 */
public class SatFilter implements MessageFilter {
    @Override
    public boolean accept(Message message) {
        return message.getSender().getRole().equals(SimGroups.SATELLITE);
    }
}
