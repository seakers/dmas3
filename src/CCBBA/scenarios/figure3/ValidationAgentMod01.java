package CCBBA.scenarios.figure3;

import java.awt.*;
import java.util.Vector;

import CCBBA.CCBBASimulation;
import CCBBA.bin.*;

public class ValidationAgentMod01 extends AbstractSimulatedAgent {
    @Override
    protected void activate() {
        getLogger().info("Activating agent");

        // Request Role
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);

        this.location = simInitialPosition();                   // current location
        this.speed = 1;                                         // displacement speed of agent
        this.sensors = simSensorList();                         // list of all sensors
        this.M = 2;                                             // planning horizon
        this.O_kq = 2;                                          // max iterations in constraint violations
        this.O_all = 100;                                       // max number of bids before tie-breaker is implemented
        this.W_solo_max = 2;                                    // max permissions to bid solo
        this.W_any_max = 5;                                     // max permissions to bid on any
        this.localResults = new IterationLists(                 // list of iteration results
                this.W_solo_max, this.W_any_max, this.M,
                this.C_merge, this.C_split, this.resources,
                this);
        this.zeta = 0;                                          // iteration counter
        this.C_merge = 0.0;                                     // Merging cost
        this.C_split = 0.0;                                     // Splitting cost
        this.resources = 50.0;                                  // Initial resources for agent
        this.miu = this.resources * 0.57/100;                   // Travel cost
        this.resourcesRemaining = this.resources;               // Current resources for agent
        this.t_0 = 0.0; //    private long t_0;                 // start time
        this.receivedResults = new Vector<>();                  // list of received results
    }



    private Dimension simInitialPosition() {
        // Describe initial conditions for agent
        int x_max = 50;
        int y_max = 50;

        int x = (int)(x_max * Math.random());
        int y = (int)(y_max * Math.random());
        return new Dimension(x, y);
//        return new Dimension(2,0);
    }

    private Vector<String> simSensorList() {
        Vector<String> sensor_list = new Vector<>();
        sensor_list.add("IR");
        return sensor_list;
    }
}