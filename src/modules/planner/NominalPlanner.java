package modules.planner;

import madkit.kernel.Message;
import modules.actions.SimulationAction;
import modules.agents.SatelliteAgent;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.measurements.RequirementPerformance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class NominalPlanner extends AbstractPlanner {
    public NominalPlanner(double planningHorizon, int requestThreshold) {
        super(planningHorizon, requestThreshold);
    }

    @Override
    public LinkedList<SimulationAction> initPlan() {
        return null;
    }

    @Override
    public LinkedList<SimulationAction> makePlan(HashMap<String, ArrayList<Message>> messages, SatelliteAgent agent) throws Exception {
        return null;
    }

    @Override
    public double calcUtility(MeasurementRequest request, HashMap<Requirement, RequirementPerformance> performance) {
        return 0;
    }
}
