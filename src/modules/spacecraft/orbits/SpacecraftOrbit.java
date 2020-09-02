package modules.spacecraft.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import modules.spacecraft.instrument.Instrument;
import modules.environment.Environment;
import modules.environment.Task;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import seakers.orekit.util.OrekitConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class SpacecraftOrbit extends OrbitData {
    private OrbitParams params;
    private ArrayList<Instrument> payload;
    private HashMap<Instrument, HashMap<Task, ArrayList<TimeInterval>>> accessTimes;

    public SpacecraftOrbit(OrbitParams params, ArrayList<Instrument> payload, Environment environment) throws OrekitException {
        super(environment.getStartDate(), environment.getEndDate(), environment.getTimeStep());
        this.params = params.copy();
        this.payload = payload;
    }

    public void calcAccessTimes(Environment environment) throws Exception {
        ArrayList<Task> environmentTasks = environment.getEnvironmentTasks();
        this.accessTimes = new HashMap<>();

        // get coverage times to each task for each sensor
        for(Instrument ins : payload){
            HashMap<Task, ArrayList<TimeInterval>> instrumentAccess = new HashMap<>();

            for(Task task : environmentTasks) {
                ArrayList<TimeInterval> taskAccess = new ArrayList<>();
                boolean access_i;
                boolean access_im = false;

                AbsoluteDate stepDate = this.startDate.getDate();
                TimeInterval interval = new TimeInterval();
                while (stepDate.compareTo(endDate) < 0) {
                    // calculate if spacecraft's instrument can access task
                    access_i = getAccess(ins, task, stepDate);

                    if(!access_im && access_i){
                        // access started
                        interval.setAccessStart(stepDate);
                    }
                    else if(access_im && !access_i){
                        // access ended
                        interval.setAccessEnd(stepDate);
                        taskAccess.add(interval.copy());
                        interval = new TimeInterval();
                    }
                    else if(access_im && access_i && (stepDate.compareTo(endDate) >= 0)){
                        // access continued until the end of the simulation
                        interval.setAccessEnd(stepDate);
                        taskAccess.add(interval.copy());
                        interval = new TimeInterval();
                    }

                    // set up next iteration
                    access_im = access_i;
                    stepDate = stepDate.shiftedBy(timeStep);
                }
                instrumentAccess.put(task,taskAccess);
            }
            accessTimes.put(ins, instrumentAccess);
        }
    }

    public boolean hasAccess(Instrument ins, Task task){
        ArrayList<TimeInterval> accessIntervals = accessTimes.get(ins).get(task);
        return (accessIntervals.size() > 0);
    }

    public boolean getAccess(Instrument ins, Task task, AbsoluteDate date) throws Exception {
        String scanType = ins.getScanningType();
        double fov = ins.getFOV();
        double lookAngle = ins.getLookAngle();
        Vector3D satPos = getPVEarth(date).getPosition();
        Vector3D satVel = getPVEarth(date).getVelocity();
        Vector3D taskPos = task.getPVEarth(date).getPosition();

        // check if in line of sight
        if(lineOfsight(satPos, taskPos)) {

            // check if in field of view of sensor
            switch (scanType) {
                case "side":
                    // calc location of task with respect to spaceraft in angles along and across track
                    double angleATdeg = rad2deg( getATAngle(satPos, satVel, taskPos) );
                    double angleCTdeg = rad2deg( getCTAngle(satPos, satVel, taskPos) );

                    // load scanning info
                    double scanningAngle = ins.getLookAngle();
                    double scanMin = lookAngle - fov/2.0 - scanningAngle;
                    double scanMax = lookAngle + fov/2.0 + scanningAngle;

                    boolean inFOVAT = (angleATdeg <= fov);
                    boolean inFOVCT = (angleCTdeg >= scanMin)&&(angleCTdeg <= scanMax);
                    return inFOVAT && inFOVCT;
                case "conical":
                    throw new Exception("Sensor scanning type not yet supported");
                default:
                    throw new Exception("Sensor scanning type not yet supported");
            }
        }
        else{
            return false;
        }
    }

    private double getATAngle(Vector3D satPos, Vector3D satVel, Vector3D taskPos){
        // declare unit vectors wrt satellite
        Vector3D satX = satVel.normalize();
        Vector3D satZ = satPos.normalize().scalarMultiply(-1);
        Vector3D satY = satZ.crossProduct(satX);

        // calculate task position relative to satellite
        Vector3D taskRelSat = taskPos.subtract(satPos);

        // calc projection of relative position on to sat x-z plane
        Vector3D relProj = satX.scalarMultiply( taskRelSat.dotProduct(satX) )
                .add( satZ.scalarMultiply( taskRelSat.dotProduct(satZ) ) );

        return Math.acos( relProj.dotProduct(satZ) / ( relProj.getNorm() * satZ.getNorm() ) );
    }

    private double getCTAngle(Vector3D satPos, Vector3D satVel, Vector3D taskPos){
        // declare unit vectors wrt satellite
        Vector3D satX = satVel.normalize();
        Vector3D satZ = satPos.normalize().scalarMultiply(-1);
        Vector3D satY = satZ.crossProduct(satX);

        // calculate task position relative to satellite
        Vector3D taskRelSat = taskPos.subtract(satPos);

        // calc projection of relative position on to sat x-z plane
        Vector3D relProj = satX.scalarMultiply( taskRelSat.dotProduct(satX) )
                .add( satZ.scalarMultiply( taskRelSat.dotProduct(satZ) ) );

        return Math.acos( relProj.dotProduct(taskRelSat) / ( relProj.getNorm() * taskRelSat.getNorm() ) );
    }

    @Override
    public void propagateOrbit() throws OrekitException {
        // load orekit data
        File orekitData = new File("./src/data/orekit-data");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));

        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));

        //initializes the look up tables for planteary position (required!)
        OrekitConfig.init(4);

        //define orbit
        double a = params.getSMA();
        double e = deg2rad( params.getECC());
        double i = deg2rad( params.getINC());
        double w = deg2rad( params.getAPRG());
        double Om = deg2rad( params.getRAAN());
        double v = deg2rad( params.getANOM());

        double mu = Constants.WGS84_EARTH_MU;
        KeplerianOrbit sat1_orbit = new KeplerianOrbit(a, e, i, w, Om, v, PositionAngle.MEAN, inertialFrame, startDate, mu);
        Propagator kepler = new KeplerianPropagator(sat1_orbit);

        // propagate by step-size
        AbsoluteDate stepDate = startDate.getDate();
        while(stepDate.compareTo(endDate) < 0){
            // calculate PV at this time, save to position vectors
            PVCoordinates pvStepInertial = kepler.propagate(stepDate).getPVCoordinates();
            PVCoordinates pvStepEarth = inertialFrame.getTransformTo(earthFrame, stepDate).transformPVCoordinates(pvStepInertial);
            pv.put(stepDate,pvStepInertial);
            pvEarth.put(stepDate,pvStepEarth);
            dates.add(stepDate);

            // advance a step
            stepDate = stepDate.shiftedBy(timeStep);
        }
    }

    @Override
    public PVCoordinates getPV(AbsoluteDate date) throws OrekitException {
        if(this.pv.containsKey(date)){
            // if already calculated, return value
            return this.pv.get(date);
        }
        else{
            // else propagate at that given time
            // load orekit data
            File orekitData = new File("./src/data/orekit-data");
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.addProvider(new DirectoryCrawler(orekitData));

            //if running on a non-US machine, need the line below
            Locale.setDefault(new Locale("en", "US"));

            //initializes the look up tables for planteary position (required!)
            OrekitConfig.init(4);

            //define orbit
            double a = params.getSMA();
            double e = deg2rad( params.getECC());
            double i = deg2rad( params.getINC());
            double w = deg2rad( params.getAPRG());
            double Om = deg2rad( params.getRAAN());
            double v = deg2rad( params.getANOM());

            double mu = Constants.WGS84_EARTH_MU;
            KeplerianOrbit sat1_orbit = new KeplerianOrbit(a, e, i, w, Om, v, PositionAngle.MEAN, inertialFrame, startDate, mu);
            Propagator kepler = new KeplerianPropagator(sat1_orbit);

            // calculate PV at this time, save to position vectors
            PVCoordinates pvStepInertial = kepler.propagate(date).getPVCoordinates();
            return pvStepInertial;
        }
    }

    @Override
    public PVCoordinates getPVEarth(AbsoluteDate date) throws OrekitException {
        if(this.pvEarth.containsKey(date)){
            // if already calculated, return value
            return this.pvEarth.get(date);
        }
        else{
            // else propagate at that given time
            PVCoordinates pvStepInertial = getPV(date);
            return inertialFrame.getTransformTo(earthFrame, date).transformPVCoordinates(pvStepInertial);
        }
    }

    private double rad2deg(double th){ return th*180.0/Math.PI; }
    private double deg2rad(double th){ return th*Math.PI/180.0; }
}
