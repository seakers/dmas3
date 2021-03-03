package modules.actions;

import madkit.kernel.Agent;
import org.orekit.time.AbsoluteDate;

public abstract class SimulationActions {
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;
    private final Agent agent;

    protected SimulationActions(Agent agent, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.agent = agent;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Getters
     * @return
     */
    public AbsoluteDate getStartDate() { return startDate; }
    public AbsoluteDate getEndDate() { return endDate; }
    public Agent getAgent() { return agent; }
}
