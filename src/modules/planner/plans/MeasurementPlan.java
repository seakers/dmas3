package modules.planner.plans;

import modules.planner.CCBBA.IterationDatum;
import modules.spacecraft.instrument.measurements.Measurement;
import modules.environment.Subtask;
import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class MeasurementPlan extends Plan{
    private Measurement measurement;
    private Subtask relevantSubtask;
    private IterationDatum plannerBid;

    public MeasurementPlan(AbsoluteDate startDate, AbsoluteDate endDate, ArrayList<Instrument> instruments, IterationDatum datum) {
        super(startDate, endDate, new ArrayList<>(), instruments);
        this.measurement = datum.getSubtask().getMainMeasurement();
        this.relevantSubtask = datum.getSubtask();
        this.plannerBid = datum.copy();
    }

    @Override
    public Plan copy() {
        return new MeasurementPlan(this.startDate, this.endDate, this.instruments, this.plannerBid);
    }

    public Subtask getRelevantSubtask() {
        return relevantSubtask;
    }
    public Measurement getMeasurement(){return this.measurement;}
    public IterationDatum getPlannerBid(){return this.plannerBid;}
}
