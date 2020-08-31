package modules.spacecraft;

import modules.spacecraft.component.Component;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.orbits.OrbitData;

import java.util.ArrayList;

public class SpacecraftDesign {
    private ArrayList<Instrument> payload;
    private ArrayList<Component> design;

    public SpacecraftDesign(ArrayList<Instrument> payload){
        this.payload = new ArrayList<>(); this.payload.addAll(payload);
    }

    public void designSpacecraft(OrbitData orbit){
        System.out.println("Spacecraft design algorithm pending.");
        this.design = new ArrayList<>();
    }

    public ArrayList<Instrument> getPayload(){return this.payload;}
}
