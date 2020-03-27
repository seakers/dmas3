/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.attitude;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.GroundPointing;
import org.orekit.attitudes.SpinStabilized;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/**
 *
 * /**
 * This class handles yaw steering law.
 * <p>
 * Yaw steering is used for satellites with no missions-related constraints on
 * yaw angle. It propagates the yaw at a constant rate either unidirectional or
 * bidirectional without changing the roll and pitch.
 * </p>
 * <p>
 * This attitude is implemented as a wrapper on top of an underlying ground
 * pointing law that defines the roll and pitch angles.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 *
 * @see GroundPointing
 */
public class OscillatingYawSteering extends SpinStabilized {

    /**
     * Serializable UID.
     */
    private static final long serialVersionUID = 20150529L;

    /**
     * the angular velocity [rad/s] to rotate in yaw.
     */
    private final double angularVelocity;

    /**
     * the angle at which the rotation in yaw should begin
     */
    private final double startAngle;

    /**
     * the angle at which the rotation in yaw should end (oscillates between
     * startAngle and endAngle)
     */
    private final double endAngle;

    /**
     * the angle at which the spacecraft is initially offset in yaw
     */
    private final double offsetAngle;

    /**
     * Flag for unidirectional or bidirectional;
     */
    private final boolean isUnidirectional;

    /**
     * the start date when oscillating yaw steering shall begin
     */
    private final AbsoluteDate startDate;

    /**
     * Creates a new instance of the yaw steering law. This constructor is for a
     * bidirectional, constant angular speed yaw law. The yaw oscillates between
     * startAngle and endAngle
     *
     * @param nonRotatingLaw underlying non-rotating attitude provider
     * @param startDate the start date when oscillating yaw steering shall begin
     * @param axis rotation axis in satellite frame
     * @param angularVelocity the angular velocity [rad/s] to rotate in yaw. In
     * the case of bidirectional, the motion is sinusoidal and angularVelocity
     * describes the average velocity over one period.
     * @param startAngle the angle at which the rotation in yaw should begin. In
     * radians[-pi, pi]
     * @param endAngle the angle at which the rotation in yaw should end
     * (oscillates between startAngle and endAngle). In radians[-pi, pi]
     * @param offsetAngle the angle at which the spacecraft is initially offset
     * in yaw. In radians[-pi, pi]
     * @exception OrekitException if the frame specified is not a
     * pseudo-inertial frame
     * @since 7.1
     */
    public OscillatingYawSteering(
            final AttitudeProvider nonRotatingLaw,
            final AbsoluteDate startDate,
            final Vector3D axis,
            final double angularVelocity,
            final double startAngle,
            final double endAngle,
            final double offsetAngle)
            throws OrekitException {
        this(nonRotatingLaw, startDate, axis, angularVelocity, startAngle, endAngle, offsetAngle, false);
    }

    /**
     * Creates a new instance of the yaw steering law. This constructor is for a
     * unidirectional, constant angular speed yaw law.
     *
     * @param nonRotatingLaw underlying non-rotating attitude provider
     * @param startDate the start date when oscillating yaw steering shall begin
     * @param axis rotation axis in satellite frame
     * @param angularVelocity the angular velocity to rotate in yaw
     * @param offsetAngle the angle at which the spacecraft is initially offset
     * in yaw. In radians[-pi, pi]
     * @exception OrekitException if the frame specified is not a
     * pseudo-inertial frame
     * @since 7.1
     */
    public OscillatingYawSteering(
            final AttitudeProvider nonRotatingLaw,
            final AbsoluteDate startDate,
            final Vector3D axis,
            final double angularVelocity,
            final double offsetAngle)
            throws OrekitException {
        this(nonRotatingLaw, startDate, axis, angularVelocity, 0, 0, offsetAngle, true);
    }

