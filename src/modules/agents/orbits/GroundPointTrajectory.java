package modules.agents.orbits;

import modules.environment.Environment;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import seakers.orekit.util.OrekitConfig;

import java.io.File;
import java.util.Locale;

public class GroundPointTrajectory extends OrbitData{
    private double lat;
    private double lon;
    private double alt;

    public GroundPointTrajectory(double lat, double lon, double alt, AbsoluteDate startDate, AbsoluteDate endDate, double timeStep) throws OrekitException {
        super(startDate, endDate, timeStep);
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    @Override
    public void propagateOrbit() throws OrekitException {
        try {
            // load orekit data
            File orekitData = new File("./src/data/orekit-data");
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.addProvider(new DirectoryCrawler(orekitData));

            //if running on a non-US machine, need the line below
            Locale.setDefault(new Locale("en", "US"));

            //initializes the look up tables for planteary position (required!)
            OrekitConfig.init(4);

            //must use IERS_2003 and EME2000 frames to be consistent with STK
            BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                    Constants.WGS84_EARTH_FLATTENING,
                                                    earthFrame);

            //define orbit
            GeodeticPoint taskLocation = new GeodeticPoint(lat, lon, alt);
            TopocentricFrame staF = new TopocentricFrame(earth, taskLocation, "Task Location");

            // propagate by step-size
            AbsoluteDate stepDate = startDate.getDate();
            while(stepDate.compareTo(endDate) < 0){
                // calculate PV at this time, save to position vectors
                PVCoordinates pvStepInertial = staF.getPVCoordinates(stepDate, inertialFrame);
                PVCoordinates pvStepEarth = staF.getPVCoordinates(stepDate, earthFrame);
                pv.put(stepDate,pvStepInertial);
                pvEarth.put(stepDate,pvStepEarth);
                dates.add(stepDate);

                // advance a step
                stepDate = stepDate.shiftedBy(timeStep);
            }

        } catch (OrekitException e) {
            e.printStackTrace();
        }

    }

    @Override
    public PVCoordinates getPV(AbsoluteDate date) throws OrekitException {
        if(this.pv.containsKey(date)){
            // if already calculated, return value
            return this.pv.get(date);
        }
        else{
            // else propagate at that given time
            // load orekit data
            File orekitData = new File("./src/data/orekit-data");
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.addProvider(new DirectoryCrawler(orekitData));

            //if running on a non-US machine, need the line below
            Locale.setDefault(new Locale("en", "US"));

            //initializes the look up tables for planteary position (required!)
            OrekitConfig.init(4);

            //must use IERS_2003 and EME2000 frames to be consistent with STK
            BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    earthFrame);

            //define orbit
            GeodeticPoint taskLocation = new GeodeticPoint(lat, lon, alt);
            TopocentricFrame staF = new TopocentricFrame(earth, taskLocation, "Task Location");

            // propagate to that time
            return staF.getPVCoordinates(date, inertialFrame);
        }
    }

    @Override
    public PVCoordinates getPVEarth(AbsoluteDate date) throws OrekitException {
        if(this.pvEarth.containsKey(date)){
            // if already calcualted, return value
            return this.pvEarth.get(date);
        }
        else{
            // else propagate at that given time
            // load orekit data
            File orekitData = new File("./src/data/orekit-data");
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.addProvider(new DirectoryCrawler(orekitData));

            //if running on a non-US machine, need the line below
            Locale.setDefault(new Locale("en", "US"));

            //initializes the look up tables for planteary position (required!)
            OrekitConfig.init(4);

            //must use IERS_2003 and EME2000 frames to be consistent with STK
            BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    earthFrame);

            //define orbit
            GeodeticPoint taskLocation = new GeodeticPoint(lat, lon, alt);
            TopocentricFrame staF = new TopocentricFrame(earth, taskLocation, "Task Location");

            // propagate to that time
            return staF.getPVCoordinates(date, earthFrame);
        }
    }
}
