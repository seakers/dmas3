package modules.planner.plans;

import madkit.kernel.Message;
import modules.planner.messages.CCBBAResultsMessage;
import modules.spacecraft.component.Component;
import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class DiePlan extends Plan{
    private CCBBAResultsMessage resultsMessage;

    public DiePlan(CCBBAResultsMessage resultsMessage, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(startDate, endDate, new ArrayList<>(), new ArrayList<>());
        this.resultsMessage = resultsMessage;
    }

    @Override
    public Plan copy() {
        return new DiePlan(this.resultsMessage, this.startDate, this.endDate);
    }
    public Message getBroadcastMessage(){return this.resultsMessage;}
}
