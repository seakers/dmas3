/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.fieldofview;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;

/**
 * This field of view models a simple cone that is always pointed nadir
 * regardless of the spacecraft attitude. It is defined with a center axis in
 * the spacecraft frame of reference, and the half aperture angle. This can be
 * used in place of nadir-pointing + SimpleConicalFOV to save on compute time on
 * computing the attitude at every time step (this class computes the attitude
 * only when computing the g() event function)
 *
 * @author nozomihitomi
 */
public class NadirSimpleConicalFOV extends SimpleConicalFieldOfView {

    private static final long serialVersionUID = -5871573780685218252L;

    private final BodyShape shape;
   
    /**
     * Constructor to create a simple conical field of view
     *
     * @param halfAngle FOV half aperture angle [rad], must be less than π/2.
     * @param shape the shape of the body to define the nadir direction
     */
    public NadirSimpleConicalFOV(double halfAngle, BodyShape shape) {
        super(Vector3D.PLUS_K, halfAngle);
        this.shape = shape;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * The g function value is for circular field of views and is the difference
     * between FOV half aperture and the absolute value of the angle between
     * target direction and field of view center. It is positive inside the FOV
     * and negative outside.
     * </p>
     *
     * @param s the current spacecraft state
     * @param target
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    protected double g(SpacecraftState s, TopocentricFrame target) throws OrekitException {

        // Compute target position/velocity at date in spacecraft frame */
        final Vector3D targetPosInert = new Vector3D(1, target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                -1, s.getPVCoordinates().getPosition());
        Vector3D spacecraftToNadirPosition = getSpacecraftToNadirPosition(s, s.getOrbit(), shape, s.getFrame());
        // Target is in the field of view if the absolute value that angle is smaller than FOV half aperture.
        // g function value is the difference between FOV half aperture and the absolute value of the angle between
        // target direction and field of view center. It is positive inside the FOV and negative outside.
        return  getHalfAngle() - Vector3D.angle(targetPosInert, spacecraftToNadirPosition);
    }
    

    @Override
    public String toString() {
        return "NadirSimpleConicalFieldOfView{" + "halfAngle=" + getHalfAngle() + '}';
    }

}
