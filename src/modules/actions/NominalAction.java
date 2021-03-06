package modules.actions;

import madkit.kernel.AbstractAgent;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Instrument;

public class NominalAction extends MeasurementAction{
    protected NominalAction(AbstractAgent agent, TopocentricFrame target, Instrument instrument, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, target, instrument, startDate, endDate, null);
    }
}
