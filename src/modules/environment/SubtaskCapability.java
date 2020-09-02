package modules.environment;

import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class SubtaskCapability {
    private Subtask parentSubtask;
    private ArrayList<Instrument> instrumentsUsed;
    private Measurement measurement;
    private Requirements requirements;
    private double spatialRes;
    private double SNR;
    private double duration;

    public SubtaskCapability(){
        instrumentsUsed = null;
        measurement = null;
        requirements = null;
        spatialRes = -1.0;
        SNR = -Double.NEGATIVE_INFINITY;
        duration = -1.0;
    }

    public SubtaskCapability(Subtask subtask, ArrayList<Instrument> instrumentsUsed, Measurement measurement, Requirements requirements, double spatialRes, double SNR, double duration) {
        this.parentSubtask = subtask;
        this.instrumentsUsed = new ArrayList<>(); this.instrumentsUsed.addAll(instrumentsUsed);
        this.measurement = measurement;
        this.requirements = requirements;
        this.spatialRes = spatialRes;
        this.SNR = SNR;
        this.duration = duration;
    }

    public Subtask getParentSubtask() {
        return parentSubtask;
    }

    public ArrayList<Instrument> getInstrumentsUsed() {
        return instrumentsUsed;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public Requirements getRequirements() {
        return requirements;
    }

    public double getSpatialRes() {
        return spatialRes;
    }

    public double getSNR() {
        return SNR;
    }

    public double getDuration() {
        return duration;
    }
}
