package modules.planner;

import madkit.kernel.Message;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.Measurement;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import modules.messages.DMASMessage;
import modules.messages.MeasurementRequestMessage;
import modules.messages.PlannerMessage;
import modules.messages.RelayMessage;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public abstract class AbstractPlanner {
    /**
     * Types of accepted planners
     */
    public static final String NONE = "none";
    public static final String CCBBA = "CCBBA";
    public static final String RELAY = "relay";
    public static final String RULES = "ruleBased";
    public static final String FIRST_PRIORITY = "firstPriority";
    public static final String[] PLANNERS = {NONE, CCBBA, RELAY, RULES};

    /**
     * Parent agent being scheduled
     */
    protected SatelliteAgent parentAgent;

    /**
     * Planning horizon in seconds
     */
    protected final double planningHorizon;

    /**
     * threshold of new measurement requests required to trigger a plan reschedule
     */
    protected final int requestThreshold;

    /**
     * toggle that allows for planners to create schedules considering
     * inter-satellite cross links
     */
    protected final boolean crossLinks;

    /**
     * List of actions to be given to an agent to be performed
     */
    protected ArrayList<SimulationAction> plan;

    /**
     * List of known and active measurement requests
     */
    protected ArrayList<MeasurementRequest> activeRequests;

    /**
     * List of known measurement requests
     */
    protected ArrayList<MeasurementRequest> knownRequests;

    public AbstractPlanner(double planningHorizon, int requestThreshold, boolean crossLinks){
        this.planningHorizon = planningHorizon;
        this.requestThreshold = requestThreshold;
        this.crossLinks = crossLinks;

        this.knownRequests = new ArrayList<>();
        this.activeRequests = new ArrayList<>();
    }

    /**
     * Creates an initial plan at the beginning of the simulation
     * @return a linked list of actions for the agent
     */
    public abstract LinkedList<SimulationAction> initPlan();

    /**
     * Creates, modifies, or maintains the plan to be performed by an agent
     * @param messageMap : map of different types of messages received by an agent
     * @param agent : agent set to perform the plan
     * @return a linked list of actions to be performed by the agent
     * @throws Exception
     */
    public abstract LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<DMASMessage>> messageMap,
                                                          SatelliteAgent agent, AbsoluteDate currentDate) throws Exception;


    /**
     * Calculates the estimated or projected utility of performing a measurement
     * of a given performance or quality
     * @param request : measurement request being answered
     * @param performance : performance of the measurement being made
     * @return utility of the measurement
     */
    public abstract double calcUtility(MeasurementRequest request,
                                       HashMap<Requirement, RequirementPerformance> performance);

    /**
     * Assigns planner to its parent agent
     * @param agent
     */
    public void setParentAgent(SatelliteAgent agent){ this.parentAgent = agent; }

    /**
     * Returns list of actions available to be performed at a given date
     * @param date  : date to be checked
     * @return      : list of available actions
     */
    protected LinkedList<SimulationAction> getAvailableActions(AbsoluteDate date){
        LinkedList<SimulationAction> actions = new LinkedList<>();

        for(SimulationAction action : this.plan){
            if(date.compareTo(action.getEndDate()) > 0){
                break;
            }
            else if( date.compareTo(action.getStartDate()) >= 0){
                actions.add(action);
            }
        }

        return actions;
    }

    /**
     * Returns list of actions that can no longer be performed by the agent as their date has passed
     * @param date
     * @return
     */
    protected LinkedList<SimulationAction> getOutdatedActions(AbsoluteDate date){
        LinkedList<SimulationAction> actions = new LinkedList<>();

        for(SimulationAction action : this.plan){
            if( date.compareTo(action.getEndDate()) > 0 ){
                actions.add(action);
            }
        }

        return actions;
    }

    /**
     * Returns a list of all NEW measurement requests sent to the parent spacecraft
     * @param messageMap : list of messages sent to parent spacecraft
     * @return array containing all new measurement requests
     */
    protected ArrayList<MeasurementRequest> readRequestMessages(HashMap<String, ArrayList<DMASMessage>> messageMap){
        ArrayList<MeasurementRequest> knownRequests = new ArrayList<>();

        for(String str : messageMap.keySet()){
            for(Message message : messageMap.get(str)){
                if(message.getClass().equals(MeasurementRequestMessage.class)){
                    MeasurementRequest request = ((MeasurementRequestMessage) message).getRequest();
                    if(!this.knownRequests.contains(request) && !knownRequests.contains(request)) {
                        knownRequests.add(request);
                    }
                }
            }
        }

        return knownRequests;
    }

    /**
     * Returns a list of all new messages to be relayed by this spacecraft
     * @param messageMap : list of messages sent to parent spacecraft
     * @return array containing all new relay requests
     */
    protected ArrayList<RelayMessage> readRelayMessages(HashMap<String, ArrayList<DMASMessage>> messageMap){
        ArrayList<RelayMessage> messages = new ArrayList<>();

        for(String str : messageMap.keySet()){
            for(Message message : messageMap.get(str)){
                if(message.getClass().equals(RelayMessage.class)){
                    RelayMessage relay = ((RelayMessage) message);
                    messages.add(relay);
                }
            }
        }

        return messages;
    }

    /**
     * Returns a list of all new messages to be relayed by this spacecraft
     * @param messageMap : list of messages sent to parent spacecraft
     * @return array containing all new relay requests
     */
    protected ArrayList<PlannerMessage> readPlannerMessages(HashMap<String, ArrayList<DMASMessage>> messageMap){
        ArrayList<PlannerMessage> messages = new ArrayList<>();

        for(String str : messageMap.keySet()){
            for(Message message : messageMap.get(str)){
                if(message.getClass().equals(PlannerMessage.class)){
                    PlannerMessage relay = ((PlannerMessage) message);
                    messages.add(relay);
                }
            }
        }

        return messages;
    }

    /**
     * Returns a list of all currently available measurement requests at a given date
     * @param date : desired date
     * @return array containing all new measurement requests
     */
    protected ArrayList<MeasurementRequest> checkActiveRequests(ArrayList<MeasurementRequest> requests, AbsoluteDate date){
        ArrayList<MeasurementRequest> activeRequests = new ArrayList<>();

        for(MeasurementRequest request : activeRequests){
            if(date.compareTo( request.getStartDate() ) >= 0
                    && date.compareTo( request.getEndDate() ) <= 0){
                activeRequests.add(request);
            }
        }

        for(MeasurementRequest request : requests){
            if(date.compareTo( request.getStartDate() ) >= 0
                    && date.compareTo( request.getEndDate() ) <= 0){
                activeRequests.add(request);
            }
        }

        return activeRequests;
    }

    /**
     * Returns a list of all currently available measurement requests at a given date
     * @param date : desired date
     * @return array containing all new measurement requests
     */
    protected ArrayList<MeasurementRequest> checkAvailableRequests(ArrayList<MeasurementRequest> requests, AbsoluteDate date){
        ArrayList<MeasurementRequest> availableRequests = new ArrayList<>();

        for(MeasurementRequest request : activeRequests){
            if(date.compareTo( request.getEndDate() ) <= 0){
                availableRequests.add(request);
            }
        }

        for(MeasurementRequest request : requests){
            if(date.compareTo( request.getEndDate() ) <= 0){
                availableRequests.add(request);
            }
        }

        return availableRequests;
    }
}
