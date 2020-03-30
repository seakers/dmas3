/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.scenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Callable;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * The barebones object of a scenario.
 * @author nhitomi
 */
public abstract class AbstractScenario implements Callable<Scenario>{
    /**
     * Name of the scenario
     */
    private final String scenarioName;

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

    /**
     * Inertial frame used in scenario
     */
    private final Frame inertialFrame;

    /**
     * Propagator factory that will create the necessary propagator for each
     * satellite
     */
    private final PropagatorFactory propagatorFactory;

    /**
     * A set of the unique constellations
     */
    private final HashSet<Constellation> uniqueConstellations;

    /**
     * The mapping of the unique satellites assigned to each coverage definition
     */
    private final HashMap<CoverageDefinition, HashSet<Satellite>> uniqueSatsAssignedToCovDef;

    /**
     * A collection of the unique satellites in this scenario. Required to only
     * propagate each satellite once and once only.
     */
    private final HashSet<Satellite> uniqueSatellites;

    /**
     * The set of coverage definitions to simulate.
     */
    private final HashSet<CoverageDefinition> covDefs;
    
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
     * @param analyses the analyses to conduct during the propagation of this
     * scenario
     */
    public AbstractScenario(String name, AbsoluteDate startDate, AbsoluteDate endDate,
            TimeScale timeScale, Frame inertialFrame, PropagatorFactory propagatorFactory,
            HashSet<CoverageDefinition> covDefs, Collection<Analysis<?>> analyses) {

        this.scenarioName = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.timeScale = timeScale;
        this.inertialFrame = inertialFrame;
        this.propagatorFactory = propagatorFactory;

        this.covDefs = covDefs;
        //record all unique satellite and constellations
        this.uniqueSatsAssignedToCovDef = new HashMap<>();
        this.uniqueConstellations = new HashSet<>();
        this.uniqueSatellites = new HashSet<>();
        includeCovDef(covDefs);
    }

    /**
     * This should only be called in the constructor in order to prevent
     * changing the accesses times or the mapping between satellites to coverage
     * definitions
     *
     * @param c
     */
    private void includeCovDef(HashSet<CoverageDefinition> c) {
        for (CoverageDefinition cdef : c) {
            covDefs.add(cdef);
            uniqueSatsAssignedToCovDef.put(cdef, new HashSet<>());
            for (Constellation constel : cdef.getConstellations()) {
                uniqueConstellations.add(constel);
                for (Satellite satellite : constel.getSatellites()) {
                    uniqueSatellites.add(satellite);
                    uniqueSatsAssignedToCovDef.get(cdef).add(satellite);
                }
            }
        }
    }

    /**
     * Gets the list of coverage definitions assigned to this scenario
     *
     * @return
     */
    public HashSet<CoverageDefinition> getCoverageDefinitions() {
        return new HashSet<>(covDefs);
    }

    /**
     * Gets the unique constellations that are simulated in this scenario
     *
     * @return
     */
    public HashSet<Constellation> getUniqueConstellations() {
        return new HashSet<>(uniqueConstellations);
    }

    /**
     * Gets the unique satellites that are simulated in this scenario
     *
     * @return
     */
    public HashSet<Satellite> getUniqueSatellites() {
        return new HashSet<>(uniqueSatellites);
    }

    /**
     * Gets the coverage definition specified by a name
     *
     * @param name of the CoverageDefinition we want to get
     * @return
     */
    public CoverageDefinition getCoverageDefinition(String name) {
        Iterator<CoverageDefinition> i = this.covDefs.iterator();
        while (i.hasNext()) {
            CoverageDefinition c = i.next();
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }
    
    /**
     * Pretty print of the dates and the time scale
     *
     * @return pretty print string of dates and time scale
     */
    public String ppDate() {
        String out = "Dates{\n";
        out += "\tStart date: " + startDate.toString(timeScale) + "\n";
        out += "\tEnd date: " + endDate.toString(timeScale) + "\n";
        out += "\tTime scale: " + timeScale.getName() + "\n";
        out += "}\n";
        return out;
    }

    /**
     * Pretty print of the inertial frame
     *
     * @return pretty print string of the inertial Frame
     */
    public String ppFrame() {
        String out = "Frame{\n";
        out += "\tInertial Frame: " + inertialFrame.getName() + "\n";
        out += "}\n";
        return out;
    }

    /**
     * Pretty print of the propagator
     *
     * @return pretty print string of the propagator
     */
    public String ppPropgator() {
        String out = "Propagator{\n";
        out += "\tPropagator: " + propagatorFactory.getPropType() + "\n";
        out += "\tForceModels{\n";
//        out += String.format("\tForceModel{\n", args)
        out += "}\n";
        out += "\n}\n";
        return out;
    }

    /**
     * Pretty print of all the objects included in this scenario
     *
     * @return pretty print string of all the objects included in this scenario
     */
    public String ppConstellations() {
        String out = "Constellations{\n";
        for (Constellation constel : uniqueConstellations) {
            out += "\tName: " + constel.getName() + "{\n";
            for (Satellite sat : constel.getSatellites()) {
                out += "\t\tSatellite{\n";
                out += "\t\t\tName: " + sat.getName() + "\n";
                out += "\t\t\tOrbit: {" + sat.ppOrbit() + "}\n";
                out += "\t\t\tPayload {\n";
                for (Instrument inst : sat.getPayload()) {
                    out += "\t\t\t\tInstrument name: " + inst.getName() + "{\n";
                    out += "\t\t\t\t\tFOV: " + inst.getName() + "\n";
                    out += "\t\t\t\t}\n";
                }
                out += "\t\t\t}\n";
                out += "\t\t}\n";
            }
            out += "\t}\n\n";
        }
        out += "}\n";
        return out;
    }

    /**
     * Pretty print of all the coverage definitions included in this scenario
     *
     * @return pretty print string of all the coverage definitions included in
     * this scenario
     */
    public String ppCoverageDefinition() {
        String out = "CoverageDefinition{\n";
        for (CoverageDefinition covdef : this.covDefs) {
            out += "\tName: " + covdef.getName() + " {\n";
            out += "\t\tViewers: {\n";
            for (Constellation constel : covdef.getConstellations()) {
                out += "\t\t\t" + constel + "\n";
            }
            out += "\t\t}\n";
            out += "\t\tNumber of Points: " + covdef.getNumberOfPoints() + "\n";
            out += "\t\tLatitude\tLongitude\tAltitude:\n";
            ArrayList<CoveragePoint> sortedTopoPts = new ArrayList<>(covdef.getPoints());
            Collections.sort(sortedTopoPts);
            for (CoveragePoint topoPt : sortedTopoPts) {
                GeodeticPoint pt = topoPt.getPoint();
                out += String.format("\t\t%1$03.6f", FastMath.toDegrees(pt.getLatitude()));
                out += String.format("\t%1$03.6f", FastMath.toDegrees(pt.getLongitude()));
                out += String.format("\t%1$03.6f\n", FastMath.toDegrees(pt.getAltitude()));
            }
            out += "\t}\n";
        }
        out += "}\n";
        return out;
    }

    public AbsoluteDate getStartDate() {
        return startDate;
    }

    public String getName() {
        return scenarioName;
    }

    public AbsoluteDate getEndDate() {
        return endDate;
    }

    @Override
    public String toString() {
        return "Scenario{" + "scenarioName=" + scenarioName + ", startDate=" + startDate + ", endDate= " + endDate + '}';
    }
    
}
