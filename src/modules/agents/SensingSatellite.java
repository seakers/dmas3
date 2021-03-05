package modules.agents;

import modules.planner.AbstractPlanner;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.Satellite;

public class SensingSatellite extends SatelliteAgent {

    public SensingSatellite(Constellation cons, Satellite sat, OrbitData orbitData, AbstractPlanner planner, SimGroups myGroups) {
        super(cons, sat, orbitData, planner, myGroups);
    }

    @Override
    public void sense() {
        getLogger().info("\t Hello! This is " + this.getName() + ". I am sensing...");
    }

    @Override
    public void think() {
        getLogger().info("\t Hello! This is " + this.getName() + ". I am thinking...");
    }

    @Override
    public void execute() {
        getLogger().info("\t Hello! This is " + this.getName() + ". I am executing...");
    }
}
