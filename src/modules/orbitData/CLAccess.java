package modules.orbitData;

import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;

public class CLAccess{
    private final Satellite sat;
    private final Satellite target;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;

    public CLAccess(Satellite sat, Satellite target, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.sat = sat;
        this.target = target;
        this.startDate = startDate;
        this.endDate = endDate;
}

    public Satellite getSat() { return sat; }
    public Satellite getTarget() { return target; }
    public AbsoluteDate getStartDate() { return startDate; }
    public AbsoluteDate getEndDate() { return endDate; }
    public void setStartDate(AbsoluteDate startDate) { this.startDate = startDate; }
    public void setEndDate(AbsoluteDate endDate) { this.endDate = endDate; }
}
