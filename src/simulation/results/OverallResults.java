package simulation.results;

import madkit.kernel.AbstractAgent;
import modules.environment.*;
import modules.planner.CCBBA.CCBBAPlanner;
import modules.planner.CCBBA.IterationDatum;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.measurements.Measurement;
import modules.spacecraft.instrument.measurements.MeasurementCapability;

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
    private double resSat = 0;
    private double snrSat = 0;
    private double revSat = 0;
    private int numMeasurements = 0;
    private long runTime = (long) 0.0;

    public OverallResults(ArrayList<Task> environmentTasks, ArrayList<Spacecraft> spaceSegment,
                          HashMap<Task, TaskCapability> capabilities, HashMap<AbstractAgent, AgentResults> agentResults,
                          long simulationTime) throws Exception {
        ArrayList<Spacecraft> agentList = spaceSegment;
        numAgents = agentResults.size();
        planningHorizon = ( (CCBBAPlanner) ( (Spacecraft) agentList.get(0)).getPlanner()).getSettings().M;
        this.runTime = simulationTime;

        countCoals(environmentTasks, capabilities);
        countScore(environmentTasks, capabilities);
        countTasks(environmentTasks, capabilities);
        countCost(agentResults);
        countReqSat(environmentTasks, capabilities);
        countNumMeasurements(environmentTasks, capabilities);
    }

    private void countNumMeasurements(ArrayList<Task> environmentTasks,HashMap<Task, TaskCapability> capabilities) throws Exception {
        for(Task task : environmentTasks) {
            for(Measurement measurement : capabilities.get(task).getCapabilities().keySet()){
                MeasurementCapability cap = capabilities.get(task).getCapabilities().get(measurement);
                numMeasurements += cap.getNumMeasurements();
            }
        }
    }

    private void countReqSat(ArrayList<Task> environmentTasks,HashMap<Task, TaskCapability> capabilities) throws Exception {
        double n = 0.0;
        for(Task task : environmentTasks) {
            TaskCapability taskCapability = capabilities.get(task);
            taskCapability.calcReqSat();

            resSat += taskCapability.getResSat();
            snrSat += taskCapability.getSnrSat();
            revSat += taskCapability.getRevSat();
            n += 1.0;
        }

        resSat = resSat/n;
        snrSat = snrSat/n;
        revSat = revSat/n;
    }

    private void countCost(HashMap<AbstractAgent, AgentResults> agentResults){
        for(AbstractAgent agent : agentResults.keySet()){
            AgentResults agentRes = agentResults.get(agent);
            overallCostPerAgent += agentRes.getOverallCost();
        }
        overallCostPerAgent = overallCostPerAgent/numAgents;
    }

    private void countTasks(ArrayList<Task> environmentTasks,HashMap<Task, TaskCapability> capabilities){
        tasksAvailable = environmentTasks.size();

        for(Task task : environmentTasks) {
            TaskCapability taskCapability = capabilities.get(task);
            Dependencies dep = task.getDependencies();
            boolean taskCompleted = false;

            for (Measurement measurement : taskCapability.getCapabilities().keySet()) {
                MeasurementCapability measurementCapability_j = taskCapability.getCapabilities().get(measurement);
                if(measurementCapability_j.getWinners().size() > 0){
                    taskCompleted = true;
                    break;
                }
            }

            if(taskCompleted){
                tasksDone++;
            }
        }
    }

    private void countScore(ArrayList<Task> environmentTasks,HashMap<Task, TaskCapability> capabilities){
        for(Task task : environmentTasks) {
            TaskCapability taskCapability = capabilities.get(task);
            Dependencies dep = task.getDependencies();

            int n_max = -1;
            for (Measurement measurement : taskCapability.getCapabilities().keySet()) {
                MeasurementCapability measurementCapability_j = taskCapability.getCapabilities().get(measurement);

                for (IterationDatum datum_j : measurementCapability_j.getPlannerBids()) {
                    // if j has dependent measurements, see if they were performed by someone else
                    scoreAchieved += datum_j.getScore();
                    utility += datum_j.getY();
                    scoreAvailable += calcMaxScore(datum_j.getSubtask());
                }
                n_max = Math.max(measurementCapability_j.getNumMeasurements(), n_max);
            }
        }
    }

    private double calcMaxScore(Subtask j){
        double Smax = j.getParentTask().getMaxScore();
        double K = j.getLevelOfPartiality();

        return Smax/K;
    }

    private void countCoals(ArrayList<Task> environmentTasks,HashMap<Task, TaskCapability> capabilities){
        for(Task task : environmentTasks){
            TaskCapability taskCapability = capabilities.get(task);
            Dependencies dep = task.getDependencies();

            ArrayList<IterationDatum> datumsCounted = new ArrayList<>();
            for(Measurement measurement : taskCapability.getCapabilities().keySet()){
                MeasurementCapability measurementCapability_j = taskCapability.getCapabilities().get(measurement);

                for(IterationDatum datum_j : measurementCapability_j.getPlannerBids()){
                    // if j has dependent measurements, see if they were performed by someone else
                    if(datumsCounted.contains(datum_j)) continue;

                    ArrayList<IterationDatum> myCoalition = new ArrayList<>();
                    Subtask j = datum_j.getSubtask();

                    for(Measurement measurement_q : j.getDepMeasurements()){
                        MeasurementCapability measurementCapability_q = taskCapability.getCapabilities().get(measurement_q);
                        for(IterationDatum datum_q : measurementCapability_q.getPlannerBids()){
                            Subtask q = datum_q.getSubtask();
                            if(dep.depends(j,q)){
                                boolean difWinners = datum_j.getZ() != datum_q.getZ();
                                boolean timeSat =
                                        (Math.abs(datum_j.getTz().durationFrom(datum_q.getTz())) <= dep.Tmax(j,q))
                                                && (Math.abs(datum_j.getTz().durationFrom(datum_q.getTz())) >= dep.Tmin(j,q));
                                boolean notChecked = !datumsCounted.contains(datum_q);

                                if(difWinners && timeSat && notChecked) {
                                    myCoalition.add(datum_q);
                                }
                            }
                        }
                    }

                    myCoalition.add(datum_j);
                    datumsCounted.addAll(myCoalition);
                    if(myCoalition.size() > 1){
                        coalsFormed++;
                    }
                    coalsAvailable++;
                }
            }
        }
    }

    public String toString(){
        this.runTime = (System.nanoTime() - this.runTime);
        StringBuilder results = new StringBuilder();
        results.append(utility + "\t" + coalsFormed + "\t" + coalsAvailable + "\t" + scoreAchieved + "\t" + scoreAvailable
                + "\t" + tasksDone + "\t" + tasksAvailable + "\t" + numAgents + "\t" + planningHorizon + "\t" + overallCostPerAgent
                + "\t" + resSat  + "\t" + snrSat + "\t" + revSat + "\t" + numMeasurements + "\t" + runTime);
        return results.toString();
    }
}