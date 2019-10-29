package CCBBA.source;

import java.util.Vector;

public class PathUtility{
    private double utility;
    private double cost;
    private double score;
    private Vector<Double> Tz;

    PathUtility(){
        this.utility = 0.0;
        this.cost = 0.0;
        this.score = 0.0;
        this.Tz = new Vector<>();
    }

    public double getUtility(){ return this.utility; }
    public double getCost(){ return this.cost;}
    public double getScore(){ return this.score; }
    public Vector<Double> getTz() { return this.Tz; }
    public void setUtility(double utility){ this.utility = utility; }
    public void setCost(double cost){ this.cost = cost; }
    public void setScore(double score){ this.score = score; }
    public void addTz(double tz_i) { this.Tz.add(tz_i); }
}
