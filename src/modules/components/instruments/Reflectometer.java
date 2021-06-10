package modules.components.instruments;

import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.FieldOfViewDefinition;

public class Reflectometer extends Instrument {
    public Reflectometer(String name, FieldOfViewDefinition fov, double mass, double averagePower) {
        super(name, fov, mass, averagePower);
    }
}
