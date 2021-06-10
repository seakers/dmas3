package modules.components.instruments;

import seakers.orekit.object.Instrument;
import seakers.orekit.object.fieldofview.FieldOfViewDefinition;

public abstract class SimulationInstrument extends Instrument {
    /**
     * Type of measurements that the instrument nominally senses
     */
    protected final String nominalMeasurementType;

    /**
     * instrument peak power [W]
     */
    protected final double power;

    /**
     * instrument average power [W]
     */
    protected final double avgPower;

    /**
     * Instrument mass [kg]
     */
    protected final double mass;

    protected boolean status;

    protected boolean fail;

    public SimulationInstrument(String name, String nominalMeasurementType, FieldOfViewDefinition fov, double mass, double peakPower, double averagePower) {
        super(name, fov, mass, averagePower);
        this.mass = mass;
        this.power = peakPower;
        this.avgPower = averagePower;
        this.nominalMeasurementType = nominalMeasurementType;
        this.status = false;
        this.fail = false;
    }

    public String getNominalMeasurementType(){return this.nominalMeasurementType;}
    public double getMass(){return mass;}
    public double getPeakPower(){return power;}
    public double getAvgPower(){return avgPower;}

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }
}
