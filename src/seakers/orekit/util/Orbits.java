/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.util;

import org.hipparchus.util.FastMath;
import org.orekit.orbits.Orbit;
import org.orekit.utils.Constants;

/**
 * This class computes the nodal and apsidal precession rates associated with j2
 * perturbation
 *
 * @author nozomihitomi
 */
public class Orbits {

    private static final double J2 = Constants.WGS84_EARTH_C20;

    private static final double RE = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

    private static final double MU = Constants.WGS84_EARTH_MU;

    /**
     * Computes the nodal precession [rad/s] due to J2 effect
     *
     * @param orbit the orbit
     * @return the nodal precession [rad/s]
     */
    public static double nodalPrecession(Orbit orbit) {
        double sma = orbit.getA();
        double ecc = orbit.getE();
        double inc = orbit.getI();
        return nodalPrecession(sma, ecc, inc);
    }
    
    /**
     * Computes the nodal precession [rad/s] due to J2 effect
     *
     * @param sma semi-major axis [m]
     * @param ecc eccentricity
     * @param inc inclination [rad]
     * @return the nodal precession [rad/s]
     */
    public static double nodalPrecession(double sma, double ecc, double inc) {
        return -1.5 * meanMotion(sma) * J2 * (RE * RE) / (sma * sma) 
                * FastMath.cos(inc)
                /((1 - ecc * ecc)*(1 - ecc * ecc));
    }
    
    /**
     * Computes the nodal precession [rad/s] due to J2 effect
     *
     * @param orbit the orbit
     * @return the nodal precession [rad/s]
     */
    public static double apsidalPrecession(Orbit orbit) {
        double sma = orbit.getA();
        double ecc = orbit.getE();
        double inc = orbit.getI();
        return apsidalPrecession(sma, ecc, inc);
    }

    /**
     * Computes the apsidal precession [rad/s] due to J2 effect
     *
     * @param sma semi-major axis [m]
     * @param ecc eccentricity
     * @param inc inclination [rad]
     * @return the apsidal precession [rad/s]
     */
    public static double apsidalPrecession(double sma, double ecc, double inc) {
        return 0.75 * meanMotion(sma) * J2 * (RE * RE) / (sma * sma)
                * (4 - 5 * FastMath.pow(FastMath.sin(inc), 2))
                * FastMath.pow(1 - ecc * ecc, -2);
    }

    /**
     * Computes the mean motion or average angular velocity [rad/s]
     *
     * @param orbit the orbit
     * @return the mean motion or average angular velocity [rad/s]
     */
    public static double meanMotion(Orbit orbit) {
        double sma = orbit.getA();
        return FastMath.sqrt(MU / FastMath.pow(sma, 3));
    }
    
    /**
     * Computes the mean motion or average angular velocity [rad/s]
     *
     * @param sma semi-major axis [m]
     * @return the mean motion or average angular velocity [rad/s]
     */
    public static double meanMotion(double sma) {
        return FastMath.sqrt(MU / (sma * sma *sma));
    }
    
    /**
     * Computes the velocity for a circular orbit with a given semi-major axis
     * @param sma semi-major axis [m]
     * @return the velocity [m/s]
     */
    public static double circularOrbitVelocity(double sma){
        return FastMath.sqrt(MU/sma);
    }
    
    /**
     * Computes the inclination for a circular SSO with a given altitude
     * @param h altitude [m]
     * @return the inclination [rad]
     */
    public static double incSSO(double h){
        double kh=10.10949;
        double cosi=Math.pow(((RE+h)/RE), 3.5)/-kh;
        return FastMath.acos(cosi);
    }
}
