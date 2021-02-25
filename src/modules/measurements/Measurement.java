package modules.measurements;

import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.util.HashMap;

public class Measurement {
    private final AbstractAgent measuringAgent;
    private final MeasurementRequest request;
    private final HashMap<Requirement, RequirementPerformance> performance;
    private final AbsoluteDate measurementDate;
    private final double utility;

    public Measurement(AbstractAgent measuringAgent, MeasurementRequest request, HashMap<Requirement, RequirementPerformance> performance, AbsoluteDate measurementDate, double utility) {
        this.measuringAgent = measuringAgent;
        this.request = request;
        this.performance = performance;
        this.measurementDate = measurementDate;
        this.utility = utility;
    }

    /**
     * Getters
     */
    public AbstractAgent getMeasuringAgent() { return measuringAgent; }
    public MeasurementRequest getRequest() { return request; }
    public HashMap<Requirement, RequirementPerformance> getPerformance() { return performance; }
    public AbsoluteDate getMeasurementDate() { return measurementDate; }
    public double getUtility() { return utility; }
}
