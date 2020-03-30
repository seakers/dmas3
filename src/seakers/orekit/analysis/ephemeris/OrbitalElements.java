/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis.ephemeris;

import org.hipparchus.util.FastMath;

/**
 * 
 * @author nozomihitomi
 */
public class OrbitalElements {
    
    /**
     * Semi-major axis[km]
     */
    private final double sa;
    
    /**
     * Eccentricity
     */
    private final double ecc;
    
    /**
     * Inclination[rad]
     */
    private final double inc;
    
    /**
     * Right ascension of the ascending node[rad]
     */
    private final double raan;
    
    /**
     * argument of perigee [rad]
     */
    private final double argPer;
    
    /**
     * anomaly [rad]
     */
    private final double anom;

    /**
     * 
     * @param sa Semi-major axis[km]
     * @param ecc Eccentricity
     * @param inc Inclination[rad]
     * @param raan Right ascension of the ascending node[rad]
     * @param argPer argument of perigee [rad]
     * @param anom anomaly [rad]
     */
    public OrbitalElements(double sa, double ecc, double inc, double raan, double argPer, double anom) {
        this.sa = sa;
        this.ecc = ecc;
        this.inc = inc;
        this.raan = raan;
        this.argPer = argPer;
        this.anom = anom;
    }

    /**
     * Returns the semi-major axis in km
     * @return 
     */
    public double getSa() {
        return sa;
    }

    /**
     * Returns the eccentricity
     * @return 
     */
    public double getEcc() {
        return ecc;
    }

    /**
     * Returns the inclination in degrees
     * @return 
     */
    public double getInc() {
        return FastMath.toDegrees(inc);
    }

    /**
     * Returns the right ascension of the ascending node in degrees
     * @return 
     */
    public double getRaan() {
        return FastMath.toDegrees(raan);
    }

    /**
     * Returns the argument of perigee in degrees
     * @return 
     */
    public double getArgPer() {
        return FastMath.toDegrees(argPer);
    }

    /**
     * Returns the mean anomaly in degrees
     * @return 
     */
    public double getMa() {
        return FastMath.toDegrees(anom);
    }
    
    @Override
    public String toString() {
        return String.format("%f,%f,%f,%f,%f,%f",
                            getSa(), getEcc(), getInc(), getRaan(), getArgPer(), getMa());
    }
    
}
