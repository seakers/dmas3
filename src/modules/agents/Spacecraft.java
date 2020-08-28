package modules.agents;

import madkit.kernel.AbstractAgent;
import modules.agents.Instrument.Instrument;
import modules.agents.orbits.OrbitData;
import modules.agents.orbits.OrbitParams;
import modules.agents.orbits.SpacecraftOrbit;
import modules.environment.Environment;
import modules.planner.CCBBA.CCBBAPlanner;
import modules.planner.Planner;
import modules.simulation.SimGroups;
import org.orekit.errors.OrekitException;

import java.util.ArrayList;

public class Spacecraft extends AbstractAgent {
    private String name;                // Satellite name
    private SpacecraftDesign design;    // Design properties
    private OrbitParams orbitParams;    // Orbital Parameters
    private SpacecraftOrbit orbit;      // Orbital Trajectory
    private Planner planner;            // Planner Properties
    private Environment environment;    // world environment

    public Spacecraft(String name, ArrayList<Instrument> payload, OrbitParams orbitParams, String planner) throws Exception {
        this.name = name;
        this.orbitParams = orbitParams;
        this.design = new SpacecraftDesign(payload);

        switch(planner){
            case "CCBBA":
                this.planner = new CCBBAPlanner();
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
            launchAgent(this.planner);
            
        } catch (OrekitException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


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
