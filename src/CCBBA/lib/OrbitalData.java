package CCBBA.lib;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.moeaframework.util.tree.Abs;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.PV;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.*;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import seakers.orekit.object.CoveragePoint;
import seakers.orekit.util.OrekitConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

public class OrbitalData {
    private double a;
    private double h;
    private double e;
    private double i;
    private double w;
    private double Om;
    private double v;
    private String dataFileName;
    private HashMap<Subtask, String> taskFileNames;
    private HashMap<AbsoluteDate, PVCoordinates> pvData;
    private HashMap<Task, HashMap<AbsoluteDate, Boolean>> accessData;
    private HashMap<Task, HashMap<AbsoluteDate, PVCoordinates>> groundTrack;
    private ArrayList<AbsoluteDate> dateData;

    //must use IERS_2003 and EME2000 frames to be consistent with STK
    private Frame inertialFrame = FramesFactory.getEME2000();

    public OrbitalData(double h, double e, double i, double w, double Om, double v){
        this.a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + h;
        this.h = h;
        this.e = e;
        this.i = i;
        this.w = w;
        this.Om = Om;
        this.v = v;

        dataFileName = "";
        taskFileNames = new HashMap<>();
        dateData = new ArrayList<>();
        pvData = new HashMap<>();
        accessData = new HashMap<>();
    }

    public void propagateOrbit(double fov, IterationResults localResults, AbsoluteDate startDate, AbsoluteDate endDate, double del_t) throws Exception {
        // generate orbit data file from orbital parameters and simulation start and end times
        DateTimeComponents startComp = startDate.getComponents(0);
        String start_date = startComp.getDate().toString();
        int s_hh = startComp.getTime().getHour();
        int s_mm = startComp.getTime().getMinute();
        double s_ss = startComp.getTime().getSecond();

        DateTimeComponents endComp = endDate.getComponents(0);
        String end_date = endComp.getDate().toString();
        int e_hh = endComp.getTime().getHour();
        int e_mm = endComp.getTime().getMinute();
        double e_ss = endComp.getTime().getSecond();

        dataFileName = String.format("a%.0f_e%.0f_i%.0f_w%.0f_om%.0f_v%.0f_s%s_%d-%d-%.0f_e%s_%d-%d-%.0f.csv" ,
                                                                        a, e, i, w, Om, v,
                                                                        start_date, s_hh, s_mm, s_ss,
                                                                        end_date, e_hh, e_mm, e_ss );

        for(IterationDatum datum : localResults.getResults()){
            Task parentTask = datum.getJ().getParentTask();
            String name = parentTask.getName();
            double lat = parentTask.getLat();
            double lon = parentTask.getLon();
            double alt = parentTask.getAlt();

            String taskDataName = String.format("%s_lat%.0f_lon%.0f_alt%.0f_s%s_%d-%d-%.0f_e%s_%d-%d-%.0f.csv", name, lat, lon, alt,
                                                                        start_date, s_hh, s_mm, s_ss,
                                                                        end_date, e_hh, e_mm, e_ss);

            taskFileNames.put(datum.getJ(), taskDataName);
        }

        // propagate orbits and generate data files
        // -agent orbit
        propagateAgentOrbit(startDate, endDate, del_t);

        // -task location
        for(IterationDatum datum : localResults.getResults()) {
            propagateTaskLocation( datum, startDate, endDate, del_t);
        }

        // -coverage analysis
        calculateCoverage(fov, localResults);

        // -calculate ground track
        calculateGroundTrack(localResults);
    }

