/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.sensitivity;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.moeaframework.analysis.sensitivity.Parameter;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import seakers.orekit.coverage.analysis.AnalysisMetric;
import seakers.orekit.coverage.analysis.FastCoverageAnalysis;
import seakers.orekit.coverage.analysis.GroundEventAnalyzer;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Satellite;

/**
 *
 * @author nozomihitomi
 */
public class CoverageVersusOrbitalElements extends SobolSensitivityAnalysis {

    /**
     * The number of threads to use in the anaylsis
     */
    private final int nThreads;

    /**
     * Assumes earth centric orbit
     */
    private final Frame inertialFrame = FramesFactory.getEME2000();

    /**
     * Assumes WGS84 model
     */
    private final double mu = Constants.WGS84_EARTH_MU;

    /**
     * The start date of the scenario
     */
    private final AbsoluteDate startDate;

    /**
     * The end date of the scenario
     */
    private final AbsoluteDate endDate;

    /**
     * The points of interest
     */
    private final Collection<CoveragePoint> poi;

    /**
     * Flag that sets the conical sensor half angle as a parameter
     */
    private final boolean testSensor;

    /**
     * The variable that sets the bounds of the conical sensor half angle [rad]
     * parameter
     */
    private final Parameter varSensor;

    /**
     * The default conical sensor half angle [rad] when it is not parameter
     */
    private final double defSensor;

    /**
     * Flag that sets semi-major axis as a parameter
     */
    private final boolean testSA;

    /**
     * The variable that sets the bounds of the semi-major axis [m] parameter
     */
    private final Parameter varSA;

    /**
     * The default semi-major axis when it is not parameter
     */
    private final double defSA;

    /**
     * Flag that sets the eccentricity as a parameter
     */
    private final boolean testE;

    /**
     * The variable that sets the bounds of the eccentricity parameter
     */
    private final Parameter varE;

    /**
     * The default eccentricity when it is not parameter
     */
    private final double defE;

    /**
     * Flag that sets the inclination as a parameter
     */
    private final boolean testI;

    /**
     * The variable that sets the bounds of the inclination [rad] parameter
     */
    private final Parameter varI;

    /**
     * The default inclination [rad] when it is not parameter
     */
    private final double defI;

    /**
     * Flag that sets the right ascension of the ascending node as a parameter
     */
    private final boolean testRAAN;

    /**
     * The variable that sets the bounds of the right ascension of the ascending
     * node [rad] parameter
     */
    private final Parameter varRAAN;

    /**
     * The default right ascension of the ascending node [rad] when it is not
     * parameter
     */
    private final double defRAAN;

    /**
     * Flag that sets the argument of perigee as a parameter
     */
    private final boolean testAP;

    /**
     * The variable that sets the bounds of the argument of perigee [rad]
     * parameter
     */
    private final Parameter varAP;

    /**
     * The default argument of perigee [rad] when it is not parameter
     */
    private final double defAP;

    /**
     * Flag that sets the true anomaly as a parameter
     */
    private final boolean testTA;

    /**
     * The variable that sets the bounds of the true anomaly [rad] parameter
     */
    private final Parameter varTA;

    /**
     * The default true anomaly [rad] when it is not parameter
     */
    private final double defTA;

    private CoverageVersusOrbitalElements(int nThreads, int n,
            AbsoluteDate startDate, AbsoluteDate endDate,
            Collection<CoveragePoint> poi,
            boolean testSensor, Parameter varSensor, double defSensor,
            boolean testSA, Parameter varSA, double defSA,
            boolean testE, Parameter varE, double defE,
            boolean testI, Parameter varI, double defI,
            boolean testRAAN, Parameter varRAAN, double defRAAN,
            boolean testAP, Parameter varAP, double defAP,
            boolean testTA, Parameter varTA, double defTA) {
        super(n);
        this.nThreads = nThreads;

        this.startDate = startDate;
        this.endDate = endDate;
        this.poi = poi;
        this.testSensor = testSensor;
        this.varSensor = varSensor;
        this.testSA = testSA;
        this.varSA = varSA;
        this.testE = testE;
        this.varE = varE;
        this.testI = testI;
        this.varI = varI;
        this.testRAAN = testRAAN;
        this.varRAAN = varRAAN;
        this.testAP = testAP;
        this.varAP = varAP;
        this.testTA = testTA;
        this.varTA = varTA;
        List<Parameter> parameters = new ArrayList<>();
        if (testSensor) {
            parameters.add(varSensor);
            this.defSensor = Double.NaN;
        } else {
            this.defSensor = defSensor;
        }
        if (testSA) {
            parameters.add(varSA);
            this.defSA = Double.NaN;
        } else {
            this.defSA = defSA;
        }
        if (testE) {
            parameters.add(varE);
            this.defE = Double.NaN;
        } else {
            this.defE = defE;
        }
        if (testI) {
            parameters.add(varI);
            this.defI = Double.NaN;
        } else {
            this.defI = defI;
        }
        if (testRAAN) {
            parameters.add(varRAAN);
            this.defRAAN = Double.NaN;
        } else {
            this.defRAAN = defRAAN;
        }
        if (testAP) {
            parameters.add(varAP);
            this.defAP = Double.NaN;
        } else {
            this.defAP = defAP;
        }
        if (testTA) {
            parameters.add(varTA);
            this.defTA = Double.NaN;
        } else {
            this.defTA = defTA;
        }
        setParameters(parameters);
    }

