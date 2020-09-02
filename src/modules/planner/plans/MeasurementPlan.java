package modules.planner.plans;

import modules.environment.Measurement;
import modules.environment.Subtask;
import modules.spacecraft.component.Component;
import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class MeasurementPlan extends Plan{
    private Measurement measurement;
    private Subtask relevantSubtask;

    public MeasurementPlan(AbsoluteDate startDate, AbsoluteDate endDate, ArrayList<Component> components, ArrayList<Instrument> instruments, Measurement measurement, Subtask subtask) {
        super(startDate, endDate, components, instruments);
        this.measurement = measurement;
        this.relevantSubtask = subtask;
    }

    @Override
    public Plan copy() {
        return new MeasurementPlan(this.startDate, this.endDate, this.components, this.instruments, this.measurement, this.relevantSubtask);
    }

    public Subtask getRelevantSubtask() {
        return relevantSubtask;
    }

    public Measurement getMeasurement(){return this.measurement;}
}
