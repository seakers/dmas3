/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;

/**
 * Stores information about a grid of points (each point is a TopocentricFrame)
 *
 * @author nozomihitomi
 */
public class CoverageDefinition implements OrekitObject, Serializable {

    public enum GridStyle {
        EQUAL_AREA, //Fewer points are placed in high latitudes
        UNIFORM //the same number of points are placed in each latitude
    }

    private static final long serialVersionUID = 2467782856759734909L;

    /**
     * Grid of points to define the target area
     */
    private HashSet<CoveragePoint> grid;

    /**
     * constellations responsible for monitoring the coverage grid
     */
    private HashSet<Constellation> constellations;

    /**
     * name of Coverage definition
     */
    private final String name;

    /**
     * Shape used to project CoveragePoints
     */
    private final BodyShape planet;

    /**
     * Creates a new grid of GeodeticPoints on the entire surface of a given
     * BodyShape ([-90 deg, 90 deg] in latitude and [0 deg, 360 deg] in
     * longitude).
     *
     * @param name of the coverage definition
     * @param granularity defines the resolution of the grid points. Input in
     * degrees
     * @param planet Body shape on which to project the CoveragePoints
     * @param style the style of the grid
     */
    public CoverageDefinition(String name, double granularity, BodyShape planet, GridStyle style) {
        this(name, granularity, -90, 90, -180, 180, planet, style);
    }

