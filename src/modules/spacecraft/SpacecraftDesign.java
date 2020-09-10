package modules.spacecraft;

import modules.spacecraft.component.ADCS;
import modules.spacecraft.component.Component;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.MassProperties;
import modules.spacecraft.orbits.SpacecraftOrbit;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class SpacecraftDesign {
    private ArrayList<Instrument> payload;
    private MassProperties massProperties;
    private ADCS adcs;

    public SpacecraftDesign(ArrayList<Instrument> payload){
        this.payload = new ArrayList<>(); this.payload.addAll(payload);
    }

    public void designSpacecraft(SpacecraftOrbit orbit) throws Exception {
        System.out.println("Spacecraft design algorithm pending.");
        this.adcs = new ADCS(payload,orbit);
        this.massProperties = new MassProperties(this);
    }

    public ArrayList<Instrument> getPayload(){return this.payload;}
    public ADCS getAdcs(){return adcs;}
    public MassProperties getMassProperties(){return  this.massProperties;}
}
