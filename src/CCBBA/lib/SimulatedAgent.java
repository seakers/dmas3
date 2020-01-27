package CCBBA.lib;

import CCBBA.source.SimulatedAbstractAgent;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class SimulatedAgent extends SimulatedAbstractAgent {
    private JSONObject inputAgent;
    private ArrayList sensorList;

    public SimulatedAgent(JSONObject inputAgent, JSONObject inputData){
        this.inputAgent = inputAgent;
    }

    @Override
    protected void activate() {
        // Request Role
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_THINK);
    }
}
