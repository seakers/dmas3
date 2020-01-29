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
        PathUtility pathUtility = new PathUtility();
        PathUtility subtaskUtility;

        // calculate path's coalition matrix - omega
        ArrayList<Subtask> localJ = agent.getWorldSubtasks();
        IterationResults localResults = agent.getLocalResults();
        ArrayList<ArrayList<SimulatedAgent>> pathOmega = new ArrayList<>();

        for(int i = 0; i < path.size(); i++) {
            ArrayList<SimulatedAgent> tempCoal = new ArrayList<>();
            for(int i_j = 0; i_j < localResults.size(); i_j++){
                SimulatedAgent tempWinner = localResults.getIterationDatum(i_j).getZ();
                if( (tempWinner != agent)
                    && (tempWinner != null)
                    && (path.get(i).getParentTask() == localResults.getIterationDatum(i_j).getJ().getParentTask()) ){
                    tempCoal.add(tempWinner);
                }

            }

            pathOmega.add(tempCoal);
        }

//        // calculate total path utility
//        for(int i = 0; i < path.size(); i++){
//            Subtask j = path.get(i);
//
//            //Calculate time of arrival
//            double t_a = calcTimeOfArrival(path, j, agent, pathUtility);
//
//            // Calculate subtask utility within path
//            subtaskUtility = calcSubtaskUtility(path,j,t_a, agent, pathOmega);
//
//            // Add to subtask utility to path utility
//            pathUtility.setUtility( pathUtility.getUtility() + subtaskUtility.getUtility() );
//
//            // Add time of arrival to path
//            pathUtility.addTz(t_a);
//        }

        return pathUtility;
    }
}
