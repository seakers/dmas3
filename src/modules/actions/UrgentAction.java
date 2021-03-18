package modules.actions;

import madkit.kernel.AbstractAgent;
import modules.measurements.MeasurementRequest;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Instrument;

public class UrgentAction extends  MeasurementAction{
    protected UrgentAction(AbstractAgent agent, MeasurementRequest request, TopocentricFrame target, Instrument instrument, String measurementType, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, target, instrument, measurementType, startDate, endDate, request);
    }
}
