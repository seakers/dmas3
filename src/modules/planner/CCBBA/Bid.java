package modules.planner.CCBBA;

import modules.environment.Subtask;

public class Bid {
    private Subtask j = null;   // subtask being bid on
    private double c = 0.0;     // utility bid
    private double cost = -1.0; // cost bid
    private double score = -1.0;// score bid
    private double t = -1.0;    // epoch [s]
    private int i_path = -1;    // location in path
    private int h = 0;

    public Bid(Subtask j){
        this.j = j;
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

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public int getI_path() {
        return i_path;
    }

    public void setI_path(int i_path) {
        this.i_path = i_path;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }
}
