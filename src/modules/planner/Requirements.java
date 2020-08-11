package modules.planner;

public class Requirements {
    /**
     * Requirement class stores the measurement requirements for each task
     */
    private double spatialResReq;
    private double swathReq;
    private double lossReq;
    private double temporalResolution;
    private int numLooks;
    private double tempResReqLooks;
    private double urgencyFactor;

    // Constructors
    public Requirements(){
        spatialResReq = -1.0;
        swathReq = -1.0;
        lossReq = -1.0;
        numLooks = -1;
        tempResReqLooks = -1.0;
        urgencyFactor = -1.0;
    }
    public Requirements(double spatialResReq, double swathReq, double lossReq, int numLooks, double tempResReqLooks, double urgencyFactor){
        this.spatialResReq = spatialResReq;
        this.swathReq = swathReq;
        this.lossReq = lossReq;
        this.numLooks = numLooks;
        this.tempResReqLooks = tempResReqLooks;
    }

    // Copy constructor
    public Requirements copy(){
        return new Requirements(this.spatialResReq, this.swathReq, this.lossReq, this.numLooks, this.tempResReqLooks, this.urgencyFactor);
    }

    // Getters and setters
    public double getSpatialResReq() { return spatialResReq; }
    public void setSpatialResReq(double spatialResReq) { this.spatialResReq = spatialResReq; }
    public double getSwathReq() { return swathReq; }
    public void setSwathReq(double swathReq) { this.swathReq = swathReq; }
    public double getLossReq() { return lossReq; }
    public void setLossReq(double lossReq) { this.lossReq = lossReq; }
    public double getTemporalResolution() { return temporalResolution; }
    public void setTemporalResolution(double temporalResolution) { this.temporalResolution = temporalResolution; }
    public int getNumLooks() { return numLooks; }
    public void setNumLooks(int numLooks) { this.numLooks = numLooks; }
    public double getTempResReqLooks() { return tempResReqLooks; }
    public void setTempResReqLooks(double tempResReqLooks) { this.tempResReqLooks = tempResReqLooks; }
    public double getUrgencyFactor(){return this.urgencyFactor;}
    public void setUrgencyFactor(double urgencyFactor){ this.urgencyFactor = urgencyFactor;}
}
