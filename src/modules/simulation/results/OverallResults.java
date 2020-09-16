package modules.simulation.results;

import madkit.kernel.AbstractAgent;
import modules.environment.Dependencies;
import modules.environment.Subtask;
import modules.environment.SubtaskCapability;
import modules.environment.Task;
import modules.planner.CCBBA.CCBBAPlanner;
import modules.planner.CCBBA.IterationDatum;
import modules.planner.CCBBA.IterationResults;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;

import java.util.ArrayList;
import java.util.HashMap;

public class OverallResults {
    private double utility = 0.0;
    private int coalsFormed = 0;
    private int coalsAvailable = 0;
    private double scoreAchieved = 0.0;
    private double scoreAvailable = 0.0;
    private int tasksDone = 0;
    private int tasksAvailable = 0;
    private int numAgents = 0;
    private int planningHorizon = 0;
    private double overallCostPerAgent = 0;

    public OverallResults(HashMap<AbstractAgent, IterationResults> ccbbaResults, ArrayList<Task> environmentTasks,
                          HashMap<AbstractAgent, AgentResults> agentResults){

        ArrayList<AbstractAgent> agentList = new ArrayList<>();
        for(AbstractAgent agents : ccbbaResults.keySet()){
            if(!agentList.contains(agents)) {
                agentList.add(agents);
            }
        }
        numAgents = agentResults.size();
        planningHorizon = ( (CCBBAPlanner) ( (Spacecraft) agentList.get(0)).getPlanner()).getSettings().M;

        IterationResults results = ccbbaResults.get(agentList.get(0));
        for(Task task : environmentTasks){
            ArrayList<Subtask> subtasksCompleted = new ArrayList<>();
            for(Subtask j : task.getSubtasks()) {
                IterationDatum datum = results.getIterationDatum(j);

                utility += datum.getY();
                scoreAchieved += datum.getScore();
                if(datum.getZ() != null) subtasksCompleted.add(j);
            }

            Dependencies dep = task.getDependencies();
            boolean sameCoalition = true;
            for(Subtask j : subtasksCompleted){
                for(Subtask q : subtasksCompleted){
                    if(j == q) continue;
                    if(!dep.depends(j,q) && !dep.depends(q,j)) {
                        sameCoalition = false;
                        break;
                    }
                }
            }
            if(subtasksCompleted.size() > 1 && sameCoalition) coalsFormed++;
            coalsAvailable++;
            scoreAvailable += task.getMaxScore();
            if(task.getCompletion()) tasksDone++;
        }

        for(AbstractAgent agent : agentResults.keySet()){
            AgentResults agentRes = agentResults.get(agent);
            overallCostPerAgent += agentRes.getOverallCost();
        }
        overallCostPerAgent = overallCostPerAgent/numAgents;
        tasksAvailable = environmentTasks.size();
    }

    public String toString(){
        StringBuilder results = new StringBuilder();
        results.append(utility + "\t" + coalsFormed + "\t" + coalsAvailable + "\t" + scoreAchieved + "\t" + scoreAvailable
                + "\t" + tasksDone + "\t" + tasksAvailable + "\t" + numAgents + "\t" + planningHorizon + "\t" + overallCostPerAgent);
        return results.toString();
    }
}