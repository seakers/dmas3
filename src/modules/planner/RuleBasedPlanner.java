package modules.planner;

import madkit.kernel.Message;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class RuleBasedPlanner extends AbstractPlanner{
    public RuleBasedPlanner(double planningHorizon, int requestThreshold, boolean crossLinks) {
        super(planningHorizon, requestThreshold, crossLinks);
    }

    @Override
    public LinkedList<SimulationAction> initPlan() {
        return null;
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<Message>> messageMap, SatelliteAgent agent, AbsoluteDate currentDate) throws Exception {
        return null;
    }

    @Override
    public double calcUtility(MeasurementRequest request, HashMap<Requirement, RequirementPerformance> performance) {
        return 0;
    }
}
