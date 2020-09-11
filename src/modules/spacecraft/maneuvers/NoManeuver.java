package modules.spacecraft.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class NoManeuver extends AttitudeManeuver {
    public NoManeuver(ArrayList<Vector3D> p_o, AbsoluteDate startDate) {
        super(p_o, p_o, startDate, startDate);
    }

    public NoManeuver(ArrayList<Vector3D> p_o, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(p_o, p_o, startDate, endDate);
    }

    @Override
    public double getSpecificTorque() {
        return 0.0;
    }
}
