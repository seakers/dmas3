package modules.planner.CCBBA;

public class CCBBASettings {
    public final int M = 4;                               // planning horizon
    public final int O_kq = 5;                            // max iterations in constraint violation
    public final int w_solo = 3;                          // permission to bid solo on a task
    public final int w_any = 10;                          // permission to bid on a task
    public final int w_all = 100;                         // total permissions to bid on a task
    public final int convIndicator = 10;                  // convergence indicator
}
