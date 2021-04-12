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

    public MeasurementAction(AbstractAgent agent, CoverageDefinition targetCovDef,
                             TopocentricFrame target, Instrument instrument, String type,
                             AbsoluteDate startDate, AbsoluteDate endDate, MeasurementRequest request) {
        super(agent, startDate, endDate);
        this.targetCovDef = targetCovDef;
        this.target = target;
        this.instrument = instrument;
        this.request = request;
        this.type = type;
    }

    public CoverageDefinition getTargetCovDef(){return targetCovDef; }
    public TopocentricFrame getTarget() { return target; }
    public Instrument getInstrument() { return instrument; }
    public MeasurementRequest getRequest() { return request; }
    public String getType(){return this.type;}
}
