/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_MAX_ITER;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/**
 *
 * @author paugarciabuzzi
 */
public class LifeTimeDetector extends AbstractEventDetector<LifeTimeDetector> {

    private static final long serialVersionUID = -6354518769897706495L;

    /**
     * The altitude below which the satellite is considered to have reached end of life [m]
     */
    private final double thresholdAltitude;
    /**
     * Constructor for the detector. Threshold for event detection is set to
     * default 1e-6 seconds
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param thresholdAltitude The altitude below which the satellite is considered to have reached end of life [m]
     */
    public LifeTimeDetector(final SpacecraftState initialState,
            final AbsoluteDate startDate, final AbsoluteDate endDate, double thresholdAltitude) {
        super(initialState, startDate, endDate);
        this.thresholdAltitude = thresholdAltitude;
    }

    /**
     * Constructor for the detector. Can set the resolution at which access time
     * is computed
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param thresholdAtltitude The altitude below which the satellite is considered to have reached end of life [m]
     * @param maxCheck maximum checking interval (s)
     * @param threshold threshold in seconds that determines the temporal
     * @param action specifies action after event is detected.
     */
    public LifeTimeDetector(final SpacecraftState initialState,
            final AbsoluteDate startDate, final AbsoluteDate endDate, double thresholdAtltitude, double maxCheck, double threshold, EventHandler.Action action) {
        super(initialState, startDate, endDate, action, maxCheck, threshold, DEFAULT_MAX_ITER);
        this.thresholdAltitude = thresholdAtltitude;
    }

    /**
     * This g() function is positive when the link budget is closed (distance
     * smaller than dmax) and negative if the satellite cannot communicate with
     * the ground point with enough power margin.
     *
     * @param s Spacecraft state
     * @return
     * @throws OrekitException
     */
    @Override
    public double g(SpacecraftState s) throws OrekitException {
        double satAltitude = s.getA()-Constants.WGS84_EARTH_EQUATORIAL_RADIUS; //might need to change that for non-circular orbits
        return satAltitude - this.thresholdAltitude;
    }

    @Override
    protected LifeTimeDetector create(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate, EventHandler.Action action, double maxCheck, double threshold, int maxIter) {
        return new LifeTimeDetector(initialState, startDate, endDate, thresholdAltitude, maxCheck, threshold, action);
    }

}