    /**
     * Creates a new grid of GeodeticPoints on the surface of a given BodyShape
     * within the given latitude and longitude bounds.
     *
     * @param name of the coverage definition
     * @param granularity defines the resolution of the grid points. Input in
     * degrees
     * @param minLatitude Minimum latitude where coverage is defined [deg]
     * @param maxLatitude Maximum latitude where coverage is defined [deg]
     * @param minLongitdue Maximum latitude where coverage is defined [deg].
     * Longitudes should be in interval of [-180,180] degrees
     * @param maxLongitude Maximum latitude where coverage is defined [deg].
     * Longitudes should be in interval of [-180,180] degreesÏ
     * @param planet Body shape on which to project the CoveragePoints
     * @param style the style of the grid
     */
    public CoverageDefinition(String name, double granularity, double minLatitude,
            double maxLatitude, double minLongitdue, double maxLongitude,
            BodyShape planet, GridStyle style) {

        this.name = name;
        this.planet = planet;

        //check to see if minimum lat/lon is less than maximum lat/lon
        if (minLatitude > maxLatitude) {
            throw new IllegalArgumentException("Minimum latitude ("
                    + minLatitude + ") must be less than maximum latitude ("
                    + maxLatitude + ")");
        }
        if (minLongitdue > maxLongitude) {
            throw new IllegalArgumentException("Minimum latitude ("
                    + minLongitdue + ") must be less than maximum latitude ("
                    + maxLongitude + ")");
        }

        //Create grid points
        this.grid = new HashSet<>();
        int numPoints = 0;
        for (double lat = minLatitude; lat <= maxLatitude; lat += granularity) {

            switch (style) {
                case UNIFORM:
                    for (double lon = minLongitdue; lon <= maxLongitude; lon += granularity) {
                        double longitude = FastMath.toRadians(lon);
                        double latitude = FastMath.toRadians(lat);
                        double altitude = 0.;
                        GeodeticPoint point = new GeodeticPoint(latitude, longitude, altitude);
                        this.grid.add(new CoveragePoint(planet, point, String.valueOf(numPoints)));
                        numPoints++;
                    }
                    break;
                case EQUAL_AREA:
                    //weigh the number of points by their latitude to get even 
                    //distribution of points within spherical grid
                    int ptsAtLat = (int) (360 / granularity * FastMath.cos(FastMath.toRadians(lat)));
                    for (double lon = minLongitdue; lon < maxLongitude; lon += 360.0 / ptsAtLat) {
                        double longitude = FastMath.toRadians(lon);
                        double latitude = FastMath.toRadians(lat);
                        double altitude = 0.;
                        GeodeticPoint point = new GeodeticPoint(latitude, longitude, altitude);
                        this.grid.add(new CoveragePoint(planet, point, String.valueOf(numPoints)));
                        numPoints++;
                    }
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("Grid style %s not supported", style));
            }
        }
    }

    /**
     * Defines a coverage shape using the given points projected onto the
     * surface of a given BodyShape
     *
     * @param name of the coverage definition
     * @param points points defining the coverage definition
     * @param planet Body shape on which to project the CoveragePoints
     */
    public CoverageDefinition(String name, Collection<GeodeticPoint> points,
            BodyShape planet) {
        this.name = name;
        this.planet = planet;
        this.grid = new HashSet<>();
        int numPoints = 0;
        for (GeodeticPoint point : points) {
            this.grid.add(new CoveragePoint(planet, point, String.valueOf(numPoints)));
        }
    }

    /**
     * Defines a coverage shape using the given points projected onto the
     * surface of a given BodyShape
     *
     * @param name of the coverage definition
     * @param points points defining the coverage definition. All the points
     * must have the same start and end dates as well as the same body shape
     */
    public CoverageDefinition(String name, Collection<CoveragePoint> points) {
        this.name = name;
        CoveragePoint refPt = points.iterator().next();
        this.planet = refPt.getParentShape();

        this.grid = new HashSet<>();
        for (CoveragePoint pt : points) {
            this.grid.add(pt);
        }
    }

    /**
     * Assigns the coverage grid to a constellation that will monitor the
     * coverage grid
     *
     * @param constellation
     */
    public void assignConstellation(Constellation constellation) {
        this.constellations = new HashSet<>();
        this.constellations.add(constellation);
    }

    /**
     * Assigns the coverage grid to a colletion of constellations that will
     * monitor the coverage grid
     *
     * @param constellation
     */
    public void assignConstellation(Collection<Constellation> constellation) {
        this.constellations = new HashSet<>(constellation);
    }

    /**
     * Checks to see if a constellation is assigned to this coverage definition
     *
     * @param constellation
     * @return
     */
    public boolean isAssigned(Constellation constellation) {
        return constellations.contains(constellation);
    }

    /**
     * Checks to see if given satellite is assigned to this coverage definition
     *
     * @param satellite
     * @return
     */
    public boolean isAssigned(Satellite satellite) {
        for (Constellation constel : constellations) {
            if (constel.hasSatellite(satellite)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the collection of constellations that is responsible for monitoring
     * the coverage grid
     *
     * @return
     */
    public HashSet<Constellation> getConstellations() {
        return constellations;
    }

    /**
     * Returns the number of points in the grid
     *
     * @return
     */
    public int getNumberOfPoints() {
        return grid.size();
    }

    /**
     * returns a new Set of points within the grid
     *
     * @return a Collection of points
     */
    public HashSet<CoveragePoint> getPoints() {
        return new HashSet<>(grid);
    }

    /**
     * returns a new SET of points within the grid that are also within
     * the specified latitude and longitude bounds
     *
     * @param latBounds latitude bounds
     * @param lonBounds longitude bounds
     * @return a Collection of points
     */
    public Set<TopocentricFrame> getPoints(double[] latBounds, double[] lonBounds) {
        if (latBounds[0] > latBounds[1] || latBounds[0] < -Math.PI / 2 || latBounds[0] > Math.PI / 2
                || latBounds[1] < -Math.PI / 2 || latBounds[1] > Math.PI / 2) {
            throw new IllegalArgumentException(
                    String.format("Expected latitude bounds to be within [-pi/2,pi/2]. Found [%f,%f]", latBounds[0], latBounds[1]));
        }
        if (lonBounds[0] > lonBounds[1] || lonBounds[0] < -Math.PI || lonBounds[0] > Math.PI
                || lonBounds[1] < -Math.PI || lonBounds[1] > Math.PI) {
            throw new IllegalArgumentException(
                    String.format("Expected longitude bounds to be within [-pi,pi]. Found [%f,%f]", lonBounds[0], lonBounds[1]));
        }
        HashSet<TopocentricFrame> points = new HashSet<>();
        for (CoveragePoint pt : grid) {
            if (pt.getPoint().getLatitude() >= latBounds[0]
                    && pt.getPoint().getLatitude() <= latBounds[1]
                    && pt.getPoint().getLongitude() >= lonBounds[0]
                    && pt.getPoint().getLongitude() <= lonBounds[1]) {
                points.add(pt);
            }
        }
        return points;
    }

    /**
     * returns a Set of latitudes occupied by points within the grid
     *
     * @return
     */
    public Set<Double> getLatitudes() {
        HashSet<Double> latitudes = new HashSet<>();
        for(CoveragePoint pt : grid){
            latitudes.add(pt.getPoint().getLatitude());
        }
        return latitudes;
    }
    
    /**
     * returns a Set of longitudes occupied by points within the grid
     *
     * @return
     */
    public Set<Double> getLongitudes() {
        HashSet<Double> latitudes = new HashSet<>();
        for(CoveragePoint pt : grid){
            latitudes.add(pt.getPoint().getLongitude());
        }
        return latitudes;
    }
    
    /**
     * returns a Set of altitude occupied by points within the grid
     *
     * @return
     */
    public Set<Double> getAltitudes() {
        HashSet<Double> latitudes = new HashSet<>();
        for(CoveragePoint pt : grid){
            latitudes.add(pt.getPoint().getAltitude());
        }
        return latitudes;
    }

    /**
     * Get the planet shape of the coverage definition
     *
     * @return
     */
    public BodyShape getPlanetShape() {
        return planet;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CoverageDefinition{" + "name=" + name + ", number of points=" + grid.size() + "}";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.grid);
        hash = 59 * hash + Objects.hashCode(this.constellations);
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
        final CoverageDefinition other = (CoverageDefinition) obj;
        if (!Objects.equals(this.grid, other.grid)) {
            return false;
        }
        if (!Objects.equals(this.constellations, other.constellations)) {
            return false;
        }
        return true;
    }
}