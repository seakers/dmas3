package modules.messages;

import madkit.kernel.Message;
import modules.measurements.Measurement;

public class MeasurementMessage extends Message {
    private final Measurement measurement;

    public MeasurementMessage(Measurement measurement){ this.measurement = measurement; }

    public Measurement getMeasurement() { return measurement; }
}
