package modules.actions;

import madkit.kernel.AbstractAgent;
import modules.measurements.MeasurementRequest;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Instrument;

public class NominalAction extends MeasurementAction{
    protected NominalAction(AbstractAgent agent, CoverageDefinition targetCovDef,
                            TopocentricFrame target, Instrument instrument,
                            String measurementType, AbsoluteDate startDate,
                            AbsoluteDate endDate) {
        super(agent, instrument, null, startDate, endDate);
    }
}
