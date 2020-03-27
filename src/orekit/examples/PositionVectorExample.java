/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.examples;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.constellations.Walker;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import seakers.orekit.analysis.vectors.VectorAnalysis;

/**
 * A minimal working example of how to set up a simulation to compute and record
 * the position vector of a satellite propagated over time
 *
 * @author nozomihitomi
 */
public class PositionVectorExample {

    /**
     * @param args the command line arguments
     * @throws org.orekit.errors.OrekitException
     */
    public static void main(String[] args) throws OrekitException {
        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));
        long start = System.nanoTime();

        String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "PositionVectorExample";
        }

        //initializes the look up tables for planteary position (required!)
        OrekitConfig.init();

        //define the start and end date of the simulation
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate startDate = new AbsoluteDate(2016, 1, 1, 00, 00, 00.000, utc);
        AbsoluteDate endDate = new AbsoluteDate(2016, 1, 5, 00, 00, 00.000, utc);

        //define the scenario parameters
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient
        Frame inertialFrame = FramesFactory.getEME2000();

        //Enter satellite orbital parameters
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 600000;
        double i = FastMath.toRadians(45);

        //Create a walker constellation
        ArrayList<Instrument> payload = new ArrayList<>();
        Walker walker = new Walker("walker1", payload, a, i, 2, 1, 0, inertialFrame, startDate, mu);

        //set parameters for numerical propagator
        Properties propertiesPropagator = new Properties();
        propertiesPropagator.setProperty("orekit.propagator.atmdrag", "true");
        propertiesPropagator.setProperty("orekit.propagator.dragarea", "10");
        propertiesPropagator.setProperty("orekit.propagator.dragcoeff", "2.2");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.sun", "true");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.moon", "true");
        propertiesPropagator.setProperty("orekit.propagator.solarpressure", "true");
        propertiesPropagator.setProperty("orekit.propagator.solararea", "10");
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.NUMERICAL, propertiesPropagator);

        //set the analyses
        double analysisTimeStep = 60;
        ArrayList<Analysis<?>> analyses = new ArrayList<>();
        for (final Satellite sat : walker.getSatellites()) {

            //create an analysis to compute the position vector of the spacecraft in the inertial frame
            //need to implement an abstract class
            analyses.add(new VectorAnalysis(startDate, endDate, analysisTimeStep, sat, pf, inertialFrame) {
                @Override
                public Vector3D getVector(SpacecraftState currentState, Frame frame) throws OrekitException {
                    return currentState.getPVCoordinates(frame).getPosition();
                }

                @Override
                public String getName() {
                    return sat.getName();
                }
            });
        }

        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                analysis(analyses).
                name("PositionVectorExample").
                propagatorFactory(pf).build();
        try {
            System.out.println(String.format("Running Scenario %s", scen));
            System.out.println(String.format("Number of satellites: %d", walker.getSatellites().size()));
            scen.call();
        } catch (Exception ex) {
            Logger.getLogger(PositionVectorExample.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        for (Analysis<?> analysis : analyses) {
            ScenarioIO.saveAnalysis(Paths.get(System.getProperty("results"), ""),
                    String.format("%s_%s", filename, analysis.getName()), analysis);
        }
        long end = System.nanoTime();
        System.out.println(String.format("Took %.4f sec", (end - start) / Math.pow(10, 9)));
        
        OrekitConfig.end();
    }

}
