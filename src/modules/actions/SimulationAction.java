package modules.actions;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import org.orekit.time.AbsoluteDate;

public abstract class SimulationAction {
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;
    private final AbstractAgent agent;

    protected SimulationAction(AbstractAgent agent, AbsoluteDate startDate, AbsoluteDate endDate) {
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
    public AbstractAgent getAgent() { return agent; }
}
