/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.analysis.ephemeris.OrbitalElementsAnalysis;
import seakers.orekit.constellations.Walker;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.object.linkbudget.LinkBudget;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;
import seakers.orekit.analysis.ephemeris.HohmannTransferAnalysis;
import seakers.orekit.analysis.vectors.VectorAnalisysEclipseSunlightDiffDrag;
import seakers.orekit.coverage.analysis.AnalysisMetric;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.EventAnalysisEnum;
import seakers.orekit.event.EventAnalysisFactory;
import seakers.orekit.event.FieldOfViewEventAnalysis;
import seakers.orekit.event.GroundBodyAngleEventAnalysis;
import seakers.orekit.object.Constellation;
import static seakers.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA;
import static seakers.orekit.object.CoverageDefinition.GridStyle.UNIFORM;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;

/**
 *
 * @author paugarciabuzzi
 */
public class LTAN2RAAN {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));
        OrekitConfig.init();
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        
        
        
        AbsoluteDate date=new AbsoluteDate(2020, 1, 2, 06, 30, 00.000, utc);
        Orbit SSO = new KeplerianOrbit(Constants.WGS84_EARTH_EQUATORIAL_RADIUS+600000, 0.0001, FastMath.toRadians(97.7874),0.0,
                FastMath.toRadians(197.811), 0.0, PositionAngle.MEAN, inertialFrame, date, mu);
        GeodeticPoint p = new GeodeticPoint(0, 0, 0);
        CoveragePoint point=new CoveragePoint(earthShape, p, "");
        Vector3D pt1=SSO.getPVCoordinates().getPosition();
        Vector3D pt2=point.getPVCoordinates(date, inertialFrame).getPosition();
        double angle=Vector3D.angle(pt1, pt2);
        
        Logger.getGlobal().finest(String.format("ANGLE=%.4f", angle));
    
    }
    
}
