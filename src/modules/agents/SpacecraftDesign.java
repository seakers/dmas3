package modules.agents;

import modules.agents.Component.Component;
import modules.agents.Instrument.Instrument;
import modules.agents.orbits.OrbitData;

import java.util.ArrayList;

public class SpacecraftDesign {
    private ArrayList<Instrument> payload;
    private ArrayList<Component> design;

    public SpacecraftDesign(ArrayList<Instrument> payload){
        this.payload = new ArrayList<>(); this.payload.addAll(payload);
    }

    public void designSpacecraft(OrbitData orbit){
        System.out.println("Spacecraft design algorithm implementation pending.");
        this.design = new ArrayList<>();
    }

    public ArrayList<Instrument> getPayload(){return this.payload;}
}
