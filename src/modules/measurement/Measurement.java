package modules.measurement;

import madkit.kernel.AbstractAgent;
import org.orekit.time.AbsoluteDate;

import java.util.HashMap;

public class Measurement {
    private final AbstractAgent measuringAgent;
    private final MeasurementRequest request;
    private final HashMap<Requirement, RequirementPerformance> performance;
    private final AbsoluteDate measurementDate;
    private AbsoluteDate downloadDate;
    private final double utility;

    public Measurement(AbstractAgent measuringAgent, MeasurementRequest request, HashMap<Requirement, RequirementPerformance> performance, AbsoluteDate measurementDate, double utility) {
        this.measuringAgent = measuringAgent;
        this.request = request;
        this.performance = performance;
        this.measurementDate = measurementDate;
        this.downloadDate = null;
        this.utility = utility;
    }

    public void setDownloadDate(AbsoluteDate downloadDate){
        this.downloadDate = downloadDate.getDate();
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
