package CCBBA.lib;

import org.orekit.estimation.measurements.PV;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.HashMap;

public class IterationDatum {
    // Info used with other agents*********************
    private Subtask j;                      // subtask who the following results belong to
    private int i_q;                        // subtask index with respect to its parent task subtask list
    private double y;                       // winner bid list
    private SimulatedAgent z;               // winner agent list
    private double tz;                      // arrival time list
    private double c;                       // self bid
    private int s;                          // iteration stamp vector
    private ArrayList<Double> x;            // Location where the measurement will be taken
    private double cost;                    // cost of performing subtask
    private double score;                   // raw score from performing subtask

    // Info used by agent
    private int h;                          // availability
    private int v;                          // number of iterations in coalition constraint violation
    private int w_solo;                     // permission to bid solo
    private int w_any;                      // permission to bid any
    private int w_all;                      // total permissions to bid on a task

    /**
     * Constructors
     * @param j - Subtask who's results belong to
     */
    public IterationDatum(Subtask j, SimulatedAgent agent){
        this.j = j;
        this.i_q = j.getParentTask().getSubtaskList().indexOf(j);
        this.y = 0.0;
        this.z = null;
        this.tz = 0.0;
        this.c = 0.0;
        this.s = 0;
        this.h = 1;
        this.v = 0;
        this.w_any = agent.getW_any();
        this.w_solo = agent.getW_solo();
        this.w_all = 100;
        this.x = new ArrayList<>(3);
        this.cost = 0.0;
        this.score = 0.0;
    }

    public IterationDatum(IterationDatum newDatum){
        this.j = newDatum.getJ();
        this.i_q = j.getParentTask().getSubtaskList().indexOf(j);
        this.y = newDatum.getY();
        this.z = newDatum.getZ();
        this.tz = newDatum.getTz();
        this.c = newDatum.getC();
        this.s = newDatum.getS();
        this.h = newDatum.getH();
        this.v = newDatum.getV();
        this.w_any = newDatum.getW_any();
        this.w_solo = newDatum.getW_solo();
        this.w_all = newDatum.getW_all();
        this.x = new ArrayList<>();
        this.x.addAll( newDatum.getX() );
        this.cost = newDatum.getCost();
        this.score = newDatum.getScore();
    }

    public void resetCoalitionCounters(SimulatedAgent agent){
        this.h = 1;
        this.v = 0;
        this.w_any = agent.getW_any();
        this.w_solo = agent.getW_solo();
    }

    /**
     * Getters and setters
     */
    public int getI_q(){ return i_q; }
    public Subtask getJ(){ return j; }
    public double getY() { return y; }
    public SimulatedAgent getZ() { return z; }
    public double getTz() { return tz; }
    public double getC() { return c; }
    public int getS() { return s; }
    public int getH() { return h;}
    public int getV() {return v; }
    public int getW_solo() { return w_solo; }
    public int getW_any() { return w_any; }
    public int getW_all(){ return w_all; }
    public ArrayList<Double> getX(){ return this.x; }
    public double getCost(){ return this.cost; }
    public double getScore(){ return this.score; }

    public void setH(int h){ this.h = h; }
    public void setY(double y) { this.y = y; }
    public void setZ(SimulatedAgent z) { this.z = z; }
    public void setTz(double tz) { this.tz = tz; }
    public void setC(double c) { this.c = c; }
    public void setS(int s) { this.s = s; }
    public void setX(ArrayList<Double> x) {
        this.x = new ArrayList<>();
        this.x.addAll(x);
    }
    public void setV(int v) { this.v = v; }
    public void setCost(double cost){ this.cost = cost; }
    public void setScore(double score) { this.score = score; }
    public void decreaseW_solo() { this.w_solo -= 1; }
    public void decreaseW_any() { this.w_any -= 1; }
    public void decreaseW_all(){ this.w_all -= 1; }
    public void increaseV(){ this.v += 1; }
    public void resetV(){ this.v = 0; }
    public void setW_any(int w_any) { this.w_any = w_any;}
    public void setW_solo(int w_solo) { this.w_solo = w_solo;}
    public void setW_all(int w_all) { this.w_all = w_all;}
}
