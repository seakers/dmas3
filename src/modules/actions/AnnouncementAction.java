package modules.actions;

import madkit.kernel.Agent;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Satellite;

public class AnnouncementAction extends SimulationActions{
    private Satellite target;

    public AnnouncementAction(Agent agent, Satellite target, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, startDate, endDate);
        this.target = target;
    }
}
