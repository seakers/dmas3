package modules.spacecraft;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.MessageFilter;
import modules.planner.plans.*;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.orbits.OrbitParams;
import modules.spacecraft.orbits.SpacecraftOrbit;
import modules.environment.Environment;
import modules.planner.CCBBA.CCBBAPlanner;
import modules.planner.Planner;
import modules.simulation.SimGroups;
import modules.planner.messages.*;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;

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

    public Spacecraft(String name, ArrayList<Instrument> payload, OrbitParams orbitParams, String planner) throws Exception {
        this.name = name;
        this.orbitParams = orbitParams;
        this.design = new SpacecraftDesign(payload);

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
            this.orbit.calcAccessTimes(environment);

            //3- Design Spacecraft
            this.design.designSpacecraft(this.orbit);

            //4- Launch Planner
            this.planner.setParentAgentAddress(this.getAgentAddressIn(SimGroups.MY_COMMUNITY,SimGroups.SIMU_GROUP,SimGroups.AGENT));
            launchAgent(this.planner);
            this.plannerAddress = this.planner.getAgentAddressIn(SimGroups.MY_COMMUNITY,SimGroups.SIMU_GROUP,SimGroups.PLANNER);

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

    public void think(){
        // check if planner has delivered a plan
        PlannerMessage plannerMessage = readMessagesFromPlanner();

        if(plannerMessage == null){
            this.plan = new WaitPlan();
        }
        else{
            this.plan = plannerMessage.getPlan();
        }

        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO);
    }

    public void execute() throws Exception{
        String planClass = this.plan.getClass().toString();
        if(planClass.equals(PlanNames.BROADCAST)){
            // broadcast message from plan
            Message message = this.plan.getBroadcastMessage();
            broadcastMessage(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT, message);
        }
        else if(planClass.equals(PlanNames.DIE)) {

        }
        else if(planClass.equals(PlanNames.MANEUVER)){

        }
        else if(planClass.equals(PlanNames.MEASURE)){

        }
        else if(planClass.equals(PlanNames.WAIT)){
            // do nothing
        }
        else{
            throw new Exception("Plan type not yet supported");
        }

        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DO);
        if(this.plan.getClass().equals(DiePlan.class)) {
            requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE);
        }
        else{
            requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_SENSE);
        }
    }

    public void die(){

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
            Message message_i = this.receivedMessages.get(i);
            sendMessageWithRole(this.plannerAddress, message_i, SimGroups.AGENT);
        }
    }
    private PlannerMessage readMessagesFromPlanner(){
        return (PlannerMessage) nextMessage(new PlannerFilter(this.plannerAddress));
    }
    public PVCoordinates getPV(AbsoluteDate date) throws OrekitException {return this.orbit.getPV(date);}
    public PVCoordinates getPVEarth(AbsoluteDate date) throws OrekitException {return this.orbit.getPVEarth(date);}
}
