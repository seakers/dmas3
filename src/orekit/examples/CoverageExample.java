/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.examples;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import seakers.orekit.constellations.Walker;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.coverage.analysis.AnalysisMetric;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.EventAnalysisEnum;
import seakers.orekit.event.EventAnalysisFactory;
import seakers.orekit.event.FieldOfViewEventAnalysis;

/**
 * A minimal working example of how to set up a simulation to compute coverage
 * metrics such as average revisit time
 *
 * @author nozomihitomi
 */
public class CoverageExample {

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
            filename = "CoverageExample";
        }

        //initializes the look up tables for planteary position (required!)
        OrekitConfig.init(4);

        //define the start and end date of the simulation
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2016, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2016, 1, 7, 00, 00, 00.000, utc);

        //define the scenario parameters
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient
        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);

        //Enter satellite orbital parameters
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 600000;
        double i = FastMath.toRadians(45);

        //define instruments and payload
        NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
        ArrayList<Instrument> payload = new ArrayList<>();
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        payload.add(view1);

        //Create a walker constellation
        Walker walker = new Walker("walker1", payload, a, i, 2, 2, 0, inertialFrame, startDate, mu);

        //create a coverage definition
        CoverageDefinition covDef1 = new CoverageDefinition("covdef1", 10, earthShape, CoverageDefinition.GridStyle.UNIFORM);

        //assign the walker constellation to the coverage definition
        covDef1.assignConstellation(walker);

        HashSet<CoverageDefinition> covDefs = new HashSet<>();
        covDefs.add(covDef1);

        //set the type of propagation
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN, new Properties());

        //can set the properties of the analyses
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.saveAccess", "false");

        //set the coverage event analyses
        EventAnalysisFactory eaf = new EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
        ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
        FieldOfViewEventAnalysis fovEvent = (FieldOfViewEventAnalysis) eaf.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
        eventanalyses.add(fovEvent);

        //build the scenario
        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventanalyses).covDefs(covDefs).
                name("CoverageExample").properties(propertiesEventAnalysis).
                propagatorFactory(pf).build();
        try {
            System.out.println(String.format("Running Scenario %s", scen));
            System.out.println(String.format("Number of points:     %d", covDef1.getNumberOfPoints()));
            System.out.println(String.format("Number of satellites: %d", walker.getSatellites().size()));
            //run the scenario
            scen.call();
        } catch (Exception ex) {
            Logger.getLogger(CoverageExample.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        //Extract the coverage and access metrics
        GroundEventAnalyzer ea = new GroundEventAnalyzer(fovEvent.getEvents(covDef1));
        DescriptiveStatistics accessStats = ea.getStatistics(AnalysisMetric.DURATION, true, new Properties());
        DescriptiveStatistics gapStats = ea.getStatistics(AnalysisMetric.DURATION, false, new Properties());

        System.out.println(String.format("Max access time %s", accessStats.getMax()));
        System.out.println(String.format("Mean access time %s", accessStats.getMean()));
        System.out.println(String.format("Min access time %s", accessStats.getMin()));
        System.out.println(String.format("50th access time %s", accessStats.getPercentile(50)));
        System.out.println(String.format("80th acceses time %s", accessStats.getPercentile(80)));
        System.out.println(String.format("90th access time %s", accessStats.getPercentile(90)));

        System.out.println(String.format("Max gap time %s", gapStats.getMax()));
        System.out.println(String.format("Mean gap time %s", gapStats.getMean()));
        System.out.println(String.format("Min gap time %s", gapStats.getMin()));
        System.out.println(String.format("50th gap time %s", gapStats.getPercentile(50)));
        System.out.println(String.format("80th gap time %s", gapStats.getPercentile(80)));
        System.out.println(String.format("90th gap time %s", gapStats.getPercentile(90)));

        //saves the start and stop time of each access at each ground point
        ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename + "_fov", scen, covDef1, fovEvent);
        
        //saves the gap metrics in a csv file for each ground point
        //ScenarioIO.saveGroundEventAnalysisMetrics(Paths.get(System.getProperty("results"), ""), filename + "_fov_metrics", scen, ea, AnalysisMetric.DURATION, false);

        long end = System.nanoTime();
        Logger.getGlobal().finest(String.format("Took %.4f sec", (end - start) / Math.pow(10, 9)));

        OrekitConfig.end();
    }

}
