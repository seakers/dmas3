package modules.spacecraft;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.environment.*;
import modules.planner.plans.*;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.measurements.Measurement;
import modules.spacecraft.instrument.measurements.MeasurementPerformance;
import modules.spacecraft.orbits.OrbitParams;
import modules.spacecraft.orbits.SpacecraftOrbit;
import modules.planner.CCBBA.CCBBAPlanner;
import modules.planner.Planner;
import modules.simulation.SimGroups;
import modules.planner.messages.*;
import modules.spacecraft.orbits.TimeInterval;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class Spacecraft extends AbstractAgent {
    private String name;                // Satellite name
    private SpacecraftDesign design;    // Design properties
    private OrbitParams orbitParams;    // Orbital Parameters
    private SpacecraftOrbit orbit;      // Orbital Trajectory
    private Planner planner;            // Planner Properties
    private AgentAddress plannerAddress;// Messaging address to planner
    private Environment environment;    // world environment
    private Plan plan;                  // current planned task
    private List<Message> receivedMessages;//list of received messages from other agents
    private ArrayList<MeasurementPlan> measurementsMade;
    private boolean planInQueue;

    public Spacecraft(String name, ArrayList<Instrument> payload, OrbitParams orbitParams, String planner) throws Exception {
        this.name = name;
        this.orbitParams = orbitParams;
        this.design = new SpacecraftDesign(payload);
        getLogger().setLevel(Level.FINER);
        switch(planner){
            case "CCBBA":
                this.planner = new CCBBAPlanner(this);
                break;
            case "PREDET":
                this.planner = null;
                throw new Exception("Planner type not yet supported");
            default:
                throw new Exception("Planner type not yet supported");
        }
    }

    @Override
    protected void activate(){
        //0- Request agent role
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_SENSE);

        try {
            //1- Propagate spacecraft orbit
            this.orbit = new SpacecraftOrbit(orbitParams, this.design.getPayload(), environment);
            this.orbit.propagateOrbit();

            //2- Calculate Task Access Times
//            this.orbit.calcAccessTimes(environment);
            this.orbit.calcLoSTimes(this,environment);

            //3- Design Spacecraft
            this.design.designSpacecraft(this.orbit);

            //4- Launch Planner
            this.planner.setParentAgentAddress(this.getAgentAddressIn(SimGroups.MY_COMMUNITY,SimGroups.SIMU_GROUP,SimGroups.AGENT));
            launchAgent(this.planner);
            this.plannerAddress = this.planner.getAgentAddressIn(SimGroups.MY_COMMUNITY,SimGroups.SIMU_GROUP,SimGroups.PLANNER);
            this.measurementsMade = new ArrayList<>();
            this.plan = null;

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void sense(){
        // get input from environment
        senseEnvironment();

        // read messages from other agents
        readMessages();

        // give sensed input to planner
        sendMessagesToPlanner();

        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_SENSE);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK);
    }

    public void think() throws Exception {
        // check if planner has delivered a plan
        ArrayList<PlannerMessage> plannerMessages = readMessagesFromPlanner();

        if(plannerMessages.size() == 0){
            if (this.plan == null) {
                // if no plan given by planner and no plan being done, then wait for new plan
                this.plan = new WaitPlan(this.environment.getCurrentDate(),
                        this.getCurrentDate().shiftedBy(this.environment.getTimeStep()));
            }
        }

        for(PlannerMessage plannerMessage : plannerMessages) {
            this.plan = plannerMessage.getPlan();

            String planClass = this.plan.getClass().toString();
            if (planClass.equals(PlanNames.BROADCAST)) {
                // broadcast message from plan
                CCBBAResultsMessage message = (CCBBAResultsMessage) ((BroadcastPlan) this.plan).getBroadcastMessage();
                broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT, message);
            } else if (planClass.equals(PlanNames.DIE)) {
                // adopt death role, this will be used by planner to only wait until the simulation ends

                // if everyone's dead, send messages to environment
                List<AgentAddress> otherAgents = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP,SimGroups.PLANNER);
                List<AgentAddress> otherAgentsDead = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP,SimGroups.PLANNER_DIE);
                int n_agents = otherAgents.size();
                int n_agents_dead = 0;
                if(otherAgentsDead != null) n_agents_dead = otherAgentsDead.size();

                if (n_agents == n_agents_dead) {
                    CCBBAResultsMessage message = (CCBBAResultsMessage) ((DiePlan) this.plan).getBroadcastMessage();
                    environment.addResult(message);
                }
            } else if (planClass.equals(PlanNames.MANEUVER)) {
                performManeuver();
            } else if (planClass.equals(PlanNames.MEASURE)) {
                makeMeasurement();
                this.measurementsMade.add((MeasurementPlan) this.plan.copy());
            } else if (planClass.equals(PlanNames.WAIT)) {
                // do nothing
                this.plan = null;
            } else {
                throw new Exception("Plan type not yet supported");
            }
        }

        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO);
    }

    public void execute() throws Exception{
        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_SENSE);
    }

    /**
     * Helper functions
     */
    private void senseEnvironment(){}
    private void readMessages(){
        this.receivedMessages = nextMessages(new AgentFilter());
    }
    private void sendMessagesToPlanner(){
        for(int i = 0; i < receivedMessages.size(); i++){
            CCBBAResultsMessage message_i = (CCBBAResultsMessage) this.receivedMessages.get(i);
            sendMessageWithRole(this.plannerAddress, message_i, SimGroups.AGENT);
        }
    }
    private ArrayList<PlannerMessage> readMessagesFromPlanner(){
        ArrayList<PlannerMessage> plannerMessages = new ArrayList<>();
        List<Message> newMessages = nextMessages(new PlannerFilter(this.plannerAddress));
        for(Message message : newMessages){
            PlannerMessage plannerMessage = (PlannerMessage) message;
            plannerMessages.add(plannerMessage);
        }
        return plannerMessages;
    }
    private void makeMeasurement() throws Exception {
        MeasurementPlan measurementPlan = (MeasurementPlan) this.plan;

        // unpack plan details
        Subtask subtask = measurementPlan.getRelevantSubtask();
        ArrayList<Instrument> instruments = measurementPlan.getInstruments();
        Measurement measurement = measurementPlan.getMeasurement();
        AbsoluteDate date = measurementPlan.getStartDate();
        Requirements requirements = measurementPlan.getRelevantSubtask().getParentTask().getRequirements();

        // calculate measurement performance
        MeasurementPerformance performance = new MeasurementPerformance(subtask,instruments, this, date);
        SubtaskCapability newCapability = new SubtaskCapability(subtask, instruments, measurement, requirements, performance, this);

        // update environment
        this.environment.updateMeasurementCapability(newCapability);
        this.environment.completeSubtask(subtask);
    }
    private void performManeuver(){
        ManeuverPlan maneuverPlan = (ManeuverPlan) this.plan;
        this.design.getAdcs().updateBodyFrame(maneuverPlan);
    }

    public boolean hasAccess(Subtask j){
        Task task = j.getParentTask();
        boolean hasAccess = false;
        for(Instrument ins : design.getPayload()){
           if(orbit.hasAccess(ins, task)){
               hasAccess = true;
           }
        }
        return hasAccess;
    }

    public ArrayList<TimeInterval> getLineOfSightTimeS(Subtask j){
        return this.orbit.getLineOfSightTimes().get(j.getParentTask());
    }
    public boolean isVisible(Instrument ins, ArrayList<Vector3D> bodyFrame, AbsoluteDate date, Vector3D objectPos) throws Exception {
        return this.design.getAdcs().isVisible(ins,bodyFrame,orbit,date,objectPos);
    }
    public boolean isVisible(Instrument ins, Vector3D pointEarth, Vector3D objectPos, SpacecraftOrbit orbit, AbsoluteDate date) throws Exception {
        return this.design.getAdcs().isVisible(ins, pointEarth, objectPos, orbit, date);
    }
    public double calcSlewAngleReq(Instrument ins, ArrayList<Vector3D> bodyFrame, SpacecraftOrbit orbit,
                                   AbsoluteDate date, Vector3D objectPos) throws Exception{
        return this.design.getAdcs().calcSlewAngleReq(ins,bodyFrame,orbit,date,objectPos);
    }
    public Vector3D getPointingWithSlew(double th, Instrument ins, SpacecraftOrbit orbit, AbsoluteDate date) throws Exception {
        return this.design.getAdcs().getPointingWithSlew(th, ins, orbit, date);
    }
    public double getAlt(AbsoluteDate date) throws OrekitException {
        return this.orbit.getAlt(date);
    }
    public double getTaskCTAngle(ArrayList<Vector3D> orbitFrame, Vector3D satPos, Vector3D taskPos){
        return  this.design.getAdcs().getTaskCTAngle(orbitFrame, satPos, taskPos);
    }
    public boolean isAlive(){
        // check life status
        var myRoles = getMyRoles(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);
        return !(myRoles.contains(SimGroups.AGENT_DIE));
    }

    private double rad2deg(double th){ return th*180.0/Math.PI; }
    private double deg2rad(double th){ return th*Math.PI/180.0; }

    /**
     * Getters and setters
     */
    public PVCoordinates getPV(AbsoluteDate date) throws OrekitException {return this.orbit.getPV(date);}
    public PVCoordinates getPVEarth(AbsoluteDate date) throws OrekitException {return this.orbit.getPVEarth(date);}
    public SpacecraftDesign getDesign(){ return this.design; }
    public ArrayList<Vector3D> getBodyFrame(){ return this.design.getAdcs().getBodyFrame(); }
    public AbsoluteDate getStartDate(){return this.orbit.getStartDate(); }
    public AbsoluteDate getEndDate(){return this.orbit.getEndDate();}
    public SpacecraftOrbit getOrbit(){return orbit;}
    public AbsoluteDate getCurrentDate(){ return this.environment.getCurrentDate(); }
    public Planner getPlanner(){return planner;}
    public ArrayList<Subtask> getOverallPath(){return ((CCBBAPlanner) this.planner).getOverallPath();}
    public String toString(){return this.name;}
}
