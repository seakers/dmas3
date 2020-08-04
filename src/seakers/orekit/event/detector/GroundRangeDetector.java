/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/**
 * Detects when an a ground point is in range of a satellite
 *
 * @author nhitomi
 */
public class GroundRangeDetector extends AbstractEventDetector<GroundRangeDetector> {

    private static final long serialVersionUID = 1L;

    /**
     * the ground target to attach the detector to
     */
    private final TopocentricFrame target;

    /**
     * maximum allowable range [m]
     */
    private final double rangeThreshold;

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable range [m]. Max check for step size is set to 600.0
     * seconds by default. Threshold for event detection is set to default 1e-6
     * seconds. This detector by default stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @param rangeThreshold maximum allowable range [m]
     */
    public GroundRangeDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate, TopocentricFrame target, double rangeThreshold) {
        super(initialState, startDate, endDate);
        this.target = target;
        this.rangeThreshold = rangeThreshold;
    }

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable range [m]. Max check for step size is set to 600.0
     * seconds by default. Threshold for event detection is set to default 1e-6
     * seconds. This detector by default stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @param rangeThreshold maximum allowable range [m]
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     *
     */
    public GroundRangeDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate, TopocentricFrame target, double rangeThreshold, double maxCheck, double threshold) {
        super(initialState, startDate, endDate, maxCheck, threshold);
        this.target = target;
        this.rangeThreshold = rangeThreshold;
    }

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable range [m].
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @param rangeThreshold maximum allowable range [m]
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action specifies action after event is detected.
     * @param maxIter maximum number of iterations in the event time search
     *
     */
    public GroundRangeDetector(SpacecraftState initialState, 
            AbsoluteDate startDate, AbsoluteDate endDate, 
            TopocentricFrame target, double rangeThreshold, 
            EventHandler.Action action, double maxCheck, double threshold, int maxIter) {
        super(initialState, startDate, endDate, action, maxCheck, threshold, maxIter);
        this.target = target;
        this.rangeThreshold = rangeThreshold;
    }

    @Override
    public double g(SpacecraftState s) throws OrekitException {
        //the target position in the inertial frame
        final Vector3D targetPosInert
                = target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();

        return rangeThreshold - Vector3D.distance(targetPosInert, s.getPVCoordinates(s.getFrame()).getPosition());
    }

    @Override
    protected GroundRangeDetector create(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate, 
            EventHandler.Action action, double maxCheck, double threshold, int maxIter) {
        return new GroundRangeDetector(initialState, startDate, endDate, target, rangeThreshold, action, maxCheck, threshold, maxIter);
    }

}
