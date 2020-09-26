package simulation.results;

import madkit.kernel.AbstractAgent;
import modules.environment.Subtask;
import modules.spacecraft.instrument.measurements.MeasurementCapability;
import modules.environment.Task;
import modules.environment.TaskCapability;
import modules.planner.CCBBA.IterationDatum;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.measurements.Measurement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class AgentResults {
    private AbstractAgent me;
    private double overallCost = 0;
    private int subtasksDone = 0;
    private ArrayList<Instrument> instruments;
    private int n_instruments = 0;
    private HashMap<Subtask, MeasurementCapability> agentMeasurementCapabilities;
    private ArrayList<Subtask> overallPath;
    private ArrayList<IterationDatum> winningBids;

    public AgentResults(Spacecraft spacecraft, ArrayList<Task> environmentTasks, HashMap<Task, TaskCapability> capabilities){
        me = spacecraft;
        agentMeasurementCapabilities = new HashMap<>();
        instruments = new ArrayList<>(spacecraft.getDesign().getPayload());
        n_instruments = instruments.size();

        ArrayList<Subtask> subtasksCompleted = new ArrayList<>();
        for(Task task : environmentTasks){
            TaskCapability taskCap = capabilities.get(task);
            Set<Measurement> keys = taskCap.getCapabilities().keySet();

            for(Measurement measurement : keys){
                MeasurementCapability cap = taskCap.getCapabilities().get(measurement);

                if(cap == null){
                    int x = 1;
                }

                ArrayList<AbstractAgent> winners = cap.getWinners();
                ArrayList<Subtask> subtasks = cap.getParentSubtasks();
                ArrayList<IterationDatum> bids = cap.getPlannerBids();
                this.winningBids = new ArrayList<>();
                for(AbstractAgent winner : winners){
                    if(winner == me){
                        int i = winners.indexOf(winner);
                        Subtask j = subtasks.get(i);
                        subtasksCompleted.add(j);
                        overallCost += bids.get(i).getCost();
                        agentMeasurementCapabilities.put(j, cap);
                        winningBids.add(bids.get(i));
                    }
                }
            }
        }
        subtasksDone = subtasksCompleted.size();
        overallPath = new ArrayList<>(spacecraft.getOverallPath());
//        overallPath = subtasksCompleted;
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
        results.append("\t|\t");

        ArrayList<IterationDatum> countedBids= new ArrayList<>();
        for(Subtask j : overallPath){
            MeasurementCapability cap = agentMeasurementCapabilities.get(j);
            IterationDatum datum = null;
            if(cap == null){
                int x = 1; continue;
            }
            for(IterationDatum dat_k : cap.getPlannerBids()){
                if(dat_k.getSubtask() == j && !countedBids.contains(dat_k)) {
                    datum = dat_k.copy();
                    countedBids.add(dat_k);
                    break;
                }
            }
            if(datum != null) results.append(j.toString() + "(" + datum.getTz() + ")\t");
        }
        results.append("\n");
        return results.toString();
    }

    public double getOverallCost(){return overallCost;}
}
