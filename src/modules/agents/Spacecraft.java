package modules.agents;

import madkit.kernel.AbstractAgent;
import modules.agents.Instrument.Instrument;
import modules.agents.orbits.OrbitData;
import modules.agents.orbits.OrbitParams;
import modules.environment.Environment;
import modules.planner.CCBBA.CCBBAPlanner;
import modules.planner.Planner;
import modules.simulation.SimGroups;

import java.util.ArrayList;

public class Spacecraft extends AbstractAgent {
    private String name;                // Satellite name
    private SpacecraftDesign design;    // Design properties
    private OrbitData orbit;            // Orbital Trajectory properties
    private Planner planner;            // Planner Properties
    private Environment environment;    // world environment

    public Spacecraft(String name, ArrayList<Instrument> payload, OrbitParams orbitParams, String planner) throws Exception {
        this.name = name;
        this.orbit = new OrbitData(orbitParams);
        this.design = new SpacecraftDesign(payload);

        switch(planner){
            case "CCBBA":
                this.planner = new CCBBAPlanner();
                break;
            default:
                throw new Exception("Planner type not yet supported");
        }
    }

    @Override
    protected void activate(){
        //0- Request agent role
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_SENSE);

        //1- Propagate spacecraft orbit
        this.orbit.propagateOrbit(environment);

        //2- Calculate Task Access Times
        this.orbit.calculateAccessTimes(environment);

        //3- Design Spacecraft
        this.design.designSpacecraft(this.orbit);

        //4- Launch Planner
        launchAgent(this.planner);
    }

    public void sense(){
        //get input from environment

        leaveRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_SENSE);
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK);
    }

    public void thinkPlan(){
        // Activate planner
        // If planner is still thinking -> stay in thinking plan
        // Else if planner is done -> move to doing phase
    }

    public void executePlan(){
        
    }
}
