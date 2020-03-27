/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.constellations;

import java.util.ArrayList;
import java.util.Collection;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.Satellite;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Instrument;
import seakers.orekit.orbit.J2KeplerianOrbit;

/**
 * A Walker constellation is defined by its parameters i:t/p/f. i is the
 * inclination, t is the total number of satellites, p is the number of equally
 * space planes, f is the relative spacing between satellites in adjacent planes
 *
 * @author SEAK1
 */
public class Walker extends Constellation {

    private static final long serialVersionUID = 6994388604876873748L;
    
    /**
     * Creates a walker delta-pattern constellation in the specified Walker
     * configuration at the specified semimajor axis. The satellites contained
     * in this constellation are not assigned any instrumentation nor are any
     * steering/attitude laws. Assumes that the reference raan and reference
     * true anomaly are 0 rad.
     *
     * @param name name of the constellation
     * @param payload the instruments to assign to each satellite in the constellation
     * @param semimajoraxis the semimajor axis of each satellite [m]
     * @param i inclination of the constellation [rad]
     * @param t the total number of satellites
     * @param p the number of equally spaced planes
     * @param f the relative spacing between satellites in adjacent planes
     * @param inertialFrame the frame in which the PVCoordinates are defined
     * (must be a pseudo-inertial frame)
     * @param startDate the date to begin simulating this constellation
     * @param mu central attraction coefficient (m³/s²)
     */
    public Walker(String name, Collection<Instrument> payload,
            double semimajoraxis, double i, int t, int p, int f, Frame inertialFrame, AbsoluteDate startDate, double mu) {
        this(name, payload, semimajoraxis, i, t, p, f, inertialFrame, startDate, mu, 0.0, 0.0);
    }

    /**
     * Creates a walker delta-pattern constellation in the specified Walker
     * configuration at the specified semimajor axis. The satellites contained
     * in this constellation are not assigned any instrumentation nor are any
     * steering/attitude laws. Can specify where the reference raan and true
     * anomaly to orient the walker configuration
     *
     * @param name name of the constellation
     * @param payload the instruments to assign to each satellite in the constellation
     * @param semimajoraxis the semimajor axis of each satellite [m]
     * @param i inclination of the constellation [rad]
     * @param t the total number of satellites
     * @param p the number of equally spaced planes
     * @param f the relative spacing between satellites in adjacent planes
     * @param inertialFrame the frame in which the PVCoordinates are defined
     * (must be a pseudo-inertial frame)
     * @param startDate the date to begin simulating this constellation
     * @param mu central attraction coefficient (m³/s²)
     * @param refRaan the reference right ascension of the ascending node of the
     * first orbital plane to begin constructing constellation [rad]
     * @param refAnom the reference true anomaly of the first satellite in the
     * first orbital plane to begin constructing constellation [rad]
     */
    public Walker(String name, Collection<Instrument> payload,
            double semimajoraxis, double i, int t, int p, int f, 
            Frame inertialFrame, AbsoluteDate startDate, double mu, double refRaan, double refAnom) {
        super(name, createConstellation(payload, semimajoraxis, i, t, p, f, inertialFrame, startDate, mu, refRaan, refAnom));
    }

    /**
     * Construct a collection of satellites in a walker configuration
     *
     * @param i inclination of the constellation [rad]
     * @param t the total number of satellites
     * @param p the number of equally spaced planes
     * @param f the relative spacing between satellites in adjacent planes
     * @param semimajoraxis the semimajor axis of each satellite [m]
     * @param inertialFrame the frame in which the PVCoordinates are defined
     * (must be a pseudo-inertial frame)
     * @param startDate the date to begin simulating this constellation
     * @param mu central attraction coefficient (m³/s²)
     * @param refRaan the reference right ascension of the ascending node of the
     * first orbital plane to begin constructing constellation [rad]
     * @param refAnom the reference true anomaly of the first satellite in the
     * first orbital plane to begin constructing constellation [rad]
     * @return
     */
    private static Collection<Satellite> createConstellation(
            Collection<Instrument> payload, double semimajoraxis, double i, int t, int p, int f, 
            Frame inertialFrame, AbsoluteDate startDate, double mu, double refRaan, double refAnom) {
        //checks for valid parameters
        if (t < 0 || p < 0) {
            throw new IllegalArgumentException(String.format("Expected t>0, p>0."
                    + " Found f=%d and p=%d", t, p));
        }
        if ((t % p) != 0) {
            throw new IllegalArgumentException(
                    String.format("Incompatible values for total number of "
                            + "satellites <t=%d> and number of planes <p=%d>. "
                            + "t must be divisible by p.", t, p));
        }
        if (f < 0 && f > p - 1) {
            throw new IllegalArgumentException(
                    String.format("Expected 0 <= f <= p-1. "
                            + "Found f = %d and p = %d.", f, p));
        }

        //Uses Walker delta pa
        final int s = t / p; //number of satellites per plane
        final double pu = 2 * FastMath.PI / t; //pattern unit
        final double delAnom = pu * p; //in plane spacing between satellites
        final double delRaan = pu * s; //node spacing
        final double phasing = pu * f;

        final ArrayList<Satellite> walker = new ArrayList<>(t);
        for (int planeNum = 0; planeNum < p; planeNum++) {
            for (int satNum = 0; satNum < s; satNum++) {
                double anom = (refAnom + satNum * delAnom + phasing * planeNum) % (2.*FastMath.PI);
                //since eccentricity = 0, doesn't matter if using true or mean anomaly
                Orbit orb = new J2KeplerianOrbit(semimajoraxis, 0.0001, i, 0.0, 
                        refRaan + planeNum * delRaan, 
                        anom,
                        PositionAngle.TRUE, inertialFrame, startDate, mu);
                Satellite sat = new Satellite(String.format("sat_walker_%d", s * planeNum + satNum), orb, null, payload);
                walker.add(sat);
            }
        }
        return walker;
    }

}