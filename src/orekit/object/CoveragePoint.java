/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object;

import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;

/**
 * A point within a coverage definition. 
 *
 * @author nozomihitomi
 */
public class CoveragePoint extends TopocentricFrame implements OrekitObject, Comparable<CoveragePoint> {

    private static final long serialVersionUID = 5330957252938339044L;

    /**
     * A coverage point is one element that is comprised within the
     * CoverageDefinition. It is defined by the latitude, longitude, and
     * altitude along with the shape of the body on which the points are
     * projected. This class also stores the accesses between a viewer (e.g.
     * satellite) and this point.
     *
     * @param parentShape The shape of the body to position the point
     * @param point The geodetic location (latitude, longitude, altitude)
     * @param name name of the point
     */
    public CoveragePoint(BodyShape parentShape, GeodeticPoint point, String name) {
        super(parentShape, point, name);
    }

    /**
     * This method can be used to order the coverage points first by latitude
     * and then by longitude in ascending order.
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(CoveragePoint o) {
        //first compare the latitudes
        if (this.getPoint().getLatitude() < o.getPoint().getLatitude()) {
            return -1;
        } else if (this.getPoint().getLatitude() > o.getPoint().getLatitude()) {
            return 1;
        } else {
            //next compare longitudes
            if (this.getPoint().getLongitude()< o.getPoint().getLongitude()) {
                return -1;
            } else if (this.getPoint().getLongitude()> o.getPoint().getLongitude()) {
                return 1;
            }
            else 
                return 0;
        }
    }

    @Override
    public int hashCode() {
        return this.getPoint().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CoveragePoint other = (CoveragePoint) obj;
        return this.getPoint().equals(other.getPoint());
    }
    
}
