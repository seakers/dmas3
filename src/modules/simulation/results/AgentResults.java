package modules.simulation.results;

import madkit.kernel.AbstractAgent;
import modules.environment.Subtask;
import modules.environment.SubtaskCapability;
import modules.environment.Task;
import modules.environment.TaskCapability;
import modules.planner.CCBBA.IterationDatum;
import modules.planner.CCBBA.IterationResults;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;

public class AgentResults {
    private AbstractAgent me;
    private double overallCost = 0;
    private int subtasksDone = 0;
    private ArrayList<Instrument> instruments;
    private int n_instruments = 0;
    private HashMap<Subtask, SubtaskCapability> agentMeasurementCapabilities;
    private ArrayList<Subtask> overallPath;

    public AgentResults(Spacecraft spacecraft, HashMap<AbstractAgent, IterationResults> ccbbaResults,
                        ArrayList<Task> environmentTasks, HashMap<Task, TaskCapability> capabilities){
        me = spacecraft;
        IterationResults results = ccbbaResults.get(spacecraft);
        instruments = new ArrayList<>(spacecraft.getDesign().getPayload());
        n_instruments = instruments.size();

        // count subtasks and cost performed by agent
        ArrayList<Subtask> subtasksCompleted = new ArrayList<>();
        for(Task task : environmentTasks){
            for(Subtask j : task.getSubtasks()){
                IterationDatum datum = results.getIterationDatum(j);
                AbstractAgent winner = datum.getZ();
                if(winner == me){
                    overallCost += datum.getCost()*1e1;
                    subtasksCompleted.add(j);
                }
            }
        }
        subtasksDone = subtasksCompleted.size();
        overallPath = new ArrayList<>(spacecraft.getOverallPath());

        // get list of measurements performed by agent
        agentMeasurementCapabilities = new HashMap<>();
        if(overallPath.size() == subtasksDone) {

        }
        else{
            int x = 1;
        }
        for (Subtask j : subtasksCompleted) {
            SubtaskCapability subtaskCapability = capabilities.get(j.getParentTask()).getSubtaskCapability(j);
            agentMeasurementCapabilities.put(j, subtaskCapability);
        }
    }

    public String toString(){
        StringBuilder results = new StringBuilder();

        results.append(me.toString() + "\t" + subtasksDone + "\t");
        for(Instrument ins : instruments){
            if(instruments.indexOf(ins) != 0){
                results.append("," + ins.toString());
            }
            else{
                results.append(ins.toString());
            }
        }
        results.append("\t");
        for(Subtask j : overallPath){
            SubtaskCapability cap = agentMeasurementCapabilities.get(j);
            results.append(j.toString() + "(" + cap.getPerformance().getDate() + ")\t");
        }
        results.append("\n");
        return results.toString();
    }

    public double getOverallCost(){return overallCost;}
}
