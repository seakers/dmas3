/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.fieldofview;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;

/**
 * This interface is connects different fields of view to their proper g()
 * function that is computed in the event detectors
 *
 * @author nozomihitomi
 */
public interface FieldOfViewDefinition {

    /**
     * This g() function is positive when the given target enters the field of
     * view and negative if the target is outside the field of view
     *
     * @param s the spacecraft's current state
     * @param target the target of interest
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    public double g_FOV(SpacecraftState s, TopocentricFrame target) throws OrekitException;

    /**
     * Get the angular offset of target point with respect to the Field Of View
     * Boundary.
     * <p>
     * The offset is roughly an angle with respect to the closest boundary
     * point, corrected by the margin and using some approximation far from the
     * Field Of View. It is positive if the target is outside of the Field Of
     * view, negative inside, and zero if the point is exactly on the boundary
     * (always taking the margin into account).
     * </p>
     * <p>
     * As Field Of View can have complex shapes that may require long
     * computation, when the target point can be proven to be outside of the
     * Field Of View, a faster but approximate computation is done, that
     * underestimate the offset. This approximation is only performed about 0.01
     * radians outside of the zone and is designed to still return a positive
     * value if the full accurate computation would return a positive value.
     * When target point is close to the zone (and furthermore when it is inside
     * the zone), the full accurate computation is performed. This setup allows
     * this offset to be used as a reliable way to detect Field Of View boundary
     * crossings, which correspond to sign changes of the offset.
     * </p>
     *
     * @param lineOfSight line of sight from the center of the Field Of View
     * support unit sphere to the target in Field Of View canonical frame
     * @return an angular offset negative if the target is visible within the
     * Field Of View and positive if it is outside of the Field Of View,
     * including the margin (note that this cannot take into account interposing
     * bodies)
     */
    public double offsetFromBoundary(Vector3D lineOfSight);

    /**
     * This g() function is positive when the given target enters the field of
     * view and negative if the target is outside the field of view
     *
     * @param lineOfSight line of sight normalized-vectors from the center of the Field Of
     * View support unit sphere to the targets in Field Of View canonical frame.
     * The matrix shall be a 3xN matrix where the rows are the x,y,z coordinates
     * and N is the number of targets
     * @return a vector containing the gvalues. Positive if the target is
     * visible within the Field Of View and negative if it is outside of the
     * Field Of View, including the margin
     */
    public RealVector g_FOV(RealMatrix lineOfSight);
}
