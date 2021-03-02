package modules.simulation;

import modules.measurements.Measurement;
import org.orekit.errors.OrekitException;

import java.util.ArrayList;

public class Results {
    private ArrayList<Measurement> measurements;

    public Results(){

    }

    public void generateRandomResults() throws OrekitException {

    }

    public ArrayList<Measurement> getMeasurements() {
        return measurements;
    }
}
