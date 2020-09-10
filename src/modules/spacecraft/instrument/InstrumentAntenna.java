package modules.spacecraft.instrument;

public class InstrumentAntenna {
    private String name;        // Antenna name
    private double dimAz;       // Azimuth dimension [m]
    private double dimEl;       // Azimuth dimension [m]
    private double mass;        // Antenna mass [kg]

    public InstrumentAntenna(String name, double dimAz, double dimEl, double mass){
        this.name = name;
        this.dimAz = dimAz;
        this.dimEl = dimEl;
        this.mass  = mass;
    }

    public double getDimAz() {
        return dimAz;
    }
    public double getDimEl() {
        return dimEl;
    }
    public double getMass() {
        return mass;
    }
}
