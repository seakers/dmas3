package modules.messages.filters;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.MessageFilter;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Only accepts messages coming from ground stations
 */
public class GndFilter implements MessageFilter {
    private ArrayList<AgentAddress> addressList;

    public GndFilter(HashMap<GndStation, AgentAddress> gndAddresses){
        this.addressList = new ArrayList<>();
        for(GndStation gnd : gndAddresses.keySet()){
            AgentAddress address = gndAddresses.get(gnd);
            if(!addressList.contains(address)) addressList.add(address);
        }
    }

    @Override
    public boolean accept(Message message) {
        return addressList.contains(message.getSender());
    }
}
