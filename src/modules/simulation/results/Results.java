package modules.simulation.results;

import madkit.kernel.AbstractAgent;
import modules.environment.Subtask;
import modules.environment.Task;
import modules.environment.TaskCapability;
import modules.planner.CCBBA.IterationResults;
import modules.spacecraft.Spacecraft;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class Results {
    private String directoryAddress;
    private OverallResults overallResults;
    private HashMap<Subtask, SubtaskResults> subtaskResults;
    private HashMap<AbstractAgent, AgentResults> agentResults;
    private ArrayList<Subtask> subtasks;

    public Results(ArrayList<Task> environmentTasks, ArrayList<Spacecraft> spaceSegment,
                    HashMap<Task, TaskCapability> capabilities, String directoryAddress) throws Exception {
        this.directoryAddress = directoryAddress;

        // compile all results
        agentResults = new HashMap<>();
        for(AbstractAgent agent : spaceSegment){
            AgentResults agentResult = new AgentResults( (Spacecraft) agent, environmentTasks, capabilities);
            agentResults.put(agent, agentResult);
        }

        subtasks = new ArrayList<>();
        subtaskResults = new HashMap<>();
        for(Task task : environmentTasks){
            for(Subtask j : task.getSubtasks()){
                subtaskResults.put(j, new SubtaskResults(j, capabilities));
                subtasks.add(j);
            }
        }
        overallResults = new OverallResults(environmentTasks, spaceSegment, capabilities, agentResults);

    }
    public void print(){
        // print out to directory
        printSubtasks();
        printAgents();
        printResults();
    }

    private void printSubtasks(){
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

        for(Subtask j : subtasks){
            printWriter.print(subtaskResults.get(j).toString());
        }

        //close file
        printWriter.close();
    }
    private void printAgents(){
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

        for(AbstractAgent a : agentResults.keySet()){
            printWriter.print(agentResults.get(a).toString());
        }

        //close file
        printWriter.close();
    }
    private void printResults(){
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = this.directoryAddress + "/results.out";
        fileWriter = null;
        try {
            fileWriter = new FileWriter( outAddress, false );
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        printWriter.print(overallResults.toString());

        //close file
        printWriter.close();
    }
}
