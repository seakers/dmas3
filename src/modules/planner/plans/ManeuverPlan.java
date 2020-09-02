package modules.planner.plans;

import modules.spacecraft.component.Component;
import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class ManeuverPlan extends Plan{
    private double th_0 = 0.0;
    private double th_f = 0.0;

    public ManeuverPlan(double th_0, double th_f, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(startDate, endDate, new ArrayList<>(), new ArrayList<>());
        this.th_0 = th_0;
        this.th_f = th_f;
    }

    @Override
    public Plan copy() {
        return new ManeuverPlan(this.th_0, this.th_f, this.startDate, this.endDate);
    }
}
