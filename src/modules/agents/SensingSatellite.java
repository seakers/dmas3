package modules.agents;

import modules.simulation.OrbitData;
import seakers.orekit.object.Constellation;
import seakers.orekit.object.Satellite;

public class SensingSatellite extends SatelliteAgent {

    public SensingSatellite(Constellation cons, Satellite sat, OrbitData orbitData) {
        super(cons, sat, orbitData);
    }

    @Override
    protected void live() {
        getLogger().info("\t Hello! I am a sensing satellite \n");
        getLogger().info("I currently have do nothing. It was fun to meet you though! Goodbye!");
        for (int i = 10; i > 0; i--) {
            pause(1000); // pauses the agent's thread for 1000 ms
        }
    }
}
