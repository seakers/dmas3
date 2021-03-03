package modules.instrument;

import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.FieldOfViewDefinition;

public class SAR extends Instrument {
    private static double freq;
    private static double peakPower;
    private static double avgPower;
    private static double dutyCycle;
    private static double pulseWidth;
    private static double prf;
    private static double nLooks;
    private static double dataRate;
    private static String nominalOps;
    private static String antenna;

    public SAR(String name, FieldOfViewDefinition fov, double mass, double averagePower,
               double freq, double peakPower, double dutyCycle, double pulseWidth, double prf,
                double nLooks, double dataRate, String nominalOps, String antenna) {
        super(name, fov, mass, averagePower);

        this.freq = freq;
        this.peakPower = peakPower;
        this.avgPower = averagePower;
        this.dutyCycle = dutyCycle;
        this.pulseWidth = pulseWidth;
        this.prf = prf;
        this.nLooks = nLooks;
        this.dataRate = dataRate;
        this.nominalOps = nominalOps;
        this.antenna = antenna;
    }
}
