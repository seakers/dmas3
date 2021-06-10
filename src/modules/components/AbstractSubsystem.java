package modules.components;

import java.util.ArrayList;

/**
 * Describes a spacecraft subsystem that can be turned actuated by the satellite and its planner
 * @author a.aguilar
 */
public abstract class AbstractSubsystem {
    /**
     * Subsystem name
     */
    protected final String name;

    /**
     * Peak power consumed when turned on [W]
     */
    protected final double power;

    /**
     * Component mass [kg]
     */
    protected final double mass;

    /**
     * subsystem's dimensions [m]
     */
    protected final ArrayList<Double> dims;

    /**
     * Subsystem's on or off status;
     */
    protected boolean status;

    /**
     * True if subsystem fails during the mission
     */
    protected boolean failed;

    public enum type{
        ADCS, COMMS, EPS, PROP, STR, THRM, PAYLOAD
    }

    public AbstractSubsystem(String name, double power, double mass, double x_dim, double y_dim, double z_dim){
        this.name = name;
        this.power = power;
        this.mass = mass;
        this.dims = new ArrayList<>(); this.dims.add(x_dim); this.dims.add(y_dim); this.dims.add(z_dim);
        this.status = false;
        this.failed = false;
    }

    /**
     * Getters for class properties
     */
    public String getName() { return name; }
    public double getPower() { return power; }
    public double getMass() { return mass; }
    public ArrayList<Double> getDims() { return dims; }
    public boolean isStatus() { return status; }
    public boolean isFailed() { return failed; }

    public void setStatus(boolean status){ this.status = status; }
    public void setFailure(boolean failed){ this.failed = failed; }
}
