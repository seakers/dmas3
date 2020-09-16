package modules.environment;

import madkit.kernel.AbstractAgent;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.measurements.Measurement;
import modules.spacecraft.instrument.measurements.MeasurementPerformance;

import java.util.ArrayList;

public class SubtaskCapability {
    private AbstractAgent agent;
    private Subtask parentSubtask;
    private ArrayList<Instrument> instrumentsUsed;
    private Measurement measurement;
    private Requirements requirements;
    private MeasurementPerformance performance;

    public SubtaskCapability(Subtask j){
        instrumentsUsed = null;
        measurement = null;
        requirements = null;
        performance = new MeasurementPerformance(j);
    }

    public SubtaskCapability(Subtask subtask, ArrayList<Instrument> instrumentsUsed, Measurement measurement, Requirements requirements, MeasurementPerformance performance, AbstractAgent winner) {
        this.agent = winner;
        this.parentSubtask = subtask;
        this.instrumentsUsed = new ArrayList<>(); this.instrumentsUsed.addAll(instrumentsUsed);
        this.measurement = measurement;
        this.requirements = requirements;
        this.performance = performance.copy();
    }
    public AbstractAgent getWinner(){return agent;}

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

    public MeasurementPerformance getPerformance(){return performance;}
}
