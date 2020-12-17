package modules.environment;

import modules.spacecraft.instrument.measurements.Measurement;

import java.util.ArrayList;

public class Subtask {
    private Measurement mainMeasurement;
    private ArrayList<Measurement> depMeasurement;
    private ArrayList<Subtask> depSubtasks;
    private Task parentTask;
    private Requirements requirements;
    private String name;
    private boolean completion;
    private int levelOfPartiality;
    private int i_q;

    public Subtask(Measurement mainMeasurement, ArrayList<Measurement> depMeasurement, Task parentTask, int i_q){
        this.mainMeasurement = mainMeasurement;
        this.depMeasurement = new ArrayList<>(); this.depMeasurement.addAll(depMeasurement);
        this.parentTask = parentTask;
        this.requirements = parentTask.getRequirements();
        this.name = makeName();
        this.completion = false;
        this.i_q = i_q;
        this.levelOfPartiality = depMeasurement.size() + 1;
        this.completion = false;
    }
    private String makeName(){
        String name = parentTask.getName() + "_" + mainMeasurement.getBand() + "_{";
        for(Measurement measurement : depMeasurement){
            if(depMeasurement.indexOf(measurement) == 0) {
                name += measurement.getBand();
            }
            else {
                name += "," + measurement.getBand();
            }
        }
        name += "}";
        return name;
    }
    public String toString(){ return this.name; }
    public ArrayList<Measurement> getDepMeasurements(){return this.depMeasurement;}
    public Measurement getMainMeasurement(){return this.mainMeasurement;}
    public int getI_q(){return this.i_q;}
    public Task getParentTask(){return this.parentTask;}
    public void setCompletion(boolean status){this.completion = status;}
    public boolean getCompletion(){return this.completion;}
    public ArrayList<Subtask> getDepSubtasks(){
        if(this.depSubtasks == null) {
            depSubtasks = new ArrayList<>();
            Dependencies dep = this.parentTask.getDependencies();
            for (Subtask j : parentTask.getSubtasks()) {
                if(dep.depends(this,j)) depSubtasks.add(j);
            }
        }
        return depSubtasks;
    }
    public double getLevelOfPartiality(){return (double) this.levelOfPartiality;}
}
