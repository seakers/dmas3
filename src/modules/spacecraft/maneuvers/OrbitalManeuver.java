package modules.spacecraft.maneuvers;

import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class OrbitalManeuver extends  Maneuver {
    private double deltaV;
    private ArrayList<Double> impulseDirection;

    public OrbitalManeuver(AbsoluteDate startDate, AbsoluteDate endDate) {
        super(startDate, endDate);
    }
}
