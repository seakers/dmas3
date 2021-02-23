package modules.simulation;

import madkit.kernel.Agent;
import modules.measurements.MeasurementRequest;
import org.json.simple.JSONObject;
import seakers.orekit.event.CrossLinkEventAnalysis;

import java.util.ArrayList;

public class Simulation extends Agent{
    ArrayList<Agent> satellites;
    ArrayList<MeasurementRequest> requests;

    public Simulation(JSONObject input, OrbitData orbitData, int simID){
        // 0- generate

//        // 1- create the simulation group
//        createGroup(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP);
//
//        // 2- launch simulation environment
//        this.environment = new Environment(prob, arch, directoryAddress, simStart);
//        launchAgent(this.environment, false);
//
//        // 3- launch architecture agents
//        launchSpaceSegment();
//
//        // 4- launch simulation scheduler
//        launchAgent(new SimScheduler(), false);
    }

    @Override
    public void activate(){
        for(Agent sat : satellites){
            this.launchAgent(sat, true);
        }
    }
}
