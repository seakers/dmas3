package modules.planner.plans;

import modules.spacecraft.maneuvers.Maneuver;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class ManeuverPlan extends Plan{
    private Maneuver maneuver;

    public ManeuverPlan(AbsoluteDate startDate, AbsoluteDate endDate, Maneuver maneuver) {
        super(startDate, endDate, new ArrayList<>(), new ArrayList<>());
        this.maneuver = maneuver;
    }

    @Override
    public Plan copy() {
        return new ManeuverPlan(this.startDate, this.endDate, this.maneuver);
    }

    public Maneuver getManeuver(){return maneuver;}
}
