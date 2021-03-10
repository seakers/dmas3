package modules.orbitData;

import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;

public class GPAccess {
    private final Satellite sat;
    private final TopocentricFrame target;
    private final Instrument instrument;
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    public GPAccess(Satellite sat, TopocentricFrame target, Instrument instrument, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.sat = sat;
        this.target = target;
        this.instrument = instrument;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Satellite getSat() { return sat; }
    public TopocentricFrame getTarget() { return target; }
    public Instrument getInstrument(){return instrument;}
    public AbsoluteDate getStartDate() { return startDate; }
    public AbsoluteDate getEndDate() { return endDate; }
}
