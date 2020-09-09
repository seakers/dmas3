package modules.spacecraft.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class SlewingManeuver extends  AttitudeManeuver {
    double th = 0.0;
    double th_deg = 0.0;

    public SlewingManeuver(ArrayList<Vector3D> p_o, double theta, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(p_o, startDate, endDate);
        this.th = theta;
        this.th_deg = rad2deg(th);
        this.p_f = calcFinalBodyFrame(p_o,theta);
    }

    private ArrayList<Vector3D> calcFinalBodyFrame(ArrayList<Vector3D> p_o, double theta){
        this.th = deg2rad(theta);
        this.th_deg = theta;

        Vector3D x_o = p_o.get(0);
        Vector3D y_o = p_o.get(1);
        Vector3D z_o = p_o.get(2);

        Vector3D x_f = x_o;
        Vector3D y_f = y_o.scalarMultiply(Math.cos(th)).add(
                z_o.scalarMultiply(Math.sin(th)) );
        Vector3D z_f = x_o.crossProduct(y_f);

        ArrayList<Vector3D> p_f = new ArrayList<>();
        p_f.add(x_f);
        p_f.add(y_f);
        p_f.add(z_f);

        return p_f;
    }

    public double getTh(){return th;}
}
