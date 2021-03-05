package modules.actions;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import madkit.kernel.Message;
import modules.measurements.MeasurementRequest;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Satellite;

import java.util.LinkedList;

public class AnnouncementAction extends SimulationAction {
    private final Satellite target;
    private final Message announcement;

    public AnnouncementAction(AbstractAgent agent, Satellite target, Message announcement, AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, startDate, endDate);
        this.target = target;
        this.announcement = announcement;
    }

    public Satellite getTarget(){return this.target;}
    public Message getAnnouncement(){ return  this.announcement; }
}
