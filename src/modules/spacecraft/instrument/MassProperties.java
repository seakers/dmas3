package modules.spacecraft.instrument;

import modules.spacecraft.SpacecraftDesign;

public class MassProperties {
    private double mass;        // [kg]
    private double dimX;        // [m]
    private double dimY;        // [m]
    private double dimZ;        // [m]
    private double Ix;          // [kg*m^2]
    private double Iy;          // [kg*m^2]
    private double Iz;          // [kg*m^2]

    public MassProperties(SpacecraftDesign design){
        mass = 50;
        dimX = 1;
        Ix = (1.0/6.0)*mass*dimX*dimX;
    }

    public double getIx(){return this.Ix;}
}
