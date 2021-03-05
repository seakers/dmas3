package modules.agents;

import modules.planner.AbstractPlanner;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.Satellite;

import java.util.logging.Level;

/**
 * Sensing Satellite Agent
 * Represents a remote sensing satellite capable of performing scientific measurements of Earth.
 * It's duties involve making measurements of its ground track during its nominal operations as
 * well as reacting to newly requested or detected urgent measurements and changing its behavior
 * to maximize the performance of the constellation.
 *
 * @author a.aguilar
 */
public class SensingSatellite extends SatelliteAgent {

    public SensingSatellite(Constellation cons, Satellite sat, OrbitData orbitData,
                            AbstractPlanner planner, SimGroups myGroups, Level loggerLevel) {
        super(cons, sat, orbitData, planner, myGroups, loggerLevel);
    }

    /**
     * Reads messages from other satellites or ground stations. Performs measurements if specified by plan.
     */
    @Override
    public void sense() {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am sensing...");
    }

    /**
     * Gives new information from messages or measurements to planner and crates/modifies plan if needed
     */
    @Override
    public void think() {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am thinking...");
    }

    /**
     * Performs attitude maneuvers or sends messages to other satellites or ground stations if specified by plan
     */
    @Override
    public void execute() {
        getLogger().finer("\t Hello! This is " + this.getName() + ". I am executing...");
    }
}
