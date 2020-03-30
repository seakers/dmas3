/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.fieldofview;

import java.util.Objects;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.events.FieldOfView;

/**
 * This class models a rectangular field of view. The field of view is defined
 * with a center axis, two axes to define the aperture directions, and two half
 * aperture angles.
 *
 * @author nozomihitomi
 */
public class RectangularFieldOfView extends CustomFieldOfView {
    private static final long serialVersionUID = 1559069965493414756L;
    
    private final Vector3D center;

    private final Vector3D axis1;

    private final Vector3D axis2;

    private final double halfAperture1;

    private final double halfAperture2;
    
    /**
     * Build a Field Of View with dihedral shape (i.e. rectangular shape).
     * @param center Direction of the FOV center, in spacecraft frame
     * @param acrossTrackHalfAperture FOV half aperture angle in the across-track direction,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param alongTrackHalfAperture FOV half aperture angle in the along-track direction,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     * @throws OrekitException 
     */
    public RectangularFieldOfView(Vector3D center,
            double acrossTrackHalfAperture, double alongTrackHalfAperture,
            double margin) throws OrekitException {
        this(Vector3D.PLUS_K, Vector3D.PLUS_I, acrossTrackHalfAperture, Vector3D.PLUS_J, alongTrackHalfAperture, margin);
    }

    /**
     * Build a Field Of View with dihedral shape (i.e. rectangular shape).
     * @param center Direction of the FOV center, in spacecraft frame
     * @param axis1 FOV dihedral axis 1, in spacecraft frame
     * @param halfAperture1 FOV dihedral half aperture angle 1,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param axis2 FOV dihedral axis 2, in spacecraft frame
     * @param halfAperture2 FOV dihedral half aperture angle 2,
     * must be less than π/2, i.e. full dihedra must be smaller then
     * an hemisphere
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     * @throws OrekitException 
     */
    public RectangularFieldOfView(Vector3D center,
            Vector3D axis1, double halfAperture1,
            Vector3D axis2, double halfAperture2,
            double margin) throws OrekitException {
        super(new FieldOfView(center, axis1, halfAperture1, axis2, halfAperture2, margin));
        this.center = center;
        this.axis1 = axis1;
        this.axis2 = axis2;
        this.halfAperture1 = halfAperture1;
        this.halfAperture2 = halfAperture2;
    }

    /**
     * Gets the vector defining the center of the field of view
     * @return 
     */
    public Vector3D getCenter() {
        return center;
    }

    /**
     * Gets the first direction defining the rectangular shape
     * @return 
     */
    public Vector3D getAxis1() {
        return axis1;
    }

    /**
     * Gets the second direction defining the rectangular shape
     * @return 
     */
    public Vector3D getAxis2() {
        return axis2;
    }

    /**
     * Gets the first half angle corresponding to the first direction or axis1
     * @return 
     */
    public double getHalfAperture1() {
        return halfAperture1;
    }

    /**
     * Gets the first half angle corresponding to the first direction or axis1
     * @return 
     */
    public double getHalfAperture2() {
        return halfAperture2;
    }

    @Override
    public String toString() {
        return "RectangularFieldOfView{ HalfAngle1=" + halfAperture1 + ", HalfAngle2=" + halfAperture2 + "}"; 
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.center);
        hash = 41 * hash + Objects.hashCode(this.axis1);
        hash = 41 * hash + Objects.hashCode(this.axis2);
        hash = 41 * hash + (int) (Double.doubleToLongBits(this.halfAperture1) ^ (Double.doubleToLongBits(this.halfAperture1) >>> 32));
        hash = 41 * hash + (int) (Double.doubleToLongBits(this.halfAperture2) ^ (Double.doubleToLongBits(this.halfAperture2) >>> 32));
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
        final RectangularFieldOfView other = (RectangularFieldOfView) obj;
        if (!Objects.equals(this.center, other.center)) {
            return false;
        }
        if (!Objects.equals(this.axis1, other.axis1)) {
            return false;
        }
        if (!Objects.equals(this.axis2, other.axis2)) {
            return false;
        }
        if (Double.doubleToLongBits(this.halfAperture1) != Double.doubleToLongBits(other.halfAperture1)) {
            return false;
        }
        if (Double.doubleToLongBits(this.halfAperture2) != Double.doubleToLongBits(other.halfAperture2)) {
            return false;
        }
        return true;
    }
    
    
}
