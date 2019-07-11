package seakers.orekit.multiagent.CCBBA;

import java.awt.*;
import java.util.Vector;

import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.StrictMath.sqrt;

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

    public void calcBidForSubtask(Vector<Subtask> oldPath, Vector<Subtask> bundle, Subtask j, Vector<Vector> iteration_results, SimulatedAbstractAgent agent){
        Vector<Double> y = iteration_results.get(0);
        Vector<SimulatedAbstractAgent> z = iteration_results.get(1);
        Vector<Double> tz = iteration_results.get(2);
        Boolean previousTaskBid = false;

        // check if an own bid exists for a subtask belonging to the same task. If false:

        if(!previousTaskBid) {
            Double oldUtility = calcPathUtility(tz, oldPath, agent);
            Vector<Vector<Subtask>> possiblePaths = generateNewPaths(oldPath, bundle, j);

            double maxPathBid = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < possiblePaths.size(); i++) {
                // Calculate utility for each path
                Vector<Subtask> newPath = possiblePaths.get(i);
                double newUtility = 0.0;
                double newPathBid = 0.0;

                for (int k = 0; k < possiblePaths.get(0).size(); k++) {
                /*
                        If true, calculate subtask bid
                        If false, arrival time = arrival time of other mates - t_corr
                    if arrival time is not the same as the one in iteration_results, add penalty
                    calculate pad bid difference

                 */
                    Subtask j_j = newPath.get(k);
                    Dimension x_new = j_j.getParentTask().getLocation();            //  calculate arrival location
                    double t_a = calcQuickestArrivalTime(newPath, j_j, agent, k);    // calculate quickest arrival time
                    // check if arrival time meets time constraints
                    //      If false, arrival time = arrival time of other mates - t_corr
                    double tempSubtaskBid = calcSubtaskUtility(newPath, j_j, t_a, agent);
                    newUtility = newUtility + tempSubtaskBid;
                }

                newPathBid = newUtility - oldUtility;
                //get max bid from all new paths
                if(newPathBid > maxPathBid){
                    maxPathBid = newPathBid;
                    c_aj = newPathBid;
                    optimalPath = newPath;
                    i_opt = optimalPath.indexOf(j);
                }
            }
        }
    }



    protected double calcPathUtility(Vector<Double> tz, Vector<Subtask> path, SimulatedAbstractAgent agent){
        double pathUtility = 0.0;
        double subtaskUtility;
        for(int i = 0; i < path.size(); i++){
            Subtask j = path.get(i);
            double t_a = tz.get(i);
            subtaskUtility = calcSubtaskUtility(path,j,t_a, agent);
            pathUtility = pathUtility + subtaskUtility;
        }
        return pathUtility;
    }

    protected Vector<Vector<Subtask>> generateNewPaths(Vector<Subtask> oldPath, Vector<Subtask> bundle, Subtask j){
        Vector<Vector<Subtask>> newPaths = new Vector<>();

        return newPaths;
    }

    protected double calcQuickestArrivalTime(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent, int i){
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

        return sqrt(delta_x)/agent.getSpeed();
    }

    /*
    public void calcBidForSubtask(Vector<Vector<Subtask>>possiblePaths, Subtask j, int j_index, SimulatedAbstractAgent agent){
        double bid = 0.0;
        double local_bid;
        Vector<Subtask> oldPath = agent.getPath();
        double utility_old = getPathUtility(oldPath, agent);

        for(int i = 0; i < possiblePaths.size(); i++){
            // calculate bid for this path
            Vector<Subtask> newPath = possiblePaths.get(i);
            double utility_new = getPathUtility(newPath, agent);

            local_bid = utility_new - utility_old;
            /*
            local_bid = getUtility(newPath, j, agent);
            Vector<Double> local_tz = new Vector<>();

            // Checks for highest bid and saves index;
            if(local_bid > bid){
                bid = local_bid;
                i_opt = i;
                //t_aj = calcStartTime(local_bid, j, possiblePaths.get(i), agent);
                x_aj = calcLocation(local_bid, j, possiblePaths.get(i), agent);
            }
        }

        c_aj = bid;
    }

    private double getPathUtility(Vector<Subtask> path, SimulatedAbstractAgent agent){
        Vector<Dimension> X_path = new Vector<>();
        Vector<Double> tz_path = new Vector<>();
        double pathUtility = 0.0;

        for(int i = 0; i < path.size(); i++){
            Subtask j = path.get(i);
            pathUtility = pathUtility + getSubtaskUtility(path, j, agent);
            Dimension x_task = j.getParentTask().getLocation();

            double tz_task;
            Dimension x_i;
            Dimension x_f = j.getParentTask().getLocation();
            double distance = 0.0;

            if(i == 0){
                x_i = new Dimension(0,0);
            }
            else{
                x_i = path.get(i-1).getParentTask().getLocation();
            }

            double delta_x =  pow( (x_f.getWidth() - x_i.getWidth()) , 2) + pow( (x_f.getHeight() - x_i.getHeight()) , 2);
            distance = sqrt(delta_x);
            tz_task = distance/agent.getSpeed();

            X_path.add(x_task);
            tz_path.add(tz_task);
        }

        return pathUtility;
    }

    */

    private double calcSubtaskUtility(Vector<Subtask> path, Subtask j, double t_a, SimulatedAbstractAgent agent){
        double S = calcSubtaskScore(path, j, t_a);
        double g = calcTravelCost(path, j, agent);
        double p = calcMergePenalty(path, j, agent);
        double c_v = calcSubtaskCost(j);

        double utility = S - g - p - c_v;
        return utility;
    }

        private double calcSubtaskScore(Vector<Subtask> path, Subtask j, double t_a){
            double S_max = j.getParentTask().getS_max();
            double K = j.getK();
            double e = calcUrgency(j, t_a);
            double alpha = calcAlpha(j.getK(), j.getParentTask().getI());
            double sigmoid = calcSigmoid(path, j);
            //double subtask_score = (S_max/K)*e*alpha*sigmoid;

            return (S_max/K)*e*alpha*sigmoid;
        }

            private double calcUrgency(Subtask j, double t_a){
                double lambda = j.getParentTask().getTC().get(3);
                double t_start = j.getParentTask().getTC().get(0);
                return exp(- lambda * (t_a-t_start) );
            }

            private double calcAlpha(double K, double I){
                if((K/I) == 1.0){
                    return 1.0;
                }
                else return (1.0 / 3.0);
            }

            private double calcSigmoid(Vector<Subtask> path, Subtask j){
                int i = path.indexOf(j);
                double delta_x;
                if(i == 0){  delta_x = 0.0; }
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
    public Vector<Subtask> getOptimalPath(){return optimalPath;}
    public int getI_opt(){return i_opt;}
}
