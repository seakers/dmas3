package modules.instruments;

import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.FieldOfViewDefinition;

public abstract class SimulationInstrument extends Instrument {
    private final String nominalMeasurementType;

    public SimulationInstrument(String name, String nominalMeasurementType, FieldOfViewDefinition fov, double mass, double averagePower) {
        super(name, fov, mass, averagePower);
        this.nominalMeasurementType = nominalMeasurementType;
    }

    public String getNominalMeasurementType(){return this.nominalMeasurementType;}
}
