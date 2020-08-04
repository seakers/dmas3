/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;
import seakers.orekit.event.detector.FOVDetector;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.parallel.ParallelRoutine;
import seakers.orekit.parallel.SubRoutine;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.util.RawSafety;

/**
 * This event analysis is used to compute when the given ground points are in
 * the field of view of the given satellites. The satellites can be grouped into
 * multiple constellations, where a satellite can belong to more than one
 * constellation. Specific accesses between a point and a
 * satellite/constellation can be retrieved.
 *
 * @author nhitomi
 */
public class FieldOfViewEventAnalysis extends AbstractGroundEventAnalysis {

    /**
     * Propagator factory that will create the necessary propagator for each
     * satellite
     */
    private final PropagatorFactory propagatorFactory;

    /**
     * a flag set by the user to toggle whether to save the access of each
     * individual satellite or to release them from memory.
     */
    private final boolean saveAllAccesses;

    /**
     * flag to dictate whether the coverage accesses of individual satellites
     * should be saved to the coverage database
     */
    private boolean saveToDB = false;

    /**
     * Stores all the accesses of each satellite if saveAllAccesses is true.
     */
    private HashMap<CoverageDefinition, HashMap<Satellite, HashMap<TopocentricFrame, TimeIntervalArray>>> allAccesses;

