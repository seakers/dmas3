package CCBBA.lib;

import java.util.ArrayList;

public class IterationResults {
    // Info used with other agents*********************
    private Subtask j;                      // subtask who the following results belong to
    private int i_q;                        // subtask index with respect to its parent task subtask list
    private double y;                       // winner bid list
    private SimulatedAgent z;               // winner agent list
    private double tz;                      // arrival time list
    private double c;                       // self bid
    private int s;                          // iteration stamp vector

    // Info used by agent
    private int h;                          // availability
    private int v;                          // number of iterations in coalition constraint violation
    private int w_solo;                     // permission to bid solo
    private int w_any;                      // permission to bid any

    public IterationResults(Subtask j){
        this.j = j;
        this.i_q = j.getParentTask().getSubtaskList().indexOf(j);
        this.y = 0.0;
        this.z = null;
        this.tz = 0.0;
        this.c = 0.0;
        this.s = 0;
        this.h = 1;
        this.v = 0;
        this.w_any = 0;
        this.w_solo = 0;
    }

    public void updateResults(IterationResults newResults) throws Exception{
        if( !this.j.equals(newResults.getJ()) ){
            throw new Exception("Result update error. Iteration results' relevant subtasks do not match");
        }
        this.y = newResults.getY();
        this.z = newResults.getZ();
        this.tz = newResults.getTz();
        this.s = newResults.getS();
        this.v = newResults.getV();
        this.w_any = newResults.getW_any();
        this.w_solo = newResults.getW_solo();
    }

    /**
     * Getters and setters
     */
    public Subtask getJ(){ return j; }
    public double getY() {
        return y;
    }
    public SimulatedAgent getZ() {
        return z;
    }
    public double getTz() {
        return tz;
    }
    public double getC() {
        return c;
    }
    public int getS() {
        return s;
    }
    public int getH() {
        return h;
    }
    public int getV() {
        return v;
    }
    public int getW_solo() {
        return w_solo;
    }
    public int getW_any() {
        return w_any;
    }
}
