package modules.actions;

import madkit.kernel.AbstractAgent;
import modules.components.AbstractSubsystem;
import org.orekit.time.AbsoluteDate;

public class SubsystemAction extends SimulationAction{

    private final boolean status;
    private final boolean failure;
    private final AbstractSubsystem.type component;
    private final String instrumentName;

    public SubsystemAction(AbstractAgent agent, AbsoluteDate startDate, AbsoluteDate endDate, boolean status, boolean failure, AbstractSubsystem.type component) {
        super(agent, startDate, endDate);
        this.status = status;
        this.failure = failure;
        this.component = component;
        this.instrumentName = "n/a";
    }

    public SubsystemAction(AbstractAgent agent, AbsoluteDate startDate, AbsoluteDate endDate, boolean status, boolean failure, String instrumentName) {
        super(agent, startDate, endDate);
        this.status = status;
        this.failure = failure;
        this.component = AbstractSubsystem.type.PAYLOAD;
        this.instrumentName = instrumentName;
    }

    public boolean getStatus() {
        return status;
    }

    public boolean getFailure() {
        return failure;
    }

    public AbstractSubsystem.type getComponent() {
        return component;
    }

    public String getInstrumentName() {
        return instrumentName;
    }
}
