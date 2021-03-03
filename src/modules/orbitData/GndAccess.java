package modules.orbitData;

import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;

public class GndAccess {
    private final Satellite sat;
    private final GndStation gnd;
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    public GndAccess(Satellite sat, GndStation gnd, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.sat = sat;
        this.gnd = gnd;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Satellite getSat() { return sat; }
    public GndStation getGnd() { return gnd; }
    public AbsoluteDate getStartDate() { return startDate; }
    public AbsoluteDate getEndDate() { return endDate; }
}
