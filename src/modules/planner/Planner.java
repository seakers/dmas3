package modules.planner;

import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import modules.environment.Subtask;
import modules.planner.plans.Plan;
import modules.spacecraft.Spacecraft;

import java.util.List;

public abstract class Planner extends AbstractAgent {
    protected AgentAddress parentAgentAddress;
    protected Spacecraft parentSpacecraft;

    public void setParentAgentAddress(AgentAddress address){this.parentAgentAddress = address;}
    public abstract void planDone() throws Exception;
    public abstract void resetResults(Subtask j);
}
