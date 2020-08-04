package seakers.orekit;

/* author Prachi - Example to compute the number of satellites for a revisit time of 15 minutes*/

import seakers.orekit.event.EventAnalysis;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.constellations.EnumerateWalkerConstellations;
import seakers.orekit.constellations.Walker;
import seakers.orekit.constellations.WalkerParameters;
import seakers.orekit.coverage.analysis.AnalysisMetric;
import seakers.orekit.coverage.analysis.FastCoverageAnalysis;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.util.OrekitConfig;

public class Orekit_Prachi {
    public static void main(String[] args)  throws OrekitException, IOException {

        // output file
        String filename;
        if (args.length > 0){
            filename = args[0];
        }
        else {
            filename = "my_example";
        }

        // initializes the look up tables for planetary position (required!)
        OrekitConfig.init(16);

        //setup logger that spits out runtime info
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        /* setting time for simulation */
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2017, 1, 1, 00, 00, 00, utc);
        AbsoluteDate endDate = new AbsoluteDate(2017, 1, 4, 00, 00, 00, utc);

        // creating an array of altitudes for Walker Constellations
        // What should be the constraints? Max? Min?
        // Where do we get this data from?
        // Can there be two different altitudes in a constellation?
        // these altitudes are semi major axises = altitude + radius of the earth
        double[] semiMajorAxis = new double[7];
        for (int i = 0; i < 7; i++){
            semiMajorAxis[i] = Constants.WGS84_EARTH_EQUATORIAL_RADIUS+(i)*100000+400000;
        }

        // creating an array of inclinations for Walker Constellations
        // Constraints?
        // Plane of Reference?
        // Max? Min? trmm goes moes geostar tropics

        double[] inclinations = new double[16];
        for (int i = 0; i < 16; i++){
            double i_deg = FastMath.toRadians(i*10+10);
            if (i >= 10){
                double kh = 10.10949;
                double cos_i = (Math.pow(((semiMajorAxis[i-10])/Constants.WGS84_EARTH_EQUATORIAL_RADIUS),3.5))/(-kh);
                i_deg = FastMath.toRadians(180*Math.acos(cos_i)/3.1415);
            }
            inclinations[i] = i_deg;
        }

// how to save using an array list
//        ArrayList<Double> inclinationList = new ArrayList<>();
//        for (int i = 0; i < 110; i+=10){
//            inclinationList.add(FastMath.toRadians(i));
//        }
//        Double[] inclinations = new Double[inclinationList.size()];
//        inclinations = inclinationList.toArray(inclinations);

        // creating an array of total number of satellites in Walker Constellations
        /*int[] numberOfSatellites = new int[30];
        for (int i = 0; i < 30; i++){
            numberOfSatellites[i] = i+1;
        }*/

        int[] numberOfSatellites = new int[8];
        int j = 32;
        for (int i = 0; i < 8; i++){
            numberOfSatellites[i] = i+j;
            j = j+3;
        }

        ArrayList<WalkerParameters> constelParams = new ArrayList<>();
        constelParams = EnumerateWalkerConstellations.fullFactWalker(semiMajorAxis, inclinations, numberOfSatellites);

        //set the frame of reference i.e. inertial
        Frame inertialFrame = FramesFactory.getEME2000();
        Frame earthFrame = null;
        try{
            earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003,true);
        } catch (OrekitException e) {
            System.exit(1);
        }
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,Constants.WGS84_EARTH_FLATTENING, earthFrame);

        // define instruments and payload
        NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
        ArrayList<Instrument> payload = new ArrayList<>();
        Instrument view1 = new Instrument("view1", fov, 100, 100);
        payload.add(view1);

        // gravitation coefficient
        double mu = Constants.WGS84_EARTH_MU;

        ArrayList<Double> averageRevisitTime = new ArrayList<>();
        ArrayList<Double> meanResponseTime = new ArrayList<>();

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File("revisitTime5.csv")));
        bufferedWriter.append("A,I,T,P,F,Rev,Resp");
        bufferedWriter.newLine();
        
        for(WalkerParameters params : constelParams){

            Walker walker = new Walker("walker1", payload, params.getA(), params.getI(), params.getT(), params.getP(), params.getF(), inertialFrame, startDate, mu);

            //set the type of propagation - probably use J2
            // Walker constellation is circular
            PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2, new Properties());

            //can set the number of resources available for propagation
            Properties propertiesEventAnalysis = new Properties();

            //create a coverage definition -equal area
            CoverageDefinition coverageDefinition = new CoverageDefinition("coverageDef", 20, earthShape, CoverageDefinition.GridStyle.EQUAL_AREA);
            coverageDefinition.assignConstellation(walker);

            HashSet<CoverageDefinition> covDefs = new HashSet<>();
            covDefs.add(coverageDefinition);

            //set the event analyses
//            EventAnalysisFactory eventAnalysisFactory = new EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
            ArrayList<EventAnalysis> eventAnalyses = new ArrayList<>();
//             FieldOfViewEventAnalysis fovEvent = (FieldOfViewEventAnalysis) eventAnalysisFactory.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
//            eventAnalyses.add(fovEvent);
            FastCoverageAnalysis fca = new FastCoverageAnalysis(startDate,endDate,inertialFrame,covDefs,FastMath.toRadians(45));

            //building the scenario
            Scenario scene = new Scenario.Builder(startDate, endDate, utc).eventAnalysis(eventAnalyses).covDefs(covDefs).name("CoverageExample").properties(propertiesEventAnalysis).propagatorFactory(pf).build();

            try {
                fca.call();
            } catch (Exception e) {
                e.printStackTrace();
            }

            GroundEventAnalyzer groundEventAnalyzer = new GroundEventAnalyzer(fca.getEvents(coverageDefinition));
            DescriptiveStatistics stats1 = groundEventAnalyzer.getStatistics(AnalysisMetric.DURATION,false, new Properties());
            DescriptiveStatistics stats2 = groundEventAnalyzer.getStatistics(AnalysisMetric.MEAN_TIME_TO_T,false, new Properties());
            averageRevisitTime.add(stats1.getMean());
            meanResponseTime.add(stats2.getMean());

            //saves the start and stop time of each access at each ground point
//            ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename + "_fov", scene, coverageDefinition, fca);

            //saves the gap metrics in a csv file for each ground point
           // ScenarioIO.saveGroundEventAnalysisMetrics(Paths.get(System.getProperty("results"), ""), filename + "_fov_metrics", scene, groundEventAnalyzer, AnalysisMetric.DURATION, false);

            long end = System.nanoTime();
            //Logger.getGlobal().finest(String.format("Took %.4f sec", (end - start) / Math.pow(10, 9)));

            System.out.println(String.format("Finished %d out of %d", averageRevisitTime.size(),constelParams.size()));
        
           
            bufferedWriter.append(Double.toString(params.getA()) + ",");
            bufferedWriter.append(Double.toString(params.getI()) + ",");
            bufferedWriter.append(Double.toString(params.getT()) + ",");
            bufferedWriter.append(Double.toString(params.getP()) + ",");
            bufferedWriter.append(Double.toString(params.getF()) + ",");
            bufferedWriter.append(Double.toString(stats1.getMean()) + ",");
            bufferedWriter.append(Double.toString(stats2.getMean()));
            bufferedWriter.newLine();
            bufferedWriter.flush();
                
           
        }

        OrekitConfig.end();
    }
}