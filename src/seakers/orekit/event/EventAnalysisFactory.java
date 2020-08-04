/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;
import seakers.orekit.parallel.ParallelRoutine;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * Creates a multitude of event analyses.
 *
 * @author nhitomi
 */
public class EventAnalysisFactory {

    /**
     * Analysis start date
     */
    private final AbsoluteDate startDate;

    /**
     * Analysis end date
     */
    private final AbsoluteDate endDate;

    /**
     * A factory to create propagators
     */
    private PropagatorFactory propagatorFactory;

    /**
     * Inertial frame
     */
    private final Frame inertialFrame;

    /**
     * Constructor specifying the propagator factory to use,
     *
     * @param startDate the start date of the analysis
     * @param endDate the end date of the analysis
     * @param inertialFrame the inertial frame of reference
     * @param propagatorFactory the propagator factory to create the propagators
     * used in the analyses
     */
    public EventAnalysisFactory(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame,
            PropagatorFactory propagatorFactory) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.propagatorFactory = propagatorFactory;
        this.inertialFrame = inertialFrame;
    }

    /**
     * Creates event analyses pertaining to coverage or ground points
     *
     * @param type
     * @param covDefs The coverage definitions that contains informations about
     * the target regions and the constellations assigned to view/access those
     * regions
     * @param prop
     * @return
     */
    public EventAnalysis createGroundPointAnalysis(EventAnalysisEnum type, Set<CoverageDefinition> covDefs, Properties prop) {
        EventAnalysis ea = null;

        switch (type) {
            case FOV:
                //Option to record the accesses of the individual satellites in 
                //the constellation and keep them separate from the combined 
                //access times of the constellation as a whole. By default, 
                //the accesses of the individual satellites are not kept in memory to conserve memory space
                String saveAllAccessesStr = prop.getProperty("fov.saveAccess", "false");

                //Option to dictate whether the coverage accesses of individual 
                //satellites should be saved to the coverage database. 
                //By default the accesses are not stored to the database.
                String saveToDBStr = prop.getProperty("fov.saveToDB", "false");

                ea = new FieldOfViewEventAnalysis(startDate, endDate, inertialFrame,
                        covDefs, propagatorFactory,
                        Boolean.parseBoolean(saveAllAccessesStr),
                        Boolean.parseBoolean(saveToDBStr));
                break;
            case GND_BODY_ANGLE:
                //Option to set the angle threshold for the angle between a 
                //specified topocentric direction and the celestial body
                double threshold = Double.parseDouble(prop.getProperty("gndbodyangle.threshold", "1.570795"));
                //Option to set the topocentric, where +X is east, +Y is north, and +Z is zenith
                double x = Double.parseDouble(prop.getProperty("gndbodyangle.x", "0"));
                double y = Double.parseDouble(prop.getProperty("gndbodyangle.y", "0"));
                double z = Double.parseDouble(prop.getProperty("gndbodyangle.z", "1"));

                Vector3D direction = new Vector3D(x, y, z);

                String bodyStr = prop.getProperty("gndbodyangle.body", "SUN");
                CelestialBody body;
                try {
                    body = CelestialBodyFactory.getBody(bodyStr);
                    ea = new GroundBodyAngleEventAnalysis(startDate, endDate, inertialFrame, covDefs, body, threshold, direction);
                } catch (OrekitException ex) {
                    Logger.getLogger(EventAnalysisFactory.class.getName()).log(Level.SEVERE, null, ex);
                    throw new UnsupportedOperationException(String.format("No known celestial body: %s", bodyStr));
                }

                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Analysis type %s is unsupported.", type));
        }

        return ea;
    }

    /**
     * Creates event analyses pertaining to ground stations
     * @param type the type of analysis
     * @param stationAssignment the assignment of satellites to ground stations
     * @param prop the properties for the analysis
     * @return 
     */
    public EventAnalysis createGroundStationAnalysis(EventAnalysisEnum type,
            HashMap<Satellite, Set<GndStation>> stationAssignment, Properties prop) {
        EventAnalysis ea = null;

        switch (type) {
            case ACCESS:
                ea = new GndStationEventAnalysis(startDate, endDate, inertialFrame,
                        stationAssignment, propagatorFactory);
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Analysis type %s is unsupported.", type));
        }

        return ea;
    }

}
