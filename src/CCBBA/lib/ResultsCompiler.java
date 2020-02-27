package CCBBA.lib;

import madkit.action.SchedulingAction;
import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.SchedulingMessage;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ResultsCompiler extends AbstractAgent {
    private Scenario environment;                           // world environment
    private String directoryAddress;
    private ArrayList<IterationResults> receivedResults;

    public ResultsCompiler(String directoryAddress){
        this.directoryAddress = directoryAddress;
    }

    @Override
    protected void activate(){
        requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.RESULTS_ROLE);
    }


    @SuppressWarnings("unused")
    private void checkResults() throws Exception {
        //Receive results
        List<AgentAddress> resultsAddress = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_DIE);
        List<AgentAddress> agentsEnvironment = getAgentsWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT_EXIST);

        if( (resultsAddress != null)&&(resultsAddress.size() == agentsEnvironment.size()) ){
            List<Message> receivedMessages = nextMessages(null);
            ArrayList<IterationResults> receivedResults = new ArrayList<>();
            ArrayList<String> senderList = new ArrayList<>();

            for (int i = 0; i < receivedMessages.size(); i++) {
                SimResultsMessage message = (SimResultsMessage) receivedMessages.get(i);

                if(!senderList.contains(message.getSenderName()) ) {
                    senderList.add(message.getSenderName());
                    receivedResults.add(message.getResults());
                }
            }

            if (receivedResults.size() >= agentsEnvironment.size()) { // checks if every agent has sent finished its tasks.
                // Every agent has finished its tasks
                getLogger().info("Terminating Sim. Saving Results.");

                // print results
                if( this.directoryAddress != null ) {
                    printResults(receivedResults);
                }

                // terminate sim
                SchedulingMessage terminate = new SchedulingMessage(SchedulingAction.SHUTDOWN);
                sendMessage(getAgentWithRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCH_ROLE), terminate);
            }
        }
    }

    private void printResults( ArrayList<IterationResults> receivedResults ) throws Exception {
        this.receivedResults = receivedResults;
        if( compareResults(receivedResults) ) {
            printWinningVectors();
            printTaskList();
//            printAgentList();
            printMetrics();
            printReport();
        }
        printAllVectors( receivedResults );
    }

    private boolean compareResults( ArrayList<IterationResults> receivedResults ) throws Exception {
        boolean consistent = true;
        int i_e = -1;

        // compare results and ensure that they match
        for(IterationResults result : receivedResults){
            for(IterationResults comparedResult : receivedResults){
                // skip if compared to itself
                if(result == comparedResult){
                    continue;
                }
                else if(result.size() != comparedResult.size()){
                    consistent = false;
                    break;
                }

                i_e = result.compareToList(comparedResult);
                if(i_e > -1) {
                    consistent = false;
                    break;
                }
            }

            if(!consistent){
                String status = String.format("\n\t\t\tFirst inconsistency at: %d\n", i_e);
                getLogger().info("ERROR: Final plans did not match." + status );
                return false;
            }
        }

        return true;
    }

    private void printWinningVectors(){
        //create new file in directory
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = this.directoryAddress + "/winning_vectors.out";
        fileWriter = null;
        try {
            fileWriter = new FileWriter( outAddress, false );
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        //obtain values
        ArrayList<SimulatedAgent> agentList = new ArrayList<>();
        for(IterationResults result : receivedResults){
            agentList.add(result.getParentAgent());
        }

        //print values
        for(int i = 0; i < receivedResults.get(0).size(); i++){
            IterationDatum datum = receivedResults.get(0).getIterationDatum(i);

            double y = datum.getY();
            double tz = datum.getTz();
            SimulatedAgent z = datum.getZ();
            int i_winner = agentList.indexOf( z ) + 1;

            printWriter.printf("%f\t%f\r", y, tz);
            if(z != null) {
                printWriter.printf("%d\t", i_winner);
            }
            else{
                printWriter.printf("-\t");
            }
            printWriter.print("\n");
        }

        //close file
        printWriter.close();
    }

    private void printTaskList(){
        //create new file in directory
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = this.directoryAddress + "/task_list.out";
        fileWriter = null;
        try {
            fileWriter = new FileWriter( outAddress, false );
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        //obtain values
        ArrayList<Task> resultsToPrint = this.environment.getScenarioTasks();
        double x = 0.0;   double y = 0.0;   double z = 0.0;

        //print values
        for(int i = 0; i < resultsToPrint.size(); i++){
            Task localTask = resultsToPrint.get(i);
            x = localTask.getLocation().get(0);
            y = localTask.getLocation().get(1);
            z = localTask.getLocation().get(2);

            // print basic info
            printWriter.printf("Task Number:\t\t%d\n",i);
            printWriter.printf("Maximum Score:\t\t%f\n", localTask.getS_Max());
            printWriter.printf("Subtask Cost Type:\t\t%s\n", localTask.getCost_type());
            printWriter.printf("Subtask Cost:\t\t%f\n", localTask.getCost());
            printWriter.printf("Number of Sensors:\t%d\n", localTask.getI());
            printWriter.printf("Number of Subtasks:\t%d\n\n", localTask.getSubtaskList().size());

            // print component lists
            printWriter.printf("Location:\t\t\t[%f, %f, %f]\n", x, y, z);
            printWriter.printf("Sensor List:\t\t%s\n", localTask.getReq_sensors());
            printWriter.printf("Subtask List:\t\t[");
            for(int j = 0; j < localTask.getSubtaskList().size(); j++){
                printWriter.printf("%s", localTask.getSubtaskList().get(j).getName());
                if(j != (localTask.getSubtaskList().size() - 1) ) { printWriter.printf(", "); }
            }
            printWriter.printf("]\n\n");

            // print time constraints
            printWriter.printf("Time Constraints:\n");
            printWriter.printf("Start Time:\t\t%f\n",localTask.getT_start());
            printWriter.printf("End Time:\t\t%f\n",localTask.getT_end());
            printWriter.printf("Task Duration:\t%f\n",localTask.getDuration());
            printWriter.printf("Corr Time:\t\t%f\n",localTask.getT_corr());
            printWriter.printf("Lambda:\t\t\t%f\n",localTask.getLambda());

            // print dependency matrix
            printWriter.printf("Dependency Matrix:\n");
            int[][] D = localTask.getD();
            for(int j = 0; j < localTask.getSubtaskList().size(); j++){
                if(j == 0){ printWriter.printf("   [");}
                else{ printWriter.printf("\t");}

                for(int k = 0; k < localTask.getSubtaskList().size(); k++){
                    printWriter.printf("%d", D[j][k]);
                    if(k != (localTask.getSubtaskList().size() - 1)) { printWriter.printf("\t"); }
                }

                if(j != (localTask.getSubtaskList().size() - 1) ) { printWriter.printf("\n"); }
                else{printWriter.printf("]\n\n");}
            }


            // print time correlation matrix
            printWriter.printf("Time Constraint Matrix:\n");
            double[][] T = localTask.getT();
            for(int j = 0; j < localTask.getSubtaskList().size(); j++){
                if(j == 0){ printWriter.printf("   [");}
                else{ printWriter.printf("\t");}

                for(int k = 0; k < localTask.getSubtaskList().size(); k++){
                    printWriter.printf("%f", T[j][k]);
                    if(k != (localTask.getSubtaskList().size() - 1)) { printWriter.printf("\t"); }
                }

                if(j != (localTask.getSubtaskList().size() - 1) ) { printWriter.printf("\n"); }
                else{printWriter.printf("]\n\n");}
            }

            // prepare for next task to print
            printWriter.print("\n");
            printWriter.print("**********************************");
            printWriter.print("\n");
        }


        //close file
        printWriter.close();
    }

    private void printMetrics() throws Exception {
        //create new file in directory
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = this.directoryAddress + "/performance_metrics.out";
        fileWriter = null;
        try {
            fileWriter = new FileWriter( outAddress, false );
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        // overall performance metrics
        //obtain values
        ArrayList resultsToPrint = new ArrayList();

        double coalitionsFormed = calcCoalitionsFormed();
        double coalitionsAvailable = countCoalitionsAvailable();
        double scoreAchieved = calcScoreAchieved();
        double scoreAvailable = calcScoreAvailable();
        double costPerResourcesPerAent = calcAvgCostPerResources();
        double mergeCost = this.receivedResults.get(0).getParentAgent().getResources().getC_merge();
        double splitCost = this.receivedResults.get(0).getParentAgent().getResources().getC_split();
        int numberOfTasksDone = countTasksDone();
        int planHorizon = this.receivedResults.get(0).getParentAgent().getM();
        int count1 = countTasksDoneWithSensors(1);
        int count2 = countTasksDoneWithSensors(2);
        int count3 = countTasksDoneWithSensors(3);

        resultsToPrint.add(coalitionsFormed);
        resultsToPrint.add(coalitionsAvailable);
        resultsToPrint.add(scoreAchieved);
        resultsToPrint.add(scoreAvailable);
        resultsToPrint.add(costPerResourcesPerAent);
        resultsToPrint.add(mergeCost);
        resultsToPrint.add(splitCost);
        resultsToPrint.add(numberOfTasksDone);
        resultsToPrint.add(planHorizon);
        resultsToPrint.add(count1);
        resultsToPrint.add(count2);
        resultsToPrint.add(count3);

        //print values
        for (Object result : resultsToPrint) {
            printWriter.print(result);
            printWriter.print("\t");
        }



        //close file
        printWriter.close();
    }

    private double calcScoreAchieved(){
        double count = 0.0;

        for(IterationDatum datum : receivedResults.get(0).getResults()){
            count += datum.getScore();
        }

        return count;
    }

    private double calcScoreAvailable(){
        double count = 0;
        for(Task V : environment.getScenarioTasks()){
            count += (V.getS_Max() * V.getReq_sensors().size());
        }
        return count;
    }

    private double calcAvgCostPerResources( ){
        ArrayList<SimulatedAgent> agentList = new ArrayList<>();
        ArrayList<Double> costPerAgent = new ArrayList<>();
        ArrayList<Integer> bidsPerAgent = new ArrayList<>();
        SimulatedAgent localZ;
        double localCost;
        double avg = 0.0;

        for (int i = 0; i < receivedResults.get(0).size(); i++) {
            localZ = receivedResults.get(0).getIterationDatum(i).getZ();
            localCost = receivedResults.get(0).getIterationDatum(i).getCost();
            if ((localZ != null) && (!agentList.contains(localZ))) {
                // add agent to list of averages
                agentList.add(localZ);
                costPerAgent.add( localCost );
                bidsPerAgent.add(1);
            }
            else if( (localZ != null) && (agentList.contains(localZ)) ){
                // agent exists in list, add its cost
                int i_list = agentList.indexOf(localZ);
                costPerAgent.set(i_list, costPerAgent.get(i_list) + localCost);
                bidsPerAgent.set(i_list, bidsPerAgent.get(i_list) + 1);
            }
        }

        for(int i = 0; i < agentList.size(); i++){
            costPerAgent.set( i, costPerAgent.get(i)/agentList.get(i).getInitialResources().getValue());
//            costPerAgent.set( i, costPerAgent.get(i)/ (double) bidsPerAgent.get(i));
        }
        for(int i = 0; i < agentList.size(); i++){
            avg += costPerAgent.get(i);
        }

        return avg / agentList.size();
    }

    private int countTasksAvailable(){
        return this.environment.getScenarioTasks().size();
    }

    private int countCoalitionsAvailable() throws Exception {
        int count = 0;
        ArrayList<Task> V = this.environment.getScenarioTasks();
        for(Task v : V){
            if(v.getReq_sensors().size() == 1){
                count += 0;
            }
            else if(v.getReq_sensors().size() == 2){
                count += 1;
            }
            else if(v.getSubtaskList().size()  == 3){
                count += 4;
            }
            else{
                throw new Exception("Input error: tasks with over three required sensors not supported");
            }
        }

        return count;
    }

    private int calcCoalitionsFormed() throws Exception {
        int count = 0;
        ArrayList<Task> V = environment.getScenarioTasks();
        IterationResults results = receivedResults.get(0);

        for(Task task : V){
            // check if it has more than winner
            ArrayList<SimulatedAgent> taskWinners = new ArrayList<>();
            SimulatedAgent localWinner;

            for(Subtask j : task.getSubtaskList()){
                localWinner = results.getIterationDatum(j).getZ();
                if( (localWinner != null) && (!taskWinners.contains(localWinner)) ){
                    taskWinners.add(localWinner);
                }
            }
            if(taskWinners.size() > 1) count++;
        }
        return count;
    }

    private int countTasksDone(){
        ArrayList<Task> V = environment.getScenarioTasks();
        int count = 0;

        for(Task v : V){
            if( v.getCompleteness() ){
                count++;
            }
        }

        return count;
    }

    private int countTasksDoneWithSensors(int n) throws Exception {
        ArrayList<Task> V = environment.getScenarioTasks();
        int count = 0;

        for(Task v : V){
            if(v.getCompleteness()){
                for(Subtask j : v.getSubtaskList()){
                    SimulatedAgent z = this.receivedResults.get(0).getIterationDatum(j).getZ();
                    if( (z != null) && (j.getDep_tasks().size() + 1 == n) ){
                        count++;
                        break;
                    }
                }
            }
        }

        return count;
    }

    private void printReport() throws Exception {
        //create new file in directory
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = this.directoryAddress + "/REPORT.out";
        fileWriter = null;
        try {
            fileWriter = new FileWriter( outAddress, false );
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        //obtain values
        //- Agents
        ArrayList<IterationResults> resultsToPrint = this.receivedResults;
        double x = 0.0;   double y = 0.0;   double z = 0.0;

        //- Tasks
        ArrayList<Task> tasksToPrint = this.environment.getScenarioTasks();

        //- Winning Vectors
        ArrayList<ArrayList> winnersToPrint = new ArrayList();

        ArrayList<Subtask> localJ = new ArrayList<>();
        ArrayList<Double> localY = new ArrayList<>();
        ArrayList<Double> localTz = new ArrayList<>();
        ArrayList<SimulatedAgent> localZ = new ArrayList<>();
        ArrayList<Integer> localS = new ArrayList<>();

        for(IterationDatum datum : receivedResults.get(0).getResults()){
            localJ.add(datum.getJ());
            localY.add(datum.getY());
            localTz.add(datum.getTz());
            localZ.add(datum.getZ());
            localS.add(datum.getS());
        }

        winnersToPrint.add(localY);
        winnersToPrint.add(localTz);
        winnersToPrint.add(localZ);
        winnersToPrint.add(localJ);
        winnersToPrint.add(localS);

        //- Metrics
        int coalitionsFormed = calcCoalitionsFormed();
        int coalitionsAvailable = countCoalitionsAvailable();
        double scoreAchieved = calcScoreAchieved();
        double scoreAvailable = calcScoreAvailable();
        double costPerResourcesPerAgent = calcAvgCostPerResources();
        double mergeCost = this.receivedResults.get(0).getParentAgent().getResources().getC_merge();
        double splitCost = this.receivedResults.get(0).getParentAgent().getResources().getC_split();
        int numberOfTasksDone = countTasksDone();
        int numberOfTasksAvailable = countTasksAvailable();
        int planHorizon = this.receivedResults.get(0).getParentAgent().getM();


        //print values
        //- Winning Vectors
        printWriter.print("__________________________________");
        printWriter.print("\n");
        printWriter.print("          WINNING VECTORS         ");
        printWriter.print("\n");
        printWriter.print("__________________________________");
        printWriter.print("\n");

        ArrayList<Task> printedTasks = new ArrayList<>();
        ArrayList<Integer> dividerIndex = new ArrayList<>();
        int i_d;

        printWriter.printf("Subtasks:\t\t\t[");
        for(int i = 0; i < localJ.size(); i++ ){
            Task tempTask = localJ.get(i).getParentTask();

            if(i == 0) {
                printedTasks.add(tempTask);
            }
            else {
                if ((!printedTasks.contains(tempTask))) {
                    printedTasks.add(tempTask);
                    printWriter.printf("\t|\t");
                    dividerIndex.add(i);
                } else if (i <= localJ.size() - 1) {
                    printWriter.printf("\t\t");
                }
            }

            if(localJ.get(i).getDep_tasks().isEmpty()){ printWriter.printf("%s_{}", localJ.get(i).getMain_task()); }
            else{ printWriter.printf("%s", localJ.get(i).getName()); }

            if(i == localJ.size()-1){ printWriter.printf("\t]\n");}
        }

        i_d = 0;
        printWriter.printf("Bids Score:\t\t\t[");
        for(int i = 0; i < winnersToPrint.get(0).size(); i++){
            if(i != winnersToPrint.get(0).size()-1) {
                printWriter.printf("%.3f", winnersToPrint.get(0).get(i));
                if( (dividerIndex.size() > i_d) && (i == dividerIndex.get(i_d)-1) ){
                    printWriter.printf("\t|\t");
                    i_d++;
                    continue;
                }
                printWriter.printf("\t\t");
            }
            else{
                printWriter.printf("%.3f\t]\n", winnersToPrint.get(0).get(i));
            }
        }

        i_d = 0;
        printWriter.printf("Measurement Time:\t[");
        for(int i = 0; i < winnersToPrint.get(1).size(); i++){
            if(i != winnersToPrint.get(1).size()-1) {
                printWriter.printf("%.2f", winnersToPrint.get(1).get(i));
                if( (dividerIndex.size() > i_d) && (i == dividerIndex.get(i_d)-1) ){
                    printWriter.printf("\t|\t");
                    i_d++;
                    continue;
                }
                printWriter.printf("\t\t");
            }
            else{
                printWriter.printf("%.3f\t]\n", winnersToPrint.get(1).get(i));
            }
        }

        i_d = 0;
        printWriter.printf("Iteration Assigned:\t[");
        for(int i = 0; i < winnersToPrint.get(4).size(); i++){
            int winnerTemp = localS.get(i);
            if(i != winnersToPrint.get(4).size()-1) {
                printWriter.printf("%d", winnerTemp);
                if( (dividerIndex.size() > i_d) && (i == dividerIndex.get(i_d)-1) ){
                    if(winnerTemp > 1000){
                        printWriter.printf("\t|\t");
                    }
                    else{
                        printWriter.printf("\t\t|\t");
                    }
                    i_d++;
                    continue;
                }
                if(winnerTemp > 999){
                    printWriter.printf("\t\t");
                }
                else{
                    printWriter.printf("\t\t\t");
                }
            }
            else{
                printWriter.printf("%d\t\t]\n", winnerTemp);
            }
        }

        i_d = 0;
        printWriter.printf("Winners:\t\t\t[");
        int winnerTemp;
        ArrayList<SimulatedAgent> agentList = getListOfAgents();

        for(int i = 0; i < winnersToPrint.get(2).size(); i++){
            winnerTemp = agentList.indexOf(winnersToPrint.get(2).get(i)) + 1;
            if( winnerTemp == 0){ printWriter.printf("-"); }
            else{ printWriter.printf("%d", winnerTemp); }

            if(i != winnersToPrint.get(1).size()-1) {
                if( (dividerIndex.size() > i_d) && (i == dividerIndex.get(i_d)-1) ){
                    printWriter.printf("\t\t|\t");
                    i_d++;
                    continue;
                }
                printWriter.printf("\t\t\t");
            }
            else{ printWriter.printf("\t\t]\n"); }
        }

        //- Performance Metrics
        printWriter.printf("\n");
        printWriter.print("__________________________________");
        printWriter.print("\n");
        printWriter.print("        PERFORMANCE METRICS       ");
        printWriter.print("\n");
        printWriter.print("__________________________________");
        printWriter.print("\n");

        printWriter.printf("Coalitions Formed:\t\t%d\n", coalitionsFormed);
        printWriter.printf("Coalitions Available:\t%d\n", coalitionsAvailable);
        printWriter.printf("Coalition Ratio:\t\t%.2f%%\n", (double) coalitionsFormed/ (double) coalitionsAvailable * 100.0);
        printWriter.printf("\n");
        printWriter.printf("Score Achieved:\t\t\t%.3f\n", scoreAchieved);
        printWriter.printf("Score Available:\t\t%.3f\n", scoreAvailable);
        printWriter.printf("Score Ratio:\t\t\t%.2f%%\n", scoreAchieved / scoreAvailable * 100.0);
        printWriter.printf("\n");
        printWriter.printf("Merge Cost:\t\t\t\t%f\n", mergeCost);
        printWriter.printf("Split Cost:\t\t\t\t%f\n", splitCost);
        printWriter.printf("\n");
        printWriter.printf("Avg Cost per Research:\t%.2f%%\n", costPerResourcesPerAgent * 100.0);
        printWriter.printf("\n");
        printWriter.printf("Tasks Done:\t\t\t\t%d\n", numberOfTasksDone);
        printWriter.printf("Tasks Available:\t\t%d\n", numberOfTasksAvailable);
        printWriter.printf("Tasks Done Ratio:\t\t%.2f%%\n", (double) numberOfTasksDone / (double) numberOfTasksAvailable * 100.0);
        printWriter.printf("\n");
        printWriter.printf("Tasks done w/ 1 sens:\t%d\n", countTasksDoneWithSensors(1) );
        printWriter.printf("Tasks done w/ 2 sens:\t%d\n", countTasksDoneWithSensors(2) );
        printWriter.printf("Tasks done w/ 3 sens:\t%d\n", countTasksDoneWithSensors(3) );
        printWriter.printf("\n");
        printWriter.printf("Planning Horizon:\t\t%d\n", planHorizon);

        //- Agents
        for(int i = 0; i < resultsToPrint.size(); i++){
            if(i == 0) {
                printWriter.printf("\n");
                printWriter.print("__________________________________");
                printWriter.print("\n");
                printWriter.print("           AGENT LIST             ");
                printWriter.print("\n");
                printWriter.print("__________________________________");
                printWriter.print("\n");
            }
            else {
                printWriter.print("\n");
                printWriter.print("**********************************");
                printWriter.print("\n");
            }

            IterationResults localResult = resultsToPrint.get(i);
            SimulatedAgent localAgent = localResult.getParentAgent();
            x = localAgent.getInitialPosition().get(0);
            y = localAgent.getInitialPosition().get(1);
            z = localAgent.getInitialPosition().get(2);

            // print basic info
            printWriter.printf("Agent Number:\t\t\t%d\n",i+1);
            printWriter.printf("Planning Horizon:\t\t%d\n",localAgent.getM());
            printWriter.printf("Max Const Iteration:\t%d\n", localAgent.getMaxItersInViolation());
            printWriter.printf("Max Solo Bids:\t\t\t%d\n",localAgent.getW_solo());
            printWriter.printf("Max Any Bids:\t\t\t%d\n",localAgent.getW_any());
            printWriter.printf("Iteration Counter:\t\t%d\n",localAgent.getZeta());
            printWriter.printf("Final Resources:\t\t%f\n\n",localAgent.getResources().getValue());



            // print component lists
            printWriter.printf("Location:\t\t\t\t[%.3f\t\t%.3f\t\t%.3f]\n", x, y, z);
            printWriter.printf("Sensor List:\t\t\t%s\n", localAgent.getSensorList());
            printWriter.printf("Bundle:\t\t\t\t\t[");
            for(int j = 0; j < localAgent.getOverallBundle().size(); j++) {
                Subtask bundleTask = localAgent.getOverallBundle().get(j);
                int i_j = localAgent.getLocalResults().getResults().indexOf( localAgent.getLocalResults().getIterationDatum(bundleTask) );
                printWriter.printf("%d", i_j);
                if (j < localAgent.getOverallBundle().size() - 1){
                    if (i_j < 100) {
                        printWriter.printf("\t\t\t");
                    } else {
                        printWriter.printf("\t\t");
                    }
                }
            }
            printWriter.printf("]\n");
            printWriter.printf("Path:\t\t\t\t\t[");
            for(int j = 0; j < localAgent.getOverallPath().size(); j++){
                Subtask bundleTask = localAgent.getOverallPath().get(j);
                int i_j =
                        localAgent.getLocalResults().getResults().indexOf( localAgent.getLocalResults().getIterationDatum(bundleTask) );
                printWriter.printf("%d", i_j);
                if (j < localAgent.getOverallPath().size() - 1){
                    if (i_j < 100) {
                        printWriter.printf("\t\t\t");
                    } else {
                        printWriter.printf("\t\t");
                    }
                }
            }
            printWriter.printf("]\n");
            printWriter.printf("X-Path:\n[\t");
            for(int j = 0; j < localAgent.getOverallX_path().size(); j++){
                if( j != 0){ printWriter.printf("\t"); }
                ArrayList<Double> taskLocation = localAgent.getOverallX_path().get(j);
                printWriter.printf("%.3f\t%.3f\t%.3f,", taskLocation.get(0), taskLocation.get(1), taskLocation.get(2));
                if( j != localAgent.getOverallX_path().size()-1 ){ printWriter.printf("\n"); }
            }
            printWriter.printf("]\n");
            printWriter.printf("Time Assignments:\t\t[");
            for(int j = 0; j < localAgent.getOverallPath().size(); j++){
                Subtask pathTask = localAgent.getOverallPath().get(j);
                int i_p = localJ.indexOf(pathTask);
                printWriter.printf("%.3f", localTz.get(i_p));
                if(j != (localAgent.getOverallPath().size() - 1) ) {
                    if(localTz.get(i_p) > 100){
                        printWriter.printf("\t");
                    }
                    else{
                        printWriter.printf("\t\t");
                    }
                }
            }
            printWriter.printf("]\n");
//            printWriter.printf("Doing Iterations:\t\t[");
//            for(int j = 0; j < localAgent.getDoingIterations().size(); j++){
//                int i_p = localAgent.getDoingIterations().get(j);
//                printWriter.printf("%d", i_p);
//                if(j != (localAgent.getOverallPath().size() - 1) ) { printWriter.printf("\t\t"); }
//            }
//            printWriter.printf("]\n\n");
        }

        //- Tasks
        x = 0.0;   y = 0.0;   z = 0.0;
        for(int i = 0; i < tasksToPrint.size(); i++) {
            if (i == 0) {
                printWriter.print("__________________________________");
                printWriter.print("\n");
                printWriter.print("            TASK LIST             ");
                printWriter.print("\n");
                printWriter.print("__________________________________");
                printWriter.print("\n");
            } else {
                printWriter.print("\n");
                printWriter.print("**********************************");
                printWriter.print("\n");
            }

            Task localTask = tasksToPrint.get(i);
            x = localTask.getLocation().get(0);
            y = localTask.getLocation().get(1);
            z = localTask.getLocation().get(2);

            double cost = localTask.getCost();

            // print basic info
            printWriter.printf("Task Number:\t\t%d\n", i);
            printWriter.printf("Maximum Score:\t\t%f\n", localTask.getS_Max());
            printWriter.printf("Subtask Cost:\t\t%f\n", cost);
            printWriter.printf("Number of Sensors:\t%d\n", localTask.getI());
            printWriter.printf("Number of Subtasks:\t%d\n\n", localTask.getSubtaskList().size());

            // print component lists
            printWriter.printf("Location:\t\t\t[%f, %f, %f]\n", x, y, z);
            printWriter.printf("Sensor List:\t\t%s\n", localTask.getReq_sensors());
            printWriter.printf("Subtask List:\t\t[");
            for (int j = 0; j < localTask.getSubtaskList().size(); j++) {
                printWriter.printf("%s_{", localTask.getSubtaskList().get(j).getMain_task());
                for (int k = 0; k < localTask.getSubtaskList().get(j).getDep_tasks().size(); k++) {
                    printWriter.printf("%s", localTask.getSubtaskList().get(j).getDep_tasks().get(k));
                    if (k != (localTask.getSubtaskList().get(j).getDep_tasks().size() - 1)) {
                        printWriter.printf(", ");
                    }
                }
                if (j == (localTask.getSubtaskList().size() - 1)) {
                    printWriter.printf("}");
                } else {
                    printWriter.printf("}, ");
                }
            }
            printWriter.printf("]\n\n");

            // print time constraints
            printWriter.printf("Time Constraints:\n");
            printWriter.printf("Start Time:\t\t%f\n", localTask.getT_start());
            printWriter.printf("End Time:\t\t%f\n", localTask.getT_end());
            printWriter.printf("Task Duration:\t%f\n", localTask.getDuration());
            printWriter.printf("Corr Time:\t\t%f\n", localTask.getT_corr());
            printWriter.printf("Lambda:\t\t\t%f\n", localTask.getLambda());

            // print dependency matrix
            printWriter.printf("Dependency Matrix:\n");
            int[][] D = localTask.getD();
            for (int j = 0; j < localTask.getSubtaskList().size(); j++) {
                if (j == 0) {
                    printWriter.printf("   [");
                } else {
                    printWriter.printf("\t");
                }

                for (int k = 0; k < localTask.getSubtaskList().size(); k++) {
                    printWriter.printf("%d", D[j][k]);
                    if (k != (localTask.getSubtaskList().size() - 1)) {
                        printWriter.printf("\t");
                    }
                }

                if (j != (localTask.getSubtaskList().size() - 1)) {
                    printWriter.printf("\n");
                } else {
                    printWriter.printf("]\n\n");
                }
            }


            // print time correlation matrix
            printWriter.printf("Time Constraint Matrix:\n");
            double[][] T = localTask.getT();
            for (int j = 0; j < localTask.getSubtaskList().size(); j++) {
                if (j == 0) {
                    printWriter.printf("   [");
                } else {
                    printWriter.printf("\t");
                }

                for (int k = 0; k < localTask.getSubtaskList().size(); k++) {
                    printWriter.printf("%f", T[j][k]);
                    if (k != (localTask.getSubtaskList().size() - 1)) {
                        printWriter.printf("\t");
                    }
                }

                if (j != (localTask.getSubtaskList().size() - 1)) {
                    printWriter.printf("\n");
                } else {
                    printWriter.printf("]\n\n");
                }
            }
        }

        //close file
        printWriter.close();
    }

    private void printAllVectors( ArrayList<IterationResults> receivedResults ){
        //create new file in directory
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = this.directoryAddress + "/all_vectors.out";

        try {
            fileWriter = new FileWriter( outAddress, false );
        } catch (IOException e) {
            e.printStackTrace();
        }

        printWriter = new PrintWriter(fileWriter);

        // print values
        ArrayList<SimulatedAgent> agentList = new ArrayList<>();
        for(IterationResults result : receivedResults){
            agentList.add(result.getParentAgent());
        }

        for(int i = 0; i < receivedResults.get(0).size(); i++){
            printWriter.printf("%d\t|\t",i);
            // bid list
            for(IterationResults result : receivedResults){
                // obtain values
                double localY = result.getIterationDatum(i).getY();
                // print values
                printWriter.printf("%f\t", localY);
            }

            // time assignments list
            for(IterationResults result : receivedResults){
                if(receivedResults.indexOf(result) == 0){
                    printWriter.printf("|\t");
                }
                // obtain values
                double localTz = result.getIterationDatum(i).getTz();
                // print values
                printWriter.printf("%f\t", localTz);
            }

            // winners
            for(IterationResults result : receivedResults){
                if(receivedResults.indexOf(result) == 0){
                    printWriter.printf("|\t");
                }
                // obtain values
                SimulatedAgent localZ = result.getIterationDatum(i).getZ();
                int i_winner = agentList.indexOf( localZ ) + 1;

                // print values
                if(localZ != null) {
                    printWriter.printf("%d\t", i_winner);
                }
                else{
                    printWriter.printf("-\t");
                }
            }

            // costs
            for(IterationResults result : receivedResults){
                if(receivedResults.indexOf(result) == 0){
                    printWriter.printf("|\t");
                }
                // obtain values
                double localCost = result.getIterationDatum(i).getCost();
                // print values
                printWriter.printf("%f\t", localCost);
            }

            // error counters
            for(IterationResults result : receivedResults){
                if(receivedResults.indexOf(result) == 0){
                    printWriter.printf("|\t");
                }
                // obtain values
                int localV = result.getIterationDatum(i).getV();
                // print values
                printWriter.printf("%d\t", localV);
            }
            printWriter.printf("\n");
        }

        //close file
        printWriter.close();
    }

    private ArrayList<SimulatedAgent> getListOfAgents(){
        ArrayList<IterationResults> resultsToPrint = this.receivedResults;
        ArrayList<SimulatedAgent> agentList = new ArrayList<>();
        for(int i = 0; i < resultsToPrint.size(); i++){
            agentList.add(resultsToPrint.get(i).getParentAgent());
        }
        return agentList;
    }
}
