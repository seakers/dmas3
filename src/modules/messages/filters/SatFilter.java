package modules.messages.filters;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.MessageFilter;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Only accepts messages coming from satellites
 */
public class SatFilter implements MessageFilter {
    private ArrayList<AgentAddress> addressList;

    public SatFilter(HashMap<Satellite, AgentAddress> satAddresses){
        this.addressList = new ArrayList<>();
        for(Satellite sat : satAddresses.keySet()){
            AgentAddress address = satAddresses.get(sat);
            if(!addressList.contains(address)) addressList.add(address);
        }
    }

    @Override
    public boolean accept(Message message) {
        return addressList.contains(message.getSender());
    }
}
