/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.constellations.EnumerateWalkerConstellations;
import seakers.orekit.constellations.Walker;
import seakers.orekit.constellations.WalkerParameters;
import seakers.orekit.coverage.analysis.AnalysisMetric;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.EventAnalysisEnum;
import seakers.orekit.event.EventAnalysisFactory;
import seakers.orekit.event.FieldOfViewEventAnalysis;
import seakers.orekit.event.GroundEventAnalysis;
import seakers.orekit.object.CoverageDefinition;
import static seakers.orekit.object.CoverageDefinition.GridStyle.EQUAL_AREA;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.object.linkbudget.LinkBudget;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;

/**
 *
 * @author paugarciabuzzi
 */
public class Tests {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        File orekitData = new File("/orekit-data");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));

        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "tropics";
        }

        OrekitConfig.init();
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);
        
        double earthRadius=Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double[] alts={400000+earthRadius,600000+earthRadius,800000+earthRadius};
        double[] incs={30,51.6,90};
        int[] ts={1,2,3,4,6,8,9,12,16};
        ArrayList<WalkerParameters> constels=EnumerateWalkerConstellations.fullFactWalker(alts, incs, ts);
        

        
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2016, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2016, 1, 14, 00, 00, 00.000, utc);
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        double earth_radius = org.orekit.utils.Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        int coverageGridGranularity = 20; // separation of points by degree

        //define instruments
//        NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
        NadirRectangularFOV fov = new NadirRectangularFOV(FastMath.toRadians(57), FastMath.toRadians(2.5), 0, earthShape);
        ArrayList<Instrument> payload = new ArrayList<>();
        double mass=6;
        double averagePower=10;
        Instrument view1 = new Instrument("view1", fov, mass, averagePower);
        payload.add(view1);
        
        Properties propertiesPropagator = new Properties();
        propertiesPropagator.setProperty("orekit.propagator.mass", "6");
        propertiesPropagator.setProperty("orekit.propagator.atmdrag", "true");
        propertiesPropagator.setProperty("orekit.propagator.dragarea", "0.13"); //worst case scenario 0.3x0.1*4+0.1x0.1
        propertiesPropagator.setProperty("orekit.propagator.dragcoeff", "2.2");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.sun", "true");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.moon", "true");
        propertiesPropagator.setProperty("orekit.propagator.solarpressure", "true");
        propertiesPropagator.setProperty("orekit.propagator.solararea", "0.058");
        
        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN,propertiesPropagator);
        //PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.NUMERICAL,propertiesPropagator);
        

        for (int i=0;i<constels.size();i++){
            double a = constels.get(i).getA();
            double incdeg=constels.get(i).getI();
            double inc = Math.toRadians(incdeg);
            int t = constels.get(i).getT();
            int p = constels.get(i).getP();
            int f = constels.get(i).getF();
             
            Walker walker = new Walker("walker1", payload, a, inc, t, p, f, inertialFrame, startDate, mu);

            CoverageDefinition covDef1 = new CoverageDefinition("covdef1", coverageGridGranularity, earthShape, EQUAL_AREA);
            covDef1.assignConstellation(walker);
            HashSet<CoverageDefinition> covDefs = new HashSet<>();
            covDefs.add(covDef1);
    

            Properties propertiesEventAnalysis = new Properties();
            propertiesEventAnalysis.setProperty("fov.numThreads", "12");


            //set the event analyses
            EventAnalysisFactory eaf = new EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
            ArrayList<EventAnalysis> eventanalyses = new ArrayList<>();
            FieldOfViewEventAnalysis fovEvent = (FieldOfViewEventAnalysis) eaf.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
            eventanalyses.add(fovEvent);

            //set the analyses
            double analysisTimeStep = 60;
            ArrayList<Analysis<?>> analyses = new ArrayList<>();

            Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                    eventAnalysis(eventanalyses).analysis(analyses).
                    covDefs(covDefs).name(String.format("%s_%s_%s_%s_%s", (a-earthRadius)/1000,incdeg,t,p,f)).properties(propertiesEventAnalysis).
                    propagatorFactory(pf).build();
            try {
                Logger.getGlobal().finer(String.format("Running Scenario %s", scen));
                Logger.getGlobal().finer(String.format("Number of points:     %d", covDef1.getNumberOfPoints()));
                Logger.getGlobal().finer(String.format("Number of satellites: %d", walker.getSatellites().size()));
                scen.call();
            } catch (Exception ex) {
                Logger.getLogger(Orekit_Pau.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException("scenario failed to complete.");
        }

            Logger.getGlobal().finer(String.format("Done Running Scenario %s", scen));

            ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename, scen, covDef1, fovEvent);
            ScenarioIO.saveGroundEventAnalysisMetrics(Paths.get(System.getProperty("results"), ""), filename, scen, covDef1, fovEvent);
            ScenarioIO.saveGroundEventAnalyzerObject(Paths.get(System.getProperty("results"), ""), filename, scen,covDef1, fovEvent);
    
    }
    }
    
}
