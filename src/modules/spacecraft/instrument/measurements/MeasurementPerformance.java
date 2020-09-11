package modules.spacecraft.instrument.measurements;

import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;

public class MeasurementPerformance {
    private Measurement mainMeasurement;    // measurement being made
    private double spatialResAZ;            // spatial resolution in the azimuth direction [m]
    private double spatialResEL;            // spatial resolution in the elevation direction[m]
    private double snr;                     // signal-to-noise ratio [dB]

    public MeasurementPerformance(Subtask j, ArrayList<Instrument> instruments, Spacecraft spacecraft, AbsoluteDate date) throws Exception {
        this.mainMeasurement = j.getMainMeasurement();
        this.spatialResAZ = calcSpatialResAZ(j,instruments,spacecraft,date);
        this.spatialResEL = calcSpatialResEL(j,instruments,spacecraft,date);
        this.snr = calcSNR(j,instruments,spacecraft,date);
    }

    // Helper Functions
    private double calcSpatialResAZ(Subtask j, ArrayList<Instrument> instruments, Spacecraft spacecraft, AbsoluteDate date) throws OrekitException {
        Vector3D satPos = spacecraft.getPVEarth(date).getPosition();
        Vector3D taskPos = j.getParentTask().getPVEarth(date).getPosition();

        double[] resolutions = new double[instruments.size()];
        for(Instrument ins : instruments){
            int i = instruments.indexOf(ins);
            double resTemp = ins.getSpatialResAZ(satPos, taskPos);
            resolutions[i] = resTemp;
        }

        if(instruments.size() == 2){
            Instrument ins1 = instruments.get(0);
            Instrument ins2 = instruments.get(1);

            boolean cond1 = ins1.getType().equals("RAD") || ins1.getType().equals("SAR");
            boolean cond2 = ins2.getType().equals("RAD") || ins2.getType().equals("SAR");
            boolean cond3 = !ins1.getType().equals( ins2.getType() );
            if(cond1 && cond2 && cond3){
                double res1 = resolutions[0];
                double res2 = resolutions[1];
                return Math.sqrt(res1 * res2);
            }
        }

        double resMin = Double.POSITIVE_INFINITY;
        for (double resolution : resolutions) {
            if (resolution < resMin) {
                resMin = resolution;
            }
        }

        return  resMin;
    }
    private double calcSpatialResEL(Subtask j, ArrayList<Instrument> instruments, Spacecraft spacecraft, AbsoluteDate date) throws OrekitException {
        Vector3D satPos = spacecraft.getPVEarth(date).getPosition();
        Vector3D taskPos = j.getParentTask().getPVEarth(date).getPosition();

        double[] resolutions = new double[instruments.size()];
        for(Instrument ins : instruments){
            int i = instruments.indexOf(ins);
            double resTemp = ins.getSpatialResEL(satPos, taskPos);
            resolutions[i] = resTemp;
        }

        if(instruments.size() == 2){
            Instrument ins1 = instruments.get(0);
            Instrument ins2 = instruments.get(1);

            boolean cond1 = ins1.getType().equals("RAD") || ins1.getType().equals("SAR");
            boolean cond2 = ins2.getType().equals("RAD") || ins2.getType().equals("SAR");
            boolean cond3 = !ins1.getType().equals( ins2.getType() );
            if(cond1 && cond2 && cond3){
                double res1 = resolutions[0];
                double res2 = resolutions[1];
                return Math.sqrt(res1 * res2);
            }
        }

        double resMin = Double.POSITIVE_INFINITY;
        for (double resolution : resolutions) {
            if (resolution < resMin) {
                resMin = resolution;
            }
        }

        return  resMin;
    }
    private double calcSNR(Subtask j, ArrayList<Instrument> instruments, Spacecraft spacecraft, AbsoluteDate date) throws Exception {
        PVCoordinates satPv = spacecraft.getPVEarth(date);
        PVCoordinates taskPv = j.getParentTask().getPVEarth(date);

        double[] SNRs = new double[instruments.size()];
        for(Instrument ins : instruments){
            int i = instruments.indexOf(ins);
            double snrTemp = ins.getSNR(j.getMainMeasurement(),satPv,taskPv);
            SNRs[i] = snrTemp;
        }

        double snrMax = 0.0;
        for (double snr : SNRs) {
            if (snrMax < snr) {
                snrMax = snr;
            }
        }

        return  snrMax;
    }

    // Getters
    public Measurement getMainMeasurement() { return mainMeasurement; }
    public double getSpatialResAz() { return spatialResAZ; }
    public double getSpatialResEl() { return spatialResEL; }
    public double getSNR() { return snr; }

    public double getSpatialRes() throws Exception {
        if(spatialResAZ == -1.0 && spatialResEL == -1.0){
            throw new Exception("spatial resolution not yet calculated");
        }

        return Math.min(spatialResAZ, spatialResEL);
    }
}
