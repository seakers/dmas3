package modules.measurements;

import madkit.kernel.AbstractAgent;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.Instrument;

import java.util.HashMap;

public class Measurement {
    private final AbstractAgent measuringAgent;
    private final Instrument instrumentUsed;
    private final String type;
    private final TopocentricFrame target;
    private final MeasurementRequest request;
    private final HashMap<Requirement, RequirementPerformance> performance;
    private final AbsoluteDate measurementDate;
    private AbsoluteDate downloadDate;
    private final double utility;

    public Measurement(AbstractAgent measuringAgent, Instrument instrumentUsed, String type, TopocentricFrame target, MeasurementRequest request,
                       HashMap<Requirement, RequirementPerformance> performance,
                       AbsoluteDate measurementDate, double utility) {
        this.instrumentUsed = instrumentUsed;
        this.type = type;
        this.measuringAgent = measuringAgent;
        this.target = target;
        this.request = request;
        this.performance = performance;
        this.measurementDate = measurementDate;
        this.downloadDate = null;
        this.utility = utility;
    }

    public void setDownloadDate(AbsoluteDate downloadDate){
        this.downloadDate = downloadDate.getDate();
    }

    public String toString(){
        StringBuilder out = new StringBuilder();

        String agentName = measuringAgent.getName();
        String targetName = target.getName();
        String instrument =  instrumentUsed.getName();

        out.append(agentName + "," + targetName + "," + instrument + "," + type);
        if(request != null){
            out.append("," + request.getId() + "," + request.getAnnounceDate() + "," + request.getStartDate() + "," + request.getEndDate());
        }
        else{
            out.append(",nil,nil,nil,nil");
        }
        out.append("," + measurementDate );

        for(Requirement req : performance.keySet()){
            RequirementPerformance perf = performance.get(req);

            out.append("," + req.getName() + "," + req.getBreakThrough() + "," + req.getGoal() + "," + req.getThreshold() + "," + req.getUnits());
            out.append("," + perf.getValue() + "," + perf.getUnits());
        }

        return out.toString();
    }

    /**
     * Getters
     */
    public AbstractAgent getMeasuringAgent() { return measuringAgent; }
    public MeasurementRequest getRequest() { return request; }
    public HashMap<Requirement, RequirementPerformance> getPerformance() { return performance; }
    public AbsoluteDate getMeasurementDate() { return measurementDate; }
    public double getUtility() { return utility; }
    public AbsoluteDate getDownloadDate() { return downloadDate; }
}
