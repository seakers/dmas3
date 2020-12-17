package modules.spacecraft.instrument.measurements;

import modules.environment.Requirements;
import modules.environment.Subtask;
import modules.environment.Task;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;

import static java.lang.Math.exp;

public class MeasurementPerformance {
    private Task parentTask;
    private Measurement mainMeasurement;    // measurement being made
    private AbsoluteDate date;              // date of measurement

    // Measurement Performance Properties
    private double spatialResAZ;            // spatial resolution in the azimuth direction [m]
    private double spatialResEL;            // spatial resolution in the elevation direction[m]
    private double snr;                     // signal-to-noise ratio [dB]
    private double incidence;               // incidence angle [°]
    private double angleCT;                 // angle of measurement across track [°]
    private double angleAT;                 // angle of measurement along track [°]
    private double revisitTime;             // average revisit time [s]


    public MeasurementPerformance(Subtask j){
        parentTask = j.getParentTask();
        mainMeasurement = j.getMainMeasurement();
        date = null;
        spatialResAZ = -1.0;
        spatialResEL = -1.0;
        snr = -1.0;
        incidence = -1.0;
        angleCT = -1.0;
        angleAT = -1.0;
        revisitTime = 0.0;
    }

    public MeasurementPerformance(Subtask j, ArrayList<Instrument> instruments, Spacecraft spacecraft, AbsoluteDate date) throws Exception {
        this.parentTask = j.getParentTask();
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
    private double calcRevTime(AbsoluteDate newDate){
        if(this.date == null) {
            return -1.0;
        }
        return newDate.durationFrom(this.date);
    }

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

    private double calcSpatialResAZ(Subtask j, ArrayList<Instrument> instruments, Spacecraft spacecraft, AbsoluteDate date) throws Exception {
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

        double resMax = 0.0;
        for (double resolution : resolutions) {
            if (resolution > resMax) {
                resMax = resolution;
            }
        }

        return  resMax;
    }
    private double calcSpatialResEL(Subtask j, ArrayList<Instrument> instruments, Spacecraft spacecraft, AbsoluteDate date) throws Exception {
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

        double resMax = 0.0;
        for (double resolution : resolutions) {
            if (resolution > resMax) {
                resMax = resolution;
            }
        }

        return  resMax;
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
    public double getIncidence() { return incidence; }
    public double getAngleCT() { return angleCT; }
    public double getAngleAT() { return angleAT; }
}