    /**
     * Creates a new instance of the yaw steering law. This constructor is for a
     * unidirectional, constant angular speed yaw law.
     *
     * @param nonRotatingLaw underlying non-rotating attitude provider
     * @param startDate the start date when oscillating yaw steering shall begin
     * @param axis rotation axis in satellite frame
     * @param angularVelocity the angular velocity to rotate in yaw. In
     * the case of bidirectional, the motion is sinusoidal and angularVelocity
     * describes the average velocity over one period.
     * @param startAngle the angle at which the rotation in yaw should begin. In
     * radians[-pi, pi]
     * @param endAngle the angle at which the rotation in yaw should end
     * (oscillates between startAngle and endAngle). In radians[-pi, pi]
     * @param offsetAngle the angle at which the spacecraft is initially offset
     * in yaw. In radians[-pi, pi]
     * @param isUnidirectional * Flag for unidirectional or bidirectional;
     * @exception OrekitException if the frame specified is not a
     * pseudo-inertial frame
     * @since 7.1
     */
    private OscillatingYawSteering(
            final AttitudeProvider nonRotatingLaw,
            final AbsoluteDate startDate,
            final Vector3D axis,
            final double angularVelocity,
            final double startAngle,
            final double endAngle,
            final double offsetAngle,
            final boolean isUnidirectional)
            throws OrekitException {
        super(nonRotatingLaw,startDate, axis, angularVelocity);
        this.angularVelocity = angularVelocity;
        this.startDate = startDate;
        this.startAngle = -1;
        this.endAngle = -1;
        this.offsetAngle = offsetAngle;
        this.isUnidirectional = isUnidirectional;

        if (startAngle < -FastMath.PI || startAngle > FastMath.PI) {
            throw new IllegalArgumentException(String.format("Start angle must be between -PI and PI. Found %f", startAngle));
        }
        if (endAngle < -FastMath.PI || endAngle > 2 * FastMath.PI) {
            throw new IllegalArgumentException(String.format("End angle must be between -PI and PI. Found %f", endAngle));
        }
        if(startAngle - endAngle < 0){
            throw new IllegalArgumentException(String.format("Start angle must be larger than end angle. Found startAngle = %f and endAngle = %f", startAngle, endAngle));
        }
        
        if (offsetAngle < -FastMath.PI || offsetAngle > 2 * FastMath.PI) {
            throw new IllegalArgumentException(String.format("Offset angle must be between -PI and PI. Found %f", offsetAngle));
        }
    }

    /**
     * Compute the base system state at given date, without compensation.
     *
     * @param pvProv provider for PV coordinates
     * @param date date at which state is requested
     * @param frame reference frame from which attitude is computed
     * @return satellite base attitude state, i.e without compensation.
     * @throws OrekitException if some specific error occurs
     */
    public Attitude getBaseState(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date, final Frame frame)
            throws OrekitException {
        return getUnderlyingAttitudeProvider().getAttitude(pvProv, date, frame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
            final AbsoluteDate date, final Frame frame)
            throws OrekitException {

        // attitude from base attitude provider
        final Attitude base = getBaseState(pvProv, date, frame);
        final Transform baseTransform = new Transform(date, base.getOrientation());

        // Compensation rotation definition :
        //  . Z satellite axis is unchanged
        //  . yaw is adjusted to contant speed defined in constructor
        final double timeDuration = date.durationFrom(startDate);
        final double rotationAngle;
        if (isUnidirectional) {
            rotationAngle = (angularVelocity * timeDuration + offsetAngle) % (2 * FastMath.PI);
        } else {
            double span = startAngle - endAngle;
            //centers the sinusoid around center of span and adjusts to offsetAngle
            rotationAngle = FastMath.sin(angularVelocity*timeDuration + 
                    FastMath.asin((-2*(offsetAngle-startAngle))/span))*(span/2);
        }
        
        //compute transform due to yaw steering
        final Transform yawTransform = new Transform(date, new Rotation(Vector3D.PLUS_K, rotationAngle, RotationConvention.FRAME_TRANSFORM));

        //combine two transforms
        final Transform combined = new Transform(date, baseTransform, yawTransform);
        
        // add compensation
        return new Attitude(date, frame, combined.getRotation(), combined.getRotationRate(), combined.getRotationAcceleration());
        
    }

}
