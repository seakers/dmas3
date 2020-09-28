package modules.spacecraft.orbits;

import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
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
    private HashMap<Task, ArrayList<TimeInterval>> lineOfSightTimes;

    public SpacecraftOrbit(OrbitParams params, ArrayList<Instrument> payload, Environment environment) throws OrekitException {
        super(environment.getStartDate(), environment.getEndDate(), environment.getTimeStep());
        this.params = params.copy();
        this.payload = payload;
    }

    public void calcLoSTimes(Spacecraft parent, Environment environment) throws  Exception{
        ArrayList<Task> environmentTasks = environment.getEnvironmentTasks();
        this.lineOfSightTimes = new HashMap<>();

        // get coverage times to each task for each sensor
        for(Task task : environmentTasks) {
            ArrayList<TimeInterval> taskLoS = new ArrayList<>();
            boolean los_i;
            boolean los_im = false;

            AbsoluteDate stepDate = this.startDate.getDate();
            TimeInterval interval = new TimeInterval();
            while (stepDate.compareTo(endDate) < 0) {
                // calculate if spacecraft's instrument can access task
                los_i = calcLineOfSight(parent, task, stepDate);

                if(!los_im && los_i){
                    // access started
                    interval.setAccessStart(stepDate);
                }
                else if(los_im && !los_i){
                    // access ended
                    interval.setAccessEnd(stepDate);
                    taskLoS.add(interval.copy());
                    interval = new TimeInterval();
                }
                else if(los_im && los_i && (stepDate.compareTo(endDate) >= 0)){
                    // access continued until the end of the simulation
                    interval.setAccessEnd(stepDate);
                    taskLoS.add(interval.copy());
                    interval = new TimeInterval();
                }

                // set up next iteration
                los_im = los_i;
                stepDate = stepDate.shiftedBy(timeStep);
            }
            lineOfSightTimes.put(task,taskLoS);
        }
    }

    public boolean calcLineOfSight(Spacecraft spacecraft, Task task, AbsoluteDate date) throws Exception {
        Vector3D satPos = getPVEarth(date).getPosition();
        Vector3D taskPos = task.getPVEarth(date).getPosition();

        // check if in line of sight
        if(!lineOfsight(satPos, taskPos)) return false;

        ArrayList<Vector3D> orbitFrame = calcOrbitFrame(this,date);
        double fovMin = 1e10;
        Instrument insMin = null;
        for(Instrument ins : spacecraft.getDesign().getPayload()){
            if(ins.getFOV() < fovMin) {
                insMin = ins;
                fovMin = ins.getFOV();
            }
        }
        String fovType = insMin.getFovType();

        if(fovType.equals("square") || fovType.equals("circular")){
            double fov = deg2rad( insMin.getFOV() );

            double ATangleTask = getTaskATAngle(orbitFrame, this.getPVEarth(date).getPosition(), taskPos);

            if(insMin.getScanningType().equals("side")){
                return (Math.abs(ATangleTask) <= fov/2.0);
            }
            else{
                throw new Exception("Scanning type not yet supported");
            }

        }
        else{
            throw new Exception("FOV type not yet supported");
        }
    }

    private double getTaskATAngle(ArrayList<Vector3D> orbitFrame, Vector3D satPos, Vector3D taskPos){
        // declare unit vectors wrt satellite
        Vector3D satX = orbitFrame.get(0);
        Vector3D satY = orbitFrame.get(1);
        Vector3D satZ = orbitFrame.get(2);

        // calculate task position relative to satellite
        Vector3D taskRelSat = taskPos.subtract(satPos);

        // calc projection of relative position on to sat x-z plane
        Vector3D relProj = satX.scalarMultiply( taskRelSat.dotProduct(satX) )
                .add( satZ.scalarMultiply( taskRelSat.dotProduct(satZ) ) ).normalize();

        double dot = relProj.dotProduct(satZ) / ( relProj.getNorm() * satZ.getNorm() );
        if(dot > 1.0 && dot <= 1.0+1e-3){
            dot = 1.0;
        }
        return Math.acos( dot );
    }

    private ArrayList<Vector3D> calcOrbitFrame(SpacecraftOrbit orbit, AbsoluteDate startDate) throws Exception {
        // returns 3 vectors representing the frame of the orbital direction
        // x points towards velocity, z towards the ground, and y to the right of x
        Vector3D x_bod = orbit.getPVEarth(startDate).getVelocity().normalize();
        Vector3D z_bod = orbit.getPVEarth(startDate).getPosition().scalarMultiply(-1).normalize();
        Vector3D y_bod = z_bod.crossProduct(x_bod);

        ArrayList<Vector3D> orbitFrame = new ArrayList<>();
        orbitFrame.add(x_bod);
        orbitFrame.add(y_bod);
        orbitFrame.add(z_bod);

        if(x_bod.getNorm() > 1+1e-3 || y_bod.getNorm() > 1+1e-3 || z_bod.getNorm() > 1+1e-3 ){
            throw new Exception("orbital frame calculation gives non-unit vectors");
        }

        return orbitFrame;
    }

    public boolean hasAccess(Instrument ins, Task task){
        ArrayList<TimeInterval> accessIntervals = lineOfSightTimes.get(task);
        return (accessIntervals.size() > 0);
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
        double e = params.getECC();
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
//            pv.put(stepDate,pvStepInertial);
//            pvEarth.put(stepDate,pvStepEarth);
            pvEarth.add(pvStepEarth);
            dates.add(stepDate);

            // advance a step
            stepDate = stepDate.shiftedBy(timeStep);
        }
    }

    @Override
    public PVCoordinates getPVEarth(AbsoluteDate date) {
//        if(this.pvEarth.containsKey(date)){
//            // if already calculated, return value
//            return this.pvEarth.get(date);
//        }
//        else{
//            // else propagate at that given time
//            PVCoordinates pvStepInertial = getPV(date);
//            return inertialFrame.getTransformTo(earthFrame, date).transformPVCoordinates(pvStepInertial);
//        }
        try {
            if(date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0){
                int i = (int) (date.durationFrom(startDate)/timeStep);
                return pvEarth.get(i);
            } else {
                throw new Exception("PV of satellite not calculated for time " + date.toString());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public double getAlt(AbsoluteDate date) throws Exception {
        // returns the altitude of the spacecraft in [m]
        double pos = this.getPVEarth(date).getPosition().getNorm();
        double Re = this.params.getRe();

        return pos-Re;
    }

    public void removeLineOfSightTimes(Subtask j, ArrayList<TimeInterval> intervalsToRemove){
        ArrayList<TimeInterval> intervals = this.lineOfSightTimes.get(j.getParentTask());
        for(TimeInterval removedInterval : intervalsToRemove){
            if(intervals.size() > 1) intervals.remove(removedInterval);
            else if(intervals.contains(removedInterval)) intervals = new ArrayList<>();
        }
        int x = 1;
    }

    private double rad2deg(double th){ return th*180.0/Math.PI; }
    private double deg2rad(double th){ return th*Math.PI/180.0; }
    public HashMap<Instrument, HashMap<Task, ArrayList<TimeInterval>>> getAccessTimes(){ return accessTimes; }
    public HashMap<Task, ArrayList<TimeInterval>> getLineOfSightTimes(){return this.lineOfSightTimes;}
}
