package modules.environment;

import madkit.kernel.Watcher;
import modules.planner.Task;
import org.json.simple.JSONObject;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.logging.Level;

public class Environment extends Watcher {
    private Level loggerLevel;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;
    private AbsoluteDate currentDate;
    private double timeStep;
    private ArrayList<Task> environmentTasks;

    // Constructor
    public Environment(String logger, AbsoluteDate startDate, AbsoluteDate endDate, double timeStep, ArrayList<Task> environmentTasks) throws Exception {
        setUpLogger(logger);
        this.startDate = startDate.getDate();
        this.currentDate = startDate.getDate();
        this.endDate = endDate.getDate();
        this.timeStep = timeStep;
        this.environmentTasks = new ArrayList<>();
        this.environmentTasks.addAll(environmentTasks);
    }


    // Helper functions
    private void stepTime(){
        this.currentDate = this.currentDate.shiftedBy(this.timeStep);
    }

    private void setUpLogger(String logger) throws Exception {
        if(logger.equals("OFF")){
            this.loggerLevel = Level.OFF;
        }
        else if(logger.equals("SEVERE")){
            this.loggerLevel = Level.SEVERE;
        }
        else if(logger.equals("WARNING")){
            this.loggerLevel = Level.WARNING;
        }
        else if(logger.equals("INFO")){
            this.loggerLevel = Level.INFO;
        }
        else if(logger.equals("CONFIG")){
            this.loggerLevel = Level.CONFIG;
        }
        else if(logger.equals("FINE")){
            this.loggerLevel = Level.FINE;
        }
        else if(logger.equals("FINER")){
            this.loggerLevel = Level.FINER;
        }
        else if(logger.equals("FINEST")){
            this.loggerLevel = Level.FINEST;
        }
        else if(logger.equals("ALL")){
            this.loggerLevel = Level.ALL;
        }
        else{
            throw new Exception("INPUT ERROR: Logger type not supported");
        }

        getLogger().setLevel(this.loggerLevel);
    }

    // Getters and Setters
    public ArrayList<Task> getScenarioTasks(){ return this.environmentTasks; }
    public AbsoluteDate getStartDate(){ return this.startDate; }
    public AbsoluteDate getEndDate(){ return this.endDate; }
}
