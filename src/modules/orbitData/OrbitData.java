package modules.orbitData;

import constants.MeasurementTypes;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import modules.agents.SatelliteAgent;
import modules.antennas.AbstractAntenna;
import modules.antennas.ParabolicAntenna;
import modules.instruments.SAR;
import modules.simulation.Dmas;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.json.simple.JSONObject;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.event.CrossLinkEventAnalysis;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.FieldOfViewAndGndStationEventAnalysis;
import seakers.orekit.object.*;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.object.fieldofview.OffNadirRectangularFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.util.OrekitConfig;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static constants.JSONFields.*;

public class OrbitData {
    /**
     * Directories containing information about the constellation selected and previous coverage calculations
     */
    private final JSONObject input;
    private final String orekitDataDir;
    private final String databaseDir;
    private final String coverageDir;
    private final String scenarioDir;
    private final String constellationsDir;

    /**
     * Constants used in orbit propagation
     */
    private final Frame earthFrame;
    private final Frame inertialFrame;
    private final BodyShape earthShape;
    private final TimeScale utc;
    private double mu;

    /**
     * Loaded information from json file as strings
     * @param consStr : name of chosen constellation
     * @param gsNetworkStr : name of chosen Ground Station Network
     * @param scenarioStr : name of chosen scenario
     * @param startDateStr : start date of simulation
     * @param endDateStr : end date of simulation
     */
    String consStr;
    String gsNetworkStr;
    String scenarioStr;
    String startDateStr;
    String endDateStr;

    /**
     * Directory address to where orbit data will be saved to or loaded from if needed
     */
    String directoryAddress;

    /**
     * Start and End dates for simulation
     */
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    /**
//     * List of instruments in instrument database from which satellites can choose to create their payload
//     */
    private final HashMap<String, Instrument> instrumentList;

    /**
     * Propagator used in coverage and trajectory calculations
     */
    private final PropagatorFactory pfJ2;
    private final PropagatorFactory pfKep;

    /**
     * Coverage results from propagation
     * @param accessesCL : Cross Link access opportunity
     * @param accessesGP : Ground Point access opportunity
     * @param accessesGS : Ground Station access opportunity
     */
    private HashMap<Constellation, HashMap<Satellite, HashMap<Satellite, TimeIntervalArray>>> accessesCL;
    private HashMap<CoverageDefinition, HashMap<Satellite,
            HashMap<TopocentricFrame, TimeIntervalArray>>> accessesGP;
    private HashMap<CoverageDefinition, HashMap<Satellite, HashMap<Instrument,
            HashMap<TopocentricFrame, TimeIntervalArray>>>> accessesGPInst;
    private HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> accessesGS;

    /**
     *  Constellations of sensing and communication satellites chosen for simulation
     */
    ArrayList<Constellation> constellations;

    /**
     *  Coverage definitions chosen for simulation
     */
    HashSet<CoverageDefinition> covDefs;

    /**
     *  Ground Station to Satellite assignment for chosen ground communications network and constellations
     */
    HashMap<Satellite, Set<GndStation>> stationAssignment;

    /**
     * Constructor
     * @param input JSON Object containing information about the constellation and scenario chosen for this simulation
     * @param orekitDataDir Location of Orekit Data directory
     * @param databaseDir Location of Instrument and Ground Station databases
     * @param coverageDir Location of previous coverage calculations
     * @param constellationsDir Location of constellation databases
     * @param scenarioDir Location of scenario databases
     */
    public OrbitData(JSONObject input, String orekitDataDir, String databaseDir,
                     String coverageDir, String constellationsDir, String scenarioDir) throws Exception {
        this.input = input;
        this.orekitDataDir = orekitDataDir;
        this.databaseDir = databaseDir;
        this.coverageDir = coverageDir;
        this.constellationsDir = constellationsDir;
        this.scenarioDir = scenarioDir;

        // load Orekit data
        loadOrekitData();
        earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        inertialFrame = FramesFactory.getEME2000();
        earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
        utc = TimeScalesFactory.getUTC();
        mu = Constants.WGS84_EARTH_MU;

        // read json file inputs
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyymmdd_HHmmssSSSS"));

        consStr = ((JSONObject) input.get(SIM)).get(CONS).toString();
        gsNetworkStr = ((JSONObject) input.get(SIM)).get(GND_STATS).toString();
        scenarioStr = ((JSONObject) input.get(SIM)).get(SCENARIO).toString();
        startDateStr = ((JSONObject) input.get(SIM)).get(START_DATE).toString();
        endDateStr = ((JSONObject) input.get(SIM)).get(END_DATE).toString();

        // Create dates from input file
        startDate = stringToDate(startDateStr);
        endDate = stringToDate(endDateStr);

        String yyyy_s = String.valueOf(startDate.getComponents(utc).getDate().getYear());
        String mm_s = String.valueOf(startDate.getComponents(utc).getDate().getMonth());
        String dd_s = String.valueOf(startDate.getComponents(utc).getDate().getDay());

        if(Integer.parseInt(mm_s) < 10) mm_s = "0" + mm_s;
        if(Integer.parseInt(dd_s) < 10) dd_s = "0" + dd_s;

        String HH_s = String.valueOf(startDate.getComponents(utc).getTime().getHour());
        String MM_s = String.valueOf(startDate.getComponents(utc).getTime().getMinute());
        String SS_s = String.valueOf(startDate.getComponents(utc).getTime().getSecond());

        if(Integer.parseInt(HH_s) < 10) HH_s = "0" + HH_s;
        if(Integer.parseInt(MM_s) < 10) MM_s = "0" + MM_s;
        if(Double.parseDouble(SS_s) < 10.0) SS_s = "0" + SS_s;

        String startDateStrDir =  yyyy_s + mm_s + dd_s + "-" + HH_s + MM_s + SS_s.replace(".","");

        String yyyy_e = String.valueOf(startDate.getComponents(utc).getDate().getYear());
        String mm_e = String.valueOf(startDate.getComponents(utc).getDate().getMonth());
        String dd_e = String.valueOf(startDate.getComponents(utc).getDate().getDay());

        if(Integer.parseInt(mm_e) < 10) mm_e = "0" + mm_e;
        if(Integer.parseInt(dd_e) < 10) dd_e = "0" + dd_e;

        String HH_e = String.valueOf(startDate.getComponents(utc).getTime().getHour());
        String MM_e = String.valueOf(startDate.getComponents(utc).getTime().getMinute());
        String SS_e = String.valueOf(startDate.getComponents(utc).getTime().getSecond());

        if(Integer.parseInt(HH_e) < 10) HH_e = "0" + HH_e;
        if(Integer.parseInt(MM_e) < 10) MM_e = "0" + MM_e;
        if(Double.parseDouble(SS_e) < 10.0) SS_e = "0" + SS_e;

        String endDateStrDir =  yyyy_e + mm_e + dd_e + "-" + HH_e + MM_e + SS_e.replace(".","");

        // if data has been calculated before, import data
        String dirName = consStr + "_" + gsNetworkStr + "_" + scenarioStr + "_" + startDateStrDir + "_" + endDateStrDir;
        directoryAddress = coverageDir + dirName;

        // Initialize Instrument database
        instrumentList = new HashMap<>();

        // Read scenario information from excel data and generate orbital parameters and coverage definitions
        constellations = loadConstellation(consStr, startDate, endDate);
        covDefs = loadCoverageDefinitions(scenarioStr, constellations);
        stationAssignment = loadGroundStations(gsNetworkStr, constellations);

        // Create and set up propagator
        Properties propertiesPropagator = setPropagatorProperties();
        pfJ2 = new PropagatorFactory(PropagatorType.J2,propertiesPropagator);
        pfKep = new PropagatorFactory(PropagatorType.KEPLERIAN,propertiesPropagator);
    }



