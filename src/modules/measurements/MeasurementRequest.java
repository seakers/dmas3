package modules.measurements;

import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;

public class MeasurementRequest {
    private final TopocentricFrame location;
    private final AbsoluteDate announceDate;
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;
    private final String type;
    private final HashMap<String, Requirement> requirements;

    public MeasurementRequest(TopocentricFrame location, AbsoluteDate announceDate, AbsoluteDate startDate, AbsoluteDate endDate, String type, HashMap<String, Requirement> requirements){

        this.location = location;
        this.announceDate = announceDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.requirements = new HashMap<>(requirements);
    }
}
