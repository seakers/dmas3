package modules.spacecraft.instrument.measurements;

import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;

public class MeasurementPerformance {
    private final Measurement mainMeasurement;    // measurement being made
    private final AbsoluteDate date;              // date of measurement
    private final double spatialResAZ;            // spatial resolution in the azimuth direction [m]
    private final double spatialResEL;            // spatial resolution in the elevation direction[m]
    private final double snr;                     // signal-to-noise ratio [dB]
    private final double incidence;               // incidence angle [°]
    private final double angleCT;                 // angle of measurement across track [°]
    private final double angleAT;                 // angle of measurement along track [°]

    public MeasurementPerformance(Subtask j){
        mainMeasurement = j.getMainMeasurement();
        date = new AbsoluteDate();
        spatialResAZ = -1.0;
        spatialResEL = -1.0;
        snr = -1.0;
        incidence = -1.0;
        angleCT = -1.0;
        angleAT = -1.0;
    }

    public MeasurementPerformance(Subtask j, ArrayList<Instrument> instruments, Spacecraft spacecraft, AbsoluteDate date) throws Exception {
        this.mainMeasurement = j.getMainMeasurement();
        this.date = date.getDate();
        this.spatialResAZ = calcSpatialResAZ(j,instruments,spacecraft,date);
        this.spatialResEL = calcSpatialResEL(j,instruments,spacecraft,date);
        this.snr = calcSNR(j,instruments,spacecraft,date);
        this.incidence = calcIncidenceAngle(j,spacecraft,date);
        this.angleCT = calcCrossTrackAngle(j,spacecraft,date);
        this.angleAT = calcAlongTrackAngle(j,spacecraft,date);
    }

    // Helper Functions
    private double calcAlongTrackAngle(Subtask j, Spacecraft spacecraft, AbsoluteDate date) throws Exception {
        ArrayList<Vector3D> orbitFrame = spacecraft.getDesign().getAdcs().calcOrbitFrame(spacecraft.getOrbit(),date);
        Vector3D satPos = spacecraft.getPVEarth(date).getPosition();
        Vector3D taskPos = j.getParentTask().getPVEarth(date).getPosition();

        return FastMath.toDegrees( spacecraft.getDesign().getAdcs().getTaskATAngle(orbitFrame,satPos,taskPos) );
    }

    private double calcCrossTrackAngle(Subtask j, Spacecraft spacecraft, AbsoluteDate date) throws Exception {
        ArrayList<Vector3D> orbitFrame = spacecraft.getDesign().getAdcs().calcOrbitFrame(spacecraft.getOrbit(),date);
        Vector3D satPos = spacecraft.getPVEarth(date).getPosition();
        Vector3D taskPos = j.getParentTask().getPVEarth(date).getPosition();

        return FastMath.toDegrees( spacecraft.getDesign().getAdcs().getTaskCTAngle(orbitFrame,satPos,taskPos) );
    }

    private double calcIncidenceAngle(Subtask j, Spacecraft spacecraft, AbsoluteDate date) throws Exception {
        Vector3D satPos = spacecraft.getPVEarth(date).getPosition();
        Vector3D taskPos = j.getParentTask().getPVEarth(date).getPosition();
        Vector3D taskRel = taskPos.subtract(satPos);

        double lookAngle = Math.acos( taskRel.dotProduct(satPos.scalarMultiply(-1))/ ( taskRel.getNorm() * satPos.scalarMultiply(-1).getNorm() ) );
        double posAngle = Math.acos( satPos.dotProduct(taskPos)/ (satPos.getNorm() * taskPos.getNorm()) );
        return FastMath.toDegrees( Math.PI/2 - (posAngle + lookAngle) );
    }

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

    /**
     * Copy constructor
     */
    private MeasurementPerformance(Measurement measurement, AbsoluteDate date, double spatialResAZ, double spatialResEL, double snr, double incidence, double angleCT, double angleAT){
        this.mainMeasurement = measurement;
        if(date == null) this.date = null;
        else this.date = date.getDate();
        this.spatialResAZ = spatialResAZ;
        this.spatialResEL = spatialResEL;
        this.snr = snr;
        this.incidence = incidence;
        this.angleCT = angleCT;
        this.angleAT = angleAT;

    }
    public MeasurementPerformance copy(){
        return new MeasurementPerformance(mainMeasurement, date, spatialResAZ, spatialResEL, snr, incidence,angleAT,angleCT);
    }
    /**
     * Getters
     */
    public Measurement getMainMeasurement() { return mainMeasurement; }
    public AbsoluteDate getDate(){return date;}
    public double getSpatialResAz() { return spatialResAZ; }
    public double getSpatialResEl() { return spatialResEL; }
    public double getSNR() { return snr; }
    public double getSpatialRes() throws Exception {
        if(spatialResAZ == -1.0 && spatialResEL == -1.0){
            throw new Exception("spatial resolution not yet calculated");
        }
        return Math.max(spatialResAZ, spatialResEL);
    }
    public double getSpatialResAZ() { return spatialResAZ; }
    public double getSpatialResEL() { return spatialResEL; }
    public double getSnr() { return snr; }
    public double getIncidence() { return incidence; }
    public double getAngleCT() { return angleCT; }
    public double getAngleAT() { return angleAT; }
}
