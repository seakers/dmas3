package modules.agents;

import modules.planner.AbstractPlanner;
import modules.simulation.OrbitData;
import modules.simulation.SimGroups;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.Satellite;

public class CommsSatellite extends SatelliteAgent {

    public CommsSatellite(Constellation cons, Satellite sat, OrbitData orbitData, AbstractPlanner planner, SimGroups myGroups) {
        super(cons, sat, orbitData, planner, myGroups);
    }

    @Override
    public void sense() {
        getLogger().info("\t Hello! I am a sensing satellite. I am sensing...\n");
        for (int i = 10; i > 0; i--) {
            pause(1000); // pauses the agent's thread for 1000 ms
        }
    }

    @Override
    public void think() {
        getLogger().info("\t Hello! I am a sensing satellite. I am thinking...\n");
        for (int i = 10; i > 0; i--) {
            pause(1000); // pauses the agent's thread for 1000 ms
        }
    }

    @Override
    public void execute() {
        getLogger().info("\t Hello! I am a sensing satellite. I am executing...\n");
        for (int i = 10; i > 0; i--) {
            pause(1000); // pauses the agent's thread for 1000 ms
        }
    }

    @Override
    protected void live() {
        getLogger().info("\t Hello! I am a communications satellite \n");
        getLogger().info("I currently have do nothing. It was fun to meet you though! Goodbye!");
        for (int i = 10; i > 0; i--) {
            pause(1000); // pauses the agent's thread for 1000 ms
        }
    }
}