    /**
     * Prints csv file of all ground points of all desired coverage definitions to be used for plotting
     * All in lat-lon
     */
    public void printGP(){
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = directoryAddress + "/" + "CovDefs.csv";
        File f = new File(outAddress);

        if(!f.exists()) {
            // if file does not exist yet, save data
            try{
                fileWriter = new FileWriter(outAddress, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            printWriter = new PrintWriter(fileWriter);

            for (CoverageDefinition covDef : covDefs) {
                for (CoveragePoint point : covDef.getPoints()) {
                    String pointStr = genPtStr(covDef, point);
                    printWriter.print(pointStr);
                }
            }
            printWriter.close();
        }
    }

    private String genPtStr(CoverageDefinition covDef, CoveragePoint pt){
        StringBuilder out = new StringBuilder();

        String defName = covDef.getName();
        String ptName = pt.getName();
        double lat = FastMath.toDegrees( pt.getPoint().getLatitude() );
        double lon = FastMath.toDegrees( pt.getPoint().getLongitude() );
        double alt = FastMath.toDegrees( pt.getPoint().getAltitude() );

        out.append(defName + "," + ptName + "," + lat + "," + lon + "," + alt + "\n");
        return out.toString();
    }

    /**
     * Propagates orbit and saves position vector and ground track lat-lon to csv file
     * All in the Earth Frame
     */
    public void trajectoryCalc(){
        double timestep = Double.parseDouble( ((JSONObject) input.get(SETTINGS)).get(TIMESTEP).toString() );

        for(Constellation cons : constellations){
            for(Satellite sat : cons.getSatellites()){

                FileWriter fileWriter = null;
                PrintWriter printWriter;
                String outAddress = directoryAddress + "/" + sat.getName() + "_pv.csv";
                File f = new File(outAddress);
                fileWriter = null;

                if(!f.exists()) {
                    // calc trajectory data
                    Propagator prop = null;
                    if(Math.abs( sat.getOrbit().getI() ) <= 0.1){
                        // if orbit is equatorial, use Keplerian propagator
                        prop = pfKep.createPropagator(sat.getOrbit(), sat.getGrossMass());
                    }
                    else{
                        // else use J2 propagator
                        prop = pfJ2.createPropagator(sat.getOrbit(), sat.getGrossMass());
                    }

                    ArrayList<PVCoordinates> pvSat = new ArrayList<>();
                    ArrayList<GeodeticPoint> gtSat = new ArrayList<>();
                    ArrayList<Double> time = new ArrayList<>();
                    for (double t = 0; t < endDate.durationFrom(startDate); t += timestep) {

                        try {
                            SpacecraftState stat = prop.propagate(startDate.shiftedBy(t));
                            pvSat.add(stat.getPVCoordinates(earthFrame));

                            Vector3D pos = stat.getPVCoordinates(earthFrame).getPosition().normalize();
                            double lat = FastMath.atan2(pos.getZ(), FastMath.sqrt(pos.getX() * pos.getX() + pos.getY() * pos.getY()));
                            double lon = FastMath.atan2(pos.getY(), pos.getX());
                            gtSat.add(new GeodeticPoint(lat, lon, 0.0));

                            time.add(t);
                        } catch (OrekitException | NullPointerException e) {
                            e.printStackTrace();
                        }
                    }

                    // save data
                    try{
                        fileWriter = new FileWriter(outAddress, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    printWriter = new PrintWriter(fileWriter);

                    for (int i = 0; i < pvSat.size(); i++) {
                        String posStr = genPVstr(pvSat.get(i), gtSat.get(i), time.get(i));
                        printWriter.print(posStr);
                    }

                    //close file
                    printWriter.close();
                }
            }
        }
    }

    public Vector3D getSatPosition(Satellite sat, AbsoluteDate date) throws Exception {
        // choose an orbit propagator
        Propagator prop;
        if(Math.abs( sat.getOrbit().getI() ) <= 0.1){
            // if orbit is equatorial, use Keplerian propagator
            prop = pfKep.createPropagator(sat.getOrbit(), sat.getGrossMass());
        }
        else{
            // else use J2 propagator
            prop = pfJ2.createPropagator(sat.getOrbit(), sat.getGrossMass());
        }

        double t = date.durationFrom(startDate);
        if(t < 0) throw new Exception("Date of measurement is before simulation start time");

        SpacecraftState stat = prop.propagate(startDate.shiftedBy(t));
        return stat.getPVCoordinates(earthFrame).getPosition();
    }

    public Vector3D getSatVelocity(Satellite sat, AbsoluteDate date) throws Exception {
        // choose an orbit propagator
        Propagator prop;
        if(Math.abs( sat.getOrbit().getI() ) <= 0.1){
            // if orbit is equatorial, use Keplerian propagator
            prop = pfKep.createPropagator(sat.getOrbit(), sat.getGrossMass());
        }
        else{
            // else use J2 propagator
            prop = pfJ2.createPropagator(sat.getOrbit(), sat.getGrossMass());
        }

        double t = date.durationFrom(startDate);
        if(t < 0) throw new Exception("Date of measurement is before simulation start time");

        SpacecraftState stat = prop.propagate(startDate.shiftedBy(t));
        return stat.getPVCoordinates(earthFrame).getVelocity();
    }

    public Vector3D getPointPosition(TopocentricFrame point, AbsoluteDate date) throws Exception {
        return point.getPVCoordinates(date, earthFrame).getPosition();
    }

    /**
     * Generates string containing position vector and lat-lon of a satellite
     * @param pv Position vector of satellite
     * @param gt Ground point of satellite
     * @param epoch
     * @return
     */
    private String genPVstr(PVCoordinates pv, GeodeticPoint gt, double epoch){
        StringBuilder out = new StringBuilder();
        double x = pv.getPosition().getX();
        double y = pv.getPosition().getY();
        double z = pv.getPosition().getZ();
        double lat = FastMath.toDegrees( gt.getLatitude() );
        double lon = FastMath.toDegrees( gt.getLongitude() );

        out.append(epoch + "," + x + "," + y + "," + z + "," + lat + "," + lon + "\n");
        return out.toString();
    }

    /**
     * Loads or calculates coverage calculation for a scenario's coverage definition and ground stations
     * as well as calculating time windows for cross-links between satellites
     * @throws Exception
     */
    public void coverageCalc() throws Exception {
        File f = new File(directoryAddress);
        if(f.exists()){
            // import data
            this.accessesCL = loadCrossLinkAccesses();
            this.accessesGP = loadGroundPointAccesses();
            this.accessesGPInst = loadGroundPointInstrumentAccesses();
            this.accessesGS = loadGroundStationAccesses();
        }
        else{ // Else, calculate information
            // create directory
            new File( directoryAddress ).mkdir();

            // propagate scenario to calculate accesses and cross-links
            Scenario scen = coverageScenario(constellations, covDefs, stationAssignment, startDate, endDate);

            // save data in object variables for sims to use
            ArrayList<EventAnalysis> events = (ArrayList<EventAnalysis>) scen.getEventAnalyses();
            this.accessesCL = ((CrossLinkEventAnalysis) events.get(0)).getAllAccesses();

            this.accessesGP = ((FieldOfViewAndGndStationEventAnalysis) events.get(1)).getAllAccesses();
            this.accessesGPInst = ((FieldOfViewAndGndStationEventAnalysis) events.get(1)).getAllAccessesInst();
            this.accessesGS = ((FieldOfViewAndGndStationEventAnalysis) events.get(1)).getAllAccessesGS();

            printAccesses();
        }
    }

    private HashMap<Constellation, HashMap<Satellite, HashMap<Satellite, TimeIntervalArray>>> loadCrossLinkAccesses() throws Exception {
        // Initialize map
        HashMap<Constellation, HashMap<Satellite, HashMap<Satellite, TimeIntervalArray>>> access = new HashMap<>();
        for(Constellation cons : constellations){
            access.put(cons, new HashMap<>());
            for(Satellite sat : cons.getSatellites()){
                access.get(cons).put(sat, new HashMap<>());
                for(Constellation consTgt : constellations){
                    for(Satellite target : consTgt.getSatellites()){
                        access.get(cons).get(sat).put(target, new TimeIntervalArray(startDate, endDate));

                    }
                }
            }
        }

        //parsing a CSV file into Scanner class constructor
        String outAddress = directoryAddress + "/" + "CrossLinks.csv";
        Scanner sc = new Scanner( new File(outAddress) );
        sc.useDelimiter("\n");   //sets the delimiter pattern
        while (sc.hasNext())  //returns a boolean value
        {
            String line = sc.next();
            String[] fields = line.split(",");

            String satStr = fields[0];
            String targetStr = fields[1];
            double t_0 = Double.parseDouble( fields[2] );
            double t_f = Double.parseDouble( fields[3] );

            Satellite sat = getSat(satStr);
            Satellite target = getSat(targetStr);
            Constellation cons = getCons(sat);

            access.get(cons).get(sat).get(target).addRiseTime(t_0);
            access.get(cons).get(sat).get(target).addSetTime(t_f);
        }
        sc.close();  //closes the scanner

        return access;
    }

    private HashMap<CoverageDefinition, HashMap<Satellite,
            HashMap<TopocentricFrame, TimeIntervalArray>>>  loadGroundPointAccesses() throws Exception {
        // Initialize map
        HashMap<CoverageDefinition, HashMap<Satellite,
                HashMap<TopocentricFrame, TimeIntervalArray>>>  access = new HashMap<>();
        for(CoverageDefinition covDef : covDefs){
            access.put(covDef, new HashMap<>());
            for(Satellite sat : this.getUniqueSats()){
                access.get(covDef).put(sat, new HashMap<>());
                for(CoveragePoint pt : covDef.getPoints()){
                    access.get(covDef).get(sat).put(pt, new TimeIntervalArray(startDate, endDate));
                }
            }
        }

        //parsing a CSV file into Scanner class constructor
        String outAddress = directoryAddress + "/" + "GPCoverage.csv";
        Scanner sc = new Scanner( new File(outAddress) );
        sc.useDelimiter("\n");   //sets the delimiter pattern
        while (sc.hasNext())  //returns a boolean value
        {
            String line = sc.next();
            String[] fields = line.split(",");

            String covDefStr = fields[0];
            String satStr = fields[1];
            String targetStr = fields[2];
            double t_0 = Double.parseDouble( fields[3] );
            double t_f = Double.parseDouble( fields[4] );

            CoverageDefinition covDef = getCovDef(covDefStr);
            Satellite sat = getSat(satStr);
            CoveragePoint target = getPoint(covDefStr, targetStr);


            access.get(covDef).get(sat).get(target).addRiseTime(t_0);
            access.get(covDef).get(sat).get(target).addSetTime(t_f);
        }
        sc.close();  //closes the scanner

        return access;
    }

    private HashMap<CoverageDefinition, HashMap<Satellite, HashMap<Instrument,
            HashMap<TopocentricFrame, TimeIntervalArray>>>>  loadGroundPointInstrumentAccesses() throws Exception {
        // Initialize map
        HashMap<CoverageDefinition, HashMap<Satellite, HashMap<Instrument,
                HashMap<TopocentricFrame, TimeIntervalArray>>>>  access = new HashMap<>();

        for(CoverageDefinition covDef : covDefs){
            access.put(covDef, new HashMap<>());
            for(Satellite sat : this.getUniqueSats()){
                access.get(covDef).put(sat, new HashMap<>());
                for(Instrument inst : sat.getPayload()) {
                    access.get(covDef).get(sat).put(inst, new HashMap<>());
                    for (CoveragePoint pt : covDef.getPoints()) {
                        access.get(covDef).get(sat).get(inst).put(pt, new TimeIntervalArray(startDate, endDate));
                    }
                }
            }
        }

        //parsing a CSV file into Scanner class constructor
        String outAddress = directoryAddress + "/" + "GPInstrumentCoverage.csv";
        Scanner sc = new Scanner( new File(outAddress) );
        sc.useDelimiter("\n");   //sets the delimiter pattern
        while (sc.hasNext())  //returns a boolean value
        {
            String line = sc.next();
            String[] fields = line.split(",");

            String covDefStr = fields[0];
            String satStr = fields[1];
            String targetStr = fields[2];
            String instrumentStr = fields[3];
            double t_0 = Double.parseDouble( fields[4] );
            double t_f = Double.parseDouble( fields[5] );

            CoverageDefinition covDef = getCovDef(covDefStr);
            Satellite sat = getSat(satStr);
            CoveragePoint target = getPoint(covDefStr, targetStr);
            Instrument inst = getInstrument(instrumentStr);

            access.get(covDef).get(sat).get(inst).get(target).addRiseTime(t_0);
            access.get(covDef).get(sat).get(inst).get(target).addSetTime(t_f);
        }
        sc.close();  //closes the scanner

        return access;
    }

    private HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>>  loadGroundStationAccesses() throws Exception {
        // Initialize map
        HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>>  access = new HashMap<>();

        for(Satellite sat : this.getUniqueSats()){
            access.put(sat, new HashMap<>());
            for(GndStation gnd : this.getUniqueGndStations()) {
                access.get(sat).put(gnd, new TimeIntervalArray(startDate, endDate));
            }
        }

        //parsing a CSV file into Scanner class constructor
        String outAddress = directoryAddress + "/" + "GSCoverage.csv";
        Scanner sc = new Scanner( new File(outAddress) );
        sc.useDelimiter("\n");   //sets the delimiter pattern
        while (sc.hasNext())  //returns a boolean value
        {
            String line = sc.next();
            String[] fields = line.split(",");

            String satStr = fields[0];
            String targetStr = fields[1];
            double t_0 = Double.parseDouble( fields[2] );
            double t_f = Double.parseDouble( fields[3] );

            Satellite sat = getSat(satStr);
            GndStation gnd = getGndStation(targetStr);

            access.get(sat).get(gnd).addRiseTime(t_0);
            access.get(sat).get(gnd).addSetTime(t_f);
        }
        sc.close();  //closes the scanner

        return access;
    }

    private GndStation getGndStation(String name) throws Exception {
        for(GndStation gnd : this.getUniqueGndStations()){
            if(gnd.getBaseFrame().getName().equals(name)) return gnd;
        }

        throw new Exception("Ground Station Network" + name + " in pre-calculated coverage does not match chosen network");
    }

    private Instrument getInstrument(String name){
//        if(name.contains("_FOR")) {
//            name = name.substring(0, name.length()-4);
//            return instrumentList.get(name).get(false);
//        }
//        else return instrumentList.get(name).get(true);

        return instrumentList.get(name);
    }

    private CoveragePoint getPoint(String covDefName, String pointName) throws Exception {
        CoverageDefinition covDef = getCovDef(covDefName);
        for(CoveragePoint pt : covDef.getPoints()){
            if(pt.getName().equals(pointName)) return pt;
        }

        throw new Exception("Coverage Point " + pointName + " in " + covDefName + " coverage definition in pre-calculated coverage does not match chosen definition");
    }

    private CoverageDefinition getCovDef(String name) throws Exception {
        for(CoverageDefinition covDef : covDefs){
            if(covDef.getName().equals(name)) return covDef;
        }

        throw new Exception("Coverage Definition " + name + " in pre-calculated coverage does not match chosen definition");
    }

    private Satellite getSat(String name) throws Exception {
        for(Constellation cons : this.constellations){
            for(Satellite sat : cons.getSatellites()){
                if(sat.getName().equals(name)) return sat;
            }
        }

        throw new Exception("Satellite " + name + " in pre-calculated coverage does not match chosen constellation");
    }

    private Constellation getCons(Satellite sat) throws Exception {
        for(Constellation cons : constellations){
            for(Satellite satCons : cons.getSatellites()){
                if(satCons.equals(sat)) return cons;
            }
        }

        throw new Exception("Satellite " + sat.getName() + " not found in constellation");
    }

//    private GndStation getGndStation(String name) throws Exception{
//        for(Satellite sat : this.stationAssignment.keySet()){
//            for(GndStation gndStation : this.stationAssignment.get(sat)){
//                if(gndStation.getName().equals(name)) return gndStation;
//            }
//        }
//
//        throw new Exception("Ground Station " + name + " in pre-calculated coverage does not match chosen network");
//    }

    /**
     * Saves ground point and ground station access information for future simulations of the same constellation
     */
    private void printAccesses(){
        printCrossLinks();
        printGPAccesses();
        printGPInstAccesses();
        printGSAccesses();
    }

    /**
     * Prints out cross links to coverage data directory
     */
    private void printCrossLinks(){
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = directoryAddress + "/" + "CrossLinks.csv";

        try{
            fileWriter = new FileWriter(outAddress, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        for(Constellation cons : accessesCL.keySet()){
            for(Satellite sat : accessesCL.get(cons).keySet()){
                for(Satellite target : accessesCL.get(cons).get(sat).keySet()){
                    TimeIntervalArray access = accessesCL.get(cons).get(sat).get(target);
                    double t_0 = 0.0;
                    double t_f = 0.0;
                    for(RiseSetTime time : access.getRiseSetTimes()){
                        if(time.isRise()){
                            t_0 = time.getTime();
                        }
                        else{
                            t_f = time.getTime();

                            String accessStr = genCLAccessStr(sat, target, t_0, t_f);
                            printWriter.print(accessStr);
                        }
                    }
                }
            }
        }

        printWriter.close();
    }

    /**
     * Generates a string containing information about an access between two sats
     * @param sat Observing satellite
     * @param target Target Satellite
     * @param t_0 Start epoch of access
     * @param t_f End epoch of access
     * @return string of the format "<sat name>, <target name>, <start epoch>, <end epoch>"
     */
    private String genCLAccessStr(Satellite sat, Satellite target, double t_0, double t_f){
        StringBuilder out = new StringBuilder();
        out.append(sat.getName() + "," + target.getName() + "," + t_0 + "," + t_f + "\n");
        return out.toString();
    }

    /**
     * Prints out cross links to coverage data directory
     */
    private void printGPAccesses(){
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = directoryAddress + "/" + "GPCoverage.csv";

        try{
            fileWriter = new FileWriter(outAddress, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        for(CoverageDefinition covDef : accessesGP.keySet()){
            for(Satellite sat : accessesGP.get(covDef).keySet()){
                for(TopocentricFrame target : accessesGP.get(covDef).get(sat).keySet()){
                    TimeIntervalArray access = accessesGP.get(covDef).get(sat).get(target);
                    double t_0 = 0.0;
                    double t_f = 0.0;

                    for(RiseSetTime time : access.getRiseSetTimes()){
                        if(time.isRise()){
                            t_0 = time.getTime();
                        }
                        else{
                            t_f = time.getTime();

                            String accessStr = genGPAccessStr(covDef, sat, target, t_0, t_f);
                            printWriter.print(accessStr);
                        }
                    }
                }
            }
        }

        printWriter.close();
    }

    /**
     * Generates a string containing information about an access between two sats
     * @param covDef Coverage definition
     * @param sat Observing satellite
     * @param target Target ground point
     * @param t_0 Start epoch of access
     * @param t_f End epoch of access
     * @return string of the format "<covdef name>, <sat name>, <target name>, <start epoch>, <end epoch>"
     */
    private String genGPAccessStr(CoverageDefinition covDef, Satellite sat, TopocentricFrame target, double t_0, double t_f){
        StringBuilder out = new StringBuilder();
        out.append(covDef.getName() + "," + sat.getName() + "," + target.getName() + "," + t_0 + "," + t_f + "\n");
        return out.toString();
    }

    /**
     * Prints out cross links to coverage data directory
     */
    private void printGPInstAccesses(){
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = directoryAddress + "/" + "GPInstrumentCoverage.csv";

        try{
            fileWriter = new FileWriter(outAddress, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        for(CoverageDefinition covDef : accessesGPInst.keySet()){
            for(Satellite sat : accessesGPInst.get(covDef).keySet()){
                for(Instrument inst : accessesGPInst.get(covDef).get(sat).keySet()) {
                    for (TopocentricFrame target : accessesGPInst.get(covDef).get(sat).get(inst).keySet()) {
                        TimeIntervalArray access = accessesGPInst.get(covDef).get(sat).get(inst).get(target);
                        double t_0 = 0.0;
                        double t_f = 0.0;

                        for (RiseSetTime time : access.getRiseSetTimes()) {
                            if (time.isRise()) {
                                t_0 = time.getTime();
                            } else {
                                t_f = time.getTime();

                                String accessStr = genGPInstAccessStr(covDef, sat, target, inst, t_0, t_f);
                                printWriter.print(accessStr);
                            }
                        }
                    }
                }
            }
        }

        printWriter.close();
    }

    /**
     * Generates a string containing information about an access between two sats
     * @param covDef Coverage definition
     * @param sat Observing satellite
     * @param target Target ground point
     * @param inst Intrument used to make said measurement
     * @param t_0 Start epoch of access
     * @param t_f End epoch of access
     * @return string of the format "<covdef name>, <sat name>, <target name>, <instrument name>, <start epoch>, <end epoch>"
     */
    private String genGPInstAccessStr(CoverageDefinition covDef, Satellite sat, TopocentricFrame target, Instrument inst, double t_0, double t_f){
        StringBuilder out = new StringBuilder();
        out.append(covDef.getName() + "," + sat.getName() + "," + target.getName()
                + "," + inst.getName() + "," + t_0 + "," + t_f + "\n");
        return out.toString();
    }

    /**
     * Prints out cross links to coverage data directory
     */
    private void printGSAccesses(){
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = directoryAddress + "/" + "GSCoverage.csv";

        try{
            fileWriter = new FileWriter(outAddress, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter = new PrintWriter(fileWriter);

        for(Satellite sat : accessesGS.keySet()){
            for (GndStation target : accessesGS.get(sat).keySet()) {
                TimeIntervalArray access = accessesGS.get(sat).get(target);
                double t_0 = 0.0;
                double t_f = 0.0;

                for (RiseSetTime time : access.getRiseSetTimes()) {
                    if (time.isRise()) {
                        t_0 = time.getTime();
                    } else {
                        t_f = time.getTime();

                        String accessStr = genGSAccessStr(sat, target, t_0, t_f);
                        printWriter.print(accessStr);
                    }
                }
            }
        }

        printWriter.close();
    }

    /**
     * Generates a string containing information about an access between two sats
     * @param sat Observing satellite
     * @param target Target ground station
     * @param t_0 Start epoch of access
     * @param t_f End epoch of access
     * @return string of the format "<covdef name>, <sat name>, <target name>, <instrument name>, <start epoch>, <end epoch>"
     */
    private String genGSAccessStr(Satellite sat, GndStation target, double t_0, double t_f){
        StringBuilder out = new StringBuilder();
        out.append(sat.getName() + "," + target.getBaseFrame().getName() + "," + t_0 + "," + t_f + "\n");
        return out.toString();
    }

    /**
     * Calculates accesses and cross-links
     * @param constellations List of constellations being simulated
     * @param covDefs List of coverage definitions to be evaluated
     * @param stationAssignments Assigment of Ground Stations to Satellites
     * @param startDate Simulation start date
     * @param endDate Simulation end date
     * @return
     */
    private Scenario coverageScenario(ArrayList<Constellation> constellations, HashSet<CoverageDefinition> covDefs,
                                      HashMap<Satellite, Set<GndStation>> stationAssignments, AbsoluteDate startDate,
                                      AbsoluteDate endDate){
        // Initiate Orekit threads
        OrekitConfig.init(4);

        // Setup logger
        setLogger();

        // Set event analysis properties
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");
        propertiesEventAnalysis.setProperty("fov.saveAccess", "true");


        //set the analyses
        ArrayList<EventAnalysis> eventAnalyses = new ArrayList<>();
        ArrayList<Analysis<?>> analyses = new ArrayList<>();

        CrossLinkEventAnalysis crossLinkEvents = new CrossLinkEventAnalysis(startDate,endDate,inertialFrame,
                constellations,pfJ2,true,false);

        FieldOfViewAndGndStationEventAnalysis fovEvents = new FieldOfViewAndGndStationEventAnalysis(startDate, endDate,
                inertialFrame, covDefs, stationAssignment,pfJ2, true, false, true);

        eventAnalyses.add(crossLinkEvents);
        eventAnalyses.add(fovEvents);

        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventAnalyses).analysis(analyses).
                covDefs(covDefs).name("Coverage_GndStations_and_Crosslinks").properties(propertiesEventAnalysis).
                propagatorFactory(pfKep).build();
        try {
            long start1 = System.nanoTime();
            scen.call();
            long end1 = System.nanoTime();
            Logger.getGlobal().finest(String.format("Took %.4f sec", (end1 - start1) / Math.pow(10, 9)));

        } catch (Exception ex) {
            Logger.getLogger(Dmas.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("scenario failed to complete.");
        }

        return scen;
    }

    /**
     * sets logger level according to input file
     */
    private void setLogger(){
        String lvl = ((JSONObject) input.get(SETTINGS)).get(LEVEL).toString();
        Level level;
        switch(lvl){
            case "OFF":
                level = Level.OFF;
                break;
            case "SEVERE":
                level = Level.SEVERE;
                break;
            case "WARNING":
                level = Level.WARNING;
                break;
            case "INFO":
                level = Level.INFO;
                break;
            case "CONFIG":
                level = Level.CONFIG;
                break;
            case "FINE":
                level = Level.FINE;
                break;
            case "FINER":
                level = Level.FINER;
                break;
            case "FINEST":
                level = Level.FINEST;
                break;
            case "ALL":
                level = Level.ALL;
                break;
            default:
                throw new InputMismatchException("Input file format error. " + lvl
                        + " not currently supported for field " + LEVEL);
        }

        Logger.getGlobal().setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        Logger.getGlobal().addHandler(handler);
    }

    /**
     * Sets propagator properties to increase accuracy of propagator
     * TODO: make these properties satellite dependent
     * @return propertiesPropagator
     */
    private Properties setPropagatorProperties(){
        Properties propertiesPropagator = new Properties();
        propertiesPropagator.setProperty("orekit.propagator.mass", "6");
        propertiesPropagator.setProperty("orekit.propagator.atmdrag", "true");
        propertiesPropagator.setProperty("orekit.propagator.dragarea", "0.075");
        propertiesPropagator.setProperty("orekit.propagator.dragcoeff", "2.2");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.sun", "true");
        propertiesPropagator.setProperty("orekit.propagator.thirdbody.moon", "true");
        propertiesPropagator.setProperty("orekit.propagator.solarpressure", "true");
        propertiesPropagator.setProperty("orekit.propagator.solararea", "0.058");
        return propertiesPropagator;
    }

    /**
     * Loads data and constants from orekit's database to the virtual machine
     * @throws OrekitException
     */
    private void loadOrekitData() throws OrekitException {
        //if running on a non-US machine, need the line below
        Locale.setDefault(new Locale("en", "US"));

        File orekitData = new File(orekitDataDir);
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.addProvider(new DirectoryCrawler(orekitData));

        StringBuffer pathBuffer = new StringBuffer();

        final File currrentDir = new File(this.orekitDataDir);
        if (currrentDir.exists() && (currrentDir.isDirectory() || currrentDir.getName().endsWith(".zip"))) {
            pathBuffer.append(currrentDir.getAbsolutePath());
            pathBuffer.append(File.separator);
            pathBuffer.append("resources");
        }
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, pathBuffer.toString());
    }

    /**
     * Converts string into an absolute date
     * @param startDate string of format YYYY-MM-DDThh:mm:ssZ
     * @return AbsoluteDate using input string
     * @throws Exception thrown if date string format is not supported
     */
    private AbsoluteDate stringToDate(String startDate) throws Exception {
        if(startDate.length() != 20){
            throw new Exception("Date format not supported");
        }

        int YYYY = Integer.parseInt(String.valueOf(startDate.charAt(0))
                + startDate.charAt(1)
                + startDate.charAt(2)
                + startDate.charAt(3));
        int MM = Integer.parseInt(String.valueOf(startDate.charAt(5))
                + startDate.charAt(6));
        int DD = Integer.parseInt(String.valueOf(startDate.charAt(8))
                + startDate.charAt(9));

        int hh = Integer.parseInt(String.valueOf(startDate.charAt(11))
                + startDate.charAt(12));
        int mm = Integer.parseInt(String.valueOf(startDate.charAt(14))
                + startDate.charAt(15));
        int ss = Integer.parseInt(String.valueOf(startDate.charAt(17))
                + startDate.charAt(18));

        return new AbsoluteDate(YYYY, MM, DD, hh, mm, ss, utc);
    }

    /**
     * Reads excel database to obtain lists of sensing and communication satellites
     * @param consStr name of chosen constellation
     * @param startDate start date of simulation
     * @param endDate end date of simulation
     * @return constellations list of two constellations: one for sensing sats and another for communications sats
     * @throws OrekitException
     */
    private ArrayList<Constellation> loadConstellation(String consStr, AbsoluteDate startDate, AbsoluteDate endDate) throws OrekitException {
        ArrayList<Constellation> constellations = new ArrayList<>();

        try {
            Workbook constelWorkbook = Workbook.getWorkbook(new File(constellationsDir + consStr + ".xls"));

            // Load Sensing Satellites
            Sheet sensingSats = constelWorkbook.getSheet("Remote Sensing");
            HashMap<String, Integer> rowIndexes = readIndexes(sensingSats.getRow(0));

            // - Nominal operations
            ArrayList<Satellite> satsSense = new ArrayList<>();
            for(int i = 1; i < sensingSats.getRows(); i++){
                // - Nominal operations
                Satellite sat = importSensingSat(sensingSats.getRow(i), rowIndexes, startDate);
                satsSense.add(sat);
            }
            Constellation constSensing = new Constellation (consStr+"_sensing",satsSense);
            constellations.add(constSensing);

            // Load Communications Satellites
            ArrayList<Satellite> satsComms = new ArrayList<>();
            Sheet commsSats = constelWorkbook.getSheet("Communications");
            rowIndexes = readIndexes(commsSats.getRow(0));
            for(int i = 1; i < commsSats.getRows(); i++){
                Satellite sat = importCommsSat(commsSats.getRow(i), rowIndexes, startDate);
                satsComms.add(sat);
            }

            Constellation constComms = new Constellation (consStr+"_comms",satsComms);
            constellations.add(constComms);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return constellations;
    }

    /**
     * Reads excel sheet of desired constellation and returns list of sensing satellites
     * @param row excel sheet row with information regarding the satellite's position and payload
     * @param columnIndexes hashmap that returns the row index of a particular parameter
     * @param startDate start dat of simulation
     * @return
     */
    private Satellite importSensingSat(Cell[] row, HashMap<String, Integer> columnIndexes, AbsoluteDate startDate) throws Exception {

        String id = row[columnIndexes.get("ID")].getContents();
        String name = row[columnIndexes.get("Name")].getContents();
        double alt = Double.parseDouble( row[columnIndexes.get("Altitude [km]")].getContents() ) * 1e3;
        double ecc = Double.parseDouble( row[columnIndexes.get("Eccentricity [-]")].getContents() );
        double inc = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Inclination [deg]")].getContents() ) );
        double raan = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("RAAN [deg]")].getContents() ) );
        double anom = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Anomaly [deg]")].getContents() ) );
        double maxRoll = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Max Roll Angle [deg]")].getContents() ) );
        double maxRollAcc = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Max Roll Acc [deg/s2]")].getContents() ) );

        ArrayList<Instrument> payload = new ArrayList<>();
        String payloadStr = row[columnIndexes.get("Payload")].getContents();

        String[] contents = payloadStr.split(", ");
        boolean nominal = false;
        for(int i = 0; i < contents.length; i++){
            for(int j = 0; j < 2; j++){
                if(j > 0) nominal = true;

                // Import Instrument database
                Instrument ins = loadInstrument(contents[i], maxRoll, maxRollAcc, nominal);

                if(ins == null) throw new InputMismatchException("Constellation input error. " + contents[i] + " not found in instrument database.");
                payload.add(ins);
            }
        }

        Orbit orb = new KeplerianOrbit(alt + Constants.WGS84_EARTH_EQUATORIAL_RADIUS, ecc, inc, 0.0, raan, anom, PositionAngle.MEAN, inertialFrame, startDate, mu);
        HashSet<CommunicationBand> satBands = new HashSet<>(); satBands.add(CommunicationBand.UHF);
        Satellite sat = new Satellite(name, orb, null, payload,
                new ReceiverAntenna(6., satBands), new TransmitterAntenna(6., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

        return sat;
    }

    /**
     * Reads excel sheet of desired constellation and returns list of communications satellites
     * @param row excel sheet row with information regarding the satellite's position and payload
     * @param columnIndexes hashmap that returns the row index of a particular parameter
     * @param startDate start date of simulation
     * @return
     */
    private Satellite importCommsSat(Cell[] row, HashMap<String, Integer> columnIndexes, AbsoluteDate startDate){

        String id = row[columnIndexes.get("ID")].getContents();
        String name = row[columnIndexes.get("Name")].getContents();
        double alt = Double.parseDouble( row[columnIndexes.get("Altitude [km]")].getContents() ) * 1e3;
        double ecc = Double.parseDouble( row[columnIndexes.get("Eccentricity [-]")].getContents() );
        double inc = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Inclination [deg]")].getContents() ) );
        double raan = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("RAAN [deg]")].getContents() ) );
        double anom = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Anomaly [deg]")].getContents() ) );
        double fov = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("FOV [deg]")].getContents() ) );
        ArrayList<Instrument> payload = new ArrayList<>();

        NadirSimpleConicalFOV fieldOfRegard = new NadirSimpleConicalFOV(FastMath.toRadians(fov/2), earthShape);
        Instrument ins = new Instrument(name+"_comms", fieldOfRegard, Propagator.DEFAULT_MASS, 1.0);
        payload.add(ins);

        if(!instrumentList.containsKey(ins.getName())){
            instrumentList.put(ins.getName(), ins);
        }

        Orbit orb = new KeplerianOrbit(alt, ecc, inc, 0.0, raan, anom, PositionAngle.MEAN, inertialFrame, startDate, mu);
        HashSet<CommunicationBand> satBands = new HashSet<>(); satBands.add(CommunicationBand.UHF);

        Satellite sat = new Satellite(name, orb, null, payload,
                new ReceiverAntenna(10., satBands), new TransmitterAntenna(10., satBands), Propagator.DEFAULT_MASS, Propagator.DEFAULT_MASS);

        return sat;
    }

    /**
     * Reads excel sheet of desired scenario and returns list of coverage definitions defined in the scenario
     * @param scenarioStr name of scenario
     * @param constellations constellations used in the simulation
     * @return List of coverage definitions from scenario
     * @throws IOException
     * @throws BiffException
     */
    private HashSet<CoverageDefinition> loadCoverageDefinitions(String scenarioStr, ArrayList<Constellation> constellations) throws IOException, BiffException {
        HashSet<CoverageDefinition> covDefs = new HashSet<>();

        Workbook scenarioWorkbook = Workbook.getWorkbook(new File( scenarioDir + scenarioStr + ".xls"));
        Sheet regionsSheet = scenarioWorkbook.getSheet("Regions");
        HashMap<String, Integer> rowIndexes = readIndexes(regionsSheet.getRow(0));
        for(int i = 1; i < regionsSheet.getRows(); i++){
            Cell[] row = regionsSheet.getRow(i);
            CoverageDefinition covDef = importCoverageDefinition(row, rowIndexes);

            covDef.assignConstellation(constellations);
            covDefs.add(covDef);
        }

        return covDefs;
    }

    /**
     * Reads excel sheet to return a singular coverage definition
     * @param column excel sheet row with information regarding the satellite's position and payload
     * @param rowIndexes hashmap that returns the row index of a particular parameter
     * @return
     */
    private CoverageDefinition importCoverageDefinition(Cell[] column, HashMap<String, Integer> rowIndexes){
        String id = column[rowIndexes.get("ID")].getContents();
        String name = column[rowIndexes.get("Name")].getContents();
        String latsStr = column[rowIndexes.get("RegionLat [deg]")].getContents();
        String lonsStr = column[rowIndexes.get("RegionLon [deg]")].getContents();

        String[] latBounds = latsStr.substring(1,latsStr.length()-1).split(",");
        String[] longBounds = lonsStr.substring(1,lonsStr.length()-1).split(",");

        double minLatitude = Math.min( Double.parseDouble(latBounds[0]), Double.parseDouble(latBounds[1]) );
        double maxLatitude = Math.max( Double.parseDouble(latBounds[0]), Double.parseDouble(latBounds[1]) );
        double minLongitude = Math.min( Double.parseDouble(longBounds[0]), Double.parseDouble(longBounds[1]) );
        double maxLongitude = Math.max( Double.parseDouble(longBounds[0]), Double.parseDouble(longBounds[1]) );

        double granularity = Double.parseDouble( column[rowIndexes.get("Granularity [deg]")].getContents() );

        return new CoverageDefinition(name, granularity, minLatitude, maxLatitude, minLongitude, maxLongitude, earthShape, CoverageDefinition.GridStyle.EQUAL_AREA);
    }

    /**
     * Reads excel sheet database of desired ground station network and returns a satellite to ground station assignment
     * @param gsNetworkStr name of desired ground station network
     * @param constellations list of constellations available in the simulation
     * @return hashmap of ground station assignments
     * @throws IOException
     * @throws BiffException
     * @throws OrekitException
     */
    private HashMap<Satellite, Set<GndStation>> loadGroundStations(String gsNetworkStr,
                                                                   ArrayList<Constellation> constellations)
            throws IOException, BiffException, OrekitException {
        HashMap<Satellite, Set<GndStation>> assignment = new HashMap<>();
        Set<GndStation> groundStations = new HashSet<>();

        Workbook groundStationsWorkbook = Workbook.getWorkbook(new File(databaseDir + "GroundStationDatabase.xls"));
        Sheet groundStationSheet = groundStationsWorkbook.getSheet(gsNetworkStr);
        if(groundStations == null) throw new InputMismatchException("Input Error. Ground Station Network '" +  gsNetworkStr +"' not found in database.");

        HashMap<String, Integer> rowIndexes = readIndexes(groundStationSheet.getRow(0));
        for(int i = 1; i < groundStationSheet.getRows(); i++){
            Cell[] row = groundStationSheet.getRow(i);
            GndStation gndStation = importGroundStation(row, rowIndexes);
            groundStations.add(gndStation);
        }

        for(Constellation cons : constellations){
            for (Satellite sat : cons.getSatellites()) {
                assignment.put(sat, groundStations);
            }
        }

        return assignment;
    }

    /**
     * Reads excel sheet to return a singular ground station
     * @param row excel sheet row with information regarding the satellite's position and payload
     * @param columnIndexes hashmap that returns the row index of a particular parameter
     * @return new ground station using information from excel database
     * @throws OrekitException
     */
    private GndStation importGroundStation(Cell[] row, HashMap<String, Integer> columnIndexes) throws OrekitException {
        String id = row[columnIndexes.get("ID")].getContents();
        String name = row[columnIndexes.get("Name")].getContents();
        double lat = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Lat [deg]")].getContents() ) );
        double lon = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Lon [deg]")].getContents() ) );
        double gainR = Double.parseDouble( row[columnIndexes.get("Gain R [dB]")].getContents() );
        double gainT = Double.parseDouble( row[columnIndexes.get("Gain T [dB]")].getContents() );
        double minElev = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Min Elevation [deg]")].getContents() ) );

        TopocentricFrame topo = new TopocentricFrame(earthShape, new GeodeticPoint( lat, lon, 0.0), id);
        HashSet<CommunicationBand> bands = new HashSet<>(); bands.add(CommunicationBand.UHF);
        return new GndStation(topo, new ReceiverAntenna(gainR, bands), new TransmitterAntenna(gainT, bands), minElev);
    }

    private Instrument loadInstrument(String insName, double maxRollAngle,
                                      double maxRollAcc, boolean nominal) throws Exception{
        try {
            if(nominal){
                if(instrumentList.containsKey(insName)) {
                    return instrumentList.get(insName);
                }
            }
            else{
                String forName = insName + "_" + Math.toDegrees(maxRollAngle) + "_FOR";
                if(instrumentList.containsKey(forName)) {
                    return instrumentList.get(forName);
                }
            }

            Workbook instrumentsWorkbook = Workbook.getWorkbook( new File(databaseDir + "InstrumentDatabase.xls"));
            Sheet sensingSats = instrumentsWorkbook.getSheet("Instruments");

            HashMap<String, Integer> columnIndexes = readIndexes(sensingSats.getRow(0));

            for(int i = 1; i < sensingSats.getRows(); i++){
                Cell[] row = sensingSats.getRow(i);
                if(!row[columnIndexes.get("Name")].getContents().equals( insName )) continue;

                Instrument ins = readInstrument(row, columnIndexes, instrumentsWorkbook, maxRollAngle, maxRollAcc, nominal);

                if(!instrumentList.containsKey(ins.getName())) instrumentList.put(ins.getName(), ins);

                return ins;
            }
        } catch (IOException | BiffException | OrekitException e) {
            e.printStackTrace();
        }
        return null;
    }

//    /**
//     * Reads database and imports instruments to be used for coverage calculations
//     * @param
//     * @return
//     */
//    private HashMap<String, HashMap<Boolean, Instrument>> loadInstruments(){
//        HashMap<String, HashMap<Boolean, Instrument>> instrumentList = new HashMap<>();
//
//        try {
//            Workbook instrumentsWorkbook = Workbook.getWorkbook( new File(databaseDir + "InstrumentDatabase.xls"));
//            Sheet sensingSats = instrumentsWorkbook.getSheet("Instruments");
//
//            HashMap<String, Integer> columnIndexes = readIndexes(sensingSats.getRow(0));
//
//            for(int i = 1; i < sensingSats.getRows(); i++){
//                HashMap<Boolean, Instrument> insSet = new HashMap<>();
//
//                Cell[] row = sensingSats.getRow(i);
//                Instrument insNom = readInstrument(row, columnIndexes, instrumentsWorkbook, true);
//                insSet.put(true,insNom);
//
//                Instrument insFOR = readInstrument(row, columnIndexes, instrumentsWorkbook, false);
//                insSet.put(false,insFOR);
//
//                instrumentList.put(insNom.getName(), insSet);
//            }
//
//        } catch (IOException | BiffException | OrekitException e) {
//            e.printStackTrace();
//        }
//
//        return instrumentList;
//    }

    /**
     * Reads row in instrument database and returns a new instrument object depending on its type
     * @param row excel row containing information of said instrument
     * @param columnIndexes hashmap that returns the row index of a particular parameter
     * @param instrumentsWorkbook excel workbook containing all instruments
     * @return Instrument object
     * @throws OrekitException
     */
    private Instrument readInstrument(Cell[] row, HashMap<String, Integer> columnIndexes, Workbook instrumentsWorkbook,
                                      double maxRollAngle, double maxRollAcc, boolean nominal) throws Exception {
        Instrument ins;

        String id = row[columnIndexes.get("ID")].getContents();
        String name = row[columnIndexes.get("Name")].getContents();
        String type = row[columnIndexes.get("Type")].getContents();
        double mass = Double.parseDouble( row[columnIndexes.get("Mass")].getContents() );
        double xDim = Double.parseDouble( row[columnIndexes.get("Dim-x")].getContents() );
        double yDim = Double.parseDouble( row[columnIndexes.get("Dim-y")].getContents() );
        double zDim = Double.parseDouble( row[columnIndexes.get("Dim-z")].getContents() );

        switch(type){
            case "SAR" :
                ins = loadSAR(name, mass, instrumentsWorkbook, maxRollAngle, maxRollAcc, nominal);
                break;
            default:
                throw new InputMismatchException("Instrument Database input error on instrument " + name + ". " + type + " type sensor not yet supported");
        }
        return ins;
    }

    /**
     * Creates a hashmap that stores the indexes of the different parameters for databases
     * @param row first row or column of data that contains field names
     * @return hashmap that, given a parameter name, returns the column or row index of said parameter
     */
    private HashMap<String, Integer> readIndexes(Cell[] row){
        HashMap<String,Integer> indexes = new HashMap<>();
        for(int i = 0; i < row.length; i++){
            indexes.put(row[i].getContents(), i);
        }
        return indexes;
    }

    private Instrument loadSAR(String name, double mass, Workbook instrumentsWorkbook,
                               double maxRollAngle, double maxRollAcc, boolean nominal) throws Exception {

        Sheet instrumentSheet = instrumentsWorkbook.getSheet(name);
        Cell[] parameters = instrumentSheet.getColumn(0);
        Cell[] values = instrumentSheet.getColumn(1);
        HashMap<String, Integer> parameterIndexes = readIndexes(parameters);

        double freq = Double.parseDouble( values[parameterIndexes.get("Frequency")].getContents() );
        double peakPower = Double.parseDouble( values[parameterIndexes.get("MaxPower")].getContents() );
        double dc = Double.parseDouble( values[parameterIndexes.get("DutyCycle")].getContents() );
        double pw = Double.parseDouble( values[parameterIndexes.get("PulseWidth")].getContents() );
        double prf = Double.parseDouble( values[parameterIndexes.get("PRF")].getContents() );
        double bw = Double.parseDouble( values[parameterIndexes.get("Bandwidth")].getContents() );
        double nLooks = Double.parseDouble( values[parameterIndexes.get("nLooks")].getContents() );
        String nominalOps = values[parameterIndexes.get("NominalOps")].getContents();
        double rb = Double.parseDouble( values[parameterIndexes.get("Datarate")].getContents() );
        String scan = values[parameterIndexes.get("Scanning")].getContents();
        double scanningAngle = Double.parseDouble( values[parameterIndexes.get("ScanningAngle")].getContents() );
        double fov_at = Double.parseDouble( values[parameterIndexes.get("FOV-AT")].getContents() );
        double fov_ct = Double.parseDouble( values[parameterIndexes.get("FOV-CT")].getContents() );
        double lookAngle = Double.parseDouble( values[parameterIndexes.get("LookAngle")].getContents() );
        String nominalType = values[parameterIndexes.get("Nominal Measurement Type")].getContents();

        // check if nominal measurement type is supported
        boolean found = false;
        for(int i = 0; i < MeasurementTypes.ALL.length; i++){
            if(nominalType.equals(MeasurementTypes.ALL[i])){
                found = true;
                break;
            }
        }
        if(!found) throw new Exception("Instrument Nominal Measurement Type not yet supported");

        AbstractAntenna antenna = loadAntenna(values, parameterIndexes);

        if(!nominal) name += "_" + Math.toDegrees( maxRollAngle ) + "_FOR";
        OffNadirRectangularFOV fieldOfRegard;
        switch(scan){
            case "conical":
                fieldOfRegard = new OffNadirRectangularFOV(0.0,
                        FastMath.toRadians(fov_ct/2.0 + lookAngle) ,
                        FastMath .toRadians(fov_at/2.0), 0.0, earthShape);

                return new SAR(name, nominalType, fieldOfRegard, mass, peakPower * dc, freq,
                        peakPower, dc, pw, prf, bw, nLooks, rb, nominalOps, antenna);

            case "side":
                if(nominal) {
                    fieldOfRegard = new OffNadirRectangularFOV(FastMath.toRadians(lookAngle),
                            FastMath.toRadians((fov_ct + scanningAngle) / 2.0 ),
                            FastMath.toRadians(fov_at / 2.0), 0.0, earthShape);
                }
                else{
                    fieldOfRegard = new OffNadirRectangularFOV(FastMath.toRadians(lookAngle),
                            maxRollAngle + FastMath.toRadians((fov_ct + scanningAngle) / 2.0 ),
                            FastMath.toRadians(fov_at / 2.0), 0.0, earthShape);
                }
                return new SAR(name, nominalType, fieldOfRegard, mass, peakPower * dc, freq, peakPower, dc, pw, prf, bw,
                        nLooks, rb, nominalOps, antenna);

            case "none":
                if(nominal) {
                    fieldOfRegard = new OffNadirRectangularFOV(FastMath.toRadians(lookAngle ),
                            FastMath.toRadians((fov_ct) / 2.0 ),
                            FastMath.toRadians(fov_at / 2.0), 0.0, earthShape);
                }
                else{
                    fieldOfRegard = new OffNadirRectangularFOV(FastMath.toRadians(lookAngle),
                            FastMath.toRadians(maxRollAngle + (fov_ct) / 2.0 ),
                            FastMath.toRadians(fov_at / 2.0), 0.0, earthShape);
                }
                return new SAR(name, nominalType, fieldOfRegard, mass, peakPower * dc, freq, peakPower, dc, pw, prf, bw,
                        nLooks, rb, nominalOps, antenna);

            default:
                throw new InputMismatchException("Instrument Database input error on instrument " + name + ". " + scan + " scanning type not yet supported");
        }
    }

    private AbstractAntenna loadAntenna(Cell[] values, HashMap<String, Integer> parameterIndexes){
        String type = values[parameterIndexes.get("AntennaType")].getContents();
        double freq = Double.parseDouble( values[parameterIndexes.get("Frequency")].getContents() );

        switch (type.toLowerCase()){
            case AbstractAntenna.PARAB:
                double diameter = Double.parseDouble( values[parameterIndexes.get("Diameter")].getContents() );
                return new ParabolicAntenna(diameter,freq);
            default:
                throw new InputMismatchException("Instrument antenna of type " + type + " not yet supported");
        }
    }

    /**
     * Getters for coverage metrics
     */
    public HashMap<Constellation, HashMap<Satellite,
            HashMap<Satellite, TimeIntervalArray>>> getAccessesCL() { return accessesCL; }
    public HashMap<CoverageDefinition, HashMap<Satellite,
            HashMap<TopocentricFrame, TimeIntervalArray>>> getAccessesGP() { return accessesGP; }
    public HashMap<CoverageDefinition, HashMap<Satellite, HashMap<Instrument,
            HashMap<TopocentricFrame, TimeIntervalArray>>>> getAccessesGPIns() { return accessesGPInst; }
    public HashMap<Satellite,
            HashMap<GndStation, TimeIntervalArray>> getAccessesGS() { return accessesGS; }

    /**
     * Getters for simulation constellation, coverage definitions, or ground station assignments
     */
    public ArrayList<Constellation> getConstellations() { return constellations; }
    public HashSet<CoverageDefinition> getCovDefs() { return covDefs; }
    public HashMap<Satellite, Set<GndStation>> getStationAssignment() { return stationAssignment; }


    public Constellation getSensingSats(){
        for(Constellation cons : constellations){
            if(cons.getName().contains("_sensing")) return cons;
        }
        return null;
    }
    public Constellation getCommsSats(){
        for(Constellation cons : constellations){
            if(cons.getName().contains("_comms")) return cons;
        }
        return null;
    }
    public ArrayList<Satellite> getUniqueSats(){
        ArrayList<Satellite> satList = new ArrayList<>();

        for(Constellation cons : constellations){
            for(Satellite sat : cons.getSatellites()){
                satList.add(sat);
            }
        }

        return satList;
    }

    /**
     * Returns list of all unique ground stations in this scenario
     * @return satList : list of gnd stats
     */
    public ArrayList<GndStation> getUniqueGndStations(){
        ArrayList<GndStation> statList = new ArrayList<>();

        for(Satellite sat : stationAssignment.keySet()){
            for(GndStation stat : stationAssignment.get(sat)){
                if(!statList.contains(stat)) statList.add(stat);
            }
        }

        return statList;
    }

    /**
     * Creates a json object with all coverage statistics for this scenario
     * @return out : JSONObject containing min, max, avg, and standard deviation of revisit times
     */
    public JSONObject coverageStats(){
        JSONObject out = new JSONObject();
        JSONObject revTime = new JSONObject();

        CoverageStats stats = new CoverageStats(this);

        revTime.put("max", stats.getMaxRevTime());
        revTime.put("min", stats.getMinRevTime());
        revTime.put("avg", stats.getAvgRevTime());
        revTime.put("std", stats.getStdRevTime());

        out.put("revTime", revTime);
        out.put("covPtg", stats.getCoveragePercentage());

        return out;
    }

    /**
     * Returns list of accesses of all ground points to be used in coverage statistics calculations
     * @return Map of a given ground point to a list of chronologically ordered accesses by all sensing satellites
     */
    public HashMap<TopocentricFrame, ArrayList<GPAccess>> orderGPAccesses(){
        HashMap<TopocentricFrame, ArrayList<GPAccess>> unordered = new HashMap<>();
        HashMap<TopocentricFrame, ArrayList<GPAccess>> ordered = new HashMap<>();

        // find all accesses, might be out of order
        for(CoverageDefinition covDef : accessesGP.keySet()){
            for(Satellite sat : accessesGP.get(covDef).keySet()){
                if(this.isCommsSat(sat)) continue;

                for(TopocentricFrame point : accessesGP.get(covDef).get(sat).keySet()){
                    unordered.put(point, new ArrayList<>(accessesGP.get(covDef).get(sat).get(point).numIntervals()));

                    double t_0 = -1.0;
                    double t_f;

                    for(RiseSetTime setTime : accessesGP.get(covDef).get(sat).get(point)){
                        if(setTime.isRise()) {
                            t_0 = setTime.getTime();
                        }
                        else {
                            t_f = setTime.getTime();

                            AbsoluteDate startDate = this.getStartDate().shiftedBy(t_0);
                            AbsoluteDate endDate = this.getStartDate().shiftedBy(t_f);

                            unordered.get(point).add(new GPAccess(sat, covDef, point, null, startDate, endDate));
                        }
                    }
                }
            }
        }

        // sort accesses chronologically
        for(TopocentricFrame point : unordered.keySet()) {
            ordered.put(point, new ArrayList<>());

            for (GPAccess acc : unordered.get(point)) {
                if (ordered.size() == 0) {
                    ordered.get(point).add(acc);
                    continue;
                }

                boolean skip = false;
                int i = 0;
                for (GPAccess accOrd : ordered.get(point)) {

                    if(acc.getEndDate().compareTo(accOrd.getStartDate()) < 0){
                        break;
                    }
                    else if(acc.getStartDate().compareTo(accOrd.getEndDate()) > 0){
                        i++;
                        continue;
                    }
                    else if(acc.getStartDate().compareTo(accOrd.getStartDate()) < 0){
                        accOrd.setStartDate(acc.getStartDate());

                        if(acc.getEndDate().compareTo(accOrd.getEndDate()) > 0){
                            accOrd.setEndDate(acc.getEndDate());
                        }
                        skip = true;
                        break;
                    }
                    else if(acc.getEndDate().compareTo(accOrd.getEndDate()) < 0){
                        skip = true;
                        break;
                    }
                    else{
                        accOrd.setEndDate(acc.getEndDate());
                        skip = true;
                        break;
                    }
                }

                if(!skip) ordered.get(point).add(i, acc);
            }
        }

        return ordered;
    }

    /**
     * Returns an array of all cross-link accesses between two satellites in chronological order
     * @param sender : sender satellite in access
     * @param target : target satellite in access
     * @return array of chronologically ordered cross-link accesses
     * @throws Exception thrown if constellation for sender sat cannot be determined
     */
    public ArrayList<CLAccess> orderCLAccesses(Satellite sender, Satellite target) throws Exception {
        Constellation cons = findConstellation(sender);
        TimeIntervalArray intervalArray = accessesCL.get(cons).get(sender).get(target);

        ArrayList<CLAccess> unordered = new ArrayList<>();
        double t_0 = -1.0;
        double t_f;
        for(RiseSetTime setTime : intervalArray.getRiseSetTimes()){
            if(setTime.isRise()) {
                t_0 = setTime.getTime();
            }
            else {
                t_f = setTime.getTime();

                AbsoluteDate startDate = this.getStartDate().shiftedBy(t_0);
                AbsoluteDate endDate = this.getStartDate().shiftedBy(t_f);

                unordered.add(new CLAccess(sender, target, startDate, endDate) );
            }
        }

        ArrayList<CLAccess> ordered = new ArrayList<>();

        for (CLAccess acc : unordered) {
            if (ordered.size() == 0) {
                ordered.add(acc);
                continue;
            }

            boolean skip = false;
            int i = 0;
            for (CLAccess accOrd : ordered) {

                if(acc.getEndDate().compareTo(accOrd.getStartDate()) < 0){
                    break;
                }
                else if(acc.getStartDate().compareTo(accOrd.getEndDate()) > 0){
                    i++;
                    continue;
                }
                else if(acc.getStartDate().compareTo(accOrd.getStartDate()) < 0){
                    accOrd.setStartDate(acc.getStartDate());

                    if(acc.getEndDate().compareTo(accOrd.getEndDate()) > 0){
                        accOrd.setEndDate(acc.getEndDate());
                    }
                    skip = true;
                    break;
                }
                else if(acc.getEndDate().compareTo(accOrd.getEndDate()) < 0){
                    skip = true;
                    break;
                }
                else{
                    accOrd.setEndDate(acc.getEndDate());
                    skip = true;
                    break;
                }
            }

            if(!skip) ordered.add(i, acc);
        }

        return ordered;
    }

    /**
     * Returns the constellation that a particular satellite belongs to
     * @param sat : desired satellite to be checked
     * @return constellation of sat
     * @throws Exception thrown if no constellation in the simulation contains the satellite
     */
    private Constellation findConstellation(Satellite sat) throws Exception {
        for(Constellation constellation : constellations){
            boolean found = true;
            if(!constellation.getSatellites().contains(sat)){
                found = false;
                continue;
            }

            if(found) return constellation;
        }

        throw new Exception("Constellation not found for satellite " + sat.toString());
    }

    /**
     * Checks if a satellite belongs to the communications constellation
     * @param sat : desired sat to be checked
     * @return true if belongs to comms constelltion
     */
    public boolean isCommsSat(Satellite sat){
        for(Constellation cons : constellations){
            if(cons.getName().contains("_comms")){
                if(cons.getSatellites().contains(sat)) return true;
            }
        }

        return false;
    }

    public Vector3D propagateSatPos(Satellite sat, AbsoluteDate date){
        Propagator prop = null;
        if(Math.abs( sat.getOrbit().getI() ) <= 0.1){
            // if orbit is equatorial, use Keplerian propagator
            prop = pfKep.createPropagator(sat.getOrbit(), sat.getGrossMass());
        }
        else{
            // else use J2 propagator
            prop = pfJ2.createPropagator(sat.getOrbit(), sat.getGrossMass());
        }

        ArrayList<PVCoordinates> pvSat = new ArrayList<>();
        ArrayList<GeodeticPoint> gtSat = new ArrayList<>();

        try {
            SpacecraftState stat = prop.propagate(date);
            pvSat.add(stat.getPVCoordinates(earthFrame));

            Vector3D pos = stat.getPVCoordinates(earthFrame).getPosition();
            return pos;
        } catch (OrekitException | NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Vector3D propagateGPPos(TopocentricFrame gp, AbsoluteDate date){
        return gp.getPVCoordinates(date,earthFrame).getPosition();
    }

    /**
     * Other getters
     */
    public String getScenarioDir(){ return scenarioDir; }
    public AbsoluteDate getStartDate(){ return startDate; }
    public AbsoluteDate getEndDate() { return endDate; }
}
