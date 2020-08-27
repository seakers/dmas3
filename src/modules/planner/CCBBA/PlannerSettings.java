package modules.planner.CCBBA;

public class PlannerSettings {
    private int zeta;                                       // iteration counter
    private int M;                                          // planning horizon
    private int O_kq;                                       // max iterations in constraint violation
    private int w_solo;                                     // permission to bid solo on a task
    private int w_any;                                      // permission to bid on a task
    private int w_all;                                      // total permissions to bid on a task
    private int convCounter;                                // convergence counter
    private int convIndicator;                              // convergence indicator

    public PlannerSettings(int planningHorizon, int maxCoalitionViolations, int maxSoloBids, int maxAnyBids, int maxAllBids, int convIndicator){
        this.zeta = 0;
        this.M = planningHorizon;
        this.O_kq = maxCoalitionViolations;
        this.w_solo = maxSoloBids;
        this.w_any = maxAnyBids;
        this.w_all = maxAllBids;
        this.convCounter = 0;
        this.convIndicator = convIndicator;
    }

    public int getIteration(){
        return this.zeta;
    }
    public void increaseIteration(){
        this.zeta++;
    }
    public int getPlanningHorizon() {
        return M;
    }
    public int getMaxCoalitionViolations(){
        return O_kq;
    }
    public int getW_solo(){
        return w_solo;
    }
    public int getW_any(){
        return w_any;
    }
    public int getW_all(){
        return w_all;
    }
    public int getConvCounter() {
        return convCounter;
    }
    public void increaseConvCounter(){
        this.convCounter++;
    }
    public void resetConvCounter(){
        this.convCounter = 0;
    }
    public int getConvIndicator(){
        return this.convIndicator;
    }
}
