/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.util;

import org.hipparchus.util.FastMath;

/**
 * Provides static methods to convert between units
 * @author nhitomi
 */
public class Units {
    
    /**
     * Converts units from degrees to radians
     * @param deg angle in degrees
     * @return angle in radians
     */
    public static double deg2rad(double deg){
        return FastMath.toRadians(deg);
    }
    
    /**
     * Converts units from radians to degrees
     * @param rad angle in radians
     * @return angle in degrees
     */
    public static double rad2deg(double rad){
        return FastMath.toDegrees(rad);
    }
    
    /**
     * Converts meters to kilometers
     * @param m distance in meters
     * @return distance in kilometers
     */
    public static double m2km(double m){
        return m/1000.;
    }
    
    /**
     * Converts kilometers to meters
     * @param km distance in kilometers
     * @return distance in meters
     */
    public static double km2m(double km){
        return km*1000.;
    }
    
}
