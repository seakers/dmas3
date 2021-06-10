package modules.components.antennas;

import modules.components.AbstractSubsystem;

import java.util.ArrayList;

abstract public class AbstractAntenna extends AbstractSubsystem {
    public static final String PARAB = "parabolic";

    protected final String type;
    protected double frequency;

    protected AbstractAntenna(double power, double mass, double x_dim, double y_dim, double z_dim, String type, double frequency) {
        super(type, power, mass, x_dim, y_dim, z_dim);
        this.type = type;
        this.frequency = frequency;
    }

    public String getType() { return type; }
    abstract public double getGain();
}
