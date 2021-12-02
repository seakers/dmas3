package modules.messages;

import madkit.kernel.AgentAddress;
import modules.measurements.Measurement;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class MeasurementMessage extends DMASMessage {
    private ArrayList<Measurement> measurements;

    public MeasurementMessage(ArrayList<Measurement> measurements, AbsoluteDate sendDate, AgentAddress originalSender, AgentAddress intendedReceiver){
        super(sendDate, originalSender, intendedReceiver);
        this.measurements = measurements;
    }

    public ArrayList<Measurement> getMeasurements() { return measurements; }
    public void setMeasurements(ArrayList<Measurement> measurements){this.measurements = new ArrayList<>(measurements);}
}
