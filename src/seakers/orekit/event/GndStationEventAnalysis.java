/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;
import seakers.orekit.event.detector.GroundStationDetector;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;
import seakers.orekit.parallel.ParallelRoutine;
import seakers.orekit.parallel.SubRoutine;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * This event analysis is used to compute when the given ground stations are in
 * accessible by the given satellites. The satellites are assigned one or more
 * ground stations to communicate with. A satellite and ground station can
 * communicate if they share the same communication bands and the satellites is
 * above the minimum elevation angle of the ground station.
 *
 * @author nhitomi
 */
public class GndStationEventAnalysis extends AbstractEventAnalysis {

    /**
     * Propagator factory that will create the necessary propagator for each
     * satellite
     */
    private final PropagatorFactory propagatorFactory;

    /**
     * the assignment of satellites to ground stations
     */
    private final Map<Satellite, Set<GndStation>> stationAssignment;

    /**
     * Stores all the accesses of each satellite if saveAllAccesses is true.
     */
    private final HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> allAccesses;

    /**
     * Creates a new event analysis.
     *
     * @param startDate of scenario
     * @param endDate of scenario
     * @param inertialFrame the inertial frame used in the simulation
     * @param propagatorFactory the factory to create propagtors for each
     * satellite
     * @param stationAssignment the assignment of satellites to ground stations.
     * A satellite can be assigned to one or more ground stations and ground
     * stations can be assigned to more than one satellite.
     */
    public GndStationEventAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame, Map<Satellite, Set<GndStation>> stationAssignment,
            PropagatorFactory propagatorFactory) {
        super(startDate, endDate, inertialFrame);
        this.propagatorFactory = propagatorFactory;
        this.allAccesses = new HashMap<>();
        this.stationAssignment = stationAssignment;
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
    public GndStationEventAnalysis call() throws OrekitException {
        //propogate each satellite individually
        ArrayList<SubRoutine> subRoutines = new ArrayList<>();

        for (Satellite sat : stationAssignment.keySet()) {
            Logger.getGlobal().finer(String.format("Acquiring ground station accesses for %s...", sat));
            Logger.getGlobal().finer(
                    String.format("Simulation dates %s to %s (%.2f days)",
                            getStartDate(), getEndDate(),
                            getEndDate().durationFrom(getStartDate()) / 86400.));
            allAccesses.put(sat, new HashMap<>());

            //propogate each satellite individually
            //if no precomuted times available, then propagate
            Propagator prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());
            //Set stepsizes and threshold for detectors
            double losStepSize = sat.getOrbit().getKeplerianPeriod() / 10.;
            double threshold = 1e-3;

            GndStationSubRoutine subRoutine
                    = new GndStationSubRoutine(sat, prop, stationAssignment.get(sat), losStepSize, threshold);
            subRoutines.add(subRoutine);
        }

        try {
            for (SubRoutine sr : ParallelRoutine.submit(subRoutines)) {
                if (sr == null) {
                    throw new IllegalStateException("Subroutine failed in field of view event.");
                }
                GndStationSubRoutine gndsta = (GndStationSubRoutine) sr;
                Satellite sat = gndsta.getSat();
                HashMap<GndStation, TimeIntervalArray> satAccesses = gndsta.getSatAccesses();
                processAccesses(sat, satAccesses);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Make all time intervals stored in accesses immutable
        for (Satellite sat : stationAssignment.keySet()) {
            for (GndStation station : stationAssignment.get(sat)) {
                TimeIntervalArray immutableArray
                        = allAccesses.get(sat).get(station).createImmutable();
                allAccesses.get(sat).put(station, immutableArray);
            }
        }

        return this;
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
    private void processAccesses(Satellite sat,
            HashMap<GndStation, TimeIntervalArray> satAccesses) {
        //save the satellite accesses 
        allAccesses.put(sat, satAccesses);
    }

    /**
     * Returns the individual accesses of a given satellite on a given ground
     * station after the scenario is finished running.
     *
     * @param sat a satellite that is assigned to the ground station
     * @return a map of ground stations and time interval array
     */
    public HashMap<GndStation, TimeIntervalArray> getSatelliteAccesses(Satellite sat) {
        return allAccesses.get(sat);
    }

    /**
     * Returns the computed accesses for each ground station by each of the
     * satellites assigned to that coverage definition
     *
     * @return the computed accesses for each ground station by each of the
     * satellites assigned to that coverage definition
     */
    public HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> getAllAccesses() {
        return allAccesses;
    }

    /**
     * Returns the computed accesses for each ground station by any of their
     * assigned satellites.
     *
     * @return The access times to each ground station
     */
    public Map<TopocentricFrame, TimeIntervalArray> getEvents() {
        Map<TopocentricFrame, TimeIntervalArray> out = new HashMap<>();

        Map<TopocentricFrame, Collection<TimeIntervalArray>> timeArrays = new HashMap<>();
        for (Satellite sat : allAccesses.keySet()) {
            for (GndStation station : allAccesses.get(sat).keySet()) {

                //check if the ground station was already added to results
                if (!timeArrays.containsKey(station.getBaseFrame())) {
                    timeArrays.put(station.getBaseFrame(), new ArrayList<>());
                }
                timeArrays.get(station.getBaseFrame()).add(allAccesses.get(sat).get(station));

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
    public Map<TopocentricFrame, TimeIntervalArray> getEvents(Satellite satellite) {
        Map<TopocentricFrame, TimeIntervalArray> out = new HashMap<>();
        Map<TopocentricFrame, Collection<TimeIntervalArray>> timeArrays = new HashMap<>();
        for (GndStation station : allAccesses.get(satellite).keySet()) {

            //check if the ground station was already added to results
            if (!timeArrays.containsKey(station.getBaseFrame())) {
                timeArrays.put(station.getBaseFrame(), new ArrayList<>());
            }
            timeArrays.get(station.getBaseFrame()).add(allAccesses.get(satellite).get(station));
        }

        for (TopocentricFrame pt : timeArrays.keySet()) {
            TimeIntervalMerger merger = new TimeIntervalMerger(timeArrays.get(pt));
            out.put(pt, merger.orCombine());
        }
        return out;
    }

    @Override
    public String getHeader() {
        StringBuilder sb = new StringBuilder();
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
    private class GndStationSubRoutine implements SubRoutine {

        /**
         * The satellite to propagate
         */
        private final Satellite sat;

        /**
         * The propagator
         */
        private final Propagator prop;

        /**
         * the set of ground stations assigned to the satellite
         */
        private final Set<GndStation> stations;

        /**
         * The step size during propagation when computing the line of sight
         * events. Generally, this can be a large step. It is used to speed up
         * the simulation.
         */
        private final double stepSize;

        /**
         * The threshold, in seconds, when conducting root finding to determine
         * when an event occurred.
         */
        private final double threshold;

        /**
         * The times, for each ground station, when it is being accessed by the
         * given satellite and its payload.
         */
        private final HashMap<GndStation, TimeIntervalArray> accesses;

        /**
         *
         * @param sat The satellite to propagate
         * @param prop The propagator
         * @param station The coverage definition to access
         * @param stepSize The step size during propagation
         * @param threshold The threshold, in seconds, when conducting root
         * finding to determine when an event occurred.
         */
        public GndStationSubRoutine(Satellite sat, Propagator prop,
                Set<GndStation> stations, double stepSize, double threshold) {
            this.sat = sat;
            this.prop = prop;
            this.stations = stations;
            this.stepSize = stepSize;
            this.threshold = threshold;

            this.accesses = new HashMap<>(stations.size());
            for (GndStation station : stations) {
                accesses.put(station, getEmptyTimeArray());
            }
        }

        //NOTE: this implementation of in the field of view is a bit fragile if propagating highly elliptical orbits (>0.75). Maybe need to use smaller time steps los and fov detectors
        @Override
        public GndStationSubRoutine call() throws Exception {
            SpacecraftState initialState = prop.getInitialState();
            Logger.getGlobal().finer(String.format("Propagating satellite %s...", sat));
            HashMap<GndStation, GroundStationDetector> map = new HashMap<>();
            for (GndStation station : stations) {
                GroundStationDetector gndstatDetec
                        = new GroundStationDetector(initialState,
                                getStartDate(), getEndDate(),
                                sat.getTransmitter(), sat.getReceiver(), station,
                                EventHandler.Action.CONTINUE, stepSize, threshold);

                prop.addEventDetector(gndstatDetec);
                map.put(station, gndstatDetec);

            }
            prop.propagate(getStartDate(), getEndDate());
            for (GndStation station : map.keySet()) {
                TimeIntervalArray fovTimeArray = map.get(station).getTimeIntervalArray();
                if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                    continue;
                }
                TimeIntervalMerger merger = new TimeIntervalMerger(accesses.get(station), fovTimeArray);
                accesses.put(station, merger.orCombine());
            }
            prop.clearEventsDetectors();
            return this;
        }

        public Satellite getSat() {
            return sat;
        }

        public HashMap<GndStation, TimeIntervalArray> getSatAccesses() {
            return accesses;
        }

    }

}
