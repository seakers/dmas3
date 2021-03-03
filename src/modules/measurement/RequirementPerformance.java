package modules.measurement;

public class RequirementPerformance {
    private final double spatResAT;
    private final double spatResCT;
    private final double accuracy;
    private final double SNR;

    public RequirementPerformance(double spatResAT, double spatResCT, double accuracy, double snr){
        this.spatResAT = spatResAT;
        this.spatResCT = spatResCT;
        this.accuracy = accuracy;
        SNR = snr;
    }
}
