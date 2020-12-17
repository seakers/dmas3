package modules.spacecraft.instrument;

public class InstrumentAntenna {
    private String name;        // Antenna name
    private double dimAz;       // Azimuth dimension [m]
    private double dimEl;       // Azimuth dimension [m]
    private double mass;        // Antenna mass [kg]
    private String type;        // Aperture type
    private double eff;

    public InstrumentAntenna(String name, double dimAz, double dimEl, double mass, String type, double eff){
        this.name = name;
        this.dimAz = dimAz;
        this.dimEl = dimEl;
        this.mass  = mass;
        this.type = type;
        this.eff = eff;
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
    public String getType() { return type;}
    public double getEff(){return eff;}
}
