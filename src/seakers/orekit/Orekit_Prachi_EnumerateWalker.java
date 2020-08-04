/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
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
import seakers.orekit.analysis.Analysis;
import seakers.orekit.constellations.EnumerateWalkerConstellations;
import seakers.orekit.constellations.Walker;
import seakers.orekit.constellations.WalkerParameters;
import seakers.orekit.coverage.analysis.AnalysisMetric;
import seakers.orekit.coverage.analysis.FastCoverageAnalysis;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.EventAnalysisFactory;
import seakers.orekit.event.FieldOfViewEventAnalysis;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.object.fieldofview.NadirRectangularFOV;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;

/**
 *
 * @author Prachi
 */
public class Orekit_Prachi_EnumerateWalker {
    
    public static void main(String[] args) throws OrekitException {
        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));
        long start = System.nanoTime();

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "tropics";
        }

        OrekitConfig.init(1);
        //setup logger
        Level level = Level.ALL;
        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2020, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2020, 1, 8, 00, 00, 00.000, utc);
    
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient

        //must use IERS_2003 and EME2000 frames to be consistent with STK
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        Frame inertialFrame = FramesFactory.getEME2000();

        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        
        
        double[] alt = new double[2];
        alt = new double[] {400,500};
        for(int i_alt = 0; i_alt<alt.length; i_alt++) {
            alt[i_alt] = alt[i_alt]*1000 + 6378000; //convert to semi major axis and meters
        }
        
        double[] inc = new double[2];
        inc = new double[] {30,40};
        for(int i_inc = 0; i_inc<inc.length; i_inc++) {
            inc[i_inc]=FastMath.toRadians(inc[i_inc]);
        }
   
        int[] sats = new int[2];
        for(int i_nsats = 1; i_nsats<=2;i_nsats++) {
            sats[i_nsats-1]=i_nsats;
        }
        
        //define walker parameters
        ArrayList<WalkerParameters> constelParams = new ArrayList<>();
        constelParams = EnumerateWalkerConstellations.fullFactWalker(alt, inc, sats);
        
        //define instruments
        //NadirSimpleConicalFOV fov = new NadirSimpleConicalFOV(FastMath.toRadians(45), earthShape);
        ArrayList<Instrument> payload=new ArrayList<>();
        NadirRectangularFOV fov = new NadirRectangularFOV(FastMath.toRadians(57), FastMath.toRadians(20), 0, earthShape);
        Instrument view1 = new Instrument("view1", fov, 420, 590);
        payload.add(view1);

        ArrayList<Double> averageRevisitTime = new ArrayList<>();
        ArrayList<Double> meanResponseTime = new ArrayList<>();

        HashMap<String,Satellite> map=new HashMap<>();
        
        for(WalkerParameters params : constelParams){

            Walker walker = new Walker("walker", payload, params.getA(), params.getI(), params.getT(), params.getP(), params.getF(), inertialFrame, startDate, mu);
//
//            
//            //set the type of propagation - probably use J2
//            // Walker constellation is circular
//            PropagatorFactory pf = new PropagatorFactory(PropagatorType.J2, new Properties());
//
//            //can set the number of resources available for propagation
//            Properties propertiesEventAnalysis = new Properties();
//
//            //create a coverage definition -equal area
//            CoverageDefinition coverageDefinition = new CoverageDefinition("coverageDef", 15, -30, 30, -180, 180, earthShape, CoverageDefinition.GridStyle.EQUAL_AREA);
//            coverageDefinition.assignConstellation(walker);
//
//            HashSet<CoverageDefinition> covDefs = new HashSet<>();
//            covDefs.add(coverageDefinition);
//
//            //set the event analyses
////            EventAnalysisFactory eventAnalysisFactory = new EventAnalysisFactory(startDate, endDate, inertialFrame, pf);
//            ArrayList<EventAnalysis> eventAnalyses = new ArrayList<>();
////            FieldOfViewEventAnalysis fovEvent = (FieldOfViewEventAnalysis) eventAnalysisFactory.createGroundPointAnalysis(EventAnalysisEnum.FOV, covDefs, propertiesEventAnalysis);
////            eventAnalyses.add(fovEvent);
//            FastCoverageAnalysis fca = new FastCoverageAnalysis(startDate,endDate,inertialFrame,covDefs,FastMath.toRadians(45));
//
//            //building the scenario
//            Scenario scene = new Scenario.Builder(startDate, endDate, utc).eventAnalysis(eventAnalyses).covDefs(covDefs).name("CoverageExample").properties(propertiesEventAnalysis).propagatorFactory(pf).build();
//
//            try {
//                fca.call();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            GroundEventAnalyzer groundEventAnalyzer = new GroundEventAnalyzer(fca.getEvents(coverageDefinition));
//            DescriptiveStatistics stats1 = groundEventAnalyzer.getStatistics(AnalysisMetric.DURATION,false, new Properties());
//            DescriptiveStatistics stats2 = groundEventAnalyzer.getStatistics(AnalysisMetric.MEAN_TIME_TO_T,false, new Properties());
//            averageRevisitTime.add(stats1.getMean());
//            meanResponseTime.add(stats2.getMean());
//
//            //saves the start and stop time of each access at each ground point
////            ScenarioIO.saveGroundEventAnalysis(Paths.get(System.getProperty("results"), ""), filename + "_fov", scene, coverageDefinition, fca);
//
//            //saves the gap metrics in a csv file for each ground point
//           // ScenarioIO.saveGroundEventAnalysisMetrics(Paths.get(System.getProperty("results"), ""), filename + "_fov_metrics", scene, groundEventAnalyzer, AnalysisMetric.DURATION, false);
//
//            long end = System.nanoTime();
//            //Logger.getGlobal().finest(String.format("Took %.4f sec", (end - start) / Math.pow(10, 9)));
//
//            System.out.println(String.format("Finished %d out of %d", averageRevisitTime.size(),constelParams.size()));
        
        for(Satellite sat:walker.getSatellites()){
                map.put(String.valueOf(sat.hashCode()), sat);
            }
        }
        
        int uniqueSats=map.size();
        System.out.printf("%d",uniqueSats);

        try(BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File("revisitTime.csv")))){
            bufferedWriter.append("A,I,T,P,F,Rev,Resp");
            bufferedWriter.newLine();
            for (int i = 0; i < constelParams.size(); i++){
                bufferedWriter.append(Double.toString(constelParams.get(i).getA()) + ",");
                bufferedWriter.append(Double.toString(constelParams.get(i).getI()) + ",");
                bufferedWriter.append(Double.toString(constelParams.get(i).getT()) + ",");
                bufferedWriter.append(Double.toString(constelParams.get(i).getP()) + ",");
                bufferedWriter.append(Double.toString(constelParams.get(i).getF()) + ",");
                bufferedWriter.append(Double.toString(averageRevisitTime.get(i)) + ",");
                bufferedWriter.append(Double.toString(meanResponseTime.get(i)));
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        OrekitConfig.end();
    }
}
