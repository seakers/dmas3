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
import java.util.Collection;
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
import seakers.orekit.event.detector.GroundStationDetector;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.GndStation;
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
public class FieldOfViewAndGndStationEventAnalysis extends AbstractGroundEventAnalysis {

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
     * the assignment of satellites to ground stations
     */
    private final Map<Satellite, Set<GndStation>> stationAssignment;

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
     * Stores all the accesses of each satellite if saveAllAccesses is true.
     */
    private final HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> allAccessesGS;

    /**
     * Creates a new event analysis.
     *
     * @param startDate of analysis
     * @param endDate of analysis
     * @param inertialFrame the inertial frame used in the simulation
     * @param propagatorFactory the factory to create propagtors for each
     * satellite
     * @param stationAssignment
     * @param covDefs
     * @param saveAllAccesses true if user wants to maintain all the accesses
     * from each individual satellite. false if user would like to only get the
     * merged accesses between all satellites (this saves memory).
     * @param saveToDB flag to dictate whether the coverage accesses of
     * individual satellites should be saved to the coverage database
     */
    public FieldOfViewAndGndStationEventAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame, Set<CoverageDefinition> covDefs, Map<Satellite, Set<GndStation>> stationAssignment,
            PropagatorFactory propagatorFactory, boolean saveAllAccesses, boolean saveToDB) {
        super(startDate, endDate, inertialFrame, covDefs);
        this.propagatorFactory = propagatorFactory;
        this.saveAllAccesses = saveAllAccesses;
        if (saveAllAccesses) {
            this.allAccesses = new HashMap<>();
            for (CoverageDefinition cdef : covDefs) {
                allAccesses.put(cdef, new HashMap<>());
            }
        }
        this.allAccessesGS = new HashMap<>();
        this.stationAssignment = stationAssignment;
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
    public FieldOfViewAndGndStationEventAnalysis call() throws OrekitException {
        for (CoverageDefinition cdef : getCoverageDefinitions()) {
            Logger.getGlobal().finer("Acquiring ground station accesses...");
            Logger.getGlobal().finer(String.format("Acquiring access times for %s...", cdef));
            Logger.getGlobal().finer(
                    String.format("Simulation dates %s to %s (%.2f days)",
                            getStartDate(), getEndDate(),
                            getEndDate().durationFrom(getStartDate()) / 86400.));

            //propogate each satellite individually
            ArrayList<SubRoutine> subRoutines = new ArrayList<>();
            for (Satellite sat : getUniqueSatellites(cdef)) {
                allAccessesGS.put(sat, new HashMap<>());
                //if no precomuted times available, then propagate
                Propagator prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());
                //Set stepsizes and threshold for detectors
                double fovStepSize = sat.getOrbit().getKeplerianPeriod() / 100.;
                double losStepSize = sat.getOrbit().getKeplerianPeriod() / 10.;
                double threshold = 1e-3;

                FieldOfViewAndGndStationSubRoutine subRoutine = new FieldOfViewAndGndStationSubRoutine(sat, prop, 
                        cdef, stationAssignment.get(sat),fovStepSize, losStepSize, threshold);
               
                subRoutines.add(subRoutine);
            }

            try {
                for (SubRoutine sr : ParallelRoutine.submit(subRoutines)) {
                    if (sr == null) {
                        throw new IllegalStateException("Subroutine failed in event analysis.");
                    }
                    FieldOfViewAndGndStationSubRoutine subr = (FieldOfViewAndGndStationSubRoutine)sr;
                    Satellite sat = subr.getSat();
                    HashMap<TopocentricFrame, TimeIntervalArray> satAccesses = subr.getSatAccesses();
                    processAccesses(sat, cdef, satAccesses);
                    HashMap<GndStation, TimeIntervalArray> satAccessesGS = subr.getSatAccessesGS();
                    processAccessesGS(sat, satAccessesGS);
                    
                    if (saveToDB) {
                        File file = new File(
                                System.getProperty("orekit.coveragedatabase"),
                                String.valueOf(sat.hashCode()));
                        writeAccesses(file, satAccesses);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }

            //Make all time intervals stored in finalAccesses immutable
            for (TopocentricFrame pt : getEvents().get(cdef).keySet()) {
                getEvents().get(cdef).put(pt, getEvents().get(cdef).get(pt).createImmutable());
            }
            //Make all time intervals stored in accesses immutable
            for (Satellite sat : stationAssignment.keySet()) {
                for (GndStation station : stationAssignment.get(sat)) {
                    TimeIntervalArray immutableArray
                            = allAccessesGS.get(sat).get(station).createImmutable();
                    allAccessesGS.get(sat).put(station, immutableArray);
                }
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
    
    /**
     * Saves the computed accesses from the satellite assigned to the ground
     * station
     *
     * @param sat the satellite
     * @param station the ground station assigned to the satellite
     * @param satAccesses the accesses computed for the satellite to its
     * assigned coverage definition
     */
    private void processAccessesGS(Satellite sat, HashMap<GndStation, TimeIntervalArray> satAccesses) {
        //save the satellite accesses 
        allAccessesGS.put(sat, satAccesses);
    }

    private void writeAccesses(File file, HashMap<TopocentricFrame, TimeIntervalArray> accesses) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(accesses);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private HashMap<TopocentricFrame, TimeIntervalArray> readAccesses(File file) {
        HashMap<TopocentricFrame, TimeIntervalArray> out = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            out = RawSafety.castHashMap(ois.readObject());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(FieldOfViewAndGndStationEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
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
     * Returns the individual accesses of a given satellite on a given ground
     * station after the scenario is finished running.
     *
     * @param sat a satellite that is assigned to the ground station
     * @return a map of ground stations and time interval array
     */
    public HashMap<GndStation, TimeIntervalArray> getSatelliteAccessesGS(Satellite sat) {
        return allAccessesGS.get(sat);
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
     * Returns the computed accesses for each ground station by each of the
     * satellites assigned to that coverage definition
     *
     * @return the computed accesses for each ground station by each of the
     * satellites assigned to that coverage definition
     */
    public HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> getAllAccessesGS() {
        return allAccessesGS;
    }
    
    
    /**
     * Returns the computed accesses for each ground station by any of their
     * assigned satellites.
     *
     * @return The access times to each ground station
     */
    public Map<TopocentricFrame, TimeIntervalArray> getEventsGS() {
        Map<TopocentricFrame, TimeIntervalArray> out = new HashMap<>();

        Map<TopocentricFrame, Collection<TimeIntervalArray>> timeArrays = new HashMap<>();
        for (Satellite sat : allAccessesGS.keySet()) {
            for (GndStation station : allAccessesGS.get(sat).keySet()) {

                //check if the ground station was already added to results
                if (!timeArrays.containsKey(station.getBaseFrame())) {
                    timeArrays.put(station.getBaseFrame(), new ArrayList<>());
                }
                timeArrays.get(station.getBaseFrame()).add(allAccessesGS.get(sat).get(station));

            }
        }
        for (TopocentricFrame pt : timeArrays.keySet()) {
            TimeIntervalMerger merger = new TimeIntervalMerger(timeArrays.get(pt));
            out.put(pt, merger.orCombine());
        }
        return out;
    }
    
    /**
     * Returns the computed accesses for each ground station by a given
     * satellite. If more than one ground station is assigned to the same
     * Topocentric Frame, their accesses will be merged into the same time
     * interval array.
     *
     * @param satellite
     * @return The access times to each ground station
     */
    public Map<TopocentricFrame, TimeIntervalArray> getEventsGS(Satellite satellite) {
        Map<TopocentricFrame, TimeIntervalArray> out = new HashMap<>();
        Map<TopocentricFrame, Collection<TimeIntervalArray>> timeArrays = new HashMap<>();
        for (GndStation station : allAccessesGS.get(satellite).keySet()) {

            //check if the ground station was already added to results
            if (!timeArrays.containsKey(station.getBaseFrame())) {
                timeArrays.put(station.getBaseFrame(), new ArrayList<>());
            }
            timeArrays.get(station.getBaseFrame()).add(allAccessesGS.get(satellite).get(station));
        }

        for (TopocentricFrame pt : timeArrays.keySet()) {
            TimeIntervalMerger merger = new TimeIntervalMerger(timeArrays.get(pt));
            out.put(pt, merger.orCombine());
        }
        return out;
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
        sb.append("Ground Station Access Event Analysis\n");
        sb.append("Assigned Constellations:\n");
        for (Satellite satellite : stationAssignment.keySet()) {
            sb.append("satellite = ");
            sb.append(satellite.getName()).append("{");
            for (GndStation station : stationAssignment.get(satellite)) {
                sb.append(String.format("Ground Station %s\n", station));
            }
            sb.append("}\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Creates a subroutine to run the field of view event analysis in parallel
     */
    private class FieldOfViewAndGndStationSubRoutine implements SubRoutine {

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
         * the set of ground stations assigned to the satellite
         */
        private final Set<GndStation> stations;
        
        /**
         * The step size during propagation when computing the field of view
         * events. Generally, this should be a small step for accurate results.
         */
        private final double fovStepSize;
        
        /**
         * The step size during propagation when computing the line of sight
         * events. Generally, this can be a large step. It is used to speed up
         * the simulation.
         */
        private final double losStepSize;

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
         * The times, for each ground station, when it is being accessed by the
         * given satellite and its payload.
         */
        private final HashMap<GndStation, TimeIntervalArray> accessesGS;

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
        public FieldOfViewAndGndStationSubRoutine(Satellite sat, Propagator prop,
                CoverageDefinition cdef, Set<GndStation> stations, double fovStepSize,
                double losStepSize, double threshold) {
            this.sat = sat;
            this.prop = prop;
            this.stations = stations;
            this.cdef = cdef;
            this.fovStepSize = fovStepSize;
            this.losStepSize = losStepSize;
            this.threshold = threshold;

            this.satAccesses = new HashMap<>(cdef.getNumberOfPoints());
            for (CoveragePoint pt : cdef.getPoints()) {
                satAccesses.put(pt, getEmptyTimeArray());
            }
            
            this.accessesGS = new HashMap<>(stations.size());
            for (GndStation station : stations) {
                accessesGS.put(station, getEmptyTimeArray());
            }
        }

        //NOTE: this implementation of in the field of view is a bit fragile if propagating highly elliptical orbits (>0.75). Maybe need to use smaller time steps los and fov detectors
        @Override
        public FieldOfViewAndGndStationSubRoutine call() throws Exception {
            Logger.getGlobal().finer(String.format("Propagating satellite %s...", sat));
            //if (prop instanceof NumericalPropagator) {
                singlePropagate();
            //} else {
            //    multiPropogate();
            //}
            return this;
        }

//        /**
//         * If using an analytical propagator, taking short cuts and propagating
//         * the satellite many many times is faster than packing all event
//         * detectors into one propagator.
//         *
//         * @throws OrekitException
//         */
//        private void multiPropogate() throws OrekitException {
//            SpacecraftState initialState = prop.getInitialState();
//            for (Instrument inst : sat.getPayload()) {
//                for (CoveragePoint pt : cdef.getPoints()) {
//                    if (!lineOfSightPotential(pt, initialState.getOrbit(), FastMath.toRadians(2.0))) {
//                        //if a point is not within 2 deg latitude of what is accessible to the satellite via line of sight, don't compute the accesses
//                        continue;
//                    }
//
//                    prop.resetInitialState(initialState);
//                    prop.clearEventsDetectors();
//
//                    //need to reset initial state of the propagators or will progate from the last stop time
//                    prop.resetInitialState(initialState);
//                    prop.clearEventsDetectors();
//                    //Next search through intervals with line of sight to compute when point is in field of view 
//                    FOVDetector fovDetec = new FOVDetector(initialState, getStartDate(), getEndDate(),
//                            pt, inst, fovStepSize, threshold, EventHandler.Action.CONTINUE);
//                    prop.addEventDetector(fovDetec);
//                    prop.propagate(getStartDate(), getEndDate());
//
//                    TimeIntervalArray fovTimeArray = fovDetec.getTimeIntervalArray();
//                    if (fovTimeArray == null || fovTimeArray.isEmpty()) {
//                        continue;
//                    }
//                    TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
//                    satAccesses.put(pt, merger.orCombine());
//                    prop.clearEventsDetectors();
//                }
//            }
//        }

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
            HashMap<GndStation, GroundStationDetector> mapGS = new HashMap<>();
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
                for (GndStation station : stations) {
                    GroundStationDetector gndstatDetec
                            = new GroundStationDetector(initialState,
                                    getStartDate(), getEndDate(),
                                    sat.getTransmitter(), sat.getReceiver(), station,
                                    EventHandler.Action.CONTINUE, losStepSize, threshold);

                    prop.addEventDetector(gndstatDetec);
                    mapGS.put(station, gndstatDetec);

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
                for (GndStation station : mapGS.keySet()) {
                    TimeIntervalArray fovTimeArray = mapGS.get(station).getTimeIntervalArray();
                    if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                        continue;
                    }
                    TimeIntervalMerger merger = new TimeIntervalMerger(accessesGS.get(station), fovTimeArray);
                    accessesGS.put(station, merger.orCombine());
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
        
        public HashMap<GndStation, TimeIntervalArray> getSatAccessesGS() {
            return accessesGS;
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
