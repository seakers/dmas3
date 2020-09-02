package modules.environment;

import java.util.ArrayList;

public class Subtask {
    private Measurement mainMeasurement;
    private ArrayList<Measurement> depMeasurement;
    private Task parentTask;
    private Requirements requirements;
    private String name;
    private boolean completion;
    private int levelOfPartiality;
    private int i_q;

    public Subtask(Measurement mainMeasurement, ArrayList<Measurement> depMeasurement, Task parentTask){
        this.mainMeasurement = mainMeasurement;
        this.depMeasurement = new ArrayList<>(); this.depMeasurement.addAll(depMeasurement);
        this.parentTask = parentTask;
        this.requirements = parentTask.getRequirements();
        this.name = makeName();
        this.completion = false;
        this.i_q = parentTask.getMeasurements().indexOf(mainMeasurement);
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
}
