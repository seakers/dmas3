package modules.agents;

import madkit.kernel.Agent;

public class SatelliteAgent extends Agent {

    @Override
    protected void live() {
        getLogger().info("\t Hello World! \n"); // This has several advantages over using System.out.println().
        // There is a tutorial about the logging mechanism of MaDKit-5.
        for (int i = 10; i > 0; i--) {
            getLogger().info("Living... I will quit in " + i + " seconds");
            pause(1000); // pauses the agent's thread for 1000 ms
        }
    }
}
