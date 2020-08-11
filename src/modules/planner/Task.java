package modules.planner;

import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;

public class Task {

    private String name;                        // Task name
    private double maxScore;                    // Maximum score
    private ArrayList<Double> location;         // Task location in {lat, lon, alt}
    private ArrayList<Measurement> measurements;   // List of frequencies required for measurement
    private Requirements requirements;          // List of measurement requirements
    private ArrayList<Subtask> subtasks;        // List of subtasks
    private double[][] D;                       // Coalition Dependency Matrix
    private double[][] T;                       // Coalition Correlation Time Matrix
    private boolean completion;

    public Task(String name, double score, double lat, double lon, double alt, ArrayList<Measurement> measurements,
                double spatialResReq, double swathReq, double lossReq, int numLooks, double tempResReqLooks,double urgencyFactor,
                AbsoluteDate startTime, AbsoluteDate endTime, double tempResMeasurements){
        try {
            this.name = name;
            this.maxScore = score;
            this.location = new ArrayList<>(); location.add(lat); location.add(lon); location.add(alt);
            this.measurements = new ArrayList<>(); this.measurements.addAll(measurements);
            this.requirements = new Requirements(spatialResReq, swathReq, lossReq, numLooks, tempResReqLooks, urgencyFactor);
            this.completion = false;
            this.subtasks = generateSubtasks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Subtask> generateSubtasks() throws Exception {
        if (requirements.getNumLooks() == 1) {
            this.subtasks = new ArrayList<>();
            int i;

            for(Measurement mainFreq : measurements){
                i = measurements.indexOf(mainFreq);
                ArrayList<Measurement> remainingFreqs = new ArrayList<>();
                for(Measurement f : measurements){
                    if(mainFreq != f) remainingFreqs.add(f);
                }

                ArrayList<ArrayList<Measurement>> depMeasurementCombinations = getCombinations(remainingFreqs);
                for(ArrayList<Measurement> depMeasurements : depMeasurementCombinations){
                    subtasks.add( new Subtask(mainFreq, depMeasurements, i, this, ) );
                }
                int x = 1;
            }

            return null;
        }
        else{
            throw new Exception("Multiple number of looks per measurement not yet supported");
        }
    }

    private ArrayList<ArrayList<Measurement>> getCombinations(ArrayList<Measurement> remainingFrequencies){
        ArrayList<ArrayList<Measurement>> combinations = new ArrayList<>();

        for(int i = 0; i < (int) Math.pow(2, remainingFrequencies.size()); i++ ){
            String bitRepresentation = Integer.toBinaryString(i);
            ArrayList<Measurement> tempSet = new ArrayList<>();

            for(int j = 0; j < bitRepresentation.length(); j++){
                int delta = remainingFrequencies.size() - bitRepresentation.length();

                if (bitRepresentation.charAt(j) == '1') {
                    tempSet.add(remainingFrequencies.get(j + delta));
                }
            }

            combinations.add(tempSet);
        }

        return combinations;
    }

    public double getLat(){return location.get(0);}
    public double getLon(){return location.get(1);}
    public double getAlt(){return location.get(2);}
    public Requirements getRequirements(){return requirements;}
    public String getName(){return name;}
}