    @Override
    public RealMatrix evaluateAll(RealMatrix values) {
        //set up resource pool
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        CompletionService<SubRoutine> ecs = new ExecutorCompletionService<>(pool);

        for (int row = 0; row < values.getRowDimension(); row++) {
            RealVector sample = values.getRowVector(row);

            double sensor, sa, ecc, inc, raan, argPer, ta;

            //only read sample's parameters if the element is a declared as a parameter. Else use default value
            int ind = 0;
            if (testSensor) {
                sensor = sample.getEntry(ind);
                ind++;
            } else {
                sensor = defSensor;
            }
            if (testSA) {
                sa = sample.getEntry(ind);
                ind++;
            } else {
                sa = defSA;
            }
            if (testE) {
                ecc = sample.getEntry(ind);
                ind++;
            } else {
                ecc = defE;
            }
            if (testI) {
                inc = sample.getEntry(ind);
                ind++;
            } else {
                inc = defI;
            }
            if (testRAAN) {
                raan = sample.getEntry(ind);
                ind++;
            } else {
                raan = defRAAN;
            }
            if (testAP) {
                argPer = sample.getEntry(ind);
                ind++;
            } else {
                argPer = defAP;
            }
            if (testTA) {
                ta = sample.getEntry(ind);
                ind++;
            } else {
                ta = defTA;
            }
            Orbit orb = new KeplerianOrbit(sa, ecc, inc, argPer, raan, ta,
                    PositionAngle.TRUE, inertialFrame, startDate, mu);
            Satellite sat = new Satellite("sat", orb, null, new ArrayList<>());

            ecs.submit(new SubRoutine(sat, sensor));
        }

        RealMatrix metrics = new Array2DRowRealMatrix(values.getRowDimension(), 5);
        for (int i = 0; i < values.getRowDimension(); i++) {
            DescriptiveStatistics stats;
            try {
                Future<SubRoutine> f = ecs.poll(60, TimeUnit.SECONDS);
                stats = f.get().getStats();
                metrics.setEntry(i, 0, stats.getMin());
                metrics.setEntry(i, 1, stats.getMean());
                metrics.setEntry(i, 2, stats.getMax());
                metrics.setEntry(i, 3, stats.getPercentile(90));
                metrics.setEntry(i, 4, stats.getPercentile(95));
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            } catch (ExecutionException ex) {
                throw new IllegalStateException(ex);
            }
        }
        pool.shutdown();
        return metrics;
    }

    public static class Builder {

        /**
         * a parameter to set the number of samples. Number of samples =
         * n*(2*p+2) where p is the number of parameters
         */
        private final int n;

        /**
         * The start date of the scenario
         */
        private final AbsoluteDate startDate;

        /**
         * The end date of the scenario
         */
        private final AbsoluteDate endDate;

        /**
         * The points of interest
         */
        private final Collection<CoveragePoint> poi;

        /**
         * the number of threads to use
         */
        private int nThreads = 1;

        /**
         * Flag that sets the conical sensor half angle as a parameter
         */
        private boolean testSensor = false;

        /**
         * The variable that sets the bounds of the conical sensor half angle
         * [rad] parameter
         */
        private Parameter varSensor = null;

        /**
         * The default conical sensor half angle [rad] when it is not parameter
         */
        private double defSensor = Math.PI / 2.;

        /**
         * Flag that sets semi-major axis as a parameter
         */
        private boolean testSA = false;

        /**
         * The variable that sets the bounds of the semi-major axis [m]
         * parameter
         */
        private Parameter varSA = null;

        /**
         * The default semi-major axis [m] when it is not parameter
         */
        private double defSA = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 600000;

