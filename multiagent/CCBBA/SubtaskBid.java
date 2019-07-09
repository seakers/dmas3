package seakers.orekit.multiagent.CCBBA;

import java.awt.*;
import java.util.Vector;

public class SubtaskBid {
    protected Integer inf = Integer.MIN_VALUE;

    protected double c_aj;              // self bid
    protected double t_aj;              // subtask start time
    protected Dimension x_aj;           // location of realization
    protected Vector<Subtask> optimalPath; // optimal task path
    protected int i_opt;                // index of optimal path

    public SubtaskBid(){
        c_aj = 0.0;
        t_aj = 0.0;
        x_aj = new Dimension(inf,inf);
        i_opt = 0;
    }

    public void calcBidForSubtask(Vector<Vector<Subtask>> possiblePaths, Subtask j, SimulatedAbstractAgent agent){
        c_aj = calcSelfBid(possiblePaths, agent);
        t_aj = calcStartTime(possiblePaths, agent);
        x_aj = calcLocation(possiblePaths, j, agent);
        optimalPath = calcOptimalPath(possiblePaths, i_opt);
    }

    private double calcSelfBid(Vector<Vector<Subtask>> possiblePaths, SimulatedAbstractAgent agent){
        double bid = 0.0;
        double local_bid;
        for(int i = 0; i < possiblePaths.size(); i++){
            local_bid = 0.0;
            // calculate bid for this path
            //local_bid = utility(possiblePaths.get(i), );

            // Checks for highest bid and saves index;
            if(local_bid > bid){
                bid = local_bid;
                i_opt = i;
            }
        }

        return bid;
    }

    private double calcStartTime(Vector<Vector<Subtask>> possiblePaths, SimulatedAbstractAgent agent){
        double t_start = 0.0;

        return t_start;
    }

    private Dimension calcLocation(Vector<Vector<Subtask>> possiblePaths, Subtask j, SimulatedAbstractAgent agent){
        // Performs task at location of task.
        // IMPROVEMENT OPPORTUNITY: decide where to do task using sigmoid function
        Dimension location = j.getParentTask().getLocation();
        return location;
    }

    private Vector<Subtask> calcOptimalPath(Vector<Vector<Subtask>> possiblePaths, int i){
        Vector<Subtask> optimalPath = possiblePaths.get(i);
        return optimalPath;
    }

    /**
     * Getters and setters
     */
    public double getC(){ return c_aj; }
    public double getTStart(){return t_aj;}
    public Dimension getX_aj(){return x_aj;}
    public Vector<Subtask> getOptimalPath(){return optimalPath;}
    public int getI_opt(){return i_opt;}
}
