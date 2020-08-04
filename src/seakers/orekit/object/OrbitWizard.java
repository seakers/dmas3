/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object;

import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

/**
 *
 * @author nozomihitomi
 */
public class OrbitWizard {

    /**
     * computes the inclination of a sun synchronous orbit (Earth centric)
     * given, the semi-major axis and eccentricity. Uses WGS84 mdoel.
     *
     * @param semimajorAxis semi-major axis of orbit
     * @param ecc eccentricity of orbit
     * @return
     */
    public static double SSOinc(double semimajorAxis, double ecc) {
        double omegaDot = 2 * FastMath.PI / (Constants.JULIAN_YEAR); //rad/s
        double mu = Constants.WGS84_EARTH_MU; // gravitation coefficient
        double r = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double lhs = -(3. / 2.) * FastMath.pow(1 - FastMath.pow(ecc, 2), -2)
                * FastMath.pow(mu / FastMath.pow(semimajorAxis, 3), 0.5)
                * (-Constants.WGS84_EARTH_C20) * FastMath.pow(r / semimajorAxis, 2);
        return FastMath.acos(omegaDot / lhs);
    }
}
