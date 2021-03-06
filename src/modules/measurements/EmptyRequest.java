package modules.measurements;

import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoveragePoint;

import java.util.HashMap;

public class EmptyRequest extends MeasurementRequest{
    public EmptyRequest(int id, CoveragePoint location, AbsoluteDate announceDate, AbsoluteDate startDate, AbsoluteDate endDate, String type, HashMap<String, Requirement> requirements, AbsoluteDate simStartDate) {
        super(id, location, announceDate, startDate, endDate, type, requirements, simStartDate);
    }
}
