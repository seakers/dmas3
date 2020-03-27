/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.fieldofview;

import java.util.Objects;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;

/**
 * This field of view models a simple cone. It is defined with a center axis in
 * the spacecraft frame of reference, and the half aperture angle.
 *
 * @author nozomihitomi
 */
public class SimpleConicalFieldOfView extends AbstractFieldOfViewDefinition{
    private static final long serialVersionUID = -5871573780685218252L;

    private final Vector3D centerAxis;

    private final double halfAngle;
    
    
    /**
     * The threshold value for cos(halfAngle)
     */
    private final double cosAngle;


    /**
     * Constructor to create a simple conical field of view
     *
     * @param centerAxis Direction of the FOV center, in spacecraft frame
     * @param halfAngle FOV half aperture angle, must be less than π/2.
     */
    public SimpleConicalFieldOfView(Vector3D centerAxis, double halfAngle) {
        super(0.0);
        this.centerAxis = centerAxis.normalize();
        this.halfAngle = halfAngle;
        this.cosAngle = FastMath.cos(halfAngle);
    }

    public Vector3D getCenterAxis() {
        return centerAxis;
    }

    public double getHalfAngle() {
        return halfAngle;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The g function value is for circular field of views and is the difference
     * between FOV half aperture and the absolute value of the angle between
     * target direction and field of view center. It is positive inside the FOV
     * and negative outside.
     * </p>
     * @param s the current spacecraft state
     * @param target
     * @return 
     * @throws org.orekit.errors.OrekitException
     */
    protected double g(SpacecraftState s, TopocentricFrame target) throws OrekitException {

        // Compute target position/velocity at date in spacecraft frame */
        final Vector3D targetPosInert = new Vector3D(1, target.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                -1, s.getPVCoordinates().getPosition());
        final Vector3D targetPosSat = s.getAttitude().getRotation().applyTo(targetPosInert);

        // Target is in the field of view if the absolute value that angle is smaller than FOV half aperture.
        // g function value is the difference between FOV half aperture and the absolute value of the angle between
        // target direction and field of view center. It is positive inside the FOV and negative outside.
        return halfAngle - Vector3D.angle(targetPosSat, centerAxis);
    }

    @Override
    public String toString() {
        return "SimpleConicalFieldOfView{" + "halfAngle=" + halfAngle + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 31 * hash + Objects.hashCode(this.centerAxis);
        hash = 31 * hash + (int) (Double.doubleToLongBits(this.halfAngle) ^ (Double.doubleToLongBits(this.halfAngle) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleConicalFieldOfView other = (SimpleConicalFieldOfView) obj;
        if (!Objects.equals(this.centerAxis, other.centerAxis)) {
            return false;
        }
        if (Double.doubleToLongBits(this.halfAngle) != Double.doubleToLongBits(other.halfAngle)) {
            return false;
        }
        return true;
    }

    @Override
    public double offsetFromBoundary(Vector3D lineOfSight) {
        return Vector3D.angle(centerAxis, lineOfSight) - halfAngle;
    }
    
    @Override
    public RealVector g_FOV(RealMatrix lineOfSight) {
        //Assuming plusK is the nadir direction
        return lineOfSight.preMultiply(
                new ArrayRealVector(centerAxis.toArray()))
                .mapSubtractToSelf(cosAngle);
    }

}
