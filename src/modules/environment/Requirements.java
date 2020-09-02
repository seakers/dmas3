package modules.environment;

import org.orekit.time.AbsoluteDate;

public class Requirements {
    /**
     * Requirement class stores the measurement requirements for each task
     */
    private double spatialResReq;
    private double swathReq;
    private double lossReq;
    private double temporalResolutionMin;
    private double temporalResolutionMax;
    private int numLooks;
    private double urgencyFactor;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;

    // Constructors
    public Requirements(){
        spatialResReq = -1.0;
//        swathReq = -1.0;
        lossReq = -1.0;
        numLooks = -1;
        temporalResolutionMin = -1.0;
        temporalResolutionMax = -1.0;
        urgencyFactor = -1.0;
        startDate = new AbsoluteDate();
        endDate = new AbsoluteDate();
    }
    public Requirements(double spatialResReq, double lossReq, int numLooks, double temporalResolutionMin, double temporalResolutionMax, double urgencyFactor, AbsoluteDate startDate, AbsoluteDate endDate){
        this.spatialResReq = spatialResReq;
        this.lossReq = lossReq;
        this.numLooks = numLooks;
        this.temporalResolutionMin = temporalResolutionMin;
        this.temporalResolutionMax = temporalResolutionMax;
        this.urgencyFactor = urgencyFactor;
        this.startDate = startDate.getDate();
        this.endDate = endDate.getDate();
    }

    // Copy constructor
    public Requirements copy(){
        return new Requirements(spatialResReq, lossReq, numLooks, temporalResolutionMin, temporalResolutionMax, urgencyFactor, startDate, endDate);
    }

    // Getters and setters
    public double getSpatialResReq() { return spatialResReq; }
    public void setSpatialResReq(double spatialResReq) { this.spatialResReq = spatialResReq; }
    public double getSwathReq() { return swathReq; }
    public void setSwathReq(double swathReq) { this.swathReq = swathReq; }
    public double getLossReq() { return lossReq; }
    public void setLossReq(double lossReq) { this.lossReq = lossReq; }
    public double getTemporalResolutionMin() { return temporalResolutionMin; }
    public double getTemporalResolutionMax() { return temporalResolutionMax; }
    public int getNumLooks() { return numLooks; }
    public void setNumLooks(int numLooks) { this.numLooks = numLooks; }
    public double getUrgencyFactor(){return this.urgencyFactor;}
    public void setUrgencyFactor(double urgencyFactor){ this.urgencyFactor = urgencyFactor;}
    public AbsoluteDate getStartDate(){return this.startDate;}
    public AbsoluteDate getEndDate(){return this.endDate;}
}
