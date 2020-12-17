package modules.spacecraft.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class AttitudeManeuver extends Maneuver {
    protected ArrayList<Vector3D> p_o = new ArrayList<>();      // original body frame wrt the orbit frame
    protected ArrayList<Vector3D> p_f = new ArrayList<>();      // final body frame wrt the orbit frame

    public AttitudeManeuver(ArrayList<Vector3D> p_o, ArrayList<Vector3D> p_f, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(startDate, endDate);
        this.p_o = new ArrayList<>();
        Vector3D x_o = p_o.get(0);
        Vector3D y_o = p_o.get(1);
        Vector3D z_o = p_o.get(2);
        this.p_o.add(x_o);
        this.p_o.add(y_o);
        this.p_o.add(z_o);

        this.p_f = new ArrayList<>();
        Vector3D x_f = p_f.get(0);
        Vector3D y_f = p_f.get(1);
        Vector3D z_f = p_f.get(2);
        this.p_f.add(x_f);
        this.p_f.add(y_f);
        this.p_f.add(z_f);
    }

    protected AttitudeManeuver(ArrayList<Vector3D> p_o,  AbsoluteDate startDate, AbsoluteDate endDate) {
        super(startDate, endDate);
        this.p_o = new ArrayList<>();
        Vector3D x_o = p_o.get(0);
        Vector3D y_o = p_o.get(1);
        Vector3D z_o = p_o.get(2);
        this.p_o.add(x_o);
        this.p_o.add(y_o);
        this.p_o.add(z_o);
    }

    protected double deg2rad(double th){
        return th*Math.PI/180;
    }
    protected double rad2deg(double th){
        return th*180/Math.PI;
    }
    public ArrayList<Vector3D> getFinalBodyFrame(){return this.p_f;}

    @Override
    public double getSpecificTorque() throws Exception {
        return 0.0;
    }
}
