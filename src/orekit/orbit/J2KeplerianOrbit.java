/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.orbit;

import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This orbit simulates the Keplerian motion of a satellite in addition to the
 * nodal precession and apsidal precession caused by the Earth's J2 effect
 *
 * @author nhitomi
 */
public class J2KeplerianOrbit extends KeplerianOrbit {

    private static final long serialVersionUID = -7042635122399329238L;

    private final double re = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

    private final double j2 = Constants.WGS84_EARTH_C20;

    public J2KeplerianOrbit(double a, double e, double i, double pa, double raan, double anomaly, PositionAngle type, Frame frame, AbsoluteDate date, double mu) throws IllegalArgumentException {
        super(a, e, i, pa, raan, anomaly, type, frame, date, mu);
    }

    public J2KeplerianOrbit(TimeStampedPVCoordinates pvCoordinates, Frame frame, double mu) throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);
    }

    public J2KeplerianOrbit(PVCoordinates pvCoordinates, Frame frame, AbsoluteDate date, double mu) throws IllegalArgumentException {
        super(pvCoordinates, frame, date, mu);
    }

    public J2KeplerianOrbit(Orbit op) {
        super(op);
    }

    @Override
    public KeplerianOrbit shiftedBy(double dt) {
        KeplerianOrbit kepOrbit = super.shiftedBy(dt);

        double sa = kepOrbit.getA();
        double ecc = kepOrbit.getE();
        double inc = kepOrbit.getI();

        double n = (2 * FastMath.PI) / kepOrbit.getKeplerianPeriod();
        double v1 = n * j2 * (re * re) / (sa * sa) * FastMath.sqrt(1 - ecc * ecc);

        double raan_dot = -1.5 * v1 * FastMath.cos(inc);
        double ap_dot = 0.75 * v1 * (4 - 5 * FastMath.pow(FastMath.sin(inc), 2));

        //add effect of nodal precession
        double raan = raan_dot * dt + kepOrbit.getRightAscensionOfAscendingNode();

        //add effect of apsidal precession
        double ap = ap_dot * dt + kepOrbit.getPerigeeArgument();

        return new KeplerianOrbit(sa, kepOrbit.getE(), inc,
                ap, raan, kepOrbit.getTrueAnomaly(), PositionAngle.TRUE, 
                kepOrbit.getFrame(), kepOrbit.getDate(), kepOrbit.getMu());
    }

}
