package modules.planner;

public class Requirements {
    /**
     * Requirement class stores the measurement requirements for each task
     */
    private double spatialResolution;
    private double swathLength;
    private double measurementLoss;
    private double temporalResolution;

    // Constructors
    public Requirements(){
        spatialResolution = -1.0;
        swathLength = -1.0;
        measurementLoss = -1.0;
    }
    public Requirements(double spatialResolution, double swathLength, double measurementLoss){
        this.spatialResolution = spatialResolution;
        this.swathLength = swathLength;
        this.measurementLoss = measurementLoss;
    }

    // Copy constructor
    public Requirements copy(){
        return new Requirements(this.spatialResolution, this.swathLength, this.measurementLoss);
    }

    // Getters and setters
    public double getSpatialResolution() { return spatialResolution; }
    public void setSpatialResolution(double spatialResolution){ this.spatialResolution = spatialResolution; }
    public double getSwathLength() { return swathLength; }
    public void setSwathLength(double swathLength) { this.swathLength = swathLength; }
    public double getMeasurementLoss() { return measurementLoss; }
    public void setMeasurementLoss(double measurementLoss){ this.measurementLoss = measurementLoss; }
}
