package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.measurements.MeasurementPerformance;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class IterationDatum {
    // Info used with other agents*********************
    private Subtask j;                      // subtask who the following results belong to
    private int i_q;                        // subtask index with respect to its parent task subtask list
    private double y;                       // winner bid
    private AbstractAgent z;                // winner agent
    private AbsoluteDate tz_date;           // measurement date
    private double c;                       // self bid
    private AbsoluteDate s;                 // bid date stamp
    private double cost;                    // cost of performing subtask
    private double score;                   // raw score from performing subtask
    private MeasurementPerformance performance; // proposed performance of the measurement bid

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
        this.tz_date = null;
        this.c = 0.0;
        this.s = null;
        this.cost = 0.0;
        this.score = 0.0;
        this.performance = new MeasurementPerformance(j);

        this.h = 1;
        this.v = 0;
        this.w_solo = settings.w_solo;
        this.w_any = settings.w_any;
        this.w_all = settings.w_all;
    }

    private IterationDatum(Subtask j, double y, AbstractAgent z, AbsoluteDate tz_date, double c,
                          AbsoluteDate s, double cost, double score,
                           MeasurementPerformance performance, int h, int v, int w_solo, int w_any, int w_all){
        this.j = j;
        this.i_q = j.getI_q();
        this.y = y;
        this.z = z;
        if(tz_date == null) this.tz_date = null;
        else this.tz_date = tz_date.getDate();
        this.c = c;
        if(s == null) this.s = null;
        else this.s = s.getDate();
        this.cost = cost;
        this.score = score;
        this.performance = performance.copy();

        this.h = h;
        this.v = v;
        this.w_solo = w_solo;
        this.w_any = w_any;
        this.w_all = w_all;
    }

    public IterationDatum copy(){
        return new IterationDatum(j, y, z, tz_date, c, s, cost, score, performance, h, v, w_solo, w_any, w_all);
    }

    public boolean equals(IterationDatum datum){
        if(j != datum.getSubtask()) return false;
        else if(Math.abs(y - datum.getY()) > 1e-3) return false;
        else if(z != datum.getZ()) return false;
        else if(Math.abs( tz_date.durationFrom(datum.getTz()) ) > 1e-3) return false;
        else if(Math.abs( s.durationFrom(datum.getS()) ) > 1e-3) return false;
        else if(Math.abs(cost - datum.getCost()) > 1e-3) return false;
        else if(Math.abs(score - datum.getScore()) > 1e-3) return false;
        return (v == datum.getV());
    }

    public void resetAvailability(){
        this.h = 1;
    }
    public void decreaseW_any(){ this.w_any -= 1;}
    public void decreaseW_solo(){ this.w_solo -= 1;}
    public void decreaseW_all(){ this.w_all -= 1;}
    public void increaseV(){ this.v += 1;}
    public void resetV(){this.v = 0;}

    /**
     * Getters and Setters
     */
    public Subtask getSubtask() { return j; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public AbstractAgent getZ() { return z; }
    public void setZ(AbstractAgent z) { this.z = z; }
    public AbsoluteDate getTz() { return tz_date; }
    public void setTz(AbsoluteDate tz) { this.tz_date = tz.getDate(); }
    public double getC() { return c; }
    public void setC(double c) { this.c = c; }
    public AbsoluteDate getS() { return s; }
    public void setS(AbsoluteDate s) { this.s = s; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public int getH() { return h; }
    public void setH(int h) { this.h = h; }
    public int getV() { return v; }
    public void setV(int v) { this.v = v; }
    public int getW_solo() { return w_solo; }
    public void setW_solo(int w_solo) { this.w_solo = w_solo; }
    public int getW_any() { return w_any; }
    public void setW_any(int w_any) { this.w_any = w_any; }
    public int getW_all() { return w_all; }
    public void setW_all(int w_all) { this.w_all = w_all; }
    public MeasurementPerformance getPerformance(){return  performance;}
    public void setPerformance(MeasurementPerformance newPerf){ this.performance = newPerf.copy(); }
}
