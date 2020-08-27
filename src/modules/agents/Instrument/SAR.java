package modules.agents.Instrument;

import modules.environment.Measurement;
import org.orekit.utils.PVCoordinates;

public class SAR extends Instrument{
    private double nLooks;

    public SAR(String name, double dataRate, double pAvg, double pPeak, Measurement freq, double bandwidth, double fov, double n, double mass, String scanningType, double scanAnglePlus, double scanAngleMinus, String type, InstrumentAntenna ant, double nLooks) {
        super(name, dataRate, pAvg, pPeak, freq, bandwidth, fov, n, mass, scanningType, scanAnglePlus, scanAngleMinus, type, ant);
        this.nLooks = nLooks;
    }

    public SAR copy(){
        return new SAR(this.name, this.dataRate, this.pAvg, this.pPeak, this.freq, this.bandwidth, this.fov, this.n, this.mass, this.scanningType, this.scanAnglePlus, this.scanAngleMinus, this.type, this.ant, this.nLooks);
    }
    public double getSNR(PVCoordinates scPosition, PVCoordinates targetPosition){return -1.0;}
    public double getSpatialRes(PVCoordinates scPosition, PVCoordinates targetPosition){return -1.0;}
}
