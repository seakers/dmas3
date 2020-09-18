package modules.spacecraft.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import seakers.orekit.util.OrekitConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public abstract class OrbitData {
    protected HashMap<AbsoluteDate, PVCoordinates> pv;          // Position vector in the inertial frame as a function of time
    protected HashMap<AbsoluteDate, PVCoordinates> pvEarth;     // Position vector in the earth frame as a function of time
    protected ArrayList<AbsoluteDate> dates;                    // List of dates in propagation
    protected AbsoluteDate startDate;                           // Start date of propagation
    protected AbsoluteDate endDate;                             // End date of propagation
    protected double timeStep;                                  // Propagation timestep
    private double Re = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

    // Reference frames: must use IERS_2003 and EME2000 frames to be consistent with STK
    protected Frame inertialFrame;
    protected Frame earthFrame;

    public OrbitData(AbsoluteDate startDate, AbsoluteDate endDate, double timeStep) throws OrekitException {
        this.pv = new HashMap<>();
        this.pvEarth = new HashMap<>();
        this.startDate  = new AbsoluteDate(); this.startDate = startDate.getDate();
        this.endDate  = new AbsoluteDate(); this.endDate = endDate.getDate();
        this.timeStep = timeStep;
        this.dates = new ArrayList<>();
        this.inertialFrame = FramesFactory.getEME2000();
        this.earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
    }

    public abstract void propagateOrbit() throws OrekitException;
    public abstract PVCoordinates getPV(AbsoluteDate date) throws OrekitException;
    public abstract PVCoordinates getPVEarth(AbsoluteDate date) throws OrekitException;
    // ^gets PV if key exists, if not, propagate/interpolate to get position

    public boolean lineOfsight(Vector3D x1, Vector3D x2){
        //calculate angle between vector x1 and vector x2
        double th = Math.acos( x1.dotProduct(x2) / (x1.getNorm() * x2.getNorm()) );

        // calculate the  maximum angle between the two vectors before they are hidden by the horizon
        double th_1 = Math.acos( Re / x1.getNorm() );
        double th_2 = Math.acos( Re / x2.getNorm() );
        if(Double.isNaN(th_1)){ th_1 = 0.0; }
        if(Double.isNaN(th_2)){ th_2 = 0.0; }
        double th_max = th_1 + th_2;

        // return if the angle between the vectors is smaller than the max
        return (th <= th_max);
    }

    // Getters
    // -Inertial Vectors
    public Vector3D getInertialPosition(AbsoluteDate date) throws OrekitException {
        return getPV(date).getPosition();
    }
    public Vector3D getInertialVelocity(AbsoluteDate date) throws OrekitException {
        return getPV(date).getVelocity();
    }
    public Vector3D getInertialAcceleration(AbsoluteDate date) throws OrekitException {
        return getPV(date).getAcceleration();
    }

    // -Earth-centered Vectors
    public Vector3D getEarthPosition(AbsoluteDate date) throws OrekitException {
        return getPVEarth(date).getPosition();
    }
    public Vector3D getEarthVelocity(AbsoluteDate date) throws OrekitException {
        return getPVEarth(date).getVelocity();
    }
    public Vector3D getEarthAcceleration(AbsoluteDate date) throws OrekitException {
        return getPVEarth(date).getAcceleration();
    }

    public ArrayList<AbsoluteDate> getDates(){return this.dates;}
    public AbsoluteDate getStartDate(){return this.startDate;}
    public AbsoluteDate getEndDate(){return this.endDate;}
}
