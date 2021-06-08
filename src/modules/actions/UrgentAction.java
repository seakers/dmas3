package modules.actions;

import madkit.kernel.AbstractAgent;
import modules.measurements.MeasurementRequest;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Instrument;

public class UrgentAction extends  MeasurementAction{
    protected UrgentAction(AbstractAgent agent, MeasurementRequest request,
                           Instrument instrument,
                           AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, instrument, request, startDate, endDate);
    }
}
