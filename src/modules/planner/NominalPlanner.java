package modules.planner;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MeasurementAction;
import modules.actions.MessageAction;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.instruments.SimulationInstrument;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.messages.MeasurementMessage;
import modules.messages.MeasurementRequestMessage;
import modules.orbitData.GPAccess;
import modules.orbitData.GndAccess;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class NominalPlanner extends AbstractPlanner {

    public static final double NominalUtility = 10.0;

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

            String type = ((SimulationInstrument) ins).getNominalMeasurementType();
            CoverageDefinition targetCovDef = access.getTargetCovDef();
            TopocentricFrame target = access.getTarget();
            AbsoluteDate startDate = access.getStartDate();
            AbsoluteDate endDate = access.getEndDate();

            measurementActions.add( new MeasurementAction(parentAgent, targetCovDef, target, ins, type,
                    startDate, endDate, null) );
        }

        // Create a message action for each pass over a ground station
        ArrayList<GndAccess> orderedGndAccess = parentAgent.getOrderedGndAccesses();
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

        // merge all plans and order chronologically
        this.plan = mergePlans(measurementActions, messageActions);

        LinkedList<SimulationAction> outActions = getAvailableActions(parentAgent.getStartDate());
        return outActions;
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<Message>> messageMap,
                                                 SatelliteAgent agent, AbsoluteDate currentDate) throws Exception {
        // updates list of known requests
        this.knownRequests.addAll( readRequestMessages(messageMap) );
        this.activeRequests = checkActiveRequests(currentDate);

        // return actions to be performed at this time
        LinkedList<SimulationAction> outActions = getAvailableActions(currentDate);

        // remove performed actions from plan
        this.plan.removeAll(outActions);
        return outActions;
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
        if(request == null) return NominalUtility;
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
