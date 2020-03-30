/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.scenario;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Logger;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.parallel.ParallelRoutine;
import seakers.orekit.parallel.SubRoutine;

/**
 * Stores information about the scenario or simulation including information of
 * start and stop times, time scales, epoch, etc.
 *
 * @author nozomihitomi
 */
public class Scenario extends AbstractScenario {

    /**
     * A set of analyses in which time intervals are recorded when an event
     * occurs
     */
    private final Collection<EventAnalysis> eventAnalyses;

    /**
     * A set of analyses in which values are recorded during the simulation at
     * fixed time steps
     */
    private final Collection<Analysis<?>> analyses;

    /**
     * Flag to signal if the scenario was simulated without errors.
     */
    private boolean done;

    /**
     * Creates a new scenario.
     *
     * @param name of scenario
     * @param startDate of scenario
     * @param endDate of scenario
     * @param timeScale of scenario
     * @param inertialFrame
     * @param propagatorFactory
     * @param covDefs
     * @param eventAnalyses
     * @param analyses the analyses to conduct during the propagation of this
     * scenario
     * @param properties the properties for the analyses
     */
    public Scenario(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame, PropagatorFactory propagatorFactory,
            HashSet<CoverageDefinition> covDefs,
            Collection<EventAnalysis> eventAnalyses,
            Collection<Analysis<?>> analyses,
            Properties properties) {

        super(name, startDate, endDate, timeScale, inertialFrame, propagatorFactory, covDefs, analyses);
        this.eventAnalyses = eventAnalyses;
        this.analyses = analyses;
    }

    @Override
    public Scenario call() throws Exception {
        done = false;
        for (EventAnalysis eventAnalysis : eventAnalyses) {
            eventAnalysis.call();
        }
        ArrayList<SubRoutine> subRoutines = new ArrayList<>();
        for (Analysis<?> analysis : analyses) {
            AnalysisSubRoutine subRoutine = new AnalysisSubRoutine(analysis);
            subRoutines.add(subRoutine);
        }
        ParallelRoutine.submit(subRoutines);
        done = true;
        return this;
    }

    /**
     * A flag that says if the simulation successfully completed. Resets every
     * time scenario is called
     *
     * @return true if the scenario simulated completely without any errors.
     * else false;
     */
    public boolean isDone() {
        return done;
    }

    /**
     * A builder pattern to set parameters for scenario
     */
    public static class Builder implements Serializable {

        private static final long serialVersionUID = -2447754795882563741L;

        //required fields
        /**
         * The time scale of the scenario
         */
        private final TimeScale timeScale;

        /**
         * Scenario start date
         */
        private final AbsoluteDate startDate;

        /**
         * Scenario end date
         */
        private final AbsoluteDate endDate;

        //optional parameters - initialized to default parameters
        /**
         * Scenario name
         */
        private String scenarioName = "scenario1";

        /**
         * Inertial frame used in scenario
         */
        private Frame inertialFrame = FramesFactory.getEME2000();

        /**
         * Propagator factory that will create the necessary propagator for each
         * satellite
         */
        private PropagatorFactory propagatorFactory = new PropagatorFactory(PropagatorType.J2);

        /**
         * Properties used in the analyses
         */
        private Properties properties = new Properties();

        /**
         * A set event analyses allow time intervals to be recorded when events
         * occur during the simulation
         */
        private Collection<EventAnalysis> eventAnalyses = new ArrayList<>();

        /**
         * A set of analyses in which values are recorded during the simulation
         * at fixed time steps
         */
        private Collection<Analysis<?>> analyses = new ArrayList<>();

        /**
         * The set of coverage definitions to simulate.
         */
        private HashSet<CoverageDefinition> covDefs = new HashSet<>();

        /**
         * The constructor for the builder
         *
         * @param startDate the start date of the scenario
         * @param endDate the end date of the scenario
         * @param timeScale the scale used to set the dates
         */
        public Builder(AbsoluteDate startDate, AbsoluteDate endDate, TimeScale timeScale) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.timeScale = timeScale;
        }

        /**
         * Option to set the Properties used in the analyses
         *
         * @param properties Properties used in the analyses
         * @return
         */
        public Builder properties(Properties properties) {
            this.properties = properties;
            return this;
        }

        /**
         * Option to set the analysis to conduct during the scenario. Event
         * analyses allow time intervals to be recorded when events occur during
         * the simulation. By default, no event analyses are conducted
         *
         * @param a
         * @return
         */
        public Builder eventAnalysis(Collection<EventAnalysis> a) {
            this.eventAnalyses = a;
            return this;
        }

        /**
         * Option to set the analysis to conduct during the scenario. Analyses
         * allow values to be recorded at fixed time steps during scenario. By
         * default, no analyses are conducted
         *
         * @param a
         * @return
         */
        public Builder analysis(Collection<Analysis<?>> a) {
            this.analyses = a;
            return this;
        }

        /**
         * Option to set the propagator factory that will create propagators for
         * each satellite. By default a J2 propagator is used.
         *
         * @param factory propagator factory that will create propagators for
         * each satellite
         * @return
         */
        public Builder propagatorFactory(PropagatorFactory factory) {
            this.propagatorFactory = factory;
            return this;
        }

        /**
         * Option to define the inertial frame in which the scenario is run.
         * EME2000 is used by default
         *
         * @param frame the inertial frame in which the scenario is run
         * @return
         */
        public Builder frame(Frame frame) {
            this.inertialFrame = frame;
            return this;
        }

        /**
         * Option to set the coverage definitions to assign to this scenario.
         *
         * @param covDefs coverage definitions to assign to this scenario
         * @return
         */
        public Builder covDefs(HashSet<CoverageDefinition> covDefs) {
            this.covDefs = covDefs;
            return this;
        }

        /**
         * The name to give this scenario. By default, the scenario is named as
         * "scenario1"
         *
         * @param name name to give this scenario
         * @return
         */
        public Builder name(String name) {
            this.scenarioName = name;
            return this;
        }

        /**
         * Builds an instance of a scenario with all the specified parameters.
         *
         * @return
         */
        public Scenario build() {
            return new Scenario(scenarioName, startDate, endDate, timeScale,
                    inertialFrame, propagatorFactory, covDefs, eventAnalyses, analyses, properties);
        }
    }

    /**
     * Gets the analyses that are assigned to this scenario
     *
     * @return
     */
    public Collection<Analysis<?>> getAnalyses() {
        return analyses;
    }

    /**
     * Gets the event analyses that are assigned to this scenario
     *
     * @return
     */
    public Collection<EventAnalysis> getEventAnalyses() {
        return eventAnalyses;
    }
    
    /**
     * Creates a subroutine to run the field of view event analysis in parallel
     */
    private class AnalysisSubRoutine implements SubRoutine {
        /**
         * The anaylisis to complete
         */
        private final Analysis<?> anal;
        
        public AnalysisSubRoutine(Analysis<?> anal){
            this.anal=anal;
        }
        @Override
        public AnalysisSubRoutine call() throws Exception {
            Logger.getGlobal().finer(String.format("Running analysis %s...", anal.getName()));
            anal.call();
            return this;
        }
        
       public Analysis<?> getAnalysis(){
           return anal;
       }
    }

}
