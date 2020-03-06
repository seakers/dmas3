package CCBBA.lib;

import java.util.ArrayList;

import static java.lang.Math.*;

public class SubtaskBid {
    private Subtask j_a;                // subtask to be bid on
    private double c;                   // self bid
    private double tz;                   // subtask start time
    private ArrayList<Double> x;        // position of measurement
    private int i_opt;                  // index of optimal path
    private double cost;                // cost of task
    private double score;               // score of a task
    private PathUtility winnerPathUtility;
    private ArrayList<Subtask> winnerPath;

    public SubtaskBid(Subtask j){
        this.j_a = j;
        this.c = 0.0;
        this.tz = 0.0;
        this.x = new ArrayList<>();
        this.i_opt = 0;
        this.cost = 0.0;
        this.score = 0.0;
        this.winnerPathUtility = new PathUtility();
        this.winnerPath = new ArrayList<>();
    }

    public void calcSubtaskBid(Subtask j, SimulatedAgent agent, double pathPenalty) throws Exception {
        this.j_a = j;

        ArrayList<Subtask> oldPath = agent.getPath();
        PathUtility oldUtility = calcPathUtility(oldPath, agent);

        ArrayList<ArrayList<Subtask>> possiblePaths = generateNewPaths(oldPath, j);

        // find optimal placement in path
        double maxPathBid = 0.0;

        for (int i = 0; i < possiblePaths.size(); i++) { // Calculate utility for each new path
            // get new path and calc utility
            ArrayList<Subtask> newPath = possiblePaths.get(i);
            PathUtility newPathUtility = calcPathUtility(newPath, agent);

            // substract path utilities to obtain subtask utility
            double newPathBid = newPathUtility.getUtility() - oldUtility.getUtility();
            if(i != possiblePaths.size()-1){  // if path modifies previously agreed order, deduct points
                newPathBid = newPathBid - pathPenalty;
            }

            //get max bid from all new paths
            if(newPathBid > maxPathBid){
                maxPathBid = newPathBid;
                this.c = newPathBid;
                this.tz = newPathUtility.getTz().get( newPath.indexOf(j) );
                this.x = newPathUtility.getX().get( newPath.indexOf(j) );
                this.i_opt = newPath.indexOf(j);
                this.cost = newPathUtility.getCost() - oldUtility.getCost();
                this.score = newPathUtility.getScore() - oldUtility.getScore();
                this.winnerPathUtility = new PathUtility(newPathUtility);
                this.winnerPath = new ArrayList<>(); this.winnerPath.addAll(newPath);
            }
        }
    }

    private ArrayList<ArrayList<Subtask>> generateNewPaths(ArrayList<Subtask> oldPath, Subtask j){
        ArrayList<ArrayList<Subtask>> newPaths = new ArrayList<>();

        for(int i = 0; i < (oldPath.size()+1); i++){
            ArrayList<Subtask> tempPath = new ArrayList<>();
            tempPath.addAll(oldPath);
            tempPath.add(i,j);
            newPaths.add(tempPath);
        }

        return newPaths;
    }

    private PathUtility calcPathUtility(ArrayList<Subtask> path, SimulatedAgent agent) throws Exception {
        PathUtility pathUtility = new PathUtility();

        // calculate path's coalition matrix - omega
        ArrayList<ArrayList<SimulatedAgent>> pathOmega = calcCoalitionMatrix(path, agent);

        // calculate total path utility
        pathUtility.calcPathUtility(path, pathOmega, agent);
        return pathUtility;
    }

    private ArrayList<ArrayList<SimulatedAgent>> calcCoalitionMatrix(ArrayList<Subtask> path, SimulatedAgent agent){
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

        return pathOmega;
    }

    public Subtask getJ_a() { return j_a; }
    public double getC() { return c; }
    public double getTz() { return tz; }
    public ArrayList<Double> getX() { return x; }
    public int getI_opt() { return i_opt; }
    public double getCost() { return cost; }
    public double getScore() { return score; }
    public ArrayList<Subtask> getWinnerPath() { return this.winnerPath; }
    public PathUtility getWinnerPathUtility() { return this.winnerPathUtility; }
}
