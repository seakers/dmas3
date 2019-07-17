package CCBBA;

import java.awt.*;
import java.util.Vector;

import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.StrictMath.sqrt;

public class SubtaskBid {
    protected double c_aj;              // self bid
    protected double t_aj;              // subtask start time
    protected Dimension x_aj;           // location of realization
    protected int i_opt;                // index of optimal path

    public SubtaskBid(){
        Integer inf = Integer.MIN_VALUE;
        c_aj = 0.0;
        t_aj = 0.0;
        x_aj = new Dimension(inf,inf);
        i_opt = 0;
    }

    public void calcBidForSubtask(Subtask j, SimulatedAbstractAgent agent){
        Vector<Subtask> oldBundle = agent.getBundle();
        Vector<Subtask> oldPath = agent.getPath();

        double maxPathBid = Double.NEGATIVE_INFINITY;
        double oldUtility = calcPathUtility(oldPath, agent);
        Vector<Vector<Subtask>> possiblePaths = generateNewPaths(oldPath, j);

        Vector<Subtask> newBundle = new Vector<>();
        for(int i = 0; i < oldBundle.size(); i++) {
            newBundle.add(oldBundle.get(i));
        }
        newBundle.add(j);

        for (int i = 0; i < possiblePaths.size(); i++) {
            // Calculate utility for each new path
            Vector<Subtask> newPath = possiblePaths.get(i);
            double newUtility = calcPathUtility(newPath, agent);
            double newPathBid = newUtility - oldUtility;

            //get max bid from all new paths
            if(newPathBid > maxPathBid){
                maxPathBid = newPathBid;
                this.c_aj = newPathBid;
                this.t_aj = calcQuickestArrivalTime(newPath, j, agent);
                this.x_aj = j.getParentTask().getLocation();
                this.i_opt = newPath.indexOf(j);
            }

        }
    }

    protected double calcPathUtility(Vector<Subtask> path, SimulatedAbstractAgent agent){
        double pathUtility = 0.0;
        double subtaskUtility = 0.0;

        for(int i = 0; i < path.size(); i++){
            Subtask j = path.get(i);
            double t_a = calcQuickestArrivalTime(path, j, agent);

            subtaskUtility = calcSubtaskUtility(path,j,t_a, agent);

            pathUtility = pathUtility + subtaskUtility;
        }
        return pathUtility;
    }

    protected Vector<Vector<Subtask>> generateNewPaths(Vector<Subtask> oldPath, Subtask j){
        Vector<Vector<Subtask>> newPaths = new Vector<>();

        for(int i = 0; i < (oldPath.size()+1); i++){
            Vector<Subtask> tempPath = new Vector<>();
            for(int k = 0; k < oldPath.size(); k++){
                tempPath.add(oldPath.get(k));
            }
            tempPath.add(i,j);
            newPaths.add(tempPath);
        }

        return newPaths;
    }

    protected double calcQuickestArrivalTime(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        double delta_x;
        Dimension x_i;
        int i = path.indexOf(j);
        if(i == 0){
            x_i = agent.getInitialPosition();
        }
        else{
            x_i = path.get(i-1).getParentTask().getLocation();
        }
        Dimension x_f = j.getParentTask().getLocation();
        delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);

        return sqrt(delta_x)/agent.getSpeed();
    }

    private double calcSubtaskUtility(Vector<Subtask> path, Subtask j, double t_a, SimulatedAbstractAgent agent){
        double S = calcSubtaskScore(path, j, t_a, agent);
        double g = calcTravelCost(path, j, agent);
        double p = calcMergePenalty(path, j, agent);
        double c_v = calcSubtaskCost(j);

        return S - g - p - c_v;
    }

    private double calcSubtaskScore(Vector<Subtask> path, Subtask j, double t_a, SimulatedAbstractAgent agent){
        double S_max = j.getParentTask().getS_max();
        double K = j.getK();
        double e = calcUrgency(j, t_a);
        double alpha = calcAlpha(j.getK(), j.getParentTask().getI());
        double sigmoid = calcSigmoid(path, j, agent);
        //double subtask_score = (S_max/K)*e*alpha*sigmoid;

        return (S_max/K)*e*alpha*sigmoid;
    }

    private double calcUrgency(Subtask j, double t_a){
        double lambda = j.getParentTask().getTC().get(3);
        double t_start = j.getParentTask().getTC().get(0);

        //return exp(- lambda * (t_a-t_start) );
        return 1.0;
    }

    private double calcAlpha(double K, double I){
        if((K/I) == 1.0){
            return 1.0;
        }
        else return (1.0 / 3.0);
    }

    private double calcSigmoid(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        int i = path.indexOf(j);
        double delta_x;
        if(i == 0){
            Dimension x_i = agent.getInitialPosition();
            Dimension x_f = j.getParentTask().getLocation();
            delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);
        }
        else{
            Dimension x_i = path.get(i-1).getParentTask().getLocation();
            Dimension x_f = j.getParentTask().getLocation();
            delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);
        }

        double gamma = j.getParentTask().getGamma();
        double distance =  sqrt(delta_x);
        double e = exp( gamma  * distance );
        //return 1.0/( 1 + e);
        return 1.0;
    }

    private double calcTravelCost(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        int i = path.indexOf(j);
        double delta_x;
        Dimension x_i;
        if(i == 0){
            x_i = new Dimension(0, 0);
        }
        else{
            x_i = path.get(i-1).getParentTask().getLocation();
        }
        Dimension x_f = j.getParentTask().getLocation();
        delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);

        double distance = sqrt(delta_x);

        return distance*agent.getMiu();
    }

    private double calcMergePenalty(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        int i = path.indexOf(j);
        double C_split = 0.0;
        double C_merge = 0.0;

        // merge cost = 2.0
        // split merge = 1.0

        if( i == 0){ // j is at beginning of path, no A(v-1) penalty
            // if merge at j, then add merge cost

        }
        else {  // j has a previous subtask in the path
            // was there a coalition at i-1?
            // if yes, is there a coalition at i?
            // if yes, is it the same as i-1?
            // if no, add split cost and merge cost
            // if yes, add NO cost;
            //if no, add split cost
            // if no, is there a coalition at i?
            // if yes, add merge cost;
            // if no, add NO cost;
        }


        return C_split + C_merge;
    }

    private double calcSubtaskCost(Subtask j){
        return j.getParentTask().getCost();
    }


    private double calcStartTime(double local_bid, Subtask j, Vector<Subtask> path, SimulatedAbstractAgent agent){
        double t_start = 0.0;

        return t_start;
    }

    private Dimension calcLocation(double local_bid, Subtask j, Vector<Subtask> path, SimulatedAbstractAgent agent){
        // Performs task at location of task.
        // IMPROVEMENT OPPORTUNITY: decide where to do task using sigmoid function
        Dimension location = j.getParentTask().getLocation();
        return location;
    }


    /**
     * Getters and setters
     */
    public double getC(){ return c_aj; }
    public double getTStart(){return t_aj;}
    public Dimension getX_aj(){return x_aj;}
    public int getI_opt(){return i_opt;}
}
