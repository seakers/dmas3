package modules.antennas;

import java.util.ArrayList;

abstract public class AbstractAntenna {
    public static final String PARAB = "parabolic";

    protected final String type;
    protected ArrayList<Double> dimensions;

    protected AbstractAntenna(String type) {
        this.type = type;
        this.dimensions = new ArrayList<>();
    }

    public String getType() { return type; }
    public ArrayList<Double> getDimensions() { return dimensions; }
}
