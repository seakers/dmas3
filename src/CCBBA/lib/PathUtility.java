package CCBBA.lib;

import java.util.ArrayList;

public class PathUtility{
    private double utility;                     // path total utility
    private double cost;                        // path total cost
    private double score;                       // path total score
    private double t_start;                     // Start of availability time
    private double t_end;                       // End of availability time
    private double duration;                    // Duration of task
    private double t_corr;                      // Correlation time
    private double lambda;                      // Score time decay parameter

    PathUtility(){
        this.utility = 0.0;
        this.cost = 0.0;
        this.score = 0.0;
        this.t_start = 0.0;
        this.t_end = 0.0;
        this.duration = 0.0;
        this.t_corr = 0.0;
        this.lambda = 0.0;
    }

    public double getUtility(){ return this.utility; }
    public double getCost(){ return this.cost;}
    public double getScore(){ return this.score; }
    public double getT_start() {
        return t_start;
    }
    public double getT_end() {
        return t_end;
    }
    public double getDuration() {
        return duration;
    }
    public double getT_corr() { return t_corr; }
    public double getLambda() {
        return lambda;
    }

    public void setUtility(double utility){ this.utility = utility; }
    public void setCost(double cost){ this.cost = cost; }
    public void setScore(double score){ this.score = score; }
    public void setT_start(double t_start) { this.t_start = t_start; }
    public void setT_end(double t_end) { this.t_end = t_end; }
    public void setDuration(double duration) { this.duration = duration; }
    public void setT_corr(double t_corr) { this.t_corr = t_corr; }
    public void setLambda(double lambda) { this.lambda = lambda; }
}
