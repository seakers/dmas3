package modules.measurements;

import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoveragePoint;

import java.util.HashMap;

public class MeasurementRequest {
    private final int id;
    private final CoveragePoint location;
    private final AbsoluteDate announceDate;
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;
    private final String type;
    private final HashMap<String, Requirement> requirements;

    public MeasurementRequest(int id, CoveragePoint location, AbsoluteDate announceDate, AbsoluteDate startDate, AbsoluteDate endDate, String type, HashMap<String, Requirement> requirements){
        this.id = id;
        this.location = location;
        this.announceDate = announceDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.requirements = new HashMap<>(requirements);
    }

    public int getId() { return id; }
    public CoveragePoint getLocation() { return location; }
    public AbsoluteDate getAnnounceDate() { return announceDate; }
    public AbsoluteDate getStartDate() { return startDate; }
    public AbsoluteDate getEndDate() { return endDate; }
    public String getType() { return type; }
    public HashMap<String, Requirement> getRequirements() { return requirements; }
}
