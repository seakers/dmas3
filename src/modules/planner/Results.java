package modules.planner;

import modules.environment.Subtask;
import modules.environment.SubtaskCapability;
import modules.environment.Task;
import modules.environment.TaskCapability;
import modules.planner.CCBBA.IterationDatum;
import modules.planner.CCBBA.IterationResults;
import modules.spacecraft.instrument.Instrument;

import java.util.ArrayList;
import java.util.HashMap;

public class Results {
    private String directoryAddress;
    private double totalUtility;
    private double totalScore;
    private double totalCostPerAgent;
    private double avgScore;
    private int coalsFormed;
    private int coalsAv;
    private int numAgents;
    private int planningHorizon;

    public Results(IterationResults results, ArrayList<Task> environmentTasks, HashMap<Task, TaskCapability> capabilities, String directoryAddress, boolean resultsMatch){
        this.directoryAddress = directoryAddress;
        if(resultsMatch){
            // compile all results
            for(Task task : environmentTasks){
                

                for(Subtask j : task.getSubtasks()){
                    IterationDatum datum = results.getIterationDatum(j);
                    SubtaskCapability capability = capabilities.get(task).getSubtaskCapability(j);

                    double utility = datum.getY();
                    double score = datum.getScore();
                    double cost = datum.getCost();
                    String winner = datum.getZ().getName();

                }
            }

            // print out to directory
        }
    }

}
