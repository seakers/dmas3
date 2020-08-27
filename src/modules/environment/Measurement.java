package modules.environment;

public class Measurement {
    private String band = "";
    private double f;
    private double lambda;
    private final double c = 3e8;

    public Measurement(double freq) {
        this.f = freq;
        this.lambda = this.c/this.f;
        this.band = findBand(this.f);
    }

    public Measurement copy() throws Exception {
        return new Measurement(this.f);
    }

    public String findBand(double f) {
        try {
            if( 3e6 <= f && f < 30e6){
                return "HF";
            }
            else if( 30e6 <= f && f < 300e6){
                return "VHF";
            }
            else if( 300e6 <= f && f < 1e9){
                return "UHF";
            }
            else if( 1e9 <= f && f < 2e9){
                return "L";
            }
            else if( 2e9 <= f && f < 4e9){
                return "S";
            }
            else if( 4e9 <= f && f < 8e9){
                return "C";
            }
            else if( 8e9 <= f && f < 12e9){
                return "X";
            }
            else if( 12e9 <= f && f < 18e9){
                return "Ku";
            }
            else if( 18e9 <= f && f < 27e9){
                return "K";
            }
            else if( 27e9 <= f && f < 40e9){
                return "Ka";
            }
            else if( 40e9 <= f && f < 75e9){
                return "V";
            }
            else if( 75e9 <= f && f < 110e9){
                return "W";
            }
            else if( 110e9 <= f && f < 300e9){
                return "mm";
            }
            else{
                throw new Exception("mMeasurement band yet not supported");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public double getLambda() { return lambda; }
    public double getF() { return f; }
    public String getBand() { return band; }
}
