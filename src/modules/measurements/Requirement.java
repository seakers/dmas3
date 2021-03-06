package modules.measurements;

import java.util.InputMismatchException;

public class Requirement {
    public static final String SPATIAL = "spatial";
    public static final String TEMPORAL = "temporal";
    public static final String ACCURACY = "accuracy";

    private final String name;
    private final double goal;
    private final double breakThrough;
    private final double threshold;
    private final String units;

    public Requirement(String name, double goal, double breakThrough, double threshold, String units){
        this.name = name;
        this.goal = goal;
        this.breakThrough = breakThrough;
        this.threshold = threshold;

        boolean match = false;
        for(String unit : Units.ALL){
            if(unit.equals(units)) {
                match = true;
                break;
            }
        }
        if(!match) throw new InputMismatchException("Requirement units " + units + " not yet supported.");
        this.units = units;
    }

    public Requirement copy(){
        return new Requirement(name, goal, breakThrough, threshold, units);
    }
    public String getName(){ return name; }
    public double getGoal() { return goal; }
    public double getBreakThrough() { return breakThrough; }
    public double getThreshold() { return threshold; }
    public String getUnits() { return units; }
}
