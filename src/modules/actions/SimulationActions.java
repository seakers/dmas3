package modules.actions;

import org.orekit.time.AbsoluteDate;

public abstract class SimulationActions {
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    protected SimulationActions(AbsoluteDate startDate, AbsoluteDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
