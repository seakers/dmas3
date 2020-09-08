package modules.spacecraft.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class AttitudeManeuver extends Maneuver {
    protected Vector3D p_o = new Vector3D(0,0,0);      // original pointing vector
    protected Vector3D p_f = new Vector3D(0,0,0);      // final pointing vector

    public AttitudeManeuver(Vector3D p_o, Vector3D p_f, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(startDate, endDate);
        this.p_o = new Vector3D(p_o.getX(), p_o.getY(), p_o.getZ());
        this.p_f = new Vector3D(p_f.getX(), p_f.getY(), p_f.getZ());
    }

    protected AttitudeManeuver(Vector3D p_o, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(startDate, endDate);
        this.p_o = new Vector3D(p_o.getX(), p_o.getY(), p_o.getZ());
    }

    protected double deg2rad(double th){
        return th*Math.PI/180;
    }
    protected double rad2deg(double th){
        return th*180/Math.PI;
    }
}