    private void propagateAgentOrbit(AbsoluteDate startDate, AbsoluteDate endDate, double del_t) throws OrekitException, FileNotFoundException {
        // --create new file in directory
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String dataAddress = "./src/CCBBA/data/orbits/agents/" + dataFileName;

        // --check if file already exists
        if( !(new File(dataAddress)).exists() ) {
            // if orbit data file does not exist already, calculate orbit
            fileWriter = null;
            try {
                fileWriter = new FileWriter(dataAddress, false);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // propagate orbit
            File orekitData = new File("./src/orekit-data");
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.addProvider(new DirectoryCrawler(orekitData));

            //if running on a non-US machine, need the line below
            Locale.setDefault(new Locale("en", "US"));

            long start = System.nanoTime();

            //initializes the look up tables for planteary position (required!)
            OrekitConfig.init(4);

            //define orbit
            double mu = Constants.WGS84_EARTH_MU;
            KeplerianOrbit sat1_orbit = new KeplerianOrbit(a, e, i, w,Om, v, PositionAngle.MEAN, inertialFrame, startDate, mu);
            Propagator kepler = new KeplerianPropagator(sat1_orbit);

            // propagate by step-size
            AbsoluteDate extrapDate = startDate;
            printWriter = new PrintWriter(fileWriter);
            while (extrapDate.compareTo(endDate) <= 0)  {
                // package data
                PVCoordinates pvSat = kepler.propagate(extrapDate).getPVCoordinates();
                String position = String.format("%f,%f,%f", pvSat.getPosition().getX(), pvSat.getPosition().getY(), pvSat.getPosition().getZ() );
                String velocity = String.format("%f,%f,%f", pvSat.getVelocity().getX(), pvSat.getVelocity().getY(), pvSat.getVelocity().getZ() );
                String acceleration = String.format("%f,%f,%f", pvSat.getAcceleration().getX(), pvSat.getAcceleration().getY(), pvSat.getAcceleration().getZ() );

                // print to text file
                String stepData;
                if(extrapDate == startDate){
                    stepData = String.format(Locale.US, "%s,%s,%s,%s", extrapDate, position, velocity, acceleration);
                }
                else{
                    stepData = String.format(Locale.US, "\n%s,%s,%s,%s", extrapDate, position, velocity, acceleration);
                }
                printWriter.print(stepData);

                // save to position vector datum
                dateData.add(extrapDate);
                pvData.put(extrapDate, pvSat);

                // advance by step-size
                extrapDate = extrapDate.shiftedBy(del_t);
            }

            //close file
            printWriter.close();
        }
        else{
            // unpackage existing files
            File csvFile = new File(dataAddress);
            BufferedReader sc = new BufferedReader(new FileReader(csvFile));

            //define the start and end date of the simulation
            TimeScale utc = TimeScalesFactory.getUTC();
            String line = null;

            while (true) {
                try {
                    if ( (line =sc.readLine()) == null) break;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                // -unpack date
                String[] data = line.split(",");

                String dateString = data[0];

                int year = Integer.parseInt( String.format("%c%c%c%c", dateString.charAt(0), dateString.charAt(1), dateString.charAt(2), dateString.charAt(3)) );
                int month = Integer.parseInt( String.format("%c%c", dateString.charAt(5), dateString.charAt(6)) );
                int day = Integer.parseInt( String.format("%c%c", dateString.charAt(8), dateString.charAt(9)) );
                int hour = Integer.parseInt( String.format("%c%c", dateString.charAt(11), dateString.charAt(12)) );
                int minute = Integer.parseInt( String.format("%c%c", dateString.charAt(14), dateString.charAt(15)) );
                double second = Double.parseDouble( String.format("%c%c%c%c%c%c", dateString.charAt(17), dateString.charAt(18), dateString.charAt(19), dateString.charAt(20), dateString.charAt(21), dateString.charAt(22)) );

                AbsoluteDate date = new AbsoluteDate(year, month, day, hour, minute, second, utc);

                // unpack position vector
                double x_pos = Double.parseDouble( data[1] );
                double y_pos = Double.parseDouble( data[2] );
                double z_pos = Double.parseDouble( data[3] );
                double x_vel = Double.parseDouble( data[4] );
                double y_vel = Double.parseDouble( data[5] );
                double z_vel = Double.parseDouble( data[6] );
                double x_acc = Double.parseDouble( data[7] );
                double y_acc = Double.parseDouble( data[8] );
                double z_acc = Double.parseDouble( data[9] );

                Vector3D position = new Vector3D(x_pos, y_pos, z_pos);
                Vector3D velocity = new Vector3D(x_vel, y_vel, z_vel);
                Vector3D acceleration = new Vector3D(x_acc, y_acc, z_acc);
                PVCoordinates pvSat = new PVCoordinates(position, velocity, acceleration);

                dateData.add(date);
                pvData.put(date, pvSat);
            }
        }
    }

    private void propagateTaskLocation(IterationDatum datum, AbsoluteDate startDate, AbsoluteDate endDate, double del_t) throws OrekitException, FileNotFoundException {
        // --create new file in directory
        String filename = taskFileNames.get(datum.getJ());
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String dataAddress = "./src/CCBBA/data/orbits/tasks/" + filename;
        HashMap<AbsoluteDate, PVCoordinates> taskOrbitData = new HashMap<>();

        // --check if file already exists
        if( !(new File(dataAddress)).exists() ) {
            // if orbit data file does not exist already, calculate orbit
            fileWriter = null;
            try {
                fileWriter = new FileWriter(dataAddress, false);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // propagate orbit
            File orekitData = new File("./src/orekit-data");
            DataProvidersManager manager = DataProvidersManager.getInstance();
            manager.addProvider(new DirectoryCrawler(orekitData));

            //if running on a non-US machine, need the line below
            Locale.setDefault(new Locale("en", "US"));

            //initializes the look up tables for planteary position (required!)
            OrekitConfig.init(4);

            //must use IERS_2003 and EME2000 frames to be consistent with STK
            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    earthFrame);


            //define orbit
            double latitude = datum.getJ().getParentTask().getLat();
            double longitude = datum.getJ().getParentTask().getLon();
            double altitude = datum.getJ().getParentTask().getAlt();
            GeodeticPoint taskLocation = new GeodeticPoint(latitude, longitude, altitude);
            TopocentricFrame staF = new TopocentricFrame(earth, taskLocation, "task location");

            // propagate by step-size
            AbsoluteDate extrapDate = startDate;
            printWriter = new PrintWriter(fileWriter);
            while (extrapDate.compareTo(endDate) <= 0)  {
                // package data
                PVCoordinates pvStation = staF.getPVCoordinates(extrapDate, inertialFrame);
                String position = String.format("%f,%f,%f", pvStation.getPosition().getX(), pvStation.getPosition().getY(), pvStation.getPosition().getZ() );
                String velocity = String.format("%f,%f,%f", pvStation.getVelocity().getX(), pvStation.getVelocity().getY(), pvStation.getVelocity().getZ() );
                String acceleration = String.format("%f,%f,%f", pvStation.getAcceleration().getX(), pvStation.getAcceleration().getY(), pvStation.getAcceleration().getZ() );

                // print to text file
                String stepData;
                if(extrapDate == startDate){
                    stepData = String.format(Locale.US, "%s,%s,%s,%s", extrapDate, position, velocity, acceleration);
                }
                else{
                    stepData = String.format(Locale.US, "\n%s,%s,%s,%s", extrapDate, position, velocity, acceleration);
                }
                printWriter.print(stepData);

                // save to position vector datum
                taskOrbitData.put(extrapDate, pvStation);

                // advance by step-size
                extrapDate = extrapDate.shiftedBy(del_t);
            }

            //close file
            printWriter.close();

            //save data to iteration results
            datum.setTaskOrbitData(taskOrbitData);
        }
        else{// unpackage existing files
            File csvFile = new File(dataAddress);
            BufferedReader sc = new BufferedReader(new FileReader(csvFile));

            //define the start and end date of the simulation
            TimeScale utc = TimeScalesFactory.getUTC();
            String line = null;

            while (true) {
                try {
                    if ( (line =sc.readLine()) == null) break;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                // -unpack date
                String[] data = line.split(",");
                String dateString = data[0];

                int year = Integer.parseInt( String.format("%c%c%c%c", dateString.charAt(0), dateString.charAt(1), dateString.charAt(2), dateString.charAt(3)) );
                int month = Integer.parseInt( String.format("%c%c", dateString.charAt(5), dateString.charAt(6)) );
                int day = Integer.parseInt( String.format("%c%c", dateString.charAt(8), dateString.charAt(9)) );
                int hour = Integer.parseInt( String.format("%c%c", dateString.charAt(11), dateString.charAt(12)) );
                int minute = Integer.parseInt( String.format("%c%c", dateString.charAt(14), dateString.charAt(15)) );
                double second = Double.parseDouble( String.format("%c%c%c%c%c%c", dateString.charAt(17), dateString.charAt(18), dateString.charAt(19), dateString.charAt(20), dateString.charAt(21), dateString.charAt(22)) );

                AbsoluteDate date = new AbsoluteDate(year, month, day, hour, minute, second, utc);

                // unpack position vector
                double x_pos = Double.parseDouble( data[1] );
                double y_pos = Double.parseDouble( data[2] );
                double z_pos = Double.parseDouble( data[3] );
                double x_vel = Double.parseDouble( data[4] );
                double y_vel = Double.parseDouble( data[5] );
                double z_vel = Double.parseDouble( data[6] );
                double x_acc = Double.parseDouble( data[7] );
                double y_acc = Double.parseDouble( data[8] );
                double z_acc = Double.parseDouble( data[9] );

                Vector3D position = new Vector3D(x_pos, y_pos, z_pos);
                Vector3D velocity = new Vector3D(x_vel, y_vel, z_vel);
                Vector3D acceleration = new Vector3D(x_acc, y_acc, z_acc);
                PVCoordinates pvStation = new PVCoordinates(position, velocity, acceleration);

                // save to position vector datum
                taskOrbitData.put(date, pvStation);
            }

            //save data to iteration results
            datum.setTaskOrbitData(taskOrbitData);
        }
    }

    private void calculateCoverage(double fov, IterationResults localResults){
        for(IterationDatum datum : localResults.getResults()){
            Task parentTask = datum.getJ().getParentTask();
            if(!accessData.containsKey(parentTask)){
                // no coverage calculated yet,
                HashMap<AbsoluteDate, Boolean> taskCoverage = new HashMap<>();

                for(AbsoluteDate date : dateData){
                    PVCoordinates satPV = pvData.get(date);
                    PVCoordinates taskPV = datum.getTaskOrbitData(date);

                    taskCoverage.put(date, isInFOV(fov, satPV, taskPV));
                }

                accessData.put(parentTask, taskCoverage);
            }
        }
    }

    private void calculateGroundTrack(IterationResults localResults) throws OrekitException {
        this.groundTrack = new HashMap<>();

        for(IterationDatum datum : localResults.getResults()){
            Task parentTask = datum.getJ().getParentTask();
            if(!groundTrack.containsKey(parentTask)){
                // define task location
                double latitude = datum.getJ().getParentTask().getLat();
                double longitude = datum.getJ().getParentTask().getLon();
                double altitude = datum.getJ().getParentTask().getAlt();

                // define frames
                Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
                BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                        Constants.WGS84_EARTH_FLATTENING,
                        earthFrame);
                GeodeticPoint taskLocation = new GeodeticPoint(latitude, longitude, altitude);
                TopocentricFrame staF = new TopocentricFrame(earth, taskLocation, "task location");

                HashMap<AbsoluteDate, PVCoordinates> taskTrack = new HashMap<>();

                for(AbsoluteDate date : dateData){
                    PVCoordinates satPV = pvData.get(date).normalize();
                    Vector3D satPos = satPV.getPosition();
                    Vector3D satVel = satPV.getVelocity();
                    Vector3D satAcc = satPV.getAcceleration();

                    Vector3D stepPos = satPos.scalarMultiply( Constants.WGS84_EARTH_EQUATORIAL_RADIUS );
                    PVCoordinates stepPV = new PVCoordinates(stepPos, satVel, satAcc);

//                    PVCoordinates track = inertialFrame.getTransformTo(staF, date).transformPVCoordinates(stepPV);
//                    taskTrack.put(date, track);
                    taskTrack.put(date, stepPV);
                }

                groundTrack.put(parentTask, taskTrack);
            }
        }
    }

    private boolean isInFOV(double fov, PVCoordinates satPV, PVCoordinates taskPV){
        Vector3D satPos = satPV.getPosition();
        Vector3D taskPos = taskPV.getPosition();
        double Re = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;

        double th = Math.acos( satPos.dotProduct(taskPos) / (satPos.getNorm() * taskPos.getNorm()) );
        double th_1 = Math.acos( Re / satPos.getNorm() );
        double th_2 = Math.acos( Re / taskPos.getNorm() );
                if(Double.isNaN(th_2)){ th_2 = 0.0; }
        double th_max = th_1 + th_2;

        double th_disp = FastMath.toDegrees(th);
        double th_max_disp = FastMath.toDegrees(th_max);
        double fov_disp = FastMath.toDegrees(fov);

        boolean isInLineOfSight = (th <= th_max);
        boolean isInSensorFieldOfView = ((FastMath.PI/2 - th_1) <= fov);

        return isInLineOfSight && isInSensorFieldOfView;
//        return isInLineOfSight;
    }

    public boolean hasAccessTo(Subtask j){
        // checks if agent has access to task in its orbit
        Task parentTask = j.getParentTask();

        for(AbsoluteDate date : dateData){
            if( accessData.get(parentTask).get(date) ){
                return true;
            }
        }

        return false;
    }

    public ArrayList<AccessTime> getAccessTimes(Subtask j){
        // returns a list of all access times from a specific time
        Task parentTask = j.getParentTask();
        ArrayList<AccessTime> accessTimes = new ArrayList<>();

        AbsoluteDate accesStart = null;
        AbsoluteDate accessEnd = null;

        boolean prevAccess = false;
        boolean currAccess = false;
        for(AbsoluteDate date : dateData){
            currAccess = accessData.get(parentTask).get(date);

            if(!prevAccess && currAccess){
                // access started at this time
                accesStart = date;
            }
            if(prevAccess && !currAccess){
                // access ended at this time
                accessEnd = date;
            }

            if(accesStart != null && accessEnd != null){
                // Access time window found
                AccessTime newAccessTime = new AccessTime(accesStart, accessEnd);
                accessTimes.add(newAccessTime);

                accesStart = null;
                accessEnd = null;
            }

            // save previous iteration
            prevAccess = currAccess;
        }

        return accessTimes;
    }

    public double getA() {
        return a;
    }
    public double getH() {
        return h;
    }
    public double getE() {
        return e;
    }
    public double getI() {
        return i;
    }
    public double getW() { return w; }
    public double getOm() { return Om; }
    public double getV() { return v; }
    public HashMap<AbsoluteDate, PVCoordinates> getGroundTrack(Subtask j){ return this.groundTrack.get(j.getParentTask()); }
    public ArrayList<AbsoluteDate> getDateData(){ return this.dateData; }
    public PVCoordinates getInitialLocation(){
        AbsoluteDate startDate = this.dateData.get(0);
        return this.pvData.get(startDate);
    }
    public PVCoordinates getNextLocation(AbsoluteDate currDate) throws Exception {
        AbsoluteDate nextDate = this.getNextDate(currDate);
        return this.pvData.get(nextDate);
    }

    public AbsoluteDate getNextDate(AbsoluteDate currDate) throws Exception{
        boolean currDateFound = false;
        for(AbsoluteDate date : this.dateData){
            if(currDateFound){
                return date;
            }
            currDateFound = ( date.equals(currDate) );
        }
        throw new Exception("Current date not contained in orbit propagation");
    }

    public PVCoordinates getGroundLocation(AbsoluteDate currDate, Subtask j){
        return this.groundTrack.get(j).get(currDate);
    }

    public void setA(double a) {
        this.a = a;
    }
    public void setH(double h) {
        this.h = h;
    }
    public void setE(double e) { this.e = e; }
    public void setI(double i) { this.i = i; }
    public void setW(double w) { this.w = w; }
    public void setOm(double om) { Om = om; }
    public void setV(double v) { this.v = v; }
}
