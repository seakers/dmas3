package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;

public class CommsLoop {
    private final Satellite sender;
    private final Satellite receiver;
    private final ArrayList<Satellite> path;
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;
    private final ArrayList<ArrayList<AbsoluteDate>> accessDates;


    public CommsLoop(Satellite sender, Satellite receiver, ArrayList<Satellite> path, AbsoluteDate startDate, AbsoluteDate endDate, ArrayList<ArrayList<AbsoluteDate>> accessDates) {
        this.sender = sender;
        this.receiver = receiver;
        this.path = path;
        this.startDate = startDate;
        this.endDate = endDate;
        this.accessDates = accessDates;
    }

    public Satellite getSender() { return sender; }
    public Satellite getReceiver() { return receiver; }
    public ArrayList<Satellite> getPath() { return path; }
    public AbsoluteDate getStartDate() { return startDate; }
    public AbsoluteDate getEndDate() { return endDate; }
    public ArrayList<ArrayList<AbsoluteDate>> getAccessDates() { return accessDates; }
}
