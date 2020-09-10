package modules.spacecraft.maneuvers;

import org.orekit.time.AbsoluteDate;

public class NoManeuver extends Maneuver {
    public NoManeuver(AbsoluteDate startDate) {
        super(startDate, startDate);
    }

    @Override
    public double getSpecificTorque() {
        return 0.0;
    }
}
