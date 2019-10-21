package CCBBA.source;

import madkit.action.SchedulingAction;
import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.SchedulingMessage;
import CCBBA.CCBBASimulation;

import java.io.*;
import java.util.List;
import java.util.Vector;

public class ResultsCompiler extends AbstractAgent {
    private String directoryAddress;
    private int numAgents;
    protected Scenario environment;
    private Vector<IterationResults> receivedResults;
    private Vector<SimulatedAbstractAgent> agentList = new Vector<>();

    public ResultsCompiler(int numAgents, String directoryAddress){
        this.directoryAddress = directoryAddress;
        this.numAgents = numAgents;
    }

    @Override
    protected void activate(){
        requestRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.RESULTS_ROLE);
    }

    private void checkResults() throws IOException {
        // Wait and check for messages
        // TBA

        //Receive results
        List<AgentAddress> resultsAddress = getAgentsWithRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.AGENT_DIE);

        if( (resultsAddress != null)&&(resultsAddress.size() == this.numAgents) ){
            List<Message> receivedMessages = nextMessages(null);
            Vector<IterationResults> receivedResults = new Vector<>();
            Vector<String> senderList = new Vector<>();

            for (int i = 0; i < receivedMessages.size(); i++) {
                myMessage message = (myMessage) receivedMessages.get(i);

                if(!senderList.contains(message.senderName) ) {
                    senderList.add(message.senderName);
                    receivedResults.add(message.myResults);
                }
            }

            if (receivedResults.size() >= this.numAgents) { // checks if every agent has sent finished its tasks.
                // Every agent has finished its tasks
                getLogger().info("Terminating Sim. Saving Results.");

                // print results
                printResults(receivedResults);

                // terminate sim
                SchedulingMessage terminate = new SchedulingMessage(SchedulingAction.SHUTDOWN);
                sendMessage(getAgentWithRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.SCH_ROLE), terminate);
            }
        }
    }

    protected void printResults( Vector<IterationResults> receivedResults ) throws IOException {
        this.receivedResults = receivedResults;
        printWinningVectors();
        printTaskList();
        printAgentList();
        printMetrics();
        printReport();
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
        Vector<Vector> resultsToPrint = new Vector();

        Vector localY = this.receivedResults.get(0).getY();
        Vector localTz = this.receivedResults.get(0).getTz();
        Vector localZ = this.receivedResults.get(0).getZ();

        resultsToPrint.add(localY);
        resultsToPrint.add(localTz);
        resultsToPrint.add(localZ);

        //print values
        for(int i = 0; i < resultsToPrint.size(); i++){
            for(int j = 0; j < resultsToPrint.get(i).size(); j++){
                printWriter.print(resultsToPrint.get(i).get(j));
                printWriter.print("\t");
            }
            printWriter.print("\n");
        }

        //close file
        printWriter.close();
    }

    private void printMetrics(){
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

        //obtain values
        Vector resultsToPrint = new Vector();

        double coalitionsFormed = calcCoalitionsFormed(this.receivedResults);
        double coalitionsAvailable = countTasksAvailable();
        double scoreAchieved = calcScoreAchieved(this.receivedResults);
        double scoreAvailable = calcScoreAvailable(this.receivedResults);
        double resourcesPerCostPerAgent = calcAvgResourcesPerCost(this.receivedResults);
        double mergeCost = this.receivedResults.get(0).getC_merge();
        double splitCost = this.receivedResults.get(0).getC_split();
        int numberOfTasksDone = countTasksDone(this.receivedResults);
        int planHorizon = this.receivedResults.get(0).getM();

        resultsToPrint.add(coalitionsFormed);
        resultsToPrint.add(coalitionsAvailable);
        resultsToPrint.add(scoreAchieved);
        resultsToPrint.add(scoreAvailable);
        resultsToPrint.add(resourcesPerCostPerAgent);
        resultsToPrint.add(mergeCost);
        resultsToPrint.add(splitCost);
        resultsToPrint.add(numberOfTasksDone);
        resultsToPrint.add(planHorizon);

        //print values
        for(int i = 0; i < resultsToPrint.size(); i++){
            printWriter.print(resultsToPrint.get(i));
            printWriter.print("\t");
        }
        printWriter.print("\n");

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
        Vector<Task> resultsToPrint = this.environment.getTasks();
        double x = 0.0;   double y = 0.0;   double z = 0.0;

        //print values
        for(int i = 0; i < resultsToPrint.size(); i++){
            Task localTask = resultsToPrint.get(i);
            x = localTask.getLocation().getHeight();
            y = localTask.getLocation().getWidth();

            double cost;
            double cost_const = localTask.getCostConst();
            double cost_prop = localTask.getCostProp();
            if( (cost_prop > 0.0)&&(cost_const <= 0.0) ){
                cost = cost_prop * 100; //<- FIX PROP
            }
            else{
                cost = cost_const;
            }

            // print basic info
            printWriter.printf("Task Number:\t\t%d\n",i);
            printWriter.printf("Maximum Score:\t\t%f\n",localTask.getS_max());
            printWriter.printf("Subtask Cost:\t\t%f\n", cost);
            printWriter.printf("Number of Sensors:\t%f\n",localTask.getI());
            printWriter.printf("Number of Subtasks:\t%d\n\n",localTask.getJ().size());

            // print component lists
            printWriter.printf("Location:\t\t\t[%f, %f, %f]\n", x, y, z);
            printWriter.printf("Sensor List:\t\t%s\n", localTask.getSensors());
            printWriter.printf("Subtask List:\t\t[");
            for(int j = 0; j < localTask.getJ().size(); j++){
                printWriter.printf("%s_{", localTask.getJ().get(j).getMain_task());
                for(int k = 0; k < localTask.getJ().get(j).getDep_tasks().size(); k++){
                    printWriter.printf("%s", localTask.getJ().get(j).getDep_tasks().get(k));
                    if(k != (localTask.getJ().get(j).getDep_tasks().size() - 1) ) {
                        printWriter.printf(", ");
                    }
                }
                if(j == (localTask.getJ().size() - 1) ) { printWriter.printf("}"); }
                else{ printWriter.printf("}, "); }
            }
            printWriter.printf("]\n\n");

            // print time constraints
            printWriter.printf("Time Constraints:\n");
            printWriter.printf("Start Time:\t\t%f\n",localTask.getTC().get(0));
            printWriter.printf("End Time:\t\t%f\n",localTask.getTC().get(1));
            printWriter.printf("Task Duration:\t%f\n",localTask.getTC().get(2));
            printWriter.printf("Corr Time:\t\t%f\n",localTask.getTC().get(3));
            printWriter.printf("Lambda:\t\t\t%f\n",localTask.getTC().get(4));

            // print dependency matrix
            printWriter.printf("Dependency Matrix:\n");
            int[][] D = localTask.getD();
            for(int j = 0; j < localTask.getJ().size(); j++){
                if(j == 0){ printWriter.printf("   [");}
                else{ printWriter.printf("\t");}

                for(int k = 0; k < localTask.getJ().size(); k++){
                    printWriter.printf("%d", D[j][k]);
                    if(k != (localTask.getJ().size() - 1)) { printWriter.printf("\t"); }
                }

                if(j != (localTask.getJ().size() - 1) ) { printWriter.printf("\n"); }
                else{printWriter.printf("]\n\n");}
            }


            // print time correlation matrix
            printWriter.printf("Time Constraint Matrix:\n");
            double[][] T = localTask.getT();
            for(int j = 0; j < localTask.getJ().size(); j++){
                if(j == 0){ printWriter.printf("   [");}
                else{ printWriter.printf("\t");}

                for(int k = 0; k < localTask.getJ().size(); k++){
                    printWriter.printf("%f", T[j][k]);
                    if(k != (localTask.getJ().size() - 1)) { printWriter.printf("\t"); }
                }

                if(j != (localTask.getJ().size() - 1) ) { printWriter.printf("\n"); }
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

    private void printAgentList(){
        //create new file in directory
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = this.directoryAddress + "/agent_list.out";
        fileWriter = null;
        try {
            fileWriter = new FileWriter( outAddress, false );
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        //obtain values
        Vector<IterationResults> resultsToPrint = this.receivedResults;
        double x = 0.0;   double y = 0.0;   double z = 0.0;

        //print values
        for(int i = 0; i < resultsToPrint.size(); i++){
            IterationResults localResult = resultsToPrint.get(i);
            SimulatedAbstractAgent localAgent = localResult.getParentAgent();
            x = localAgent.getInitialPosition().getHeight();
            y = localAgent.getInitialPosition().getWidth();

            // print basic info
            printWriter.printf("Agent Number:\t\t\t%d\n",i);
            printWriter.printf("Planning Horizon:\t\t%d\n",localAgent.getM());
            printWriter.printf("Max Const Iteration:\t%d\n", localAgent.getO_kq());
            printWriter.printf("Max Solo Bids:\t\t\t%d\n",localAgent.getW_solo_max());
            printWriter.printf("Max Any Bids:\t\t\t%d\n",localAgent.getW_any_max());
            printWriter.printf("Iteration Counter:\t\t%d\n",localAgent.getZeta());
            printWriter.printf("Resources:\t\t\t\t%d\n\n",localAgent.getJ().size());



            // print component lists
            printWriter.printf("Location:\t\t\t\t[%f, %f, %f]\n", x, y, z);
            printWriter.printf("Sensor List:\t\t\t%s\n", localAgent.getSensors());
            printWriter.printf("Bundle:\t\t\t\t\t[");
            for(int j = 0; j < localAgent.getOverallBundle().size(); j++){
                Subtask bundleTask = localAgent.getOverallBundle().get(j);
                printWriter.printf("%s_{", bundleTask.getMain_task());
                for(int k = 0; k < bundleTask.getDep_tasks().size(); k++){
                    printWriter.printf("%s", bundleTask.getDep_tasks().get(k));
                    if(k != (bundleTask.getDep_tasks().size() - 1) ) { printWriter.printf(", "); }
                }
                if(j == (localAgent.getOverallBundle().size() - 1) ) { printWriter.printf("}"); }
                else{ printWriter.printf("}, "); }
            }
            printWriter.printf("]\n");
            printWriter.printf("Path:\t\t\t\t\t[");
            for(int j = 0; j < localAgent.getOverallPath().size(); j++){
                Subtask pathTask = localAgent.getOverallPath().get(j);
                printWriter.printf("%s_{", pathTask.getMain_task());
                for(int k = 0; k < pathTask.getDep_tasks().size(); k++){
                    printWriter.printf("%s", pathTask.getDep_tasks().get(k));
                    if(k != (pathTask.getDep_tasks().size() - 1) ) { printWriter.printf(", "); }
                }
                if(j == (localAgent.getOverallPath().size() - 1) ) { printWriter.printf("}"); }
                else{ printWriter.printf("}, "); }
            }
            printWriter.printf("]\n\n");


            // prepare for next task to print
            printWriter.print("\n");
            printWriter.print("**********************************");
            printWriter.print("\n");
        }


        //close file
        printWriter.close();
    }

    private void printReport(){
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
        Vector<IterationResults> resultsToPrint = this.receivedResults;
        double x = 0.0;   double y = 0.0;   double z = 0.0;

        //- Tasks
        Vector<Task> tasksToPrint = this.environment.getTasks();

        //- Winning Vectors
        Vector<Vector> winnersToPrint = new Vector();

        Vector<Subtask> localJ = this.receivedResults.get(0).getJ();
        Vector<Double> localY = this.receivedResults.get(0).getY();
        Vector<Double> localTz = this.receivedResults.get(0).getTz();
        Vector<SimulatedAbstractAgent> localZ = this.receivedResults.get(0).getZ();
        Vector<Integer> localS = this.receivedResults.get(0).getS();

        winnersToPrint.add(localY);
        winnersToPrint.add(localTz);
        winnersToPrint.add(localZ);
        winnersToPrint.add(localJ);
        winnersToPrint.add(localS);

        //- Metrics
        int coalitionsFormed = calcCoalitionsFormed(this.receivedResults);
        int coalitionsAvailable = countTasksAvailable();
        double scoreAchieved = calcScoreAchieved(this.receivedResults);
        double scoreAvailable = calcScoreAvailable(this.receivedResults);
        double resourcesPerCostPerAgent = calcAvgResourcesPerCost(this.receivedResults);
        double mergeCost = this.receivedResults.get(0).getC_merge();
        double splitCost = this.receivedResults.get(0).getC_split();
        int numberOfTasksDone = countTasksDone(this.receivedResults);
        int planHorizon = this.receivedResults.get(0).getM();


        //print values
        //- Winning Vectors
        printWriter.print("__________________________________");
        printWriter.print("\n");
        printWriter.print("          WINNING VECTORS         ");
        printWriter.print("\n");
        printWriter.print("__________________________________");
        printWriter.print("\n");

        Vector<Task> printedTasks = new Vector<>();
        Vector<Integer> dividerIndex = new Vector<>();
        int i_d = 0;

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
                } else if (i < localJ.size() - 1) {
                    printWriter.printf("\t\t");
                }
            }

            if(localJ.get(i).getDep_tasks().isEmpty()){ printWriter.printf("%s_{}", localJ.get(i).getMain_task()); }
            else{ printWriter.printf("%s", localJ.get(i).getName()); }

            if(i == localJ.size()-1){ printWriter.printf("\t]\n");}
        }

        printWriter.printf("Bids Score:\t\t\t[");
        for(int i = 0; i < winnersToPrint.get(0).size(); i++){
            if(i != winnersToPrint.get(0).size()-1) {
                printWriter.printf("%.3f", winnersToPrint.get(0).get(i));
                if(i == dividerIndex.get(i_d)-1){
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
                if(i == dividerIndex.get(i_d)-1){
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
                if(i == dividerIndex.get(i_d)-1){
                    if(winnerTemp > 1000){
                        printWriter.printf("\t|\t");
                    }
                    else{
                        printWriter.printf("\t\t|\t");
                    }
                    i_d++;
                    continue;
                }
                if(winnerTemp > 1000){
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
        Vector<SimulatedAbstractAgent> agentList = getListOfAgents();

        for(int i = 0; i < winnersToPrint.get(2).size(); i++){
            winnerTemp = agentList.indexOf(winnersToPrint.get(2).get(i)) + 1;
            if( winnerTemp == 0){ printWriter.printf("-"); }
            else{ printWriter.printf("%d", winnerTemp); }

            if(i != winnersToPrint.get(1).size()-1) {
                if(i == dividerIndex.get(i_d)-1){
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
        printWriter.printf("Score Ratio:\t\t\t%.2f%%\n", scoreAchieved/ scoreAvailable * 100.0);
        printWriter.printf("\n");
        printWriter.printf("Merge Cost:\t\t\t\t%f\n", mergeCost);
        printWriter.printf("Split Cost:\t\t\t\t%f\n", splitCost);
        printWriter.printf("\n");
        printWriter.printf("Avg Resource por Cost:\t%f\n", resourcesPerCostPerAgent);
        printWriter.printf("\n");
        printWriter.printf("Tasks Done:\t\t\t\t%d\n", numberOfTasksDone);
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
            SimulatedAbstractAgent localAgent = localResult.getParentAgent();
            x = localAgent.getInitialPosition().getHeight();
            y = localAgent.getInitialPosition().getWidth();

            // print basic info
            printWriter.printf("Agent Number:\t\t\t%d\n",i+1);
            printWriter.printf("Planning Horizon:\t\t%d\n",localAgent.getM());
            printWriter.printf("Max Const Iteration:\t%d\n", localAgent.getO_kq());
            printWriter.printf("Max Solo Bids:\t\t\t%d\n",localAgent.getW_solo_max());
            printWriter.printf("Max Any Bids:\t\t\t%d\n",localAgent.getW_any_max());
            printWriter.printf("Iteration Counter:\t\t%d\n",localAgent.getZeta());
            printWriter.printf("Resources:\t\t\t\t%f\n\n",localAgent.readResources());



            // print component lists
            printWriter.printf("Location:\t\t\t\t[%.3f\t\t%.3f\t\t%.3f]\n", x, y, z);
            printWriter.printf("Sensor List:\t\t\t%s\n", localAgent.getSensors());
            printWriter.printf("Bundle:\t\t\t\t\t[");
            for(int j = 0; j < localAgent.getOverallBundle().size(); j++){
                Subtask bundleTask = localAgent.getOverallBundle().get(j);
                printWriter.printf("%s_{", bundleTask.getMain_task());
                for(int k = 0; k < bundleTask.getDep_tasks().size(); k++){
                    printWriter.printf("%s", bundleTask.getDep_tasks().get(k));
                    if(k != (bundleTask.getDep_tasks().size() - 1) ) { printWriter.printf(", "); }
                }
                if(j == (localAgent.getOverallBundle().size() - 1) ) { printWriter.printf("}"); }
                else{ printWriter.printf("}\t\t"); }
            }
            printWriter.printf("]\n");
            printWriter.printf("Path:\t\t\t\t\t[");
            for(int j = 0; j < localAgent.getOverallPath().size(); j++){
                Subtask pathTask = localAgent.getOverallPath().get(j);
                printWriter.printf("%s_{", pathTask.getMain_task());
                for(int k = 0; k < pathTask.getDep_tasks().size(); k++){
                    printWriter.printf("%s", pathTask.getDep_tasks().get(k));
                    if(k != (pathTask.getDep_tasks().size() - 1) ) { printWriter.printf(", "); }
                }
                if(j == (localAgent.getOverallPath().size() - 1) ) { printWriter.printf("}"); }
                else{ printWriter.printf("}\t\t"); }
            }
            printWriter.printf("]\n");
            printWriter.printf("Time Assignments:\t\t[");
            for(int j = 0; j < localAgent.getOverallPath().size(); j++){
                Subtask pathTask = localAgent.getOverallPath().get(j);
                int i_p = localJ.indexOf(pathTask);
                printWriter.printf("%.3f", localTz.get(i_p));
                if(j != (localAgent.getOverallPath().size() - 1) ) {
                    if(localTz.get(i_p) > 100){
                        printWriter.printf("\t\t");
                    }
                    else{
                        printWriter.printf("\t\t\t");
                    }
                }
            }
            printWriter.printf("]\n");
            printWriter.printf("Doing Iterations:\t\t[");
            for(int j = 0; j < localAgent.getDoingIterations().size(); j++){
                int i_p = localAgent.getDoingIterations().get(j);
                printWriter.printf("%d", i_p);
                if(j != (localAgent.getOverallPath().size() - 1) ) { printWriter.printf("\t\t"); }
            }
            printWriter.printf("]\n\n");
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
            x = localTask.getLocation().getHeight();
            y = localTask.getLocation().getWidth();

            double cost;
            double cost_const = localTask.getCostConst();
            double cost_prop = localTask.getCostProp();
            if ((cost_prop > 0.0) && (cost_const <= 0.0)) {
                cost = cost_prop * 100; //<- FIX PROP
            } else {
                cost = cost_const;
            }

            // print basic info
            printWriter.printf("Task Number:\t\t%d\n", i);
            printWriter.printf("Maximum Score:\t\t%f\n", localTask.getS_max());
            printWriter.printf("Subtask Cost:\t\t%f\n", cost);
            printWriter.printf("Number of Sensors:\t%f\n", localTask.getI());
            printWriter.printf("Number of Subtasks:\t%d\n\n", localTask.getJ().size());

            // print component lists
            printWriter.printf("Location:\t\t\t[%f, %f, %f]\n", x, y, z);
            printWriter.printf("Sensor List:\t\t%s\n", localTask.getSensors());
            printWriter.printf("Subtask List:\t\t[");
            for (int j = 0; j < localTask.getJ().size(); j++) {
                printWriter.printf("%s_{", localTask.getJ().get(j).getMain_task());
                for (int k = 0; k < localTask.getJ().get(j).getDep_tasks().size(); k++) {
                    printWriter.printf("%s", localTask.getJ().get(j).getDep_tasks().get(k));
                    if (k != (localTask.getJ().get(j).getDep_tasks().size() - 1)) {
                        printWriter.printf(", ");
                    }
                }
                if (j == (localTask.getJ().size() - 1)) {
                    printWriter.printf("}");
                } else {
                    printWriter.printf("}, ");
                }
            }
            printWriter.printf("]\n\n");

            // print time constraints
            printWriter.printf("Time Constraints:\n");
            printWriter.printf("Start Time:\t\t%f\n", localTask.getTC().get(0));
            printWriter.printf("End Time:\t\t%f\n", localTask.getTC().get(1));
            printWriter.printf("Task Duration:\t%f\n", localTask.getTC().get(2));
            printWriter.printf("Corr Time:\t\t%f\n", localTask.getTC().get(3));
            printWriter.printf("Lambda:\t\t\t%f\n", localTask.getTC().get(4));

            // print dependency matrix
            printWriter.printf("Dependency Matrix:\n");
            int[][] D = localTask.getD();
            for (int j = 0; j < localTask.getJ().size(); j++) {
                if (j == 0) {
                    printWriter.printf("   [");
                } else {
                    printWriter.printf("\t");
                }

                for (int k = 0; k < localTask.getJ().size(); k++) {
                    printWriter.printf("%d", D[j][k]);
                    if (k != (localTask.getJ().size() - 1)) {
                        printWriter.printf("\t");
                    }
                }

                if (j != (localTask.getJ().size() - 1)) {
                    printWriter.printf("\n");
                } else {
                    printWriter.printf("]\n\n");
                }
            }


            // print time correlation matrix
            printWriter.printf("Time Constraint Matrix:\n");
            double[][] T = localTask.getT();
            for (int j = 0; j < localTask.getJ().size(); j++) {
                if (j == 0) {
                    printWriter.printf("   [");
                } else {
                    printWriter.printf("\t");
                }

                for (int k = 0; k < localTask.getJ().size(); k++) {
                    printWriter.printf("%f", T[j][k]);
                    if (k != (localTask.getJ().size() - 1)) {
                        printWriter.printf("\t");
                    }
                }

                if (j != (localTask.getJ().size() - 1)) {
                    printWriter.printf("\n");
                } else {
                    printWriter.printf("]\n\n");
                }
            }
        }


        //close file
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

    private double calcScoreAvailable( Vector<IterationResults> receivedResults){
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
        Vector<Task> V = environment.getTasks();
        Vector<SimulatedAbstractAgent> localZ = receivedResults.get(0).getZ();

        for(Task task : V){
            // check if it has more than winner
            Vector<SimulatedAbstractAgent> taskWinners = new Vector<>();
            int i_j;
            SimulatedAbstractAgent localWinner;
            for(Subtask j : task.getJ()){
                i_j = receivedResults.get(0).getJ().indexOf(j);
                localWinner = localZ.get(i_j);
                if( (localWinner != null) && (!taskWinners.contains(localWinner)) ){
                    taskWinners.add(localWinner);
                }
            }
            if(taskWinners.size() > 1) count++;
        }
        return count;
    }

    private double calcAvgResourcesPerCost( Vector<IterationResults> receivedResults){
        Vector<SimulatedAbstractAgent> agentList = new Vector<>();
        Vector<SimulatedAbstractAgent> localZ = receivedResults.get(0).getZ();
        Vector<Double> cost = receivedResults.get(0).getCost();
        double avg = 0;
        double resources;
        double localCost;

        for (int i = 0; i < localZ.size(); i++) {
            if ((localZ.get(i) != null) && (!agentList.contains(localZ.get(i)))) {
                agentList.add(localZ.get(i));
                resources = localZ.get(i).getResources();
                localCost = 0;

                for (int j = i; j < localZ.size(); j++) {
                    if (localZ.get(j) == localZ.get(i)) {
                        localCost = localCost + cost.get(j);
                    }
                }
                avg = avg + resources / localCost;
            }
        }

        return avg / receivedResults.size();

    }

    private int countTasksAvailable(){
        Vector<Task> taskList = this.environment.getTasks(); // <- TEMPORARY SOLUTION. ONLY VALID FOR VALIDATION SCENARIOS
        return taskList.size()/2;
    }

    private Vector<SimulatedAbstractAgent> getListOfAgents(){
        Vector<IterationResults> resultsToPrint = this.receivedResults;
        Vector<SimulatedAbstractAgent> agentList = new Vector<>();
        for(int i = 0; i < resultsToPrint.size(); i++){
            agentList.add(resultsToPrint.get(i).getParentAgent());
        }
        return agentList;
    }
}
