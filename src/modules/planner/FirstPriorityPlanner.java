package modules.planner;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.actions.MeasurementAction;
import modules.actions.MessageAction;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.Measurement;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.messages.DMASMessage;
import modules.messages.MeasurementMessage;
import modules.orbitData.GPAccess;
import modules.orbitData.GndAccess;
import modules.orbitData.OrbitData;
import modules.simulation.Simulation;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class FirstPriorityPlanner  extends AbstractPlanner {
    private OrbitData orbitData;
    private HashMap<MeasurementRequest, MeasurementAction> urgentMeasurements;
    private ArrayList<Integer> cantReach;

    public FirstPriorityPlanner(double planningHorizon, int requestThreshold, boolean crosslinks, OrbitData orbitData) {
        super(planningHorizon, requestThreshold, crosslinks);

        this.orbitData = orbitData;
        this.urgentMeasurements = new HashMap<>();
        this.cantReach = new ArrayList<>();
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

            if(endDate.durationFrom(startDate) < 1){
                endDate = startDate.shiftedBy(1);
            }

            measurementActions.add( new MeasurementAction(parentAgent, ins, access.getTargetCovDef(),
                    access.getTarget(), "Nominal Measurement", startDate, endDate) );
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

            messageActions.add( new MessageAction(parentAgent, targetAddress,
                    message, startDate, endDate));
        }

        // merge all plans and order chronologically
        this.plan = mergePlans(measurementActions, messageActions);
//        this.plan = new ArrayList(messageActions);


        return getAvailableActions(parentAgent.getStartDate());
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<DMASMessage>> messageMap, SatelliteAgent agent, AbsoluteDate currentDate) throws Exception {
        // updates list of known requests
        ArrayList<MeasurementRequest> newRequests = readRequestMessages(messageMap);
        this.knownRequests.addAll(newRequests);
        this.activeRequests = checkAvailableRequests(newRequests, currentDate);

        // create measurement actions for urgent measurement requests
        int n_urgent = urgentMeasurements.size();
        for (MeasurementRequest req : activeRequests) {
            if (!urgentMeasurements.containsKey(req) && !cantReach.contains(req.getId()) && isFirst(req)) {
                Instrument ins = null;
                for (Instrument inst : parentAgent.getSat().getPayload()) {
                    if (inst.getName().contains("_FOR")) {
                        ins = inst;
                        break;
                    }
                }
//                ArrayList<Double> access = getEarliestAccess(parentAgent.getSat(), req);
//
//                if(access != null) {
//                    AbsoluteDate startDate = parentAgent.getStartDate().shiftedBy(access.get(0));
//                    AbsoluteDate endDate = parentAgent.getStartDate().shiftedBy(access.get(1));
//
//                    MeasurementAction action = new MeasurementAction(parentAgent, ins, req, startDate, endDate);
//                    urgentMeasurements.put(req, action);
//                }
//                else{
//                    int x = 1;
//                }
                ArrayList<ArrayList<Double>> accesses = getAccesses(parentAgent.getSat(), req);

                for(ArrayList<Double> access : accesses){
                    AbsoluteDate startDate = parentAgent.getStartDate().shiftedBy(access.get(0));
                    AbsoluteDate endDate = parentAgent.getStartDate().shiftedBy(access.get(1));

                    MeasurementAction action = new MeasurementAction(parentAgent, ins, req, startDate, endDate);
                    urgentMeasurements.put(req, action);
                }
            }
//            else if (getEarliestAccess(parentAgent.getSat(), req) == null || !isFirst(req)) {
//                if (!cantReach.contains(req.getId())) cantReach.add(req.getId());
//            }
            if (!cantReach.contains(req.getId())) cantReach.add(req.getId());
        }
        int n_urgent_new = urgentMeasurements.size();

        if (n_urgent != n_urgent_new){
            this.plan = mergePlans();
        }

        // return actions to be performed at this time
        LinkedList<SimulationAction> outActions = getAvailableActions(currentDate);

        if(outActions.size() > 0){
            int x = 1;
        }

        // remove performed actions from plan
        this.plan.removeAll(outActions);

        for(SimulationAction action : outActions){
            if(action.getClass().equals(MeasurementAction.class)) {
                if (((MeasurementAction) action).getRequest() != null ) {
                    cantReach.add(((MeasurementAction) action).getRequest().getId());
                }
                else{
                    int x = 1;
                }
            }
        }

        return outActions;
    }

    @Override
    public double calcUtility(MeasurementRequest request, HashMap<Requirement, RequirementPerformance> performance) {
        return 10*Math.random();
    }

    private boolean isFirst(MeasurementRequest req){
        return true;

//        CoverageDefinition covDef = req.getCovDef();
//        double myEarliest = -1.0;
//        double earliest = -1.0;
//
//        ArrayList<Satellite> satList = new ArrayList<>( orbitData.getSensingSats().getSatellites() );
//
//        for(Satellite sat : satList){
//            ArrayList<Double> accessArray =  getEarliestAccess(sat, req);
//            if (accessArray == null) continue;
//
//            double access = accessArray.get(0);
//
//            if(sat.getName().equals(parentAgent.getSat().getName())){
//                myEarliest = access;
//            }
//
//            if(earliest == -1.0){
//                earliest = access;
//            }
//            else if(earliest > access){
//                earliest = access;
//            }
//        }
//
//        return (myEarliest == earliest) && (myEarliest != -1.0);
    }

    /**
     * Returns the earliest time a satellite can perform measurement requests given its time availability
     * @param req
     * @return
     */
    private ArrayList<Double> getEarliestAccess(Satellite sat, MeasurementRequest req){
        ArrayList<Double> access = new ArrayList<>();
        CoverageDefinition covDef = req.getCovDef();
        for(Instrument ins : sat.getPayload()){
            if(!ins.getName().contains("_FOR")) {
                continue;
            }

            HashMap<TopocentricFrame, TimeIntervalArray> insAccess = orbitData.getAccessesGPIns().get(covDef).get(sat).get(ins);
            TopocentricFrame gp = req.getLocation();
            if(insAccess.containsKey(gp) && insAccess.get(gp).numIntervals() > 0){
                double rise = 0.0;
                double set = 0.0;

                for(RiseSetTime riseSetTime : insAccess.get(gp).getRiseSetTimes()){
                    if(riseSetTime.isRise()){
                        rise = riseSetTime.getTime();
                    }
                    else{
                        set = riseSetTime.getTime();

                        AbsoluteDate riseDate = parentAgent.getStartDate().shiftedBy(rise);
                        AbsoluteDate setDate = parentAgent.getStartDate().shiftedBy(set);

                        if( (req.getStartDate().compareTo(riseDate) >= 0 && req.getStartDate().compareTo(setDate) <= 0)
                                || (req.getEndDate().compareTo(riseDate) >= 0 && req.getEndDate().compareTo(setDate) <= 0)
                                || (req.getStartDate().compareTo(riseDate) <= 0 && req.getEndDate().compareTo(setDate) >= 0)){

                            access.add(rise);
                            access.add(set);
                            return access;
                        }
                    }
                }
            }
        }
        return null;
    }

    private ArrayList<ArrayList<Double>> getAccesses(Satellite sat, MeasurementRequest req){
        ArrayList<ArrayList<Double>> accesses = new ArrayList<>();

        CoverageDefinition covDef = req.getCovDef();
        for(Instrument ins : sat.getPayload()){
            if(!ins.getName().contains("_FOR")) {
                continue;
            }

            HashMap<TopocentricFrame, TimeIntervalArray> insAccess = orbitData.getAccessesGPIns().get(covDef).get(sat).get(ins);
            TopocentricFrame gp = req.getLocation();
            if(insAccess.containsKey(gp) && insAccess.get(gp).numIntervals() > 0){
                double rise = 0.0;
                double set = 0.0;
                ArrayList<Double> access;

                for(RiseSetTime riseSetTime : insAccess.get(gp).getRiseSetTimes()){
                    if(riseSetTime.isRise()){
                        rise = riseSetTime.getTime();
                    }
                    else{
                        set = riseSetTime.getTime();

                        AbsoluteDate riseDate = parentAgent.getStartDate().shiftedBy(rise);
                        AbsoluteDate setDate = parentAgent.getStartDate().shiftedBy(set);

                        if( (req.getStartDate().compareTo(riseDate) >= 0 && req.getStartDate().compareTo(setDate) <= 0)
                                || (req.getEndDate().compareTo(riseDate) >= 0 && req.getEndDate().compareTo(setDate) <= 0)
                                || (req.getStartDate().compareTo(riseDate) <= 0 && req.getEndDate().compareTo(setDate) >= 0)){

                            access = new ArrayList<>();
                            access.add(rise);
                            access.add(set);
                            accesses.add(access);
                        }
                    }
                }
            }
        }
        return accesses;
    }

    /**
     * Merges measurements and message actions into a single chronologically arranged plan
     * @return
     */
    private ArrayList<SimulationAction> mergePlans(){
        ArrayList<MeasurementAction> measurementActions = new ArrayList<>();
        for(MeasurementRequest req : urgentMeasurements.keySet()){
            measurementActions.add(urgentMeasurements.get(req));
        }
        ArrayList<SimulationAction> oldPlan = new ArrayList<>();
        for(SimulationAction act : plan){
            oldPlan.add(act);
        }

        ArrayList<SimulationAction> merged = new ArrayList<>();

        // arrange actions chronologically
        while(!measurementActions.isEmpty() || !oldPlan.isEmpty()) {
            if (measurementActions.isEmpty()) {
                merged.addAll(oldPlan);
                break;
            } else if (oldPlan.isEmpty()) {
                merged.addAll(measurementActions);
                break;
            } else {
                if (measurementActions.get(0).getStartDate().compareTo(oldPlan.get(0).getStartDate()) <= 0) {
                    merged.add(measurementActions.get(0));
                    measurementActions.remove(0);
                } else {
                    merged.add(oldPlan.get(0));
                    oldPlan.remove(0);
                }
            }
        }

        return merged;
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
