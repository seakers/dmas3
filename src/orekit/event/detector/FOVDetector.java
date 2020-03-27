/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import seakers.orekit.object.Instrument;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/**
 * Detector responsible for tracking when the target enters an instrument's
 * field of view
 *
 * @author nozomihitomi
 */
public class FOVDetector extends AbstractEventDetector<FOVDetector> {

    private static final long serialVersionUID = 3765360560924280705L;

    private final TopocentricFrame target;

    private final Instrument instrument;

    /**
     * Constructor for the detector. Must use a instrument/target pair. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the target to attach the detector to
     * @param instrument the instrument that will observe the target\
     */
    public FOVDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate,
            TopocentricFrame target, Instrument instrument) {
        this(initialState, startDate, endDate, target, instrument,
                DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, EventHandler.Action.STOP);
    }

    /**
     * Constructor for the detector. Must use a instrument/target pair. Can set
     * the resolution at which access time is computed. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the target to attach the detector to
     * @param instrument the instrument that will observe the target resolution
     * of the access times
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public FOVDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate,
            TopocentricFrame target, Instrument instrument, double maxCheck, double threshold) {
        this(initialState, startDate, endDate, target, instrument,
                maxCheck, threshold, DEFAULT_MAX_ITER, EventHandler.Action.STOP);
    }

    /**
     * Constructor for the detector. Must use a instrument/target pair. Can set
     * the resolution at which access time is computed
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the target to attach the detector to
     * @param instrument the instrument that will observe the target resolution
     * of the access times
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action specifies action after event is detected.
     */
    public FOVDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate,
            TopocentricFrame target, Instrument instrument, double maxCheck, double threshold, EventHandler.Action action) {
        this(initialState, startDate, endDate, target, instrument,
                maxCheck, threshold, DEFAULT_MAX_ITER, action);
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
     * @param pvTarget Position/velocity provider of the considered target
     * @param instrument the instrument that will observe the target
     */
    private FOVDetector(final SpacecraftState initialState,
            final AbsoluteDate startDate, final AbsoluteDate endDate,
            final TopocentricFrame target, final Instrument instrument,
            final double maxCheck, final double threshold, final int maxIter, EventHandler.Action action) {
        super(initialState, startDate, endDate, action, maxCheck, threshold, maxIter);

        this.instrument = instrument;
        this.target = target;
    }

    /**
     * Get the position/velocity provider of the target .
     *
     * @return the position/velocity provider of the target
     */
    public TopocentricFrame getPVTarget() {
        return target;
    }

    /**
     * The implementation of this g() function relies on the implementation of
     * the FieldOfViewDetector but first computes line of sight. If there is no
     * line of sight between the satellite and the target, the more expensive
     * computation of the FieldOfViewDetector g function is not executed. This
     * g() function is positive when the target enters the field of view and
     * negative if the target is outside the field of view
     *
     * @param s
     * @return
     * @throws OrekitException
     */
    @Override
    public double g(SpacecraftState s) throws OrekitException {
        //only compute the more expensive FieldOfViewDetector g function if 
        //the target and satellite meet the minimum elevation threshold
        return instrument.getFOV().g_FOV(s, target);
    }

    @Override
    protected FOVDetector create(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate, EventHandler.Action action, double maxCheck, double threshold, int maxIter) {
        return new FOVDetector(initialState, startDate, endDate, target, instrument, maxCheck, threshold, maxIter, action);
    }
}
