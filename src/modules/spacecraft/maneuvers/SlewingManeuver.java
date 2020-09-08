package modules.spacecraft.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

public class SlewingManeuver extends  AttitudeManeuver {
    public SlewingManeuver(Vector3D vel, Vector3D p_o, double theta, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(p_o, startDate, endDate);
        this.p_f = calcFinalPointingVector(vel,p_o,theta);
    }

    private Vector3D calcFinalPointingVector(Vector3D vel, Vector3D p_o, double theta){
        Vector3D x_o = vel.normalize();
        Vector3D z_o = x_o.crossProduct(p_o);
        Vector3D p_f = ( p_o.scalarMultiply( Math.cos(deg2rad(theta)) ) )
                            .add( z_o.scalarMultiply( Math.sin(deg2rad(theta)) ) );
        return p_f;
    }
}
