package modules.actions;

import madkit.kernel.AbstractAgent;
import modules.measurements.MeasurementRequest;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Instrument;

public class MeasurementAction extends SimulationAction{
    private final TopocentricFrame target;
    private final Instrument instrument;
    private final MeasurementRequest request;

    public MeasurementAction(AbstractAgent agent, TopocentricFrame target, Instrument instrument, AbsoluteDate startDate, AbsoluteDate endDate, MeasurementRequest request) {
        super(agent, startDate, endDate);
        this.target = target;
        this.instrument = instrument;
        this.request = request;
    }

    public TopocentricFrame getTarget() { return target; }
    public Instrument getInstrument() { return instrument; }
    public MeasurementRequest getRequest() { return request; }
}
