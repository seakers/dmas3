package modules.measurements;

public class RequirementPerformance {
    private final Requirement parentRequirement;
    private final double score;
    private final String units;

    public RequirementPerformance(Requirement requirement, double score){
        this.parentRequirement = requirement;
        this.score = score;
        this.units = requirement.getUnits();
    }
}