    /**
     * Creates a new event analysis.
     *
     * @param startDate of analysis
     * @param endDate of analysis
     * @param inertialFrame the inertial frame used in the simulation
     * @param propagatorFactory the factory to create propagtors for each
     * satellite
     * @param covDefs
     * @param saveAllAccesses true if user wants to maintain all the accesses
     * from each individual satellite. false if user would like to only get the
     * merged accesses between all satellites (this saves memory).
     * @param saveToDB flag to dictate whether the coverage accesses of
     * individual satellites should be saved to the coverage database
     */
    public FieldOfViewEventAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame, Set<CoverageDefinition> covDefs,
            PropagatorFactory propagatorFactory, boolean saveAllAccesses,
            boolean saveToDB) {
        super(startDate, endDate, inertialFrame, covDefs);
        this.propagatorFactory = propagatorFactory;
        this.saveAllAccesses = saveAllAccesses;
        if (saveAllAccesses) {
            this.allAccesses = new HashMap<>();
            for (CoverageDefinition cdef : covDefs) {
                allAccesses.put(cdef, new HashMap<>());
            }
        }

        this.saveToDB = saveToDB;
    }

    /**
     * Runs the scenario from the start date to the end date. Running the
     * scenario propagates the orbits of each satellite in the constellation and
     * computes the accesses between the satellites and the ground stations or
     * grid points. The accesses are stored and are accessible after the
     * simulation is run.
     *
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    @Override
    public FieldOfViewEventAnalysis call() throws OrekitException {
        for (CoverageDefinition cdef : getCoverageDefinitions()) {
            Logger.getGlobal().finer(String.format("Acquiring access times for %s...", cdef));
            Logger.getGlobal().finer(
                    String.format("Simulation dates %s to %s (%.2f days)",
                            getStartDate(), getEndDate(),
                            getEndDate().durationFrom(getStartDate()) / 86400.));

            //propogate each satellite individually
            ArrayList<SubRoutine> subRoutines = new ArrayList<>();
            for (Satellite sat : getUniqueSatellites(cdef)) {
                //first check if the satellite accesses are already saved in the database
                File file = new File(
                        System.getProperty("orekit.coveragedatabase"),
                        String.valueOf(sat.hashCode()));
                if (file.canRead()) {
                    HashMap<TopocentricFrame, TimeIntervalArray> satAccesses = readAccesses(file);
                    processAccesses(sat, cdef, satAccesses);
                    break;
                }

                //if no precomuted times available, then propagate
                Propagator prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());
                //Set stepsizes and threshold for detectors
                double fovStepSize = sat.getOrbit().getKeplerianPeriod() / 100.;
                double threshold = 1e-3;

                FieldOfViewSubRoutine subRoutine = new FieldOfViewSubRoutine(sat, prop, cdef, fovStepSize, threshold);
                subRoutines.add(subRoutine);
            }

            try {
                for (SubRoutine sr : ParallelRoutine.blockingSubmit(subRoutines)) {
                    if (sr == null) {
                        throw new IllegalStateException("Subroutine failed in field of view event.");
                    }
                    FieldOfViewSubRoutine fovsr = (FieldOfViewSubRoutine)sr;
                    Satellite sat = fovsr.getSat();
                    HashMap<TopocentricFrame, TimeIntervalArray> satAccesses = fovsr.getSatAccesses();
                    processAccesses(sat, cdef, satAccesses);
                    
                    if (saveToDB) {
                        File file = new File(
                                System.getProperty("orekit.coveragedatabase"),
                                String.valueOf(sat.hashCode()));
                        writeAccesses(file, satAccesses);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }

            //Make all time intervals stored in finalAccesses immutable
            for (TopocentricFrame pt : getEvents().get(cdef).keySet()) {
                getEvents().get(cdef).put(pt, getEvents().get(cdef).get(pt).createImmutable());
            }
        }

        return this;
    }

    /**
     * Saves the computed accesses from the satellite assigned to the coverage
     * definition.
     *
     * @param sat the satellite
     * @param cdef the coverage definition that the satellite is assigned to
     * @param satAccesses the accesses computed for the satellite to its
     * assigned coverage definition
     */
    protected void processAccesses(Satellite sat, CoverageDefinition cdef,
            HashMap<TopocentricFrame, TimeIntervalArray> satAccesses) {
        //save the satellite accesses 
        if (saveAllAccesses) {
            allAccesses.get(cdef).put(sat, satAccesses);
        }

        //merge the time accesses across all satellite for each coverage definition
        if (getEvents().containsKey(cdef)) {
            Map<TopocentricFrame, TimeIntervalArray> mergedAccesses
                    = EventIntervalMerger.merge(getEvents().get(cdef), satAccesses, false);
            getEvents().put(cdef, mergedAccesses);
        } else {
            getEvents().put(cdef, satAccesses);
        }
    }

    private void writeAccesses(File file, HashMap<TopocentricFrame, TimeIntervalArray> accesses) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(accesses);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private HashMap<TopocentricFrame, TimeIntervalArray> readAccesses(File file) {
        HashMap<TopocentricFrame, TimeIntervalArray> out = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            out = RawSafety.castHashMap(ois.readObject());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return out;
    }

    /**
     * Returns the individual accesses of a given satellite on a given coverage
     * definition after the scenario is finished running.
     *
     * @param covDef the coverage definition of interest
     * @param sat a satellite that is assigned to the coverage definition
     * @return If the scenario is set to save the individual satellite accesses
     * and the satellite is assigned to the coverage definition, a map of
     * coverage points and time interval array will be returned. else null
     */
    public HashMap<TopocentricFrame, TimeIntervalArray> getSatelliteAccesses(CoverageDefinition covDef, Satellite sat) {
        return allAccesses.get(covDef).get(sat);
    }

    /**
     * Returns the flag that marks whether each satellite's accesses should be
     * saved.
     *
     * @return
     */
    public boolean isSaveAllAccesses() {
        return saveAllAccesses;
    }

    /**
     * Returns the computed accesses for each coverage definition by each of the
     * satellites assigned to that coverage definition
     *
     * @return
     */
    public HashMap<CoverageDefinition, HashMap<Satellite, HashMap<TopocentricFrame, TimeIntervalArray>>> getAllAccesses() {
        return allAccesses;
    }

    /**
     * Returns the flag that dictates whether the individual satellite coverage
     * accesses are to be saved in the database
     *
     * @return
     */
    public boolean isSaveToDB() {
        return saveToDB;
    }

    @Override
    public String getHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("Field of View Event Analysis\n");
        sb.append("Assigned Constellations:\n");
        for (CoverageDefinition cdef : getCoverageDefinitions()) {
            sb.append("CoverageDefinition = ");
            sb.append(cdef.getName()).append("{");
            for (Constellation constel : cdef.getConstellations()) {
                sb.append(String.format("\tConstelation %s: %d satellites\n", constel.getName(), constel.getSatellites().size()));
                for (Satellite sat : constel.getSatellites()) {
                    sb.append(String.format("\t\tSatellite %s\n", sat.toString()));
                }
            }
            sb.append("}\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Creates a subroutine to run the field of view event analysis in parallel
     */
    private class FieldOfViewSubRoutine implements SubRoutine {

        /**
         * The satellite to propagate
         */
        private final Satellite sat;

        /**
         * The propagator
         */
        private final Propagator prop;

        /**
         * The coverage definition to access
         */
        private final CoverageDefinition cdef;
        
        /**
         * The step size during propagation when computing the field of view
         * events. Generally, this should be a small step for accurate results.
         */
        private final double fovStepSize;

        /**
         * The threshold, in seconds, when conducting root finding to determine
         * when an event occurred.
         */
        private final double threshold;

        /**
         * The times, for each point, when it is being accessed by the given
         * satellite and its payload.
         */
        private final HashMap<TopocentricFrame, TimeIntervalArray> satAccesses;

        /**
         *
         * @param sat The satellite to propagate
         * @param prop The propagator
         * @param cdef The coverage definition to access
         * @param fovStepSize The step size during propagation when computing
         * the field of view events. Generally, this should be a small step for
         * accurate results.
         * @param threshold The threshold, in seconds, when conducting root
         * finding to determine when an event occurred.
         */
        public FieldOfViewSubRoutine(Satellite sat, Propagator prop,
                CoverageDefinition cdef,
                double fovStepSize, double threshold) {
            this.sat = sat;
            this.prop = prop;
            this.cdef = cdef;
            this.fovStepSize = fovStepSize;
            this.threshold = threshold;

            this.satAccesses = new HashMap<>(cdef.getNumberOfPoints());
            for (CoveragePoint pt : cdef.getPoints()) {
                satAccesses.put(pt, getEmptyTimeArray());
            }
        }

        //NOTE: this implementation of in the field of view is a bit fragile if propagating highly elliptical orbits (>0.75). Maybe need to use smaller time steps los and fov detectors
        @Override
        public FieldOfViewSubRoutine call() throws Exception {
            Logger.getGlobal().finer(String.format("Propagating satellite %s...", sat));
            if (prop instanceof NumericalPropagator) {
                singlePropagate();
            } else {
                multiPropogate();
            }
            return this;
        }

        /**
         * If using an analytical propagator, taking short cuts and propagating
         * the satellite many many times is faster than packing all event
         * detectors into one propagator.
         *
         * @throws OrekitException
         */
        private void multiPropogate() throws OrekitException {
            SpacecraftState initialState = prop.getInitialState();
            for (Instrument inst : sat.getPayload()) {
                for (CoveragePoint pt : cdef.getPoints()) {
                    if (!lineOfSightPotential(pt, initialState.getOrbit(), FastMath.toRadians(2.0))) {
                        //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
                        continue;
                    }

                    prop.resetInitialState(initialState);
                    prop.clearEventsDetectors();

                    //need to reset initial state of the propagators or will progate from the last stop time
                    prop.resetInitialState(initialState);
                    prop.clearEventsDetectors();
                    //Next search through intervals with line of sight to compute when point is in field of view 
                    FOVDetector fovDetec = new FOVDetector(initialState, getStartDate(), getEndDate(),
                            pt, inst, fovStepSize, threshold, EventHandler.Action.CONTINUE);
                    prop.addEventDetector(fovDetec);
                    prop.propagate(getStartDate(), getEndDate());

                    TimeIntervalArray fovTimeArray = fovDetec.getTimeIntervalArray();
                    if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                        continue;
                    }
                    TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
                    satAccesses.put(pt, merger.orCombine());
                    prop.clearEventsDetectors();
                }
            }
        }

        /**
         * If using a numerical propagator, don't try to take short cuts.
         * Packing all event detectors in the propagator is faster than
         * propagating the satellite many many times.
         *
         * @throws OrekitException
         */
        private void singlePropagate() throws OrekitException {
            SpacecraftState initialState = prop.getInitialState();
            HashMap<CoveragePoint, FOVDetector> map = new HashMap<>();
            for (Instrument inst : sat.getPayload()) {
                for (CoveragePoint pt : cdef.getPoints()) {
                    if (!lineOfSightPotential(pt, initialState.getOrbit(), FastMath.toRadians(2.0))) {
                        //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
                        continue;
                    }

                    FOVDetector fovDetec = new FOVDetector(initialState, getStartDate(), getEndDate(),
                            pt, inst, fovStepSize, threshold, EventHandler.Action.CONTINUE);
                    prop.addEventDetector(fovDetec);
                    map.put(pt, fovDetec);
                }
                prop.propagate(getStartDate(), getEndDate());
                for (CoveragePoint pt : map.keySet()) {
                    TimeIntervalArray fovTimeArray = map.get(pt).getTimeIntervalArray();
                    if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                        continue;
                    }
                    TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
                    satAccesses.put(pt, merger.orCombine());
                }
                prop.clearEventsDetectors();
            }
        }

        public Satellite getSat() {
            return sat;
        }

        public HashMap<TopocentricFrame, TimeIntervalArray> getSatAccesses() {
            return satAccesses;
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
