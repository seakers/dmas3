package modules.messages;

import madkit.kernel.Message;
import modules.measurements.Measurement;

import java.util.ArrayList;

public class MeasurementMessage extends Message {
    private ArrayList<Measurement> measurements;

    public MeasurementMessage(ArrayList<Measurement> measurements){ this.measurements = measurements; }

    public ArrayList<Measurement> getMeasurements() { return measurements; }
    public void setMeasurements(ArrayList<Measurement> measurements){this.measurements = new ArrayList<>(measurements);}
}
