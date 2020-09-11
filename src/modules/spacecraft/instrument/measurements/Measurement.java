package modules.spacecraft.instrument.measurements;

import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.maneuvers.AttitudeManeuver;
import modules.spacecraft.maneuvers.Maneuver;
import modules.spacecraft.orbits.SpacecraftOrbit;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

import static java.lang.Math.acos;

public class Measurement {
    // measurement type information
    protected String band = "";       // band used
    protected double f;               // frequency [Hz]
    protected double lambda;          // wavelength [m]
    protected double B;               // bandwidth [Hz]
    protected final double c = 3e8;   // speed of light [m/s]

    // measurement performance information
    protected double dtheta;          // angular resolution [°]
    protected double rangeRes;        // range resolution [m]
    protected double swadth;          // swath width [m]
    protected double spatialResAT;    // spatial resolution along track [m]
    protected double spatialResCT;    // spatial resolution cross track [m]
    protected double snr;             // Signal to Noise Ratio; [dB]
    protected Spacecraft agent;       // agent that performed the measurement

    public Measurement(double freq) {
        this.f = freq;
        this.lambda = this.c/this.f;
        this.band = findBand(this.f);
        this.B = 0;

        dtheta = -1.0;
        rangeRes = -1.0;
        swadth = -1.0;
        spatialResAT = -1.0;
        spatialResCT = -1.0;
        snr = -1.0;
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
                throw new Exception("Measurement band yet not supported");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected double rad2deg(double th){ return th*180/Math.PI; }
    protected double deg2rad(double th){ return th/180*Math.PI; }

    public double getLambda() { return lambda; }
    public double getF() { return f; }
    public String getBand() { return band; }
    public double calcSpatialResolution(){ return -1.0; }
    public double calcSNR(){ return -1.0; }
    protected double calcSwadth(Spacecraft spacecraft, AbsoluteDate date){return -1.0;}
}
