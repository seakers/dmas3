package modules.instruments;

import modules.antennas.AbstractAntenna;
import org.orekit.gnss.antenna.Antenna;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.FieldOfViewDefinition;

public class SAR extends SimulationInstrument {
    private final  double freq;
    private final double peakPower;
    private final double avgPower;
    private final double dutyCycle;
    private final double pulseWidth;
    private final double prf;
    private final double bandwidth;
    private final double nLooks;
    private final double dataRate;
    private final String nominalOps;
    private final AbstractAntenna antenna;

    public SAR(String name, String nominalMeasurementType, FieldOfViewDefinition fov, double mass, double averagePower,
               double freq, double peakPower, double dutyCycle, double pulseWidth, double prf,
               double bandwidth, double nLooks, double dataRate, String nominalOps, AbstractAntenna antenna) {
        super(name, nominalMeasurementType, fov, mass, averagePower);

        this.freq = freq;
        this.peakPower = peakPower;
        this.avgPower = averagePower;
        this.dutyCycle = dutyCycle;
        this.pulseWidth = pulseWidth;
        this.prf = prf;
        this.bandwidth= bandwidth;
        this.nLooks = nLooks;
        this.dataRate = dataRate;
        this.nominalOps = nominalOps;
        this.antenna = antenna;
    }

    public double getFreq() { return freq; }
    public double getPeakPower() { return peakPower; }
    public double getAvgPower() { return avgPower; }
    public double getDutyCycle() { return dutyCycle; }
    public double getPulseWidth() { return pulseWidth; }
    public double getPrf() { return prf; }
    public double getBandwidth(){ return bandwidth; }
    public double getnLooks() { return nLooks; }
    public double getDataRate() { return dataRate; }
    public String getNominalOps() { return nominalOps; }
    public AbstractAntenna getAntenna() { return antenna; }
}
