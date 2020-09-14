package modules.planner.CCBBA;

import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.measurements.MeasurementPerformance;
import modules.spacecraft.maneuvers.Maneuver;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;

public class Bid {
    private Subtask j;                              // subtask being bid on
    private double c = 0.0;                         // utility bid
    private double cost = -1.0;                     // cost bid
    private double score = -1.0;                    // score bid
    private AbsoluteDate t = new AbsoluteDate();    // date of measurement
    private int i_path = -1;                        // location in path
    private Maneuver maneuver;                      // maneuver to perform task j
    private PathUtility winningPathUtility;         // path utility description for winning bid and path

    public Bid(Subtask j){
        this.j = j;
    }

    public void calcBid(Spacecraft parentSpacecraft, CCBBAPlanner planner) throws Exception {
        ArrayList<Subtask> oldPath = planner.getPath();

        // Calculate the utility for the existing path
        PathUtility oldPathUtility = new PathUtility(parentSpacecraft, planner, oldPath);

        // Generate new paths
        ArrayList<ArrayList<Subtask>> possiblePaths = generateNewPaths(oldPath,j);

        // Calculate the utility for each possible path and pick the one with the highest utility
        double maxPathBid = 0.0;
        for(ArrayList<Subtask> newPath : possiblePaths){
            PathUtility newPathUtility = new PathUtility(parentSpacecraft, planner, newPath);

            double newPathBid = newPathUtility.getPathUtility() - oldPathUtility.getPathUtility();

            if(newPath.indexOf(j) != newPath.size()-1) newPathBid -= 10.0; // if new bid disrupts previously agreed plan, add penalty

            if(newPathBid > maxPathBid){
                this.c = newPathBid;
                this.cost = newPathUtility.getPathCost() - oldPathUtility.getPathCost();
                this.score = newPathUtility.getPathScore() - oldPathUtility.getPathScore();
                this.i_path = newPath.indexOf(j);
                this.t = newPathUtility.getTz().get(i_path);
                this.maneuver = newPathUtility.getManeuvers().get(i_path);
                this.winningPathUtility = newPathUtility.copy();
                maxPathBid = newPathBid;
            }
        }
    }

    public ArrayList<ArrayList<Subtask>> generateNewPaths(ArrayList<Subtask> oldPath, Subtask j){
        ArrayList<ArrayList<Subtask>> newPaths = new ArrayList<>();

        for(int i = 0; i < (oldPath.size()+1); i++){
            ArrayList<Subtask> tempPath = new ArrayList<>(oldPath);
            tempPath.add(i,j);
            newPaths.add(tempPath);
        }

        return newPaths;
    }

    public Subtask getJ() {
        return j;
    }

    public void setJ(Subtask j) {
        this.j = j;
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public AbsoluteDate getT() {
        return t;
    }

    public void setT(AbsoluteDate t) {
        this.t = t.getDate();
    }

    public int getI_path() {
        return i_path;
    }

    public void setI_path(int i_path) {
        this.i_path = i_path;
    }

    public ArrayList<Subtask> getPath(){ return this.winningPathUtility.getPath(); }
    public PathUtility getWinningPathUtility(){ return  this.winningPathUtility; }
    public ArrayList<Subtask> getWinnerPath(){return this.winningPathUtility.getPath(); }
    public ArrayList<Maneuver> getManeuvers(){return this.winningPathUtility.getManeuvers(); }
    public ArrayList<MeasurementPerformance> getPerformanceList(){return this.winningPathUtility.getPerformanceList();}
    public ArrayList<ArrayList<Instrument>> getInstrumentsUsed(){return this.winningPathUtility.getInstrumentsUsed();}
}
