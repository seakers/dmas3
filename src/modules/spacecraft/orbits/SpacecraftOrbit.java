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
    private HashMap<Task, ArrayList<TimeInterval>> lineOfSightTimes;

    public SpacecraftOrbit(OrbitParams params, ArrayList<Instrument> payload, Environment environment) throws OrekitException {
        super(environment.getStartDate(), environment.getEndDate(), environment.getTimeStep());
        this.params = params.copy();
        this.payload = payload;
    }

    public void calcLoSTimes(Environment environment) throws  Exception{
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
                los_i = calcLineOfSight(task, stepDate);

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

    public boolean calcLineOfSight(Task task, AbsoluteDate date) throws Exception {
        Vector3D satPos = getPVEarth(date).getPosition();
        Vector3D taskPos = task.getPVEarth(date).getPosition();

        // check if in line of sight
        return lineOfsight(satPos, taskPos);
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

    public double getAlt(AbsoluteDate date) throws OrekitException {
        // returns the altitude of the spacecraft in [m]
        double pos = this.getPVEarth(date).getPosition().getNorm();
        double Re = this.params.getRe();

        return pos-Re;
    }

    private double rad2deg(double th){ return th*180.0/Math.PI; }
    private double deg2rad(double th){ return th*Math.PI/180.0; }
    public HashMap<Instrument, HashMap<Task, ArrayList<TimeInterval>>> getAccessTimes(){ return accessTimes; }
    public HashMap<Task, ArrayList<TimeInterval>> getLineOfSightTimes(){return this.lineOfSightTimes;}
}
