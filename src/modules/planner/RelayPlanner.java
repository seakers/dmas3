package modules.planner;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MessageAction;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.messages.DMASMessage;
import modules.messages.RelayMessage;
import modules.messages.MeasurementRequestMessage;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Relay Planner is a planner made for relay and communication satellites. Its role is to
 * schedule two types of actions:
 * 1) Measurement Requests - Whenever a ground station is accessed by a comms satellite,
 *                           it lets it know what measurement requests are available at
 *                           that time. The comms satellite's job is to then send this
 *                           information to other comms satellites and the sensing satellites
 *                           in the constellation.
 * 2) Relay Messages       - Whenever a satellite wants to send information either to the
 *                           ground or to another satellite, it may use communications
 *                           satellites to relay that information. If a communications
 *                           satellite receives a relay request message, it shall forward it
 *                           to its intended receiver.
 * @author a.aguilar
 */
public class RelayPlanner extends AbstractPlanner{
    public RelayPlanner(double planningHorizon, int requestThreshold) {
        super(planningHorizon, requestThreshold, true);
    }

    /**
     * Creates an initial plan at the beginning of the simulation. Since there are no messages
     * to be relayed at the beginning of the simulation, an empty plan is generated
     * @return a blank linked list of actions for the agent
     */
    @Override
    public LinkedList<SimulationAction> initPlan() {
        this.plan = new ArrayList<>();
        return new LinkedList<>();
    }

    /**
     * Reads new incoming messages and schedules new messages to be sent. Always schedules to
     * the next access the agent has with its intended target
     * @param messageMap : map of different types of messages received by an agent
     * @param agent : agent set to perform the plan
     * @return a linked list of actions to be performed by the agent
     * @throws Exception
     */
    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<DMASMessage>> messageMap,
                                                 SatelliteAgent agent, AbsoluteDate currentDate) throws Exception {

        // check for new incoming messages
        boolean empty = false;
        for(String str : messageMap.keySet()){
            if(!messageMap.get(str).isEmpty()){
                empty = true;
                break;
            }
        }

        // if new messages received, reconsider plan
        if(!empty) {
            LinkedList<SimulationAction> unordered = generateUnorderedPlan(messageMap, agent);
            LinkedList<SimulationAction> ordered = orderNewPlan(unordered);

            this.plan = mergePlans(this.plan, ordered);
        }

        // return only the actions to be performed in the current date
        return getAvailableActions(currentDate);
    }

    /**
     * Reads new incoming messages and generates an array of message actions to perform by the
     * agent. These actions are generated in the order messages are read and are thus out of
     * order chronologically.
     * @param messageMap : map of different types of messages received by an agent
     * @param agent : agent set to perform the plan
     * @return actions : array of all new actions to be performed by the agent
     * @throws Exception : throws an exception when it receives a type of message it is
     * unsuited to deal with
     */
    private LinkedList<SimulationAction> generateUnorderedPlan(HashMap<String, ArrayList<DMASMessage>> messageMap,
                                                              SatelliteAgent agent) throws Exception {
        ArrayList<SimulationAction> actions = new ArrayList<>();

        for(String str : messageMap.keySet()){
            if (MeasurementRequestMessage.class.toString().equals(str)) {
                // if measurement requests are received, send them to all satellites
                ArrayList<DMASMessage> reqMessages = messageMap.get(str);

                for(DMASMessage message : reqMessages){
                    MeasurementRequestMessage reqMessage = (MeasurementRequestMessage) message;
                    for(Satellite target : agent.getSatAddresses().keySet()){
                        if(target == agent.getSat()) continue;

                        AgentAddress targetAddress = agent.getSatAddresses().get(target);
                        MeasurementRequestMessage newReqMessage = reqMessage.copy(targetAddress);

                        ArrayList<AbsoluteDate> nextAccess = agent.getNextAccess(target);
                        AbsoluteDate startDate = nextAccess.get(0);
                        AbsoluteDate endDate = nextAccess.get(1);

                        MessageAction action = new MessageAction(agent, targetAddress, newReqMessage, startDate, endDate);
                        actions.add(action);
                    }
                }
            }
            else if (RelayMessage.class.toString().equals(str)) {
                // if relay requests are received, send them to their respective targets
                ArrayList<DMASMessage> reqMessages = messageMap.get(str);

                for(DMASMessage message : reqMessages){
                    RelayMessage relayMessage = (RelayMessage) message;

                    DMASMessage messageToSend = relayMessage.getMessageToRelay();
                    AgentAddress targetAddress = relayMessage.getNextTarget();

                    if(targetAddress == agent.getMyAddress()) throw new Exception("Relay Error. Check intended receiver");

                    ArrayList<AbsoluteDate> nextAccess = agent.getNextAccess(targetAddress);
                    AbsoluteDate startDate = nextAccess.get(0);
                    AbsoluteDate endDate = nextAccess.get(1);

                    MessageAction action = new MessageAction(agent,targetAddress, messageToSend, startDate, endDate);
                    actions.add(action);
                }
            }
            else {
                throw new Exception("Received message of type " + str + " not yet supported");
            }
        }

        return new LinkedList<>(actions);
    }

    /**
     * Orders newly generated tasks chronologically.
     * @param actions : array of unordered list of actions to be scheduled to agent
     * @return ordered : a linked list of the scheduled actions in chronological order
     */
    private LinkedList<SimulationAction> orderNewPlan(LinkedList<SimulationAction> actions){
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

    /**
     * Makes a new plan from the existing plan that is yet to be performed and from the
     * new actions scheduled from the new messages received by the agent.
     * @param oldPlan : old plan pending to be performed
     * @param newPlan : new plan from incoming messages
     * @return plan : linked list of the new plan to be performed by the agent
     */
    private ArrayList<SimulationAction> mergePlans(ArrayList<SimulationAction> oldPlan,
                                                   LinkedList<SimulationAction> newPlan){
        ArrayList<SimulationAction> plan = new ArrayList<>();

        while(!oldPlan.isEmpty() || !newPlan.isEmpty()){
            if(oldPlan.isEmpty()) {
                plan.addAll(newPlan);
                break;
            }
            else if(newPlan.isEmpty()) {
                plan.addAll(oldPlan);
                break;
            }
            else{
                if(oldPlan.get(0).getStartDate().compareTo(newPlan.getFirst().getStartDate()) <= 0){
                    plan.add(oldPlan.get(0));
                    oldPlan.remove(0);
                }
                else{
                    plan.add(newPlan.poll());
                }
            }
        }

        return plan;
    }

    @Override
    public double calcUtility(MeasurementRequest request, HashMap<Requirement,
                                RequirementPerformance> performance) {
        return -1;
    }
}
