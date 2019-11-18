package CCBBA.bin;

import CCBBA.CCBBASimulation;
import madkit.kernel.AbstractAgent;

import java.awt.*;
import java.util.Vector;

public class AbstractSimulatedAgent extends AbstractAgent {
    /**
     * Agent's Scenario
     */
    protected Scenario environment;

    /**
     * Properties
     */
    protected Dimension location = new Dimension();                 // current location
    protected double speed;                                         // displacement speed of agent
    protected Vector<String> sensors = new Vector<>();              // list of all sensors
    protected Vector<Subtask> J = new Vector<>();                   // list of all subtasks
    protected double miu;                                           // Travel cost
    protected int M;                                                // planning horizon
    protected int O_kq;                                             // max iterations in constraint violations
    protected int W_solo_max;                                       // max permissions to bid solo
    protected int W_any_max;                                        // max permissions to bid on any
    protected IterationLists localResults;                          // list of iteration results
    protected int zeta = 0;                                         // iteration counter
    protected double C_merge;                                       // Merging cost
    protected double C_split;                                       // Splitting cost
    protected double resources;                                     // Initial resources for agent
    protected double resourcesRemaining;                            // Current resources for agent
    protected double t_0; //    private long t_0;                   // start time
    protected Vector<IterationResults> receivedResults;             // list of received results
    protected Vector<Subtask> bundle;                               // subtask bundle
    protected Vector<Subtask> path;                                 // subtask path

    /**
     * Activator - constructor
     */
    protected void activate() {
        getLogger().info("Activating agent");

        // Request Role
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_THINK1);

        this.location = new Dimension();                        // current location
        this.speed = 1;                                         // displacement speed of agent
        this.sensors = new Vector<>();                          // list of all sensors
        this.J = new Vector<>();                                // list of all subtasks
        this.miu = 0;                                           // Travel cost
        this.M = 1;                                             // planning horizon
        this.O_kq = 2;                                          // max iterations in constraint violations
        this.W_solo_max = 5;                                    // max permissions to bid solo
        this.W_any_max = 10;                                    // max permissions to bid on any
        this.localResults = new IterationLists(this.J,          // list of iteration results
                this.W_solo_max, this.W_any_max, this.M,
                this.C_merge, this.C_split, this.resources,
                this);
        this.zeta = 0;                                          // iteration counter
        this.C_merge = 0.0;                                     // Merging cost
        this.C_split = 0.0;                                     // Splitting cost
        this.resources = 0.0;                                   // Initial resources for agent
        this.resourcesRemaining = 0.0;                          // Current resources for agent
        this.t_0 = 0.0; //    private long t_0;                 // start time
        this.receivedResults = new Vector<>();                  // list of received results.
        this.bundle = new Vector<>();                           // subtask bundle
    }


    /**
     * Getters and Setters
     */
    public Scenario getEnvironment() { return environment;}
    public Dimension getLocation() { return location; }
    public double getSpeed() { return speed; }
    public Vector<String> getSensors() { return sensors; }
    public Vector<Subtask> getJ() { return J; }
    public double getMiu() { return miu; }
    public int getM() { return M; }
    public int getO_kq() { return O_kq; }
    public int getW_solo_max() { return W_solo_max; }
    public int getW_any_max() { return W_any_max; }
    public IterationLists getLocalResults() { return localResults; }
    public int getZeta() { return zeta; }
    public double getC_merge() { return C_merge; }
    public double getC_split() { return C_split; }
    public double getResources() { return resources; }
    public double getResourcesRemaining() { return resourcesRemaining; }
    public double getT_0() { return t_0; }
    public Vector<Subtask> getBundle(){return this.bundle;}
    public Vector<Subtask> getPath(){return this.path;}

    public void setEnvironment(Scenario environment) { this.environment = environment; }
    public void setLocation(Dimension location) { this.location = location; }
    public void setSpeed(double speed) { this.speed = speed; }
    public void setSensors(Vector<String> sensors) { this.sensors = sensors; }
    public void setJ(Vector<Subtask> j) { J = j; }
    public void setMiu(double miu) { this.miu = miu; }
    public void setM(int m) { this.M = m; }
    public void setO_kq(int o_kq) { this.O_kq = o_kq; }
    public void setW_solo_max(int w_solo_max) { this.W_solo_max = w_solo_max; }
    public void setW_any_max(int w_any_max) { W_any_max = w_any_max; }
    public void setLocalResults(IterationLists localResults) { this.localResults = localResults; }
    public void setZeta(int zeta) { this.zeta = zeta; }
    public void setC_merge(double c_merge) { this.C_merge = c_merge; }
    public void setC_split(double c_split) { this.C_split = c_split; }
    public void setResources(double resources) { this.resources = resources; }
    public void setResourcesRemaining(double resourcesRemaining) { this.resourcesRemaining = resourcesRemaining; }
    public void setT_0(double t_0) { this.t_0 = t_0; }
    public void setReceivedResults(Vector<IterationResults> receivedResults) { this.receivedResults = receivedResults; }

}
