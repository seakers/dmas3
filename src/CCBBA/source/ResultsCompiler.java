package CCBBA;

import madkit.action.SchedulingAction;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Message;
import madkit.message.SchedulingMessage;

import java.io.*;
import java.util.List;
import java.util.Vector;

public class ResultsCompiler extends AbstractAgent {
    private String directoryAddress;
    private int numAgents;
    protected Scenario environment;
    private Vector<IterationResults> receivedResults;

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
        List<Message> receivedMessages = nextMessages(null);
        Vector<IterationResults> receivedResults = new Vector<>();
        for(int i = 0; i < receivedMessages.size(); i++){
            myMessage message = (myMessage) receivedMessages.get(i);
            receivedResults.add(message.myResults);
        }

        if(receivedResults.size() >= this.numAgents){ // checks if every agent has sent finished its tasks.
            // Every agent has finished its tasks
            getLogger().info("Terminating Sim. Saving Results.");

            // print results
            printResults(receivedResults);

            // terminate sim
            SchedulingMessage terminate = new SchedulingMessage(SchedulingAction.SHUTDOWN);
            sendMessage(getAgentWithRole(CCBBASimulation.MY_COMMUNITY, CCBBASimulation.SIMU_GROUP, CCBBASimulation.SCH_ROLE), terminate);
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
        double scoreAvailable = calcScoreAvailale(this.receivedResults);
        double resourcesPerCostPerAgent = calcAvgResourcesPerCost(this.receivedResults);
        double mergeCost = this.receivedResults.get(0).getC_merge();
        double splitCost = this.receivedResults.get(0).getC_split();
        double numberOfTasksDone = countTasksDone(this.receivedResults);
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

            // print basic info
            printWriter.printf("Task Number:\t\t%d\n",i);
            printWriter.printf("Maximum Score:\t\t%f\n",localTask.getS_max());
            printWriter.printf("Subtask Cost:\t\t%f\n", localTask.getCost());
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
            for(int j = 0; j < localAgent.getBundle().size(); j++){
                Subtask bundleTask = localAgent.getBundle().get(j);
                printWriter.printf("%s_{", bundleTask.getMain_task());
                for(int k = 0; k < bundleTask.getDep_tasks().size(); k++){
                    printWriter.printf("%s", bundleTask.getDep_tasks().get(k));
                    if(k != (bundleTask.getDep_tasks().size() - 1) ) { printWriter.printf(", "); }
                }
                if(j == (localAgent.getBundle().size() - 1) ) { printWriter.printf("}"); }
                else{ printWriter.printf("}, "); }
            }
            printWriter.printf("]\n");
            printWriter.printf("Path:\t\t\t\t\t[");
            for(int j = 0; j < localAgent.getPath().size(); j++){
                Subtask pathTask = localAgent.getPath().get(j);
                printWriter.printf("%s_{", pathTask.getMain_task());
                for(int k = 0; k < pathTask.getDep_tasks().size(); k++){
                    printWriter.printf("%s", pathTask.getDep_tasks().get(k));
                    if(k != (pathTask.getDep_tasks().size() - 1) ) { printWriter.printf(", "); }
                }
                if(j == (localAgent.getPath().size() - 1) ) { printWriter.printf("}"); }
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

        //print values

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
        Vector<Task> coalitionList = new Vector<>();

        for(int i = 0; i < receivedResults.size(); i++){
            Vector<Subtask> tempBundle = receivedResults.get(i).getBundle();
            Vector<Vector<SimulatedAbstractAgent>> tempOmega = receivedResults.get(i).getOmega();

            for(int j = 0; j < tempBundle.size(); j++){
                if(tempOmega.get(j).size() > 0){                        // if the task in the bundle has a coalition partner
                    if(!coalitionList.contains(tempBundle.get(j).getParentTask())){     // check if task related to that coalition has been counted
                        coalitionList.add(tempBundle.get(j).getParentTask());           // if not, add coalition to list
                        count++;                                        // increase coalition count
                    }
                }
            }
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

    private double countTasksAvailable(){
        Vector<Task> taskList = this.environment.getTasks(); // <- TEMPORARY SOLUTION. ONLY VALID FOR VALIDATION SCENARIOS
        return taskList.size()/2.0;
    }
}
