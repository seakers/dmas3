package modules.planner.plans;

import madkit.kernel.Message;
import modules.spacecraft.component.Component;
import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public abstract class Plan {
    protected AbsoluteDate startDate;               // start date of planned task
    protected AbsoluteDate endDate;                 // end date of planned task
    protected ArrayList<Component> components;      // list of active components during task
    protected ArrayList<Instrument> instruments;    // list of active instrument during task
    protected Message broadcastMessage;             // message to be broadcast my agent

    public abstract Plan copy();
    public Message getBroadcastMessage(){return this.broadcastMessage;}
}
