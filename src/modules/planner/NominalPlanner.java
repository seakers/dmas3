package modules.planner;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MeasurementAction;
import modules.actions.MessageAction;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.messages.MeasurementMessage;
import modules.messages.MeasurementRequestMessage;
import modules.orbitData.GPAccess;
import modules.orbitData.GndAccess;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class NominalPlanner extends AbstractPlanner {
    public NominalPlanner(double planningHorizon, int requestThreshold, boolean crosslinks) {
        super(planningHorizon, requestThreshold, crosslinks);

        this.knownRequests = new ArrayList<>();
        this.activeRequests = new ArrayList<>();
    }

    @Override
    public LinkedList<SimulationAction> initPlan() {
        // create measurement actions for each ground point access
        ArrayList<GPAccess> orderGPAccess = parentAgent.orderGPAccesses();
        ArrayList<MeasurementAction> measurementActions = new ArrayList<>(orderGPAccess.size());
        for(GPAccess access : orderGPAccess){
            Instrument ins = access.getInstrument();
            if(ins.getName().contains("_FOR")) continue;

            TopocentricFrame target = access.getTarget();
            AbsoluteDate startDate = access.getStartDate();
            AbsoluteDate endDate = access.getEndDate();

            measurementActions.add( new MeasurementAction(parentAgent, target, ins,
                    startDate, endDate, null) );
        }

        // Create a message action for each pass over a ground station
        ArrayList<GndAccess> orderedGndAccess = parentAgent.orderGndAccesses();
        ArrayList<MessageAction> messageActions = new ArrayList<>(orderedGndAccess.size());
        for(GndAccess access : orderedGndAccess){

            GndStation target = access.getGnd();
            AgentAddress targetAddress = parentAgent.getTargetAddress(target);

            MeasurementMessage message = new MeasurementMessage(null);
            AbsoluteDate startDate = access.getStartDate();
            AbsoluteDate endDate = access.getEndDate();

            messageActions.add( new MessageAction(parentAgent, targetAddress,
                    message, startDate, endDate));
        }

        this.plan = mergePlans(measurementActions, messageActions);

        LinkedList<SimulationAction> outActions = getAvailableActions(parentAgent.getStartDate());
        return outActions;
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<Message>> messageMap,
                                                 SatelliteAgent agent, AbsoluteDate currentDate) throws Exception {
        this.knownRequests.addAll( readRequestMessages(messageMap) );
        this.activeRequests = checkActiveRequests(currentDate);

        LinkedList<SimulationAction> outActions = getAvailableActions(currentDate);
        this.plan.removeAll(outActions);
        return outActions;
    }

    /**
     * Returns a list of all new measurement requests sent to the parent spacecraft
     * @param messageMap : list of messages sent to parent spacecraft
     * @return array containing all new measurement requests
     */
    private ArrayList<MeasurementRequest> readRequestMessages(HashMap<String, ArrayList<Message>> messageMap){
        ArrayList<MeasurementRequest> knownRequests = new ArrayList<>();

        for(String str : messageMap.keySet()){
            for(Message message : messageMap.get(str)){
                if(message.getClass().equals(MeasurementRequestMessage.class)){
                    MeasurementRequest request = ((MeasurementRequestMessage) message).getRequest();
                    if(!knownRequests.contains(request)) {
                        knownRequests.add(request);
                    }
                }
                else{
                    continue;
                }
            }
        }

        return knownRequests;
    }

    /**
     * Returns a list of all currently available measurement requests at a given date
     * @param date : desired date
     * @return array containing all new measurement requests
     */
    private ArrayList<MeasurementRequest> checkActiveRequests(AbsoluteDate date){
        ArrayList<MeasurementRequest> activeRequests = new ArrayList<>();

        for(MeasurementRequest request : knownRequests){
            if(date.compareTo( request.getStartDate() ) >= 0
                && date.compareTo( request.getEndDate() ) <= 0){
                activeRequests.add(request);
            }
        }

        return activeRequests;
    }

    @Override
    public double calcUtility(MeasurementRequest request, HashMap<Requirement, RequirementPerformance> performance) {
        if(request == null) return 10.0;
        return -1;
    }

    private ArrayList<SimulationAction> mergePlans(ArrayList<MeasurementAction> measurementActions, ArrayList<MessageAction> messageActions){
        ArrayList<SimulationAction> merged = new ArrayList<>();

        // arrange actions chronologically
        while(!measurementActions.isEmpty() || !messageActions.isEmpty()) {
            if (measurementActions.isEmpty()) {
                merged.addAll(messageActions);
                break;
            } else if (messageActions.isEmpty()) {
                merged.addAll(measurementActions);
                break;
            } else {
                if (measurementActions.get(0).getStartDate().compareTo(messageActions.get(0).getStartDate()) <= 0) {
                    merged.add(measurementActions.get(0));
                    measurementActions.remove(0);
                } else {
                    merged.add(messageActions.get(0));
                    messageActions.remove(0);
                }
            }
        }

        return merged;
    }
}
