/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.propagation;

import java.util.Properties;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DTM2000;
import org.orekit.forces.drag.DTM2000InputParameters;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.drag.MarshallSolarActivityFutureEstimation;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import static org.orekit.forces.gravity.potential.GravityFieldFactory.ICGEM_FILENAME;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.SGP4;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * This class is an alternative to the built-in Orekit propagator builder.
 * Propagator builders rely on some conversion that I couldn't figure out. This
 * class is more straight forward and shall accomplish the desired task of
 * creating propagators for satellites
 *
 * @author nozomihitomi
 */
public class PropagatorFactory {

    /**
     * Determines the complexity of the propagator
     */
    private final PropagatorType propType;

    /**
     * Properties that hold contain of the numerical propagator parameters
     * including force models
     */
    private Properties properties;

    /**
     * This constructor is used when creating analytical propagators (e.g.
     * Keplerian, J2)
     *
     * @param propType
     */
    public PropagatorFactory(PropagatorType propType) {
        this.propType = propType;
        this.properties = new Properties();
    }

    /**
     * This constructor should be used if a numerical propagator is desired.
     * Need to specify the orbit type and the ode integrator.
     *
     * @param propType
     * @param properties that hold some of the numerical propagator parameters
     * including force models
     */
    public PropagatorFactory(PropagatorType propType, Properties properties) {
        this.propType = propType;
        this.properties = properties;
    }

    public Propagator createPropagator(Orbit orbit, double mass) throws OrekitException {
        switch (propType) {
            case KEPLERIAN:
                return createKeplerianPropagator(orbit, mass);
            case J2:
                return createJ2Propagator(orbit, mass);
            case NUMERICAL:
                //MASS PROPAGATION

                //set integrator steps and tolerances
                final double dP = 0.001;
                final double minStep = 0.00000001;
                final double maxStep = 1000;
                final double initStep = 60;
                final double[][] tolerance = NumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);
                double[] absTolerance = tolerance[0];
                double[] relTolerance = tolerance[1];

                //create integrator object and set some propoerties
                //DormandPrince853 is an implementation of some Runge Kutta method
                AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, absTolerance, relTolerance);
                integrator.setInitialStepSize(initStep);
                return createNumericalPropagator(orbit,integrator, properties);
            default:
                throw new UnsupportedOperationException(String.format("Propagator of type %s is not supported by factory or by this constructor.", propType));
        }
    }

    /**
     * Creates a Keplerian propagator
     *
     * @param orbit to create a propagator for
     * @param mass mass of spacecraft[kg]
     * @return
     * @throws OrekitException
     */
    private Propagator createKeplerianPropagator(Orbit orbit, double mass) throws OrekitException {
        return new KeplerianPropagator(orbit, Propagator.DEFAULT_LAW, orbit.getMu(), mass);
    }

    /**
     * Creates a J2 propagator assuming an Earth-centric orbit. Currently set
     * for using GRIM5C1 model
     *
     * @param orbit
     * @return
     * @throws OrekitException
     */
    private Propagator createJ2Propagator(Orbit orbit, double mass) throws OrekitException {

        return new EcksteinHechlerPropagator(orbit, mass, Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_MU, Constants.WGS84_EARTH_C20, 0, 0, 0, 0);
    }
    
    /**
     * Creates a TLE propagator
     *
     * @param tle to create a propagator for
     * @param mass mass of spacecraft[kg]
     * @return
     * @throws OrekitException
     */
    private Propagator createTLEPropagator(TLE tle, double mass) throws OrekitException {
        return new SGP4(tle, Propagator.DEFAULT_LAW, mass);
    }

    /**
     * Creates a numerical propagator. Assumed that the position of the orbit is
     * given with true anomaly.
     *
     * @param orbit
     * @return
     * @throws OrekitException
     */
    private Propagator createNumericalPropagator(Orbit orbit, ODEIntegrator integrator, Properties properties) throws OrekitException {
        double mass = Double.parseDouble(properties.getProperty("orekit.propagator.mass", "10"));
        SpacecraftState s = new SpacecraftState(orbit, mass);
        NumericalPropagator prop = new NumericalPropagator(integrator);
        prop.resetInitialState(s);
        prop.setOrbitType(orbit.getType());
        prop.setPositionAngleType(PositionAngle.TRUE);

        //Frames and Bodies creation (must use IERS_2003 and EME2000 frames to be consistent with STK)
        final Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
        
        //Add the gravity model with Harmonics
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        ICGEMFormatReader reader = new ICGEMFormatReader(ICGEM_FILENAME, false);
        GravityFieldFactory.addPotentialCoefficientsReader(reader);
        final NormalizedSphericalHarmonicsProvider harmonicsProvider = GravityFieldFactory.getNormalizedProvider(21, 21);
        prop.addForceModel(new HolmesFeatherstoneAttractionModel(earthFrame, harmonicsProvider));

        //check if add the drag model (DTM2000 model)
        if (Boolean.parseBoolean(properties.getProperty("orekit.propagator.atmdrag", "false"))) {
            double dragArea = Double.parseDouble(properties.getProperty("orekit.propagator.dragarea", "10"));
            double dragCoeff = Double.parseDouble(properties.getProperty("orekit.propagator.dragcoeff", "2.2"));

            String supportedNames = "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}F10\\.(?:txt|TXT)";
            MarshallSolarActivityFutureEstimation.StrengthLevel strengthlevel = MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE;
            DTM2000InputParameters parameters = new MarshallSolarActivityFutureEstimation(supportedNames, strengthlevel);

            Atmosphere atmosphere = new DTM2000(parameters, CelestialBodyFactory.getSun(), earth);
            DragSensitive spacecraft = new IsotropicDrag(dragArea, dragCoeff);
            prop.addForceModel(new DragForce(atmosphere, spacecraft));
        }

        //check if add the third body attraction (sun and moon)
        if (Boolean.parseBoolean(properties.getProperty("orekit.propagator.thirdbody.sun", "false"))) {
            prop.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        }
        if (Boolean.parseBoolean(properties.getProperty("orekit.propagator.thirdbody.moon", "false"))) {
            prop.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));
        }

        //check if add the solar radiation pressure model
        if (Boolean.parseBoolean(properties.getProperty("orekit.propagator.solarpressure", "false"))) {
            double solarArea = Double.parseDouble(properties.getProperty("orekit.propagator.solararea", "10"));
            double equatorialRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
            double cr = 1;
            RadiationSensitive spacecraft = new IsotropicRadiationSingleCoefficient(solarArea, cr);
            prop.addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(), equatorialRadius, spacecraft));
        }
        
        return prop;
    }

    public PropagatorType getPropType() {
        return propType;
    }

}
