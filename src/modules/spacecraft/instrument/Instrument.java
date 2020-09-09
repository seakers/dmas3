package modules.spacecraft.instrument;

import modules.environment.Measurement;
import org.orekit.utils.PVCoordinates;

public abstract class Instrument {
    protected String name;            // Instrument name
    protected double dataRate;        // Data rate [Mbps]
    protected double pAvg;            // Average Power [W]
    protected double pPeak;           // Peak Power [W]
    protected Measurement freq;       // Sensed frequency [Hz]
    protected double bandwidth;       // Sensed bandwidth [Hz]
    protected double fov;             // Field of view [deg]
    protected String fovType;         // Shape of FOV
    protected double n;               // Look angle off-nadir [deg]
    protected double mass;            // Instrument Mass [kg]
    protected String scanningType;    // Instrument Scanning Capability Type
    protected double scanAnglePlus;   // Scanning angle [deg]
    protected double scanAngleMinus;  // Scanning angle [deg]
    protected String type;            // Type of Instrument
    protected InstrumentAntenna ant;  // Antenna used for this instrument

    public Instrument(String name, double dataRate, double pAvg, double pPeak, Measurement freq, double bandwidth, double fov, String fovType, double n, double mass, String scanningType, double scanAnglePlus, double scanAngleMinus, String type, InstrumentAntenna ant) {
        this.name = name;
        this.dataRate = dataRate;
        this.pAvg = pAvg;
        this.pPeak = pPeak;
        this.freq = freq;
        this.bandwidth = bandwidth;
        this.fov = fov;
        this.fovType = fovType;
        this.n = n;
        this.mass = mass;
        this.scanningType = scanningType;
        this.scanAnglePlus = scanAnglePlus;
        this.scanAngleMinus = scanAngleMinus;
        this.type = type;
        this.ant = ant;
    }

    public abstract Instrument copy();
    public abstract double getSNR(PVCoordinates scPosition, PVCoordinates targetPosition);
    public abstract double getSpatialRes(PVCoordinates scPosition, PVCoordinates targetPosition);

    // Getters
    public String getName() {
        return name;
    }

    public double getDataRate() {
        return dataRate;
    }

    public double getpAvg() {
        return pAvg;
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
        return fov;
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

    public String getFovType(){return this.fovType;}
}