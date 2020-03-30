/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.coverage.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.event.FieldOfViewEventAnalysis;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Satellite;
import seakers.orekit.parallel.ParallelRoutine;
import seakers.orekit.parallel.SubRoutine;

/**
 * This is a method for an approximation of a coverage analysis. The satellite
 * is flown in a circular orbit with a nadir-facing conical sensor. The orbit is
 * propagated in fixed time steps assuming J2 perturbations about the Earth
 * using the WGS84 model.
 *
 * @author nhitomi
 */
public class FastCoverageAnalysis extends FieldOfViewEventAnalysis {

    /**
     * The equitorial radius of the Earth using the WGS84 model
     */
    private final double re = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

    /**
     * The j2 term for the earth using the WGS84 model
     */
    private final double j2 = Constants.WGS84_EARTH_C20;

    /**
     * The angular velocity of the Earth using the WGS84 model
     */
    private final double rotRate = Constants.WGS84_EARTH_ANGULAR_VELOCITY;

    /**
     * The minimum radius of the earth (north-south direction)
     */
    private final double minRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS * (1 - Constants.WGS84_EARTH_FLATTENING);

    /**
     * Half angle for a simple conical field of view sensor [rad]
     */
    private final double halfAngle;

    /**
     *
     * @param startDate the start date of this analysis
     * @param endDate the end date of this analysis
     * @param inertialFrame the inertial frame to use in this analysis
     * @param covDefs the coverage definitions involved in this analysis
     * @param halfAngle Half angle for a simple conical field of view sensor
     * [rad]
     */
    public FastCoverageAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame, Set<CoverageDefinition> covDefs,
            double halfAngle) {
        super(startDate, endDate, inertialFrame, covDefs, null, true, true);
        this.halfAngle = halfAngle;
    }

    @Override
    public FieldOfViewEventAnalysis call() throws OrekitException {
        ArrayList<SubRoutine> subroutines = new ArrayList<>();
        
        for (CoverageDefinition cdef : getCoverageDefinitions()) {
            Logger.getGlobal().finer(String.format("Acquiring access times for %s...", cdef));
            Logger.getGlobal().finer(
                    String.format("Simulation dates %s to %s (%.2f days)",
                            getStartDate(), getEndDate(),
                            getEndDate().durationFrom(getStartDate()) / 86400.));

            for (Satellite sat : getUniqueSatellites(cdef)) {
                KeplerianOrbit orb = new KeplerianOrbit(sat.getOrbit());
                double losTimeStep = orb.getKeplerianPeriod() / 10;
                double fovTimeStep = orb.getKeplerianPeriod() / 500;

                Task subRoutine = new Task(sat, cdef, losTimeStep, fovTimeStep);
                subroutines.add(subRoutine);
            }
            
            try {
                for (SubRoutine sr : ParallelRoutine.submit(subroutines)) {
                    if (sr == null) {
                        throw new IllegalStateException("Subroutine failed in field of view event.");
                    }
                    
                    Task subRoutine = (Task)sr;
                    
                    Satellite sat = subRoutine.getSat();
                    HashMap<TopocentricFrame, TimeIntervalArray> satAccesses = subRoutine.getSatAccesses();
                    processAccesses(sat, cdef, satAccesses);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(FastCoverageAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(FastCoverageAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return this;
    }

    /**
     *
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @return returns the x value of a root in between the x values of the two
     * points given
     */
    private double linearInterpolate(double x1, double y1, double x2, double y2) {
        if (x2 < x1) {
            throw new IllegalArgumentException("x1 must be less than x2.");
        }
        if (y1 * y2 > 0) {
            throw new IllegalArgumentException("linear interpolation has no root crossing");
        }
        double slope = (y2 - y1) / (x2 - x1);
        return (-y1 / slope) + x1;
    }

    /**
     *
     * @param orb
     * @param time the epoch time from the start date
     * @return
     */
    private Vector3D getPosition(KeplerianOrbit orb, double time) {
        double sa = orb.getA();
        double inc = orb.getI();

        //change in mean anomaly [rad/s] assuming constant angular velocity for sircular orbit
        double ta_dot = (2 * FastMath.PI) / orb.getKeplerianPeriod();

        //nodal precession [rad/s] due to J2 effect
        double raan_dot = -1.5 * ta_dot * j2 * (re * re) / (sa * sa) * FastMath.cos(inc);

        //compute raan and ta for each time step
        double raan = raan_dot * time + orb.getRightAscensionOfAscendingNode();
        
        //sometimes when ecc = 0.0 the KeplerianOrbit defines argument of perigee as the same as true anomaly. 
        //Here since only ecc = 0.0 is allowed, we take the sum of argument of perigee and true anomaly as the anomaly
        double ta = ta_dot * time + orb.getAnomaly(PositionAngle.TRUE) + orb.getPerigeeArgument();

        return orbitalElem2xyz(sa, inc, raan, ta);
    }

    /**
     * Computes the x,y,z position in the rotating earth frame of a satellite in
     * an orbit around the Earth. Semi-major axis and inclination are assumed to
     * be fixed
     *
     * @param sa semi-major axis [km] that remains constant
     * @param inc inclination [rad] that remains constant
     * @param raan a vector of right ascension of the ascending node [rad]. Must
     * be the same length of the vector of true anomaly
     * @param ta a vector of true anomaly [rad]. Must be the same length of the
     * vector of right ascension of the ascending node
     * @param t a vector of the time steps
     * @return A matrix where the columns are the x, y, z position of the
     * satellite in the rotating earth frame, and the time vector in the last
     * column
     */
    private Vector3D orbitalElem2xyz(double sa, double inc, double raan, double ta) {
        //constant inclination
        double cosInc = FastMath.cos(inc);
        double sinInc = FastMath.sin(inc);
        double cosRAAN = FastMath.cos(raan);
        double sinRAAN = FastMath.sin(raan);
        double cosTA = FastMath.cos(ta);
        double sinTA = FastMath.sin(ta);

        double x = sa * (cosRAAN * cosTA - sinRAAN * sinTA * cosInc);
        double y = sa * (sinRAAN * cosTA + cosRAAN * sinTA * cosInc);
        double z = sa * sinTA * sinInc;
        return new Vector3D(x, y, z);
    }

    /**
     * Creates a subroutine to run the field of view event analysis in parallel
     */
    private class Task implements SubRoutine {

        /**
         * The satellite to propagate
         */
        private final Satellite sat;

        /**
         * The coverage definition to access
         */
        private final CoverageDefinition cdef;

        /**
         * The step size during propagation when computing the line of sight
         * events. Generally, this can be a large step. It is used to speed up
         * the simulation.
         */
        private final double losStepSize;

        /**
         * The step size during propagation when computing the field of view
         * events. Generally, this should be a small step for accurate results.
         */
        private final double fovStepSize;

        /**
         * The times, for each point, when it is being accessed by the given
         * satellite and its payload.
         */
        private final HashMap<TopocentricFrame, TimeIntervalArray> satAccesses;

        /**
         *
         * @param sat The satellite to propagate
         * @param cdef The coverage definition to access
         * @param losStepSize The step size during propagation when computing
         * the line of sight events. Generally, this can be a large step. It is
         * used to speed up the simulation.
         * @param fovStepSize The step size during propagation when computing
         * the field of view events. Generally, this should be a small step for
         * accurate results.
         */
        public Task(Satellite sat, CoverageDefinition cdef,
                double losStepSize, double fovStepSize) {
            this.sat = sat;
            this.cdef = cdef;
            this.losStepSize = losStepSize;
            this.fovStepSize = fovStepSize;

            this.satAccesses = new HashMap<>(cdef.getNumberOfPoints());
            for (CoveragePoint pt : cdef.getPoints()) {
                satAccesses.put(pt, getEmptyTimeArray());
            }
        }

        //NOTE: this implementation of in the field of view is a bit fragile if propagating highly elliptical orbits (>0.75). Maybe need to use smaller time steps los and fov detectors
        @Override
        public Task call() throws Exception {
            KeplerianOrbit orb = new KeplerianOrbit(sat.getOrbit());
            Logger.getGlobal().finer(String.format("Propagating satellite %s...", sat));
            //identify accesses and create time interval array for each coverage point
            for (CoveragePoint pt : cdef.getPoints()) {
                if (!lineOfSightPotential(pt, orb, FastMath.toRadians(2.0))) {
                    //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
                    continue;
                }

                //compute line of sight first
                TimeIntervalArray losTimeArray = new TimeIntervalArray(getStartDate(), getEndDate());

                Vector3D initPtPos = pt.getPVCoordinates(getStartDate(), getInertialFrame()).getPosition();
                double prevT = 0;
                double currentT = 0;
                while (currentT < getEndDate().durationFrom(getStartDate())) {
                    Vector3D ptPos = getPtPos(initPtPos, currentT);
                    Vector3D satPos = getPosition(orb, currentT);
                    double cosThetas = satPos.dotProduct(ptPos) / (satPos.getNorm() * ptPos.getNorm());
 
                    //the mininum cos(theta) value required for line of sight
                    double minCosTheta = minRadius / orb.getA();

                    //losVal > 0 means that sat has line of sight
                    double losVal = cosThetas - minCosTheta;
                    if (losVal > 0 && !losTimeArray.isAccessing()) {
                        //be on the conservative side and take earlier time stamp
                        losTimeArray.addRiseTime(prevT);
                    } else if (losVal < 0 && losTimeArray.isAccessing()) {
                        //be on the conservative side and take later time stamp
                        losTimeArray.addSetTime(currentT);
                    }
                    prevT = currentT;
                    currentT += losStepSize;
                }

                TimeIntervalArray array = new TimeIntervalArray(getStartDate(), getEndDate());
                double date0 = 0;
                double date1 = Double.NaN;
                for (RiseSetTime interval : losTimeArray) {
                    if (interval.isRise()) {
                        date0 = interval.getTime();
                    } else {
                        date1 = interval.getTime();
                    }
                    if (!Double.isNaN(date1)) {
                        currentT = date0;
                        prevT = date0;
                        double prevVal = Double.NaN;
                        while (currentT < date1) {
                            Vector3D satPos = getPosition(orb, currentT);
                            Vector3D negSatPos = satPos.negate();
                            Vector3D ptPos = getPtPos(initPtPos, currentT);
                            Vector3D sat2pt = ptPos.add(negSatPos);
                            double ang = Vector3D.angle(negSatPos, sat2pt);
                            double val = halfAngle - ang;
                            if (val > 0 && !array.isAccessing()) {
                                if (Double.isNaN(prevVal)) {
                                    array.addRiseTime(prevT);
                                } else {
                                    array.addRiseTime(linearInterpolate(prevT, prevVal, currentT, val));
                                }
                            } else if (val < 0 && array.isAccessing()) {
                                array.addSetTime(linearInterpolate(prevT, prevVal, currentT, val));
                                //there can only be one access per line-of-sight interval
                                break;
                            }
                            prevT = currentT;
                            currentT += fovStepSize;
                            prevVal = val;
                        }
                        //close access if loss of line of sight
                        if(array.isAccessing()){
                            array.addSetTime(date1);
                        }
                        date1 = Double.NaN;
                    }
                }
                satAccesses.put(pt, array);
            }
            return this;
        }

        public Satellite getSat() {
            return sat;
        }

        public HashMap<TopocentricFrame, TimeIntervalArray> getSatAccesses() {
            return satAccesses;
        }

        private Vector3D getPtPos(Vector3D ptPosECF, double epochTime) {
            double theta = epochTime * rotRate;

            //create rotation matrix
            //for rotation matrices, transpose equals inverse, which is why entry 0,1 is -sin(theta)
            RealMatrix ecf2eci = MatrixUtils.createRealMatrix(3, 3);
            double cosTheta = FastMath.cos(theta);
            double sinTheta = FastMath.sin(theta);
            ecf2eci.setEntry(0, 0, cosTheta);
            ecf2eci.setEntry(0, 1, -sinTheta);
            ecf2eci.setEntry(0, 2, 0);
            ecf2eci.setEntry(1, 0, sinTheta);
            ecf2eci.setEntry(1, 1, cosTheta);
            ecf2eci.setEntry(1, 2, 0);
            ecf2eci.setEntry(2, 0, 0);
            ecf2eci.setEntry(2, 1, 0);
            ecf2eci.setEntry(2, 2, 1);

            return new Vector3D(ecf2eci.operate(ptPosECF.toArray()));
        }

        /**
         * checks to see if a point will ever be within the line of sight from a
         * satellite's orbit assuming that the inclination remains constant and
         * the point's altitude = 0. Some margin can be added to this
         * computation since it is an approximate computation (neglects
         * oblateness of Earth for example).
         *
         * @param pt the point being considered
         * @param orbit orbit being considered.
         * @param latitudeMargin the positive latitude margin [rad] within which
         * a point can lie to be considered to be in the possible region for
         * light of sight.
         * @return true if the point may be within the line of sight to the
         * satellite at any time in its flight. else false
         */
        private boolean lineOfSightPotential(CoveragePoint pt, Orbit orbit, double latitudeMargin) throws OrekitException {
            //this computation assumes that the orbit frame is in ECE
            double distance2Pt = pt.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition().getNorm();
            double distance2Sat = orbit.getPVCoordinates().getPosition().getNorm();
            double subtendedAngle = FastMath.acos(distance2Pt / distance2Sat);

            return FastMath.abs(pt.getPoint().getLatitude()) - latitudeMargin < subtendedAngle + orbit.getI();
        }

    }

}
