/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import seakers.orekit.object.CoveragePoint;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Calculates when a satellite has line of sight to a ground point
 *
 * @author nozomihitomi
 */
public class LOSDetector extends AbstractEventDetector<LOSDetector> {

    private static final long serialVersionUID = 2969875053072513593L;

    /**
     * The ground point that needs to be viewed
     */
    private final CoveragePoint pt;

    /**
     * The shape of the body on which the point lies
     */
    private final BodyShape shape;

    /**
     * The inertial frame used in the scenario
     */
    private final Frame inertialFrame;

    /**
     * The minimum radius of the earth (north-south direction)
     */
    private final double minRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING);

    /**
     * Build a new line of sight detector.
     * <p>
     * This simple constructor takes default values for maximal checking
     * interval ({@link #DEFAULT_MAXCHECK}) and convergence threshold
     * ({@link #DEFAULT_THRESHOLD}). This detector by default stops when an event is detected.</p>
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param pt The ground point that needs to be viewed
     * @param shape The shape of the body on which the point lies
     * @param inertialFrame The inertial frame used in the scenario evaluated
     */
    public LOSDetector(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate, 
            final CoveragePoint pt, final BodyShape shape, final Frame inertialFrame) {
        this(initialState, startDate, endDate, pt, shape, inertialFrame, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, EventHandler.Action.STOP);
    }

    /**
     * Build a new line of sight detector.
     * <p>
     * This detector by default stops when an event is detected.</p>
     * <p>
     * The maximal interval between line of sight checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param pt The ground point that needs to be viewed
     * @param shape The shape of the body on which the point lies
     * @param inertialFrame The inertial frame used in the scenario evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public LOSDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate,
            final CoveragePoint pt, final BodyShape shape, final Frame inertialFrame, final double maxCheck, final double threshold) {
        this(initialState, startDate, endDate, pt, shape, inertialFrame, maxCheck, threshold, DEFAULT_MAX_ITER, EventHandler.Action.STOP);
    }
    
     /**
     * Build a new line of sight detector.
     * <p>The maximal interval between line of sight checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param pt The ground point that needs to be viewed
     * @param shape The shape of the body on which the point lies
     * @param inertialFrame The inertial frame used in the scenario evaluated
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action specifies action after event is detected.
     */
    public LOSDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate,
            final CoveragePoint pt, final BodyShape shape, final Frame inertialFrame, 
            final double maxCheck, final double threshold, EventHandler.Action action) {
        this(initialState, startDate, endDate, pt, shape, inertialFrame, maxCheck, threshold, DEFAULT_MAX_ITER, action);
    }

    /**
     * Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder API
     * with the various {@code withXxx()} methods to set up the instance in a
     * readable manner without using a huge amount of parameters.
     * </p>
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param pt The ground point that needs to be viewed
     * @param shape The shape of the body on which the point lies
     * @param inertialFrame The inertial frame used in the scenario evaluated
     */
    private LOSDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate,
            final CoveragePoint pt, final BodyShape shape, final Frame inertialFrame, final double maxCheck, final double threshold,
            final int maxIter, EventHandler.Action action) {
        super(initialState, startDate, endDate, action, maxCheck, threshold, maxIter);
        this.pt = pt;
        this.shape = shape;
        this.inertialFrame = inertialFrame;
    }

    @Override
    public double g(SpacecraftState s) throws OrekitException {
        // The spacecraft position in the inertial frame
        Vector3D satPosInertNorm = s.getPVCoordinates(inertialFrame).getPosition().normalize();

        Vector3D ptPosInertNorm = pt.getPVCoordinates(s.getDate(), inertialFrame).getPosition().normalize();

        double cosThetas = ptPosInertNorm.dotProduct(satPosInertNorm);

        //the mininum cos(theta) value required for line of sight
        double minCosTheta = minRadius / s.getA();

        //losVal > 0 means that sat has line of sight
        return cosThetas - minCosTheta;
    }

    @Override
    protected LOSDetector create(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate, 
            EventHandler.Action action, double maxCheck, double threshold, int maxIter) {
        return new LOSDetector(initialState, startDate, endDate, pt, shape, inertialFrame, maxCheck, threshold, maxIter, action);
    }

}