        /**
         * Flag that sets the eccentricity as a parameter
         */
        private boolean testE = false;

        /**
         * The variable that sets the bounds of the eccentricity parameter
         */
        private Parameter varE = null;

        /**
         * The default eccentricity when it is not parameter
         */
        private double defE = 0;

        /**
         * Flag that sets the inclination as a parameter
         */
        private boolean testI = false;

        /**
         * The variable that sets the bounds of the inclination [rad] parameter
         */
        private Parameter varI = null;

        /**
         * The default inclination [rad] when it is not parameter
         */
        private double defI = Math.PI/2;

        /**
         * Flag that sets the right ascension of the ascending node as a
         * parameter
         */
        private boolean testRAAN = false;

        /**
         * The variable that sets the bounds of the right ascension of the
         * ascending node [rad] parameter
         */
        private Parameter varRAAN = null;

        /**
         * The default right ascension of the ascending node [rad] when it is
         * not parameter
         */
        private double defRAAN = 0.0;

        /**
         * Flag that sets the argument of perigee as a parameter
         */
        private boolean testAP = false;

        /**
         * The variable that sets the bounds of the argument of perigee [rad]
         * parameter
         */
        private Parameter varAP = null;

        /**
         * The default argument of perigee [rad] when it is not parameter
         */
        private double defAP = 0.0;

        /**
         * Flag that sets the true anomaly as a parameter
         */
        private boolean testTA = false;

        /**
         * The variable that sets the bounds of the true anomaly [rad] parameter
         */
        private Parameter varTA = null;

        /**
         * The default true anomaly [rad] when it is not parameter
         */
        private double defTA = 0.0;

        /**
         * Constructs a builder for the coverage vs orbital elements sensitivity
         * analysis
         *
         * @param n a parameter to that defines the number of samples. Number of
         * samples = n*(2*p+2) where p is the number of parameters
         * @param startDate The start date of the scenario
         * @param endDate The end date of the scenario
         * @param poi The points of interest
         */
        public Builder(int n, AbsoluteDate startDate, AbsoluteDate endDate,
                Collection<CoveragePoint> poi) {
            this.n = n;
            this.startDate = startDate;
            this.endDate = endDate;
            this.poi = poi;
        }

        /**
         * Sets the number of threads to use for the analysis
         *
         * @param nThreads the number of threads to use
         * @return this builder
         */
        public Builder setNThreads(int nThreads) {
            this.nThreads = nThreads;
            return this;
        }

        /**
         * Declares the conical sensor half angle as a parameter to test
         *
         * @param lowerbound the lower bound of the conical sensor half angle
         * [rad]
         * @param upperbound the upper bound of the conical sensor half angle
         * [rad]
         * @return this builder
         */
        public Builder setSensorParam(double lowerbound, double upperbound) {
            this.testSensor = true;
            this.varSensor = new Parameter("sensor", lowerbound, upperbound);
            return this;
        }

        /**
         * Declares the conical sensor half angle is not a parameter to test and
         * assigns it a default value
         *
         * @param def the default value for the conical sensor half angle [rad]
         * @return this builder
         */
        public Builder setSensorDef(double def) {
            this.testSensor = false;
            this.defSensor = def;
            return this;
        }

        /**
         * Declares the semi-major axis as a parameter to test
         *
         * @param lowerbound the lower bound of the semi-major axis [m]
         * @param upperbound the upper bound of the semi-major axis [m]
         * @return this builder
         */
        public Builder setSAParam(double lowerbound, double upperbound) {
            this.testSA = true;
            this.varSA = new Parameter("sa", lowerbound, upperbound);
            return this;
        }

        /**
         * Declares the semi-major axis is not a parameter to test and assigns
         * it a default value
         *
         * @param def the default value for the semi-major axis [m]
         * @return this builder
         */
        public Builder setSADef(double def) {
            this.testSA = false;
            this.defSA = def;
            return this;
        }

        /**
         * Declares the eccentricity as a parameter to test
         *
         * @param lowerbound the lower bound of the eccentricity
         * @param upperbound the upper bound of the eccentricity
         * @return this builder
         */
        public Builder setEParam(double lowerbound, double upperbound) {
            this.testE = true;
            this.varE = new Parameter("ecc", lowerbound, upperbound);
            return this;
        }

        /**
         * Declares the eccentricity is not a parameter to test and assigns it a
         * default value
         *
         * @param def the default value for the eccentricity
         * @return this builder
         */
        public Builder setEDef(double def) {
            this.testE = false;
            this.defE = def;
            return this;
        }

