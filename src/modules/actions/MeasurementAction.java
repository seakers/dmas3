package modules.actions;

import madkit.kernel.AbstractAgent;
import modules.measurements.MeasurementRequest;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Instrument;

public class MeasurementAction extends SimulationAction{
    private final CoverageDefinition targetCovDef;
    private final TopocentricFrame target;
    private final Instrument instrument;
    private final MeasurementRequest request;
    private final String type;

    public MeasurementAction(AbstractAgent agent, Instrument instrument, MeasurementRequest request,
                             AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, startDate, endDate);
        this.targetCovDef = request.getCovDef();
        this.target = request.getLocation();
        this.instrument = instrument;
        this.request = request;
        this.type = request.getType();
    }

    public CoverageDefinition getTargetCovDef(){return targetCovDef; }
    public TopocentricFrame getTarget() { return target; }
    public Instrument getInstrument() { return instrument; }
    public MeasurementRequest getRequest() { return request; }
    public String getType(){return this.type;}
}
