package modules.components;

import modules.components.instruments.SimulationInstrument;
import modules.orbitData.OrbitData;
import seakers.orekit.object.Instrument;
import seakers.orekit.object.Satellite;

import java.util.ArrayList;

public class SatelliteDesign {
    /**
     * coverage data for spacecraft
     */
    private OrbitData orbitData;

    /**
     * name of parent spacecraft
     */
    private Satellite parentSat;

    /**
     * Subsystems in spacecraft
     */
    private ADCSSubsystem adcs;
    private CommsSubsystem comms;
    private ElectronicPowerSubsystem eps;
    private PropulsionSubsystem prop;
    private ArrayList<SimulationInstrument> payload;
    private StructuralSubsystem str;
    private ThermalSubsystem thermal;

    public SatelliteDesign(Satellite parentSat, OrbitData orbitData){
        this.payload = new ArrayList<>();
        for(Instrument ins : parentSat.getPayload()){
            if(ins.getName().contains("_FOR")) continue;
            payload.add((SimulationInstrument) ins);
        }
        this.orbitData = orbitData;
        this.parentSat = parentSat;
    }

    public void designSpacecraft(){
        comms = designComms();

        adcs = prelimADCSDesign();
        eps = prelimEPSDesign();
        prop = prelimPropDesign();
        str = prelimStrDesign();
        thermal = prelimThermalDesign();

        double mass = getWetMass();
        while(Math.abs(mass - getDryMass()) > 1e3){
            //TODO Include satellite design algorithm
        }
    }

    private double getPayloadMass(){
        double mass = 0;
        for(SimulationInstrument ins : payload){
            mass += ins.getMass();
        }
        return mass;
    }

    public double getDryMass(){
        return (adcs.getMass() + comms.getMass() + eps.getMass() + prop.getStrMass() + getPayloadMass() + str.getMass() + thermal.getMass());
    }

    public double getWetMass(){
        return getDryMass() + prop.getPropMass();
    }

    /**
     *  PRELIMINARY DESIGN FUNCTIONS
     *  TODO Add proper numbers for initial estimates for mass and power based on mass-fractions
     */
    private CommsSubsystem designComms(){
        return new CommsSubsystem("comms",0,0,0,0,0);
    }

    private ADCSSubsystem prelimADCSDesign(){
        return new ADCSSubsystem("adcs", 0,0,0,0,0);
    }
    private ElectronicPowerSubsystem prelimEPSDesign(){
        return new ElectronicPowerSubsystem("eps", 0,0,0,0,0);
    }
    private PropulsionSubsystem prelimPropDesign(){
        return new PropulsionSubsystem("prop", 0,0,0,0,0,0);
    }
    private StructuralSubsystem prelimStrDesign(){
        return new StructuralSubsystem("str", 0,0,0,0);
    }
    private ThermalSubsystem prelimThermalDesign(){
        return new ThermalSubsystem("thermal", 0,0,0,0,0);
    }

    public ADCSSubsystem getAdcs() {
        return adcs;
    }

    public CommsSubsystem getComms() {
        return comms;
    }

    public ElectronicPowerSubsystem getEps() {
        return eps;
    }

    public PropulsionSubsystem getProp() {
        return prop;
    }

    public ArrayList<SimulationInstrument> getPayload() {
        return payload;
    }

    public StructuralSubsystem getStr() {
        return str;
    }

    public ThermalSubsystem getThermal() {
        return thermal;
    }
}
