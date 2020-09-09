package modules.environment;

import modules.spacecraft.orbits.GroundPointTrajectory;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;

public class Task {

    private String name;                        // Task name
    private double maxScore;                    // Maximum score
    private ArrayList<Double> location;         // Task location in {lat, lon, alt}
    private ArrayList<Measurement> measurements;// List of frequencies required for measurement
    private Requirements requirements;          // List of measurement requirements
    private ArrayList<Subtask> subtasks;        // List of subtasks
    private Dependencies dependencies;          // List of subtask dependencies
    private int I;                              // Number of measurements
    private int N_j;                            // Number of subtasks
    private GroundPointTrajectory pv;           // Trajectory of task as a ground point
    private boolean completion;

    public Task(String name, double score, double lat, double lon, double alt, ArrayList<Measurement> measurements,
                double spatialResReq, double lossReq, int numLooks, double temporalResolutionMin, double temporalResolutionMax,
                AbsoluteDate startDate, AbsoluteDate endDate, double timestep, double urgencyFactor){
        try {
            this.name = name;
            this.maxScore = score;
            this.location = new ArrayList<>(); location.add(lat); location.add(lon); location.add(alt);
            this.measurements = new ArrayList<>();
            for(int n = 0; n < numLooks; n++){
                for(Measurement measurement : measurements){
                    Measurement temp = measurement.copy();
                    this.measurements.add(temp);
                }
            }
            this.I = this.measurements.size();
            this.requirements = new Requirements(spatialResReq, lossReq, numLooks, temporalResolutionMin, temporalResolutionMax, urgencyFactor, startDate, endDate);
            this.completion = false;
            this.subtasks = generateSubtasks(measurements);
            this.N_j = this.subtasks.size();
            this.dependencies = new Dependencies(N_j, subtasks, requirements);
            this.pv = new GroundPointTrajectory(lat, lon, alt, startDate, endDate, timestep);
            this.pv.propagateOrbit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Subtask> generateSubtasks( ArrayList<Measurement> indMeasurements) throws Exception {
        ArrayList<Subtask> subtasksList = new ArrayList<>();
        if (requirements.getNumLooks() == 1 || (requirements.getNumLooks() > 1 && indMeasurements.size() == 1)) {
            for(Measurement mainFreq : this.measurements){
                ArrayList<Measurement> remainingFreqs = new ArrayList<>();
                for(Measurement f : this.measurements){
                    if(mainFreq != f) remainingFreqs.add(f);
                }

                ArrayList<ArrayList<Measurement>> depMeasurementCombinations = getCombinations(remainingFreqs);
                for(ArrayList<Measurement> depMeasurements : depMeasurementCombinations){
                    int i_q = subtasksList.size();
                    subtasksList.add( new Subtask(mainFreq, depMeasurements, this, i_q) );
                }
            }
        }
        else{
            throw new Exception("Multiple number of looks for different types of measurements not yet supported");
        }
        return subtasksList;
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
    public ArrayList<Measurement> getMeasurements(){return measurements;}
    public PVCoordinates getPV(AbsoluteDate date) throws OrekitException {
        return this.pv.getPV(date);
    }
    public PVCoordinates getPVEarth(AbsoluteDate date) throws OrekitException {
        return this.pv.getPVEarth(date);
    }
    public ArrayList<Subtask> getSubtasks(){return this.subtasks;}
    public void completeSubtasks(Subtask j){
        j.setCompletion(true);

        int subtasksCompleted = 0;
        for(Subtask q : this.subtasks){
            if(this.dependencies.mutuallyExclusive(j,q)){
                q.setCompletion(true);
            }
            if(q.getCompletion() == true) subtasksCompleted++;
        }
        if(subtasksCompleted >= this.N_j) this.completion = true;
    }
    public boolean getCompletion(){return this.completion;}
    public Dependencies getDependencies(){return dependencies;}
}