        /**
         * Declares the inclination as a parameter to test
         *
         * @param lowerbound the lower bound of the inclination [rad]
         * @param upperbound the upper bound of the inclination [rad]
         * @return this builder
         */
        public Builder setIParam(double lowerbound, double upperbound) {
            this.testI = true;
            this.varI = new Parameter("inc", lowerbound, upperbound);
            return this;
        }

        /**
         * Declares the inclination is not a parameter to test and assigns it a
         * default value
         *
         * @param def the default value for the inclination [rad]
         * @return this builder
         */
        public Builder setIDef(double def) {
            this.testI = false;
            this.defI = def;
            return this;
        }

        /**
         * Declares the right ascension of the ascending node as a parameter to
         * test
         *
         * @param lowerbound the lower bound of the right ascension of the
         * ascending node [rad]
         * @param upperbound the upper bound of the right ascension of the
         * ascending node [rad]
         * @return this builder
         */
        public Builder setRAANParam(double lowerbound, double upperbound) {
            this.testRAAN = true;
            this.varRAAN = new Parameter("raan", lowerbound, upperbound);
            return this;
        }

        /**
         * Declares the right ascension of the ascending node is not a parameter
         * to test and assigns it a default value
         *
         * @param def the default value for the right ascension of the ascending
         * node [rad]
         * @return this builder
         */
        public Builder setRAANDef(double def) {
            this.testRAAN = false;
            this.defRAAN = def;
            return this;
        }

        /**
         * Declares the argument of perigee as a parameter to test
         *
         * @param lowerbound the lower bound of the argument of perigee [rad]
         * @param upperbound the upper bound of the argument of perigee [rad]
         * @return this builder
         */
        public Builder setAPParam(double lowerbound, double upperbound) {
            this.testAP = true;
            this.varAP = new Parameter("argPer", lowerbound, upperbound);
            return this;
        }

        /**
         * Declares the argument of perigee is not a parameter to test and
         * assigns it a default value
         *
         * @param def the default value for the argument of perigee [rad]
         * @return this builder
         */
        public Builder setAPDef(double def) {
            this.testAP = false;
            this.defAP = def;
            return this;
        }

        /**
         * Declares the true anomaly as a parameter to test
         *
         * @param lowerbound the lower bound of the true anomaly [rad]
         * @param upperbound the upper bound of the true anomaly [rad]
         * @return this builder
         */
        public Builder setTAParam(double lowerbound, double upperbound) {
            this.testTA = true;
            this.varTA = new Parameter("ta", lowerbound, upperbound);
            return this;
        }

        /**
         * Declares the true anomaly is not a parameter to test and assigns it a
         * default value
         *
         * @param def the default value for the true anomaly [rad]
         * @return this builder
         */
        public Builder setTADef(double def) {
            this.testTA = false;
            this.defTA = def;
            return this;
        }

        /**
         * Builds a new analysis for coverage vs. orbital elements. By default
         * all orbital elements are not considered as parameters in the
         * analysis.
         *
         * @return
         */
        public CoverageVersusOrbitalElements build() {
            return new CoverageVersusOrbitalElements(
                    nThreads, n, startDate, endDate, poi,
                    testSensor, varSensor, defSensor,
                    testSA, varSA, defSA,
                    testE, varE, defE,
                    testI, varI, defI,
                    testRAAN, varRAAN, defRAAN,
                    testAP, varAP, defAP,
                    testTA, varTA, defTA);
        }
    }

    /**
     *
     */
    private class SubRoutine implements Callable<SubRoutine> {

        final private double conicalHalfAngle;

        final private Satellite sat;

        private DescriptiveStatistics stats;

        public SubRoutine(Satellite sat, double conicalHalfAngle) {
            this.sat = sat;
            this.conicalHalfAngle = conicalHalfAngle;
        }

        @Override
        public SubRoutine call() throws Exception {
            Set<CoverageDefinition> covDefs = new HashSet<>();
            CoverageDefinition cdef = new CoverageDefinition("cdef", poi);
            Constellation constel = new Constellation("constel", new ArrayList<>(Collections.singletonList(sat)));
            cdef.assignConstellation(constel);
            covDefs.add(cdef);
            FastCoverageAnalysis fca = new FastCoverageAnalysis(startDate, endDate, inertialFrame, covDefs, conicalHalfAngle);
            fca.call();

            GroundEventAnalyzer ea = new GroundEventAnalyzer(fca.getEvents(cdef));
            this.stats = ea.getStatistics(AnalysisMetric.DURATION, false, new Properties());
            return this;
        }

        public DescriptiveStatistics getStats() {
            return stats;
        }

    }

}
