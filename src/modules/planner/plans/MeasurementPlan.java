package modules.planner.plans;

import modules.spacecraft.instrument.measurements.Measurement;
import modules.environment.Subtask;
import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class MeasurementPlan extends Plan{
    private Measurement measurement;
    private Subtask relevantSubtask;

    public MeasurementPlan(AbsoluteDate startDate, AbsoluteDate endDate, ArrayList<Instrument> instruments, Subtask subtask) {
        super(startDate, endDate, new ArrayList<>(), instruments);
        this.measurement = subtask.getMainMeasurement();
        this.relevantSubtask = subtask;
    }

    @Override
    public Plan copy() {
        return new MeasurementPlan(this.startDate, this.endDate, this.instruments, this.relevantSubtask);
    }

    public Subtask getRelevantSubtask() {
        return relevantSubtask;
    }
    public Measurement getMeasurement(){return this.measurement;}
}
