package CCBBA.source;

public class PathUtility{
    private double utility;
    private double cost;
    private double score;

    PathUtility(){
        this.utility = 0.0;
        this.cost = 0.0;
        this.score = 0.0;
    }

    public double getUtility(){ return this.utility; }
    public double getCost(){ return this.cost;}
    public double getScore(){ return this.score; }
    public void setUtility(double utility){ this.utility = utility; }
    public void setCost(double cost){ this.cost = cost; }
    public void setScore(double score){ this.score = score; }
}
