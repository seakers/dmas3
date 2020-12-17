package modules.spacecraft.instrument;

import modules.spacecraft.instrument.measurements.Measurement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.utils.PVCoordinates;

public abstract class Instrument {
    // properties
    protected String name;            // Instrument name
    protected double dataRate;        // Data rate [Mbps]
    protected double pPeak;           // Peak Power [W]
    protected double pAvg;            // Average Power [W]
    protected double pulseWidth;      // pulse width [s]
    protected Measurement freq;       // Sensed frequency [Hz]
    protected double bandwidth;       // Sensed bandwidth [Hz]
    protected double n;               // Look angle off-nadir [deg]
    protected double mass;            // Instrument Mass [kg]
    protected String scanningType;    // Instrument Scanning Capability Type
    protected double scanAnglePlus;   // Scanning angle [deg]
    protected double scanAngleMinus;  // Scanning angle [deg]
    protected String type;            // Type of Instrument
    protected InstrumentAntenna ant;  // Antenna used for this instrument

    // capabilities
    protected double dtheta;          // angular resolution [deg]
    protected double sigma_dB = -30;  // back scatter cross-section [dB]

    public Instrument(String name, double dataRate, double pPeak, double pulseWidth, Measurement freq, double bandwidth, double n, double mass, String scanningType, double scanAnglePlus, double scanAngleMinus, String type, InstrumentAntenna ant) throws Exception {
        this.name = name;
        this.dataRate = dataRate;
        this.pPeak = pPeak;
        this.pAvg = pPeak;
        this.pulseWidth = pulseWidth;
        this.freq = freq;
        this.bandwidth = bandwidth;
        this.n = n;
        this.mass = mass;
        this.scanningType = scanningType;
        this.scanAnglePlus = scanAnglePlus;
        this.scanAngleMinus = scanAngleMinus;
        this.type = type;
        this.ant = ant;

        switch(ant.getType()){
            case "circular":
                this.dtheta = Math.asin( 1.22 * freq.getLambda() / ant.getDimEl() ) * 180/Math.PI;
                break;
            case "square":
                this.dtheta = 3e8/(ant.getDimAz()*freq.getF()) * 180/Math.PI;
            default:
                throw new Exception("Antenna geometry not yet supported");
        }

    }

    public abstract Instrument copy() throws Exception;
    public abstract double getSNR(Measurement measurement, PVCoordinates satPV, PVCoordinates targetPV) throws Exception;
    public abstract double getSpatialResAZ(Vector3D scPosition, Vector3D targetPosition);
    public abstract double getSpatialResEL(Vector3D scPosition, Vector3D targetPosition);

    protected double calcRangeRes(Vector3D scPosition, Vector3D targetPosition){
        double th = calcOffNadirAngle(scPosition, targetPosition);
        return 3e8/(2 * this.bandwidth * Math.sin(th));
    }
    protected double calcOffNadirAngle(Vector3D scPosition, Vector3D targetPosition){
        Vector3D targetRel = targetPosition.subtract(scPosition);
        Vector3D nadir = scPosition.scalarMultiply(-1).normalize();
        return Math.acos( targetRel.dotProduct(nadir) / (targetRel.getNorm() * nadir.getNorm()) );
    }
    protected double lin2dB(double x){
        return 10.0*Math.log10(x);
    }
    protected double dB2lin(double x){
        return Math.pow(10, x/10.0);
    }
    protected double estimateNoiseTemp(Measurement measurement){
        double f = measurement.getF();
        if( f < 2e9){
            return 150;
        }
        else if( f < 12e9){
            return  25;
        }
        else return 100;
    }

    // Getters
    public String getName() {
        return name;
    }

    public double getDataRate() {
        return dataRate;
    }

    public double getPulseWidth() {
        return pulseWidth;
    }

    public double getpPeak() {
        return pPeak;
    }

    public Measurement getFreq() {
        return freq;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    public double getFOV() {
        return dtheta;
    }

    public double getLookAngle() {
        return n;
    }

    public double getMass() {
        return mass;
    }

    public String getScanningType() {
        return scanningType;
    }

    public double getScanAnglePlus() {
        return scanAnglePlus;
    }

    public double getScanAngleMinus() {
        return scanAngleMinus;
    }

    public String getType() {
        return type;
    }

    public InstrumentAntenna getAnt() {
        return ant;
    }

    public String toString(){return this.name; }

    public String getFovType(){return this.ant.getType();}

    public double getPavg(){return pAvg;}
}