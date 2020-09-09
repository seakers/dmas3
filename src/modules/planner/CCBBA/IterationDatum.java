package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class IterationDatum {
    // Info used with other agents*********************
    private Subtask j;                      // subtask who the following results belong to
    private int i_q;                        // subtask index with respect to its parent task subtask list
    private double y;                       // winner bid
    private AbstractAgent z;                // winner agent
    private double tz;                      // measurement epoch [s]
    private AbsoluteDate tz_date;           // measurement date
    private double c;                       // self bid
    private AbsoluteDate s;                 // bid date stamp
    private double spatialRes;              // spatial resolution of measurement bid [m]
    private double snr;                     // Signal to Noise Ratio of measurement bid [dB]
    private double cost;                    // cost of performing subtask
    private double score;                   // raw score from performing subtask
    private boolean locked;                 // locking trigger that prevents a task from being dropped by the winner agent

    // Info used by agent
    private int h;                          // availability
    private int v;                          // number of iterations in coalition constraint violation
    private int w_solo;                     // permission to bid solo
    private int w_any;                      // permission to bid any
    private int w_all;                      // total permissions to bid on a task

    public IterationDatum(Subtask j, CCBBASettings settings){
        this.j = j;
        this.i_q = j.getI_q();
        this.y = 0.0;
        this.z = null;
        this.tz = -1.0;
        this.tz_date = null;
        this.c = 0.0;
        this.s = null;
        this.spatialRes = -1.0;
        this.snr = -1.0;
        this.cost = 0.0;
        this.score = 0.0;
        this.locked = false;

        this.h = 1;
        this.v = 0;
        this.w_solo = settings.w_solo;
        this.w_any = settings.w_any;
        this.w_all = settings.w_all;
    }

    private IterationDatum(Subtask j, double y, AbstractAgent z, double tz, AbsoluteDate tz_date, double c,
                          AbsoluteDate s, double spatialRes, double snr, double cost, double score, int h, int v,
                           int w_solo, int w_any, int w_all){
        this.j = j;
        this.i_q = j.getI_q();
        this.y = y;
        this.z = z;
        this.tz = tz;
        if(tz_date == null) this.tz_date = null;
        else this.tz_date = tz_date.getDate();
        this.c = c;
        if(s == null) this.s = null;
        else this.s = s.getDate();
        this.spatialRes = spatialRes;
        this.snr = snr;
        this.cost = cost;
        this.score = score;

        this.h = h;
        this.v = v;
        this.w_solo = w_solo;
        this.w_any = w_any;
        this.w_all = w_all;
    }

    public IterationDatum copy(){
        return new IterationDatum(j, y, z, tz, tz_date, c, s, spatialRes, snr, cost, score, h, v, w_solo, w_any, w_all);
    }

    public void resetAvailability(){
        this.h = 1;
    }

    /**
     * Getters and Setters
     */
    public Subtask getSubtask() {
        return j;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public AbstractAgent getZ() {
        return z;
    }

    public void setZ(AbstractAgent z) {
        this.z = z;
    }

    public double getTz() {
        return tz;
    }

    public void setTz(double tz) {
        this.tz = tz;
    }

    public AbsoluteDate getTz_date() {
        return tz_date;
    }

    public void setTz_date(AbsoluteDate tz_date) {
        this.tz_date = tz_date;
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
    }

    public AbsoluteDate getS() {
        return s;
    }

    public void setS(AbsoluteDate s) {
        this.s = s;
    }

    public double getSpatialRes() {
        return spatialRes;
    }

    public void setSpatialRes(double spatialRes) {
        this.spatialRes = spatialRes;
    }

    public double getSnr() {
        return snr;
    }

    public void setSnr(double snr) {
        this.snr = snr;
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

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public int getV() {
        return v;
    }

    public void setV(int v) {
        this.v = v;
    }

    public int getW_solo() {
        return w_solo;
    }

    public void setW_solo(int w_solo) {
        this.w_solo = w_solo;
    }

    public int getW_any() {
        return w_any;
    }

    public void setW_any(int w_any) {
        this.w_any = w_any;
    }

    public int getW_all() {
        return w_all;
    }

    public void setW_all(int w_all) {
        this.w_all = w_all;
    }
}
