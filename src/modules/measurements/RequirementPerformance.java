package modules.measurements;

public class RequirementPerformance {
    private final Requirement parentRequirement;
    private final double value;
    private final String units;

    public RequirementPerformance(Requirement requirement, double value){
        this.parentRequirement = requirement;
        this.value = value;
        this.units = requirement.getUnits();
    }

    public Requirement getParentRequirement() { return parentRequirement; }
    public double getValue() { return value; }
    public String getUnits() { return units; }
}
