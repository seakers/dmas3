package modules.planner;

import madkit.kernel.Message;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class BasicPlanner extends AbstractPlanner {
    @Override
    public LinkedList<SimulationAction> initPlan() {
        return null;
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<Message>> messages, SatelliteAgent agent) throws Exception {
        return null;
    }
}
