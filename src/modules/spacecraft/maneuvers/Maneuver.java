package modules.spacecraft.maneuvers;

import org.orekit.time.AbsoluteDate;

public abstract class Maneuver {
    protected AbsoluteDate startDate;
    protected AbsoluteDate endDate;

    public Maneuver(AbsoluteDate startDate, AbsoluteDate endDate){
        this.startDate = startDate.getDate();
        this.endDate = endDate.getDate();
    }
    public AbsoluteDate getStartDate(){return startDate;}
    public AbsoluteDate getEndDate(){return endDate;}
}
