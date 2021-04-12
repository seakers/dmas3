package modules.orbitData;

import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;

public class GPAccess {
    private final Satellite sat;
    private final CoverageDefinition targetCovDef;
    private final TopocentricFrame target;
    private final Instrument instrument;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;

    public GPAccess(Satellite sat, CoverageDefinition targetCovDef, TopocentricFrame target, Instrument instrument, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.sat = sat;
        this.targetCovDef = targetCovDef;
        this.target = target;
        this.instrument = instrument;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Satellite getSat() { return sat; }
    public CoverageDefinition getTargetCovDef(){return targetCovDef;}
    public TopocentricFrame getTarget() { return target; }
    public Instrument getInstrument(){return instrument;}
    public AbsoluteDate getStartDate() { return startDate; }
    public AbsoluteDate getEndDate() { return endDate; }
    public void setStartDate(AbsoluteDate startDate) { this.startDate = startDate; }
    public void setEndDate(AbsoluteDate endDate) { this.endDate = endDate; }
}
