package modules.planner;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MeasurementAction;
import modules.actions.MessageAction;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.messages.DMASMessage;
import modules.messages.MeasurementMessage;
import modules.orbitData.GPAccess;
import modules.orbitData.GndAccess;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Creates plans a measurement plan based on the ground points that will be observed by the satellite in its nominal
 * mode of operations. Does not perform any urgent measurement requests.
 *
 * @author a.aguilar
 */
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

            AbsoluteDate startDate = access.getStartDate();
            AbsoluteDate endDate = access.getEndDate();

            measurementActions.add( new MeasurementAction(parentAgent,ins, null, startDate, endDate) );
        }

        // Create a message action for each pass over a ground station
        ArrayList<GndAccess> orderedGndAccess = parentAgent.getOrderedGndAccesses();
        ArrayList<MessageAction> messageActions = new ArrayList<>(orderedGndAccess.size());
        for(GndAccess access : orderedGndAccess){

            GndStation target = access.getGnd();
            AgentAddress targetAddress = parentAgent.getTargetAddress(target);

            MeasurementMessage message = new MeasurementMessage(null, null, parentAgent.getMyAddress(), targetAddress);
            AbsoluteDate startDate = access.getStartDate();
            AbsoluteDate endDate = access.getEndDate();

            messageActions.add( new MessageAction(parentAgent, targetAddress, message, startDate, endDate));
        }

        // merge all plans and order chronologically
        this.plan = mergePlans(measurementActions, messageActions);

        return getAvailableActions(parentAgent.getStartDate());
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<DMASMessage>> messageMap,
                                                 SatelliteAgent agent, AbsoluteDate currentDate) throws Exception {
        // updates list of known requests
        ArrayList<MeasurementRequest> newRequests = readRequestMessages(messageMap);
        this.knownRequests.addAll( newRequests );
        this.activeRequests = checkActiveRequests(newRequests, currentDate);

        // return actions to be performed at this time
        LinkedList<SimulationAction> outActions = getAvailableActions(currentDate);

        // remove performed actions from plan
        this.plan.removeAll(outActions);
        return outActions;
    }

    @Override
    public double calcUtility(MeasurementRequest request, HashMap<Requirement, RequirementPerformance> performance) {
        if(request == null) return NominalUtility;
        return -1;
    }

    /**
     * Merges measurements and message actions into a single chronologically arranged plan
     * @param measurementActions    : list of nominal measurement actions to be performed by satellite
     * @param messageActions        : list of possible message actions available to satellite
     * @return
     */
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
