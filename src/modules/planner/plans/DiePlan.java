package modules.planner.plans;

import modules.spacecraft.component.Component;
import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class DiePlan extends Plan{
    public DiePlan(AbsoluteDate startDate, AbsoluteDate endDate) {
        super(startDate, endDate, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public Plan copy() {
        return new DiePlan(this.startDate, this.endDate);
    }
}
