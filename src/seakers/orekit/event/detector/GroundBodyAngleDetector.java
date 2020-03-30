/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoveragePoint;

/**
 * Detects when the angle between a celestial body (e.g. sun or moon) and a
 * direction centered on a geodetic point surpasses a threshold. The geodetic
 * point is on the surface of the Earth and direction is given in a topocentric
 * ECE (Earth centric Earth-fixed), where the X axis in the local horizontal
 * plane (normal to zenith direction) and following the local parallel towards
 * East, Y axis in the horizontal plane (normal to zenith direction), and
 * following the local meridian towards North Z axis towards the Zenith
 * direction.
 *
 * @author nhitomi
 */
public class GroundBodyAngleDetector extends AbstractEventDetector<GroundBodyAngleDetector> {

    private static final long serialVersionUID = 1L;

    /**
     * The ground target
     */
    private final CoveragePoint target;

    /**
     * The maximum allowable angle from the sun to the desired direction.
     */
    private final double maxAngle;

    /**
     * The direction used to compute the maximum allowable angle from.
     */
    private final Vector3D direction;

    /**
     * The celestial body
     */
    private final CelestialBody body;

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable angle (e.g. max elevation angle). This constructor
     * assumes that the threshold is with respect to the Zenith direction. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @param body the celestial body
     * @param maxAngle the maximum allowable angle between the sun and the
     * Zenith direction of the ground target [rad]
     * @throws org.orekit.errors.OrekitException
     */
    public GroundBodyAngleDetector(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate,
            CoveragePoint target, CelestialBody body, double maxAngle) throws OrekitException {
        this(initialState, startDate, endDate, target, body, maxAngle, Vector3D.PLUS_K);
    }

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable angle (e.g. max elevation angle). This constructor
     * assumes that the threshold is with respect to the Zenith direction. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @param body the celestial body
     * @param maxAngle the maximum allowable angle between the sun and the given
     * direction [rad]
     * @param direction the direction in the topocentric frame of the point.
     * @throws org.orekit.errors.OrekitException
     */
    public GroundBodyAngleDetector(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate, CoveragePoint target, CelestialBody body,
            double maxAngle, Vector3D direction) throws OrekitException {
        this(initialState, startDate, endDate, target, body, maxAngle, direction,
                EventHandler.Action.STOP, DEFAULT_MAXCHECK,
                DEFAULT_THRESHOLD, DEFAULT_MAX_ITER);
    }

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable angle (e.g. max elevation angle). This constructor
     * assumes that the threshold is with respect to the Zenith direction. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @param body the celestial body
     * @param maxAngle the maximum allowable angle between the sun and the given
     * direction [rad]
     * @param direction the direction in the topocentric frame of the point.
     * @throws org.orekit.errors.OrekitException
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public GroundBodyAngleDetector(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate, CoveragePoint target, CelestialBody body,
            double maxAngle, Vector3D direction, double maxCheck,
            double threshold) throws OrekitException {
        this(initialState, startDate, endDate,
                target, body, maxAngle, direction, EventHandler.Action.STOP,
                DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER);
    }

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable angle (e.g. max elevation angle). This constructor
     * assumes that the threshold is with respect to the Zenith direction. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @param body the celestial body
     * @param maxAngle the maximum allowable angle between the sun and the given
     * direction [rad]
     * @param direction the direction in the topocentric frame of the point.
     * @throws org.orekit.errors.OrekitException
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action specifies action after event is detected.
     */
    public GroundBodyAngleDetector(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate, CoveragePoint target, CelestialBody body,
            double maxAngle, Vector3D direction, EventHandler.Action action,
            double maxCheck, double threshold) throws OrekitException {
        this(initialState, startDate, endDate,
                target, body, maxAngle, direction, action,
                DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER);
    }

    /**
     * Constructor for the detector. Must provide a ground target and the
     * maximum allowable angle (e.g. max elevation angle). This constructor
     * assumes that the threshold is with respect to the Zenith direction. Max
     * check for step size is set to 600.0 seconds by default. Threshold for
     * event detection is set to default 1e-6 seconds. This detector by default
     * stops when an event is detected.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param target the ground target to attach the detector to
     * @param body the celestial body
     * @param maxAngle the maximum allowable angle between the sun and the given
     * direction [rad]
     * @param direction the direction in the topocentric frame of the point.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param action specifies action after event is detected.
     * @param maxIter maximum number of iterations in the event time search
     * @throws org.orekit.errors.OrekitException
     */
    public GroundBodyAngleDetector(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate,
            CoveragePoint target, CelestialBody body, double maxAngle, Vector3D direction,
            EventHandler.Action action, double maxCheck, double threshold,
            int maxIter) throws OrekitException {
        super(initialState, startDate, endDate, action, maxCheck, threshold, maxIter);
        this.target = target;
        this.maxAngle = maxAngle;
        this.direction = direction;
        this.body = body;
    }

    @Override
    public double g(SpacecraftState s) throws OrekitException {
        //the target position in the inertial frame
        final Vector3D targetPosInert
                = target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D sunPosInert
                = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D targetToSunPosInert = targetPosInert.add(sunPosInert);

        final Transform trans = s.getFrame().getTransformTo(target, s.getDate());

        final Vector3D targetToSunPosTopo = trans.transformVector(targetToSunPosInert);

        double p = maxAngle - Vector3D.angle(direction, targetToSunPosTopo);
        return p;
    }

    @Override
    protected GroundBodyAngleDetector create(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate,
            EventHandler.Action action, double maxCheck, double threshold, int maxIter) {
        try {
            return new GroundBodyAngleDetector(initialState, startDate, endDate, target, body, maxAngle, direction, action, maxCheck, threshold, maxIter);
        } catch (OrekitException ex) {
            Logger.getLogger(GroundBodyAngleDetector.class.getName()).log(Level.SEVERE, null, ex);
        }
        throw new IllegalStateException("Could not create GroundSunAngleDetector. Check creation of Sun Object");
    }

}
