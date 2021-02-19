package modules.measurements;

public class Requirement {
    private final double goal;
    private final double breakThrough;
    private final double threshold;
    private final String units;

    public Requirement(double goal, double breakThrough, double threshold, String units){
        this.goal = goal;
        this.breakThrough = breakThrough;
        this.threshold = threshold;
        this.units = units;
    }

    public Requirement copy(){
        return new Requirement(goal, breakThrough, threshold, units);
    }
}
