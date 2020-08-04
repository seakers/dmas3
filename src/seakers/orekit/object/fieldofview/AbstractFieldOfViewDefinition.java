/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.fieldofview;

import java.io.Serializable;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class is responsible for making field of view detection more
 * computationally efficient by computing the elevation between ground targets
 * and the spacecraft. The user can decide to use a minimum elevation (from the
 * perspective of the target) that must be exceeded before attempting a more
 * expensive field of view computation.
 *
 * @author nozomihitomi
 */
public abstract class AbstractFieldOfViewDefinition implements FieldOfViewDefinition, Serializable {

    private static final long serialVersionUID = -2441300006537852331L;

    private final double minElevation;

    /**
     * Use this constructor to set the minimum elevation that must be exceeded
     * before the more expensive field of view computation is carried out.
     *
     * @param minElevation the minimum elevation threshold [radians] that must
     * be met before the FieldOfViewDetector g() function is computed
     */
    public AbstractFieldOfViewDefinition(double minElevation) {
        this.minElevation = minElevation;
    }

    /**
     * This constructor assumes that line of sight is required before the more
     * expensive field of view computation is carried out.
     */
    public AbstractFieldOfViewDefinition() {
        this(0.0);
    }

    /**
     * Function to see if the given target is in line of sight with the
     * spacecraft (regardless of attitude). Implementation is similar to the
     * g(s) function from the elevation detector. That is, the g_lineOfSight
     * function value is the difference between the current elevation (and
     * azimuth if necessary) and the reference mask or minimum value (i.e. set
     * to 0degrees). This function will return a positive value if the satellite
     * and the target have line of sight. The function will return a negative
     * value if the satellite and the target do not have line of sight
     *
     * @param s the current spacecraft state
     * @param target The target to be viewed
     * @return This function will return a positive value if the satellite and
     * the target have line of sight. The function will return a negative value
     * if the satellite and the target do not have line of sight
     * @throws org.orekit.errors.OrekitException
     */
    public double g_lineOfSight(SpacecraftState s, TopocentricFrame target) throws OrekitException {

        final double trueElevation = target.getElevation(s.getPVCoordinates().getPosition(),
                s.getFrame(), s.getDate());
        return trueElevation - minElevation;
    }

    /**
     * The unique g function for a given field of view shape.
     *
     * @param s the spacecraft's current state
     * @param target the target of interest
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    protected abstract double g(SpacecraftState s, TopocentricFrame target) throws OrekitException;

    /**
     * This g() function is positive when the given target enters the field of
     * view and negative if the target is outside the field of view. Assumes
     * that line of sight is required
     *
     * @param s the spacecraft's current state
     * @param target the target of interest
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    @Override
    public double g_FOV(SpacecraftState s, TopocentricFrame target) throws OrekitException {
        double los =  g_lineOfSight(s, target);
        if(los > 0){
            return g(s, target);
        }else{
            return los;
        }
    }

    /**
     * Gets the nadir pointing vector at the current spacecraft state.
     *
     * @param s current spacecraft state
     * @param pvProv
     * @param shape
     * @param frame
     * @return
     * @throws OrekitException
     */
    protected Vector3D getSpacecraftToNadirPosition(final SpacecraftState s, final PVCoordinatesProvider pvProv, final BodyShape shape,
            final Frame frame) throws OrekitException {
        final Vector3D nadirPosRef = getNadirPosition(pvProv, shape, s.getDate(), frame);
        return nadirPosRef.subtract(s.getPVCoordinates(s.getFrame()).getPosition());
    }

    /**
     * Gets the nadir position in the spacecraft frame
     *
     * @param pvProv pv coordinate provider for the spacecraft
     * @param shape The shape of the body which the satellite orbits
     * @param date The date at which to obtain the nadir position
     * @param frame The frame in which to obtain the nadir vector
     * @return The position vector of the nadir point in the given frame of
     * reference
     * @throws org.orekit.errors.OrekitException
     */
    protected Vector3D getNadirPosition(final PVCoordinatesProvider pvProv, final BodyShape shape,
            final AbsoluteDate date, final Frame frame) throws OrekitException {

        // transform from specified reference frame to body frame
        final Transform refToBody = frame.getTransformTo(shape.getBodyFrame(), date);
        return nadirRef(pvProv.getPVCoordinates(date, frame), shape, refToBody).getPosition();
    }

    /**
     * Compute ground point in nadir direction, in reference frame. This method
     * is based on the nadir pointing law NadirPointin
     *
     * @param scRef spacecraft coordinates in reference frame
     * @param The shape of the body which the satellite orbits
     * @param refToBody transform from reference frame to body frame
     * @return intersection point in body frame (only the position is set!)
     * @exception OrekitException if line of sight does not intersect body
     */
    private TimeStampedPVCoordinates nadirRef(final TimeStampedPVCoordinates scRef, final BodyShape shape, final Transform refToBody)
            throws OrekitException {

        final Vector3D satInBodyFrame = refToBody.transformPosition(scRef.getPosition());

        // satellite position in geodetic coordinates
        final GeodeticPoint gpSat = shape.transform(satInBodyFrame, shape.getBodyFrame(), scRef.getDate());

        // nadir position in geodetic coordinates
        final GeodeticPoint gpNadir = new GeodeticPoint(gpSat.getLatitude(), gpSat.getLongitude(), 0.0);

        // nadir point position in body frame
        final Vector3D pNadirBody = shape.transform(gpNadir);

        // nadir point position in reference frame
        final Vector3D pNadirRef = refToBody.getInverse().transformPosition(pNadirBody);

        return new TimeStampedPVCoordinates(scRef.getDate(), pNadirRef, Vector3D.ZERO, Vector3D.ZERO);

    }

    /**
     * This method returns a rotation matrix that will transform vectors v1 and
     * v2 to point toward nadir and the vector that is normal to the orbital
     * plane, respectively. Normal vector is used instead of the velocity vector
     * because the velocity vector and nadir vector may not be orthogonal
     *
     * @param v1 Vector to line up with nadir
     * @param v2 Vector to line up with the velocity vector
     * @param s the current spacecraft state
     * @param pvProv the pv provider for the satellite
     * @param shape the shape of the body to define nadir direction
     * @param frame the reference frame to translate vectors to
     * @return
     * @throws OrekitException
     */
    protected Rotation alignWithNadirAndNormal(Vector3D v1, Vector3D v2,
            final SpacecraftState s, final PVCoordinatesProvider pvProv, final BodyShape shape,
            final Frame frame) throws OrekitException {
        final Vector3D nadirRef = getSpacecraftToNadirPosition(s, pvProv, shape, frame).normalize();
        Vector3D velRef = pvProv.getPVCoordinates(s.getDate(), frame).getVelocity().normalize();
        Vector3D orbitNormal = nadirRef.crossProduct(velRef).normalize();

        return new Rotation(nadirRef, orbitNormal, v1, v2);
    }
}
