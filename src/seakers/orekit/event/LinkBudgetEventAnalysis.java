/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;
import seakers.orekit.event.detector.FOVDetector;
import seakers.orekit.event.detector.LBDetector;
import seakers.orekit.event.detector.LOSDetector;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.object.linkbudget.LinkBudget;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * This event analysis is used to compute when the given ground points are in
 * the field of view of the given satellites and the link budget is closed.
 *
 * @author nhitomi
 */
public class LinkBudgetEventAnalysis extends AbstractGroundEventAnalysis {

    /**
     * Propagator factory that will create the necessary propagator for each
     * satellite
     */
    private final PropagatorFactory propagatorFactory;

    /**
     * a flag set by the user to toggle whether to save the link budget
     * intervals of each individual satellite or to release them from memory.
     */
    private final boolean saveAllLinkIntervals;

    /**
     * Stores all the link intervals of each satellite if saveAllLinkIntervals
     * is true.
     */
    private HashMap<CoverageDefinition, HashMap<Satellite, HashMap<TopocentricFrame, TimeIntervalArray>>> allLinkIntervals;

    /**
     * the number of threads to use in parallel processing
     */
    private final int numThreads;

    /**
     * Link budget object containing all communications parameters
     */
    private final LinkBudget lb;

    /**
     * Creates a new scenario.
     *
     * @param startDate of scenario
     * @param endDate of scenario
     * @param inertialFrame
     * @param propagatorFactory
     * @param covDefs
     * @param saveAllLinkIntervals true if user wants to maintain all the link
     * intervals from each individual satellite. false if user would like to
     * only get the merged link intervals between all satellites (this saves
     * memory).
     * @param numThreads number of threads to uses in parallelization of the
     * scenario by dividing up the coverage grid points across multiple threads
     * @param lb object containing all the communications parameters
     */
    public LinkBudgetEventAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            Frame inertialFrame, HashSet<CoverageDefinition> covDefs,
            PropagatorFactory propagatorFactory, boolean saveAllLinkIntervals,
            int numThreads, LinkBudget lb) {
        super(startDate, endDate, inertialFrame, covDefs);
        this.propagatorFactory = propagatorFactory;
        this.saveAllLinkIntervals = saveAllLinkIntervals;
        if (saveAllLinkIntervals) {
            this.allLinkIntervals = new HashMap<>();
        }

        this.numThreads = numThreads;

        this.lb = lb;
    }

    /**
     * Runs the scenario from the start date to the end date. Running the
     * scenario propagates the orbits of each satellite in the constellation and
     * computes the intervals between the satellites and the ground stations or
     * grid points in which the link budget is closed. The intervals are stored
     * and are accessible after the simulation is run.
     *
     * @return
     * @throws org.orekit.errors.OrekitException
     */
    @Override
    public LinkBudgetEventAnalysis call() throws OrekitException {
        //set up resource pool
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CompletionService<LinkBudgetSubRoutine> ecs = new ExecutorCompletionService<>(pool);

        for (CoverageDefinition cdef : getCoverageDefinitions()) {
            Logger.getGlobal().finer(String.format("Acquiring access times for %s...", cdef));
            Logger.getGlobal().finer(
                    String.format("Simulation dates %s to %s (%.2f days)",
                            getStartDate(), getEndDate(),
                            getEndDate().durationFrom(getStartDate()) / 86400.));
            if (saveAllLinkIntervals) {
                allLinkIntervals.put(cdef, new HashMap<>());
            }

            //propogate each satellite individually
            int nSubRoutines = 0;
            for (Satellite sat : getUniqueSatellites(cdef)) {
                //first check if the satellite accesses are already saved in the database
//                File file = new File(
//                        System.getProperty("orekit.coveragedatabase"),
//                        String.valueOf(sat.hashCode()));
//                if (file.canRead()) {
//                    HashMap<CoveragePoint, TimeIntervalArray> satAccesses = readAccesses(file);
//                    processAccesses(sat, cdef, satAccesses);
//                    break;
//                }

                //if no precomuted times available, then propagate
                Propagator prop = propagatorFactory.createPropagator(sat.getOrbit(), sat.getGrossMass());
                //Set stepsizes and threshold for detectors
                double losStepSize = sat.getOrbit().getKeplerianPeriod() / 10.;
                double fovStepSize = sat.getOrbit().getKeplerianPeriod() / 100.;
                double threshold = 1e-3;
                double lbStepSize = fovStepSize / 10.;

                LinkBudgetSubRoutine subRoutine = new LinkBudgetSubRoutine(sat, prop, cdef, losStepSize, fovStepSize, threshold, lb, lbStepSize);
                ecs.submit(subRoutine);
                nSubRoutines++;
            }

            for (int i = 0; i < nSubRoutines; i++) {
                LinkBudgetSubRoutine subRoutine = null;
                try {
                    subRoutine = ecs.take().get();
                } catch (InterruptedException ex) {
                    Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (subRoutine == null) {
                    pool.shutdown();
                    throw new IllegalStateException("Subroutine failed in field of view event.");
                }

                Satellite sat = subRoutine.getSat();
                HashMap<TopocentricFrame, TimeIntervalArray> satLinkBudgetIntervals = subRoutine.getSatLinkBudgetIntervals();
                processLinkBudgetIntervals(sat, cdef, satLinkBudgetIntervals);

            }

            //Make all time intervals stored in finalAccesses immutable
            for (TopocentricFrame pt : getEvents().get(cdef).keySet()) {
                getEvents().get(cdef).put(pt, getEvents().get(cdef).get(pt).createImmutable());
            }
        }

        pool.shutdown();
        return this;
    }

    /**
     * Saves the computed link intervals from the satellite assigned to the
     * coverage definition.
     *
     * @param sat the satellite
     * @param cdef the coverage definition that the satellite is assigned to
     * @param satAccesses the accesses computed for the satellite to its
     * assigned coverage definition
     */
    private void processLinkBudgetIntervals(Satellite sat, CoverageDefinition cdef,
            HashMap<TopocentricFrame, TimeIntervalArray> satLinkBudgetIntervals) {
        //save the satellite link intervals 
        if (saveAllLinkIntervals) {
            allLinkIntervals.get(cdef).put(sat, satLinkBudgetIntervals);
        }

        //merge the link intervals across all satellite for each coverage definition
        if (getEvents().containsKey(cdef)) {
            Map<TopocentricFrame, TimeIntervalArray> mergedAccesses
                    = EventIntervalMerger.merge(getEvents().get(cdef), satLinkBudgetIntervals, false);
            getEvents().put(cdef, mergedAccesses);
        } else {
            getEvents().put(cdef, satLinkBudgetIntervals);
        }
    }

//    private void writeAccesses(File file, HashMap<CoveragePoint, TimeIntervalArray> accesses) {
//        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
//            oos.writeObject(accesses);
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    private HashMap<CoveragePoint, TimeIntervalArray> readAccesses(File file) {
//        HashMap<CoveragePoint, TimeIntervalArray> out = new HashMap<>();
//        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
//            out = (HashMap<CoveragePoint, TimeIntervalArray>) ois.readObject();
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ClassNotFoundException ex) {
//            Logger.getLogger(FieldOfViewEventAnalysis.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return out;
//    }
    /**
     * Returns the individual link intervals of a given satellite on a given
     * coverage definition after the scenario is finished running.
     *
     * @param covDef the coverage definition of interest
     * @param sat a satellite that is assigned to the coverage definition
     * @return If the scenario is set to save the individual satellite link
     * intervals and the satellite is assigned to the coverage definition, a map
     * of coverage points and time interval array will be returned. else null
     */
    public HashMap<TopocentricFrame, TimeIntervalArray> getSatelliteLinkIntervals(CoverageDefinition covDef, Satellite sat) {
        return allLinkIntervals.get(covDef).get(sat);
    }

    /**
     * Returns the flag that marks whether each satellite's link interval should
     * be saved.
     *
     * @return
     */
    public boolean isSaveAllLinkIntervals() {
        return saveAllLinkIntervals;
    }

    /**
     * Returns the computed link interval for each coverage definition by each
     * of the satellites assigned to that coverage definition
     *
     * @return
     */
    public HashMap<CoverageDefinition, HashMap<Satellite, HashMap<TopocentricFrame, TimeIntervalArray>>> getAllLinkIntervals() {
        return allLinkIntervals;
    }

    /**
     * Gets the propagator factory used to create new propagators for this
     * scenario
     *
     * @return
     */
    public PropagatorFactory getPropagatorFactory() {
        return propagatorFactory;
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
    private class LinkBudgetSubRoutine implements Callable<LinkBudgetSubRoutine> {

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
         * The step size during propagation when computing the line of sight
         * events. Generally, this can be a large step. It is used to speed up
         * the simulation.
         */
        private final double losStepSize;

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
         * The times, for each point, when the link budget is closed by the
         * given satellite and its payload.
         */
        private final HashMap<TopocentricFrame, TimeIntervalArray> satlinkBudgetIntervals;

        /**
         * Link budget parameters.
         */
        private final LinkBudget lb;

        /**
         * The threshold, in seconds, when conducting root finding to determine
         * when an event occurred.
         */
        private final double lbStepSize;

        /**
         *
         * @param sat The satellite to propagate
         * @param prop The propagator
         * @param cdef The coverage definition to access
         * @param losStepSize The step size during propagation when computing
         * the line of sight events. Generally, this can be a large step. It is
         * used to speed up the simulation.
         * @param fovStepSize The step size during propagation when computing
         * the field of view events. Generally, this should be a small step for
         * accurate results.
         * @param threshold The threshold, in seconds, when conducting root
         * finding to determine when an event occurred.
         */
        public LinkBudgetSubRoutine(Satellite sat, Propagator prop,
                CoverageDefinition cdef, double losStepSize,
                double fovStepSize, double threshold, LinkBudget lb, double lbStepSize) {
            this.sat = sat;
            this.prop = prop;
            this.cdef = cdef;
            this.losStepSize = losStepSize;
            this.fovStepSize = fovStepSize;
            this.threshold = threshold;
            this.lb = lb;
            this.lbStepSize = lbStepSize;

            this.satlinkBudgetIntervals = new HashMap<>(cdef.getNumberOfPoints());
            for (TopocentricFrame pt : cdef.getPoints()) {
                satlinkBudgetIntervals.put(pt, getEmptyTimeArray());
            }
        }

        //NOTE: this implementation of in the field of view is a bit fragile if propagating highly elliptical orbits (>0.75). Maybe need to use smaller time steps los and fov detectors
        @Override
        public LinkBudgetSubRoutine call() throws Exception {
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
            Logger.getGlobal().finer(String.format("Propagating satellite %s...", sat));
            for (Instrument inst : sat.getPayload()) {
                for (CoveragePoint pt : cdef.getPoints()) {
                    prop.resetInitialState(initialState);
                    prop.clearEventsDetectors();

                    //First find all intervals with line of sight.
                    LOSDetector losDetec = new LOSDetector(
                            prop.getInitialState(), getStartDate(), getEndDate(),
                            pt, cdef.getPlanetShape(), getInertialFrame(),
                            losStepSize, threshold, EventHandler.Action.CONTINUE);
                    prop.addEventDetector(losDetec);
                    prop.propagate(getStartDate(), getEndDate());
                    TimeIntervalArray losTimeArray = losDetec.getTimeIntervalArray();
                    if (losTimeArray == null || losTimeArray.isEmpty()) {
                        continue;
                    }

                    //need to reset initial state of the propagators or will progate from the last stop time
                    prop.resetInitialState(initialState);
                    prop.clearEventsDetectors();
                    //Next search through intervals with line of sight to compute when point is in field of view 
                    FOVDetector fovDetec = new FOVDetector(initialState, getStartDate(), getEndDate(),
                            pt, inst, fovStepSize, threshold, EventHandler.Action.STOP);
                    prop.addEventDetector(fovDetec);

                    double date0 = 0;
                    double date1 = Double.NaN;
                    for (RiseSetTime interval : losTimeArray) {
                        if (interval.isRise()) {
                            date0 = interval.getTime();
                        } else {
                            date1 = interval.getTime();
                        }

                        if (!Double.isNaN(date1)) {
                            //first propagation will find the start time when the point is in the field of view
                            SpacecraftState s = prop.propagate(getStartDate().shiftedBy(date0), getStartDate().shiftedBy(date1));
                            if (Math.abs(s.getDate().durationFrom(initialState.getDate()) - date1) < 1e-6) {
                                //did not find an access
                                break;
                            }

                            //check to see if the first access was closing an access
                            if (!fovDetec.isOpen()) {
                                break;
                            }

                            //second propagation will find the end time when the point is in the field of view
                            prop.propagate(s.getDate(), getStartDate().shiftedBy(date1));

                            date1 = Double.NaN;
                        }
                    }
                    TimeIntervalArray fovTimeArray = fovDetec.getTimeIntervalArray();
                    if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                        continue;
                    }
                    //TimeIntervalMerger merger = new TimeIntervalMerger(satAccesses.get(pt), fovTimeArray);
                    //satAccesses.put(pt, merger.orCombine());
                    prop.clearEventsDetectors();

                    //link budget
                    prop.resetInitialState(initialState);

                    //Next search through intervals with line of sight to compute when point is in field of view 
                    LBDetector lbDetec = new LBDetector(initialState, getStartDate(), getEndDate(), pt, lb, lbStepSize, threshold, EventHandler.Action.CONTINUE);
                    prop.addEventDetector(lbDetec);
                    prop.propagate(getStartDate(), getEndDate());
                    TimeIntervalArray lbTimeArray = lbDetec.getTimeIntervalArray();
                    if (lbTimeArray == null || lbTimeArray.isEmpty()) {
                        continue;
                    }
                    TimeIntervalMerger mergerlb = new TimeIntervalMerger(fovTimeArray, lbTimeArray);
                    satlinkBudgetIntervals.put(pt, mergerlb.andCombine());
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
            Logger.getGlobal().finer(String.format("Propagating satellite %s...", sat));
            HashMap<CoveragePoint, FOVDetector> map = new HashMap<>();
            HashMap<CoveragePoint, LBDetector> map2 = new HashMap<>();

            for (Instrument inst : sat.getPayload()) {
                for (CoveragePoint pt : cdef.getPoints()) {
                    FOVDetector fovDetec = new FOVDetector(initialState, getStartDate(), getEndDate(),
                            pt, inst, fovStepSize, threshold, EventHandler.Action.CONTINUE);
                    prop.addEventDetector(fovDetec);
                    map.put(pt, fovDetec);

                }
                prop.propagate(getStartDate(), getEndDate());
                prop.clearEventsDetectors();
                prop.resetInitialState(initialState);
                for (CoveragePoint pt : cdef.getPoints()) {
                    LBDetector lbDetec = new LBDetector(initialState, getStartDate(), getEndDate(), pt, lb, lbStepSize, threshold, EventHandler.Action.CONTINUE);
                    prop.addEventDetector(lbDetec);
                    prop.addEventDetector(lbDetec);
                    map2.put(pt, lbDetec);
                }
                prop.propagate(getStartDate(), getEndDate());

                for (CoveragePoint pt : map.keySet()) {
                    TimeIntervalArray fovTimeArray = map.get(pt).getTimeIntervalArray();
                    TimeIntervalArray lbTimeArray = map2.get(pt).getTimeIntervalArray();
                    if (fovTimeArray == null || fovTimeArray.isEmpty()) {
                        continue;
                    }
                    if (lbTimeArray == null || lbTimeArray.isEmpty()) {
                        continue;
                    }
                    TimeIntervalMerger mergerlb = new TimeIntervalMerger(fovTimeArray, lbTimeArray);
                    satlinkBudgetIntervals.put(pt, mergerlb.andCombine());
                }
                prop.clearEventsDetectors();
            }
        }

        public Satellite getSat() {
            return sat;
        }

        public CoverageDefinition getCoverageDefinition() {
            return cdef;
        }

        public HashMap<TopocentricFrame, TimeIntervalArray> getSatLinkBudgetIntervals() {
            return satlinkBudgetIntervals;
        }

    }

}
