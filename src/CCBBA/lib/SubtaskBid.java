package CCBBA.lib;

import java.util.ArrayList;

public class SubtaskBid {
    private double c;                   // self bid
    private double t;                   // subtask start time
    private ArrayList<Double> x;        // position of measurement
    private int i_opt;                  // index of optimal path
    private double cost;                // cost of task
    private double score;               // score of a task

    public SubtaskBid(){
        this.c = 0.0;
        this.t = 0.0;
        this.x = new ArrayList<>();
        this.i_opt = 0;
        this.cost = 0;
        this.score = 0.0;
    }

    public void calcSubtaskBid(Subtask j, SimulatedAgent agent){
        ArrayList<Subtask> oldBundle = agent.getBundle();
        ArrayList<Subtask> oldPath = agent.getPath();
        PathUtility oldUtility = calcPathUtility(oldPath, agent);
    }

    private PathUtility calcPathUtility(ArrayList<Subtask> path, SimulatedAgent agent){
        return new PathUtility();
    }
}
