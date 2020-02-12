package CCBBA.lib;

import madkit.kernel.Message;

import java.util.ArrayList;

public class SimResultsMessage extends Message {
    private ArrayList<Subtask> overallBundle;               // list of tasks in agent's past plans
    private ArrayList<Subtask> overallPath;                 // path taken to execute past bundles
    private ArrayList<ArrayList<Double>> overallX_path;     // location of execution of each element in previous bundles
    private ArrayList<ArrayList<SimulatedAgent>> overallOmega; // Coalition mate matrix of previous bundle
    private IterationResults results;


    public SimResultsMessage(SimulatedAgent agent){
        this.results = new IterationResults( agent.getLocalResults(), agent );
        overallBundle = new ArrayList<>();
        overallOmega = new ArrayList<>();
        overallPath = new ArrayList<>();
        overallX_path = new ArrayList<>();

        overallBundle.addAll(agent.getOverallBundle());
        overallOmega.addAll(agent.getOverallOmega());
        overallPath.addAll(agent.getOverallPath());
        overallX_path.addAll(agent.getOverallX_path());
    }

    public ArrayList<Subtask> getOverallBundle() { return overallBundle; }
    public ArrayList<Subtask> getOverallPath() { return overallPath; }
    public ArrayList<ArrayList<Double>> getOverallX_path() { return overallX_path; }
    public ArrayList<ArrayList<SimulatedAgent>> getOverallOmega() { return overallOmega; }
    public IterationResults getResults() { return results; }
}
