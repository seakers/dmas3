package modules.orbitData;

import modules.actions.ManeuverAction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

import java.util.InputMismatchException;

/**
 * Represents a satellite's current attitude with respect to the orbit frame
 * Frame Definitions:
 *  Earth Frame: see orekit standard earth frame
 *  Orbit Frame: (for circular orbits only), expressed with respect to the Earth-frame
 *      x-axis - in the direction of the tangential velocity vector
 *      z-axis - in the direction from the satellite towards the ground
 *      y-axis - z-axis cossed with x-axis
 *  Attitude Frame: expressed with respect to the Orbit Frame
 *
 * @author a.aguilar
 */
public class Attitude {
    /**
     * Attitude x, y, and z axis vectors expressed in the Orbit Frame
     */
    private Vector3D i;
    private Vector3D j;
    private Vector3D k;

    /**
     * peak angular acceleration in radians per sec^2
     */
    private double peakRollAcc;

    /**
     * current roll angle in radians
     */
    private double rollAngle;

    /**
     * Maximum roll angle in radians
     */
    private double maxRollAngle;

    /**
     * Constructor creates and instance of an attitude object
     * @param offNadirAngle : off-nadir angle in radians
     * @param maxRollAngle : max roll angle in plus-minus direction in radians
     * @param peakRollAcc : peak roll angle acceleration in radians per second squared
     */
    public Attitude(double offNadirAngle, double maxRollAngle, double peakRollAcc){
        this.rollAngle = offNadirAngle;
        this.maxRollAngle = maxRollAngle;
        this.peakRollAcc = peakRollAcc;

        if(Math.abs( this.rollAngle ) > maxRollAngle)
            throw new InputMismatchException("Initial Roll angle exceeds maximum roll angle");

        i = new Vector3D(1.0,                  0.0,                   0.0);
        j = new Vector3D(0.0, Math.cos(offNadirAngle), -Math.sin(offNadirAngle));
        k = new Vector3D(0.0, Math.sin(offNadirAngle),  Math.cos(offNadirAngle));

        i.normalize();
        j.normalize();
        k.normalize();
    }

    public void updateAttitude(ManeuverAction maneuver, AbsoluteDate currentDate) throws Exception{
        if(currentDate.compareTo(maneuver.getStartDate()) < 0 ||
            currentDate.compareTo(maneuver.getEndDate()) > 0){
            throw new Exception("Trying to perform maneuver before scheduled time");
        }

        double th = calcNewRollAngle(maneuver, currentDate);
        updateAttitude(th);
    }

    private double calcNewRollAngle(ManeuverAction maneuver, AbsoluteDate currentDate){
        double th0 = maneuver.getInitialRollAngle();
        double thf = maneuver.getFinalRollAngle();
        double tau = maneuver.getDuration();
        double t = maneuver.getStartDate().durationFrom(currentDate);

        double th;
        if(th0 < thf){
            if(t < tau/2){
                th = th0 + 0.5 * peakRollAcc * Math.pow(t , 2.0);
            }
            else{
                double th0p = th0 + 0.5 * peakRollAcc * Math.pow( tau/2.0 , 2.0);
                double thd0p = peakRollAcc * tau/2.0;

                th = th0p + thd0p * t - 0.5 * peakRollAcc * Math.pow(t , 2.0);
            }
        }
        else{
            if(t < tau/2){
                th = th0 - 0.5 * peakRollAcc * Math.pow(t , 2.0);
            }
            else{
                double th0p = th0 - 0.5 * peakRollAcc * Math.pow( tau/2.0 , 2.0);
                double thd0p = -peakRollAcc * tau/2.0;

                th = th0p + thd0p * t + 0.5 * peakRollAcc * Math.pow(t , 2.0);
            }
        }

        return th;
    }

    private void updateAttitude(double rollAngle){
        this.rollAngle = rollAngle;

        i = new Vector3D(1.0,              0.0,               0.0);
        j = new Vector3D(0.0, Math.cos(rollAngle), -Math.sin(rollAngle));
        k = new Vector3D(0.0, Math.sin(rollAngle),  Math.cos(rollAngle));

        i.normalize();
        j.normalize();
        k.normalize();
    }

    public double getPeakRollAcc() { return peakRollAcc; }
    public double getRollAngle() { return rollAngle; }
    public double getMaxRollAngle() { return maxRollAngle; }
}
