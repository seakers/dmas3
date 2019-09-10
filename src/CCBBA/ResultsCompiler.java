package CCBBA;

import madkit.action.SchedulingAction;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Message;
import madkit.message.SchedulingMessage;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;

public class ResultsCompiler extends AbstractAgent {
    private int numAgents;
    protected Scenario environment;

    public ResultsCompiler(int numAgents){
        this.numAgents = numAgents;
    }

    @Override
    protected void activate(){
        requestRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.RESULTS_ROLE);
    }

    private void checkResults() throws IOException {
        // check for messages
        //Receive results
        List<Message> receivedMessages = nextMessages(null);
        Vector<IterationResults> receivedResults = new Vector<>();
        for(int i = 0; i < receivedMessages.size(); i++){
            myMessage message = (myMessage) receivedMessages.get(i);
            receivedResults.add(message.myResults);
        }

        if(receivedResults.size() >= this.numAgents){ // checks if every agent has sent finished its tasks.
            getLogger().info("Terminating Sim. Saving Results.");
            // Every agent has finished its tasks

            // print results
            printResults(receivedResults);

            // terminate sim
            SchedulingMessage terminate = new SchedulingMessage(SchedulingAction.SHUTDOWN);
            sendMessage(getAgentWithRole(AgentSimulation.MY_COMMUNITY, AgentSimulation.SIMU_GROUP, AgentSimulation.SCH_ROLE), terminate);
        }
    }

    protected void printResults( Vector<IterationResults> receivedResults ) throws IOException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDateTime now = LocalDateTime.now();

        // CoalitionsFormed CoalitionsAvailable ScoreAchieved ScoreAvailable TotalCost ResourcePerAgent MergeCost SplitCost NumberofTasksDone PlanHorizon
        int coalitionsFormed = calcCoalitionsFormed(receivedResults);
        int coalitionsAvailable = 0;
        double scoreAchieved = calcScoreAchieved(receivedResults);
        double scoreAvaiable = calcScoreAvailale(receivedResults);
        double totalCost = 0;
        double resourcesPerAgent = 0;
        double mergeCost = receivedResults.get(0).getC_merge();
        double splitCost = receivedResults.get(0).getC_split();
        double numberOfTasksDone = countTasksDone(receivedResults);
        int planHorizon = receivedResults.get(0).getM();

        // Create a new file with
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String address = "src/CCBBA/Results/results-"+ dtf.format(now) + ".out";

        fileWriter = null;
        try {
            fileWriter = new FileWriter( address );
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);
        printWriter.printf("CoalitionsFormed\tCoalitionsAvailable\tScoreAchieved\tScoreAvailable\tTotalCost\tResourcesPerAgent\tMergeCost\tSplitCost\tNumberOfTasksDone\tPlanHorizon\n");
        printWriter.printf("%d\t%d\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%d\n",coalitionsFormed, coalitionsAvailable, scoreAchieved, scoreAvaiable, totalCost, resourcesPerAgent, mergeCost, splitCost, numberOfTasksDone, planHorizon);
        printWriter.close();
    }

    private double calcScoreAchieved( Vector<IterationResults> receivedResults){
        double count = 0;
        Vector<Double> localY = receivedResults.get(0).getY();
        for(int i = 0; i < localY.size(); i ++){
            count = count + localY.get(i);
        }
        return count;
    }

    private double calcScoreAvailale( Vector<IterationResults> receivedResults){
        double count = 0;
        Vector<Task> V = environment.getTasks();
        for(int i = 0; i < V.size(); i++){
            count = count + V.get(i).getS_max();
        }
        return count;
    }

    private int countTasksDone( Vector<IterationResults> receivedResults){
        int count = 0;
        Vector<SimulatedAbstractAgent> localZ = receivedResults.get(0).getZ();
        for(int i = 0; i < localZ.size(); i++){
            if(localZ.get(i) != null){
                count++;
            }
        }
        return count;
    }

    private int calcCoalitionsFormed(Vector<IterationResults> receivedResults){
        int count = 0;
        for(int i = 0; i < receivedResults.size(); i++){
            Vector<Subtask> tempBundle = receivedResults.get(i).getBundle();
            Vector<Vector<SimulatedAbstractAgent>> tempOmega = receivedResults.get(i).getOmega();
            Vector<Subtask> coalitionList = new Vector<>();

            for(int j = 0; j < tempBundle.size(); j++){
                if(tempOmega.get(j).size() > 0){ // if the task in the bundle has a coalition partner
                    if(!coalitionList.contains(tempBundle.get(j))){ // check if task related to that coalition has been counted
                        coalitionList.add(tempBundle.get(j)); // if not, add coalition to list
                        count++; // increase coalition count
                    }
                }
            }
        }
        return count;
    }
}
