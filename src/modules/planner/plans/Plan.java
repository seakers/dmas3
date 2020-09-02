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

    public Plan(AbsoluteDate startDate, AbsoluteDate endDate, ArrayList<Component> components, ArrayList<Instrument> instruments) {
        this.startDate = startDate.getDate();
        this.endDate = endDate.getDate();
        this.components = new ArrayList<>(); this.components.addAll(components);
        this.instruments = new ArrayList<>(); this.instruments.addAll(instruments);
    }

    public abstract Plan copy();

    public AbsoluteDate getStartDate() {
        return startDate;
    }

    public AbsoluteDate getEndDate() {
        return endDate;
    }

    public ArrayList<Component> getComponents() {
        return components;
    }

    public ArrayList<Instrument> getInstruments() {
        return instruments;
    }
}
