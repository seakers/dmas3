package modules.simulation;

import modules.measurements.Measurement;
import org.orekit.errors.OrekitException;

import java.util.ArrayList;

public class Results {
    private ArrayList<Measurement> measurements;

    public Results(){

    }

    public void generateRandomResults() throws OrekitException {
        measurements = new ArrayList<>();
        int n = (int) (100.0 * Math.random());

        for(int i = 1; i < n; i++){
            Measurement newMeasurement = new Measurement();
            if(Math.random() > 0.2){
                newMeasurement.randomize();
            }

            measurements.add(newMeasurement);
        }
    }

    public ArrayList<Measurement> getMeasurements() {
        return measurements;
    }
}
