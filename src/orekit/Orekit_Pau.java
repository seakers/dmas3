/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit;

import java.io.File;
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

import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
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
public class Orekit_Pau {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        File orekitData = new File("./src/orekit-data");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));

        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));
        long start = System.nanoTime();

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "tropics";
        }

        OrekitConfig.init(4);
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2020, 1, 8, 00, 00, 00.000, utc);
        //AbsoluteDate endDate = new AbsoluteDate(2021, 1, 1, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        //Enter satellite orbital parameters
        int h=600000;
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+h;
        double ideg=30;
        double i = FastMath.toRadians(ideg);
        //define instruments
        //NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
        NadirRectangularFOV fov = new NadirRectangularFOV(FastMath.toRadians(57), FastMath.toRadians(20), 0, earthShape);
        ArrayList<Instrument> payload = new ArrayList<>();
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        payload.add(view1);
        
        //number of total satellites
        int t=10;
        //number of planes
        int p=1;
        //
        int f=0;
//        
//        Walker constel = new Walker("walker1", payload, a, i, t, p, f, inertialFrame, startDate, mu);
        ArrayList<Satellite> satellites=new ArrayList<>();
        Orbit orb1 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(0), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
        Satellite sat1 = new Satellite("sat1", orb1, null, payload);
//        Orbit orb2 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(0), Math.toRadians(60), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat2 = new Satellite("sat2", orb2, null, payload);
//        Orbit orb3 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(0), Math.toRadians(120), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat3 = new Satellite("sat3", orb3, null, payload);
//        Orbit orb4 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(0), Math.toRadians(180), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat4 = new Satellite("sat4", orb4, null, payload);
//        Orbit orb5 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(0), Math.toRadians(240), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat5 = new Satellite("sat5", orb5, null, payload);
//        Orbit orb6 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(0), Math.toRadians(299.99), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat6 = new Satellite("sat6", orb6, null, payload);
//        Orbit orb7 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(0), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat7 = new Satellite("sat7", orb7, null, payload);
//        Orbit orb8 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(60), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat8 = new Satellite("sat8", orb8, null, payload);
//        Orbit orb9 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(120), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat9 = new Satellite("sat9", orb9, null, payload);
//        Orbit orb10 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(180), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat10 = new Satellite("sat10", orb10, null, payload);
//        Orbit orb11 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(240), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat11 = new Satellite("sat11", orb11, null, payload);
//        Orbit orb12 = new KeplerianOrbit(a, 0.0001, i, 0.0, Math.toRadians(180), Math.toRadians(300), PositionAngle.MEAN, inertialFrame, startDate, mu);
//        Satellite sat12 = new Satellite("sat12", orb12, null, payload);
        satellites.add(sat1);
//        satellites.add(sat2);
//        satellites.add(sat3);
//        satellites.add(sat4);
//        satellites.add(sat5);
//        satellites.add(sat6);
//        satellites.add(sat7);
//        satellites.add(sat8);
//        satellites.add(sat9);
//        satellites.add(sat10);
//        satellites.add(sat11);
//        satellites.add(sat12);


        Constellation constel = new Constellation ("tropics2",satellites);

        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 9, earthShape, EQUAL_AREA);

        covDef1.assignConstellation(constel);
        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef1);
        
        Properties propertiesPropagator = new Properties();
        propertiesPropagator.setProperty("orekit.propagator.mass", "6");
        propertiesPropagator.setProperty("orekit.propagator.atmdrag", "true");
        propertiesPropagator.setProperty("orekit.propagator.dragarea", "0.075");
        propertiesPropagator.setProperty("orekit.propagator.dragcoeff", "2.2");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.sun", "true");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.moon", "true");
        propertiesPropagator.setProperty("orekit.propagator.solarpressure", "true");
        propertiesPropagator.setProperty("orekit.propagator.solararea", "0.058");

        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN,propertiesPropagator);
        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);
        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2MODIFIED,propertiesPropagator);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.NUMERICAL,propertiesPropagator);
        
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");

        
        //set the event analyses
        EventAnalysisFactory eaf = new EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
        ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
        FieldOfViewEventAnalysis fovEvent = (FieldOfViewEventAnalysis) eaf.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
        eventanalyses.add(fovEvent);

//        //set the analyses
//        double analysisTimeStep = 60;
        ArrayList<Analysis<?>> analyses = new ArrayList<>();
//        for (Satellite sat : walker.getSatellites()) {
//            //analyses.add(new OrbitalElementsAnalysis(startDate, endDate, analysisTimeStep, sat, pf));
//        }
        
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses).analysis(analyses).
                covDefs(covDefs).name("sat1").properties(propertiesEventAnalysis).
                propagatorFactory(pf).build();
        try {
            Logger.getGlobal().finer(String.format("Running Scenario %s", scen));
            Logger.getGlobal().finer(String.format("Number of points:     %d", covDef1.getNumberOfPoints()));
            Logger.getGlobal().finer(String.format("Number of satellites: %d", constel.getSatellites().size()));
            scen.call();
        } catch (Exception ex) {
            Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        Logger.getGlobal().finer(String.format("Done Running Scenario %s", scen));
        Properties props=new Properties();
        GroundEventAnalyzer ea = new GroundEventAnalyzer(fovEvent.getEvents(covDef1));
        DescriptiveStatistics accessStats = ea.getStatistics(AnalysisMetric.DURATION, true,props);
        DescriptiveStatistics gapStats = ea.getStatistics(AnalysisMetric.DURATION, false,props);

        System.out.println(String.format("Max access time %s", accessStats.getMax()));
        System.out.println(String.format("Mean access time %s", accessStats.getMean()));
        System.out.println(String.format("Min access time %s", accessStats.getMin()));
        System.out.println(String.format("50th access time %s", accessStats.getPercentile(50)));
        System.out.println(String.format("80th access time %s", accessStats.getPercentile(80)));
        System.out.println(String.format("90th access time %s", accessStats.getPercentile(90)));

        System.out.println(String.format("Max gap time %s", gapStats.getMax()));
        System.out.println(String.format("Mean gap time %s", gapStats.getMean()));
        System.out.println(String.format("Min gap time %s", gapStats.getMin()));
        System.out.println(String.format("50th gap time %s", gapStats.getPercentile(50)));
        System.out.println(String.format("80th gap time %s", gapStats.getPercentile(80)));
        System.out.println(String.format("90th gap time %s", gapStats.getPercentile(90)));
//
        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename, scen, covDef1, fovEvent);
        //ScenarioIO.saveGroundEventAnalysisMetrics(Paths.get(System.getProperty("results"), ""), filename, scen, covDef1, fovEvent);
        ScenarioIO.saveGroundEventAnalyzerObject(Paths.get(System.getProperty("results"), ""), filename, scen,covDef1, fovEvent);
//        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename + "_gsa", scen, covDef1, gndSunAngEvent);
 //        ScenarioIO.saveReadMe(Paths.get(path, ""), filename, scenComp);
        
        for (Analysis<?> analysis : analyses) {
            ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                    String.format("%s_%s",scen.toString(),analysis.getName()), analysis);
        }
        long end = System.nanoTime();
        Logger.getGlobal().finest(String.format("Took %.4f sec", (end - start) / Math.pow(10, 9)));
        
        OrekitConfig.end();
    }
    
}
