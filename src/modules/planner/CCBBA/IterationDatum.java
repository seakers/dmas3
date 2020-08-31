package modules.planner.CCBBA;

import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class IterationDatum {
    // Info used with other agents*********************
    private Subtask j;                      // subtask who the following results belong to
    private int i_q;                        // subtask index with respect to its parent task subtask list
    private double y;                       // winner bid
    private Spacecraft z;                   // winner agent
    private double tz;                      // measurement epoch [s]
    private AbsoluteDate tz_date;           // measurement date
    private double c;                       // self bid
    private AbsoluteDate s;                 // bid date stamp
    private double spatialRes;              // spatial resolution of measurement bid [m]
    private double snr;                     // Signal to Noise Ratio of measurement bid [dB]
    private double cost;                    // cost of performing subtask
    private double score;                   // raw score from performing subtask

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

        this.h = 1;
        this.v = 0;
        this.w_solo = settings.w_solo;
        this.w_any = settings.w_any;
        this.w_all = settings.w_all;
    }
}
