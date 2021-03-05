package modules.planner;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.AnnouncementAction;
import modules.actions.MessageAction;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.messages.RelayMessage;
import modules.messages.MeasurementRequestMessage;
import modules.simulation.Simulation;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class RelayPlanner extends AbstractPlanner{
    @Override
    public LinkedList<SimulationAction> initPlan() {
        return new LinkedList<>();
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<Message>> messageMap, SatelliteAgent agent) throws Exception {
        ArrayList<SimulationAction> unordered = generateUnorderedPlan(messageMap, agent);
        LinkedList<SimulationAction> ordered = orderNewPlan(unordered);
        return mergePlans(agent.getPlan(), ordered);
    }

    private ArrayList<SimulationAction> generateUnorderedPlan(HashMap<String, ArrayList<Message>> messageMap, SatelliteAgent agent) throws Exception {
        ArrayList<SimulationAction> actions = new ArrayList<>();

        for(String str : messageMap.keySet()){
            if (MeasurementRequestMessage.class.equals(str)) {
                // if measurement requests are received, send them to all satellites
                ArrayList<Message> reqMessages = messageMap.get(str);

                for(Message message : reqMessages){
                    MeasurementRequestMessage reqMessage = (MeasurementRequestMessage) message;
                    for(Satellite target : agent.getSatAddresses().keySet()){
                        if(target == agent.getSat()) continue;

                        ArrayList<AbsoluteDate> nextAccess = agent.getNextAccess(target);
                        AbsoluteDate startDate = nextAccess.get(0);
                        AbsoluteDate endDate = nextAccess.get(1);

                        AnnouncementAction action = new AnnouncementAction(agent, target, reqMessage, startDate, endDate);
                        actions.add(action);
                    }
                }
            }
            else if (RelayMessage.class.equals(str)) {
                // if relay requests are received, send them to their respective targets
                ArrayList<Message> reqMessages = messageMap.get(str);

                for(Message message : reqMessages){
                    RelayMessage relayMessage = (RelayMessage) message;

                    AgentAddress targetAddress = relayMessage.getNextTarget();
                    Message messageToSend = relayMessage.getMessageToRelay();

                    if(targetAddress == agent.getMyAddress()) throw new Exception("Relay Error. Check intended receiver");

                    ArrayList<AbsoluteDate> nextAccess = agent.getNextAccess(targetAddress);
                    AbsoluteDate startDate = nextAccess.get(0);
                    AbsoluteDate endDate = nextAccess.get(1);

                    MessageAction action = new MessageAction(agent, messageToSend, targetAddress, startDate, endDate);
                    actions.add(action);
                }
            }
            else {
                throw new Exception("Received message of type " + str + " not yet supported");
            }
        }

        return actions;
    }

    private LinkedList<SimulationAction> orderNewPlan(ArrayList<SimulationAction> actions){
        ArrayList<SimulationAction> ordered = new ArrayList<>();

        for(SimulationAction action : actions){
            if(ordered.isEmpty()) {
                ordered.add(action);
                continue;
            }

            int i = 0;
            for(SimulationAction orderedAction : ordered){
                if(action.getStartDate().compareTo(orderedAction.getStartDate()) <= 0){
                    ordered.add(i,action);
                    break;
                }

                i++;
            }
        }

        return new LinkedList<>(ordered);
    }

    private LinkedList<SimulationAction> mergePlans(LinkedList<SimulationAction> oldPlan, LinkedList<SimulationAction> newPlan){
        return null;
    }
}
