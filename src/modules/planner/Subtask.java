package modules.planner;

import java.util.ArrayList;

public class Subtask {
    private Measurement mainMeasurement;
    private ArrayList<Measurement> depMeasurement;
    private Task parentTask;
    private Requirements requirements;
    private String name;
    private boolean completion;
    private int levelOfPartiality;

    public Subtask(Measurement mainMeasurement, ArrayList<Measurement> depMeasurement, Task parentTask){
        this.mainMeasurement = mainMeasurement;
        this.depMeasurement = new ArrayList<>(); this.depMeasurement.addAll(depMeasurement);
        this.parentTask = parentTask;
        this.requirements = parentTask.getRequirements();
        this.name = makeName();
        this.completion = false;
        int =
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
}
