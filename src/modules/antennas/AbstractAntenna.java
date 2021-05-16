package modules.antennas;

import java.util.ArrayList;

abstract public class AbstractAntenna {
    public static final String PARAB = "parabolic";

    protected final String type;
    protected ArrayList<Double> dimensions;
    protected double frequency;

    protected AbstractAntenna(String type, double frequency) {
        this.type = type;
        this.dimensions = new ArrayList<>();
        this.frequency = frequency;
    }

    public String getType() { return type; }
    public ArrayList<Double> getDimensions() { return dimensions; }
    abstract public double getGain();
}
