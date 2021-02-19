package modules.simulation;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import modules.instruments.SAR;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import seakers.orekit.analysis.Analysis;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.event.CrossLinkEventAnalysis;
import seakers.orekit.event.EventAnalysis;
import seakers.orekit.event.FieldOfViewAndGndStationEventAnalysis;
import seakers.orekit.event.GroundEventAnalysis;
import seakers.orekit.object.*;
import seakers.orekit.object.communications.ReceiverAntenna;
import seakers.orekit.object.communications.TransmitterAntenna;
import seakers.orekit.object.fieldofview.NadirSimpleConicalFOV;
import seakers.orekit.object.fieldofview.OffNadirRectangularFOV;
import seakers.orekit.propagation.PropagatorFactory;
import seakers.orekit.propagation.PropagatorType;
import seakers.orekit.scenario.Scenario;
import seakers.orekit.scenario.ScenarioIO;
import seakers.orekit.util.OrekitConfig;

import java.io.*;
import java.nio.file.Paths;
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
    private Frame earthFrame;
    private Frame inertialFrame;
    private BodyShape earthShape;
    private TimeScale utc;
    private double mu;

    /**
     * List of instruments in instrument database from which sats can choose to create their payload
     */
    private final HashMap<String, Instrument> instrumentList;

    /**
     * Coverage results from propagation
     * @accessesCL : Cross Link access opportunity
     * @accessesGP : Ground Point access opportunity
     * @accessesGS : Ground Station access opportunity
     */
    private HashMap<Constellation, HashMap<Satellite, HashMap<Satellite, TimeIntervalArray>>> accessesCL;
    private HashMap<CoverageDefinition, HashMap<Satellite,
            HashMap<TopocentricFrame, TimeIntervalArray>>> accessesGP;
    private HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> accessesGS;

    /**
     *  Constellations of sensing and communication satellites chosen for simulation
     */
    ArrayList<Constellation> constellations = null;

    /**
     *  Coverage definitions chosen for simulation
     */
    HashSet<CoverageDefinition> covDefs = null;

    /**
     *  Ground Station to Satellite assignment for chosen ground communications network and constellations
     */
    HashMap<Satellite, Set<GndStation>> stationAssignment = null;

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
                     String coverageDir, String constellationsDir, String scenarioDir){
        this.input = input;
        this.orekitDataDir = orekitDataDir;
        this.databaseDir = databaseDir;
        this.coverageDir = coverageDir;
        this.constellationsDir = constellationsDir;
        this.scenarioDir = scenarioDir;

        // Import Instrument database
        instrumentList = loadInstruments(input);
    }

    /**
     * Loads or calculates coverage calculation for a scenario's coverage definition and ground stations
     * as well as calculating time windows for cross-links between satellites
     * @throws Exception
     */
    public void propagate() throws Exception {
        // load Orekit data
        loadOrekitData();
        earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        inertialFrame = FramesFactory.getEME2000();
        earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, earthFrame);
        utc = TimeScalesFactory.getUTC();
        mu = Constants.WGS84_EARTH_MU;

        // read json file inputs
        String consStr = input.get(CONSTELLATION).toString();
        String gsNetworkStr = input.get(GROUND_STATIONS).toString();
        String scenarioStr = input.get(SCENARIO).toString();
        String startDateStr = input.get(START_DATE).toString();
        String endDateStr = input.get(END_DATE).toString();

        // Create dates from input file
        AbsoluteDate startDate = stringToDate(startDateStr);
        AbsoluteDate endDate = stringToDate(endDateStr);

        // Read scenario information from excel data and generate orbital parameters and coverage definitions
        constellations = loadConstellation(consStr, startDate, endDate);
        covDefs = loadCoverageDefinitions(scenarioStr, constellations);
        stationAssignment = loadGroundStations(gsNetworkStr, constellations);

        // if data has been calculated before, import data
        String dirName = consStr + "_" + gsNetworkStr + "_" + scenarioStr + "_" + startDateStr + "_" + endDateStr;
        String directoryAddress = coverageDir + dirName;

        File f = new File(directoryAddress);
        if(f.exists()){
            // import data

            int x = 1;

            // TODO: Enable creating new directory and saving/accessing coverage data
        }
        else{ // Else, calculate information
            // create directory
//            new File( directoryAddress ).mkdir(); TODO: uncomment once data io is implemented

            // propagate scenario to calculate accesses and cross-links
            Scenario scen = propagateScenario(constellations, covDefs, stationAssignment, startDate, endDate);

            // save data in object variables for sims to use
            ArrayList<EventAnalysis> events = (ArrayList<EventAnalysis>) scen.getEventAnalyses();
            this.accessesCL = ((CrossLinkEventAnalysis) events.get(0)).getAllAccesses();

            this.accessesGP = ((FieldOfViewAndGndStationEventAnalysis) events.get(1)).getAllAccesses();
            this.accessesGS = ((FieldOfViewAndGndStationEventAnalysis) events.get(1)).getAllAccessesGS();
        }
    }

    /**
     * Calculates accesses and cross-links
     * @param constellations List of constellations being simulated
     * @param covDefs List of coverage definitions to be evaluated
     * @param stationAssignment Assigment of Ground Stations to Satellites
     * @param startDate Simulation start date
     * @param endDate Simulation end date
     * @return
     */
    private Scenario propagateScenario(ArrayList<Constellation> constellations, HashSet<CoverageDefinition> covDefs,
                                       HashMap<Satellite, Set<GndStation>> stationAssignment, AbsoluteDate startDate,
                                       AbsoluteDate endDate){
        // Initiate Orekit threads
        OrekitConfig.init(4);

        // Setup logger
        setLogger();

        // Set propagator properties
        Properties propertiesPropagator = setPropagatorProperties();
        PropagatorFactory pf = new PropagatorFactory(PropagatorType.KEPLERIAN,propertiesPropagator);

        // Set event analysis properties
        Properties propertiesEventAnalysis = new Properties();
        propertiesEventAnalysis.setProperty("fov.numThreads", "4");
        propertiesEventAnalysis.setProperty("fov.saveAccess", "true");


        //set the analyses
        ArrayList<EventAnalysis> eventAnalyses = new ArrayList<>();
        ArrayList<Analysis<?>> analyses = new ArrayList<>();

        CrossLinkEventAnalysis crossLinkEvents = new CrossLinkEventAnalysis(startDate,endDate,inertialFrame,
                constellations,pf,true,false);
        FieldOfViewAndGndStationEventAnalysis fovEvents = new FieldOfViewAndGndStationEventAnalysis(startDate, endDate,
                inertialFrame, covDefs, stationAssignment,pf, true, false);

        eventAnalyses.add(crossLinkEvents);
        eventAnalyses.add(fovEvents);

        Scenario scen = new Scenario.Builder(startDate, endDate, utc).
                eventAnalysis(eventAnalyses).analysis(analyses).
                covDefs(covDefs).name("Coverage_GndStations_and_Crosslinks").properties(propertiesEventAnalysis).
                propagatorFactory(pf).build();
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
        String lvl = input.get(LEVEL).toString();
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
            ArrayList<Satellite> satsSense = new ArrayList<>();
            Sheet sensingSats = constelWorkbook.getSheet("Remote Sensing");
            HashMap<String, Integer> rowIndexes = readIndexes(sensingSats.getRow(0));
            for(int i = 1; i < sensingSats.getRows(); i++){
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

        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
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
    private Satellite importSensingSat(Cell[] row, HashMap<String, Integer> columnIndexes, AbsoluteDate startDate){

        String id = row[columnIndexes.get("ID")].getContents();
        String name = row[columnIndexes.get("Name")].getContents();
        double alt = Double.parseDouble( row[columnIndexes.get("Altitude [km]")].getContents() ) * 1e3;
        double ecc = Double.parseDouble( row[columnIndexes.get("Eccentricity [-]")].getContents() );
        double inc = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Inclination [deg]")].getContents() ) );
        double raan = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("RAAN [deg]")].getContents() ) );
        double anom = FastMath.toRadians( Double.parseDouble( row[columnIndexes.get("Anomaly [deg]")].getContents() ) );
        ArrayList<Instrument> payload = new ArrayList<>();
        String payloadStr = row[columnIndexes.get("Payload")].getContents();
        String[] contents = payloadStr.split(", ");
        for(int i = 0; i < contents.length; i++){
            Instrument ins = instrumentList.get(contents[i]);
            if(ins == null) throw new InputMismatchException("Constellation input error. " + contents[i] + " not found in instrument database.");
            payload.add(ins);
        }

        Orbit orb = new KeplerianOrbit(alt, ecc, inc, 0.0, raan, anom, PositionAngle.MEAN, inertialFrame, startDate, mu);
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
        String[] longBounds = lonsStr.substring(1,latsStr.length()-1).split(",");

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
        HashMap<Satellite, Set<GndStation>> stationAssignment = new HashMap<>();
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
            for(Satellite sat : cons.getSatellites()){
                stationAssignment.put(sat,groundStations);
            }
        }

        return stationAssignment;
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

    /**
     * Reads database and imports instruments to be used for coverage calculations
     * @param input
     * @return
     */
    private HashMap<String, Instrument> loadInstruments(JSONObject input){
        HashMap<String, Instrument> instrumentList = new HashMap<>();

        try {
            Workbook instrumentsWorkbook = Workbook.getWorkbook( new File(databaseDir + "InstrumentDatabase.xls"));
            Sheet sensingSats = instrumentsWorkbook.getSheet("Instruments");

            HashMap<String, Integer> columnIndexes = readIndexes(sensingSats.getRow(0));

            for(int i = 1; i < sensingSats.getRows(); i++){
                Cell[] row = sensingSats.getRow(i);
                Instrument ins = readInstrument(row, columnIndexes, instrumentsWorkbook);
                instrumentList.put(ins.getName(), ins);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        } catch (OrekitException e) {
            e.printStackTrace();
        }

        return instrumentList;
    }

    /**
     * Reads row in instrument database and returns a new instrument object depending on its type
     * @param row excel row containing information of said instrument
     * @param columnIndexes hashmap that returns the row index of a particular parameter
     * @param instrumentsWorkbook excel workbook containing all instruments
     * @return Instrument object
     * @throws OrekitException
     */
    private Instrument readInstrument(Cell[] row, HashMap<String, Integer> columnIndexes, Workbook instrumentsWorkbook) throws OrekitException {
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
                ins = loadSAR(name, mass, instrumentsWorkbook);
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

    private SAR loadSAR(String name, double mass, Workbook instrumentsWorkbook){
        Sheet instrumentSheet = instrumentsWorkbook.getSheet(name);
        Cell[] parameters = instrumentSheet.getColumn(0);
        Cell[] values = instrumentSheet.getColumn(1);
        HashMap<String, Integer> parameterIndexes = readIndexes(parameters);

        double freq = Double.parseDouble( values[parameterIndexes.get("Frequency")].getContents() );
        double peakPower = Double.parseDouble( values[parameterIndexes.get("MaxPower")].getContents() );
        double dc = Double.parseDouble( values[parameterIndexes.get("DutyCycle")].getContents() );
        double pw = Double.parseDouble( values[parameterIndexes.get("PulseWidth")].getContents() );
        double prf = Double.parseDouble( values[parameterIndexes.get("PRF")].getContents() );
        double nLooks = Double.parseDouble( values[parameterIndexes.get("nLooks")].getContents() );
        String nominalOps = values[parameterIndexes.get("NominalOps")].getContents();
        String antenna = values[parameterIndexes.get("Antenna")].getContents();
        double rb = Double.parseDouble( values[parameterIndexes.get("Datarate")].getContents() );
        String scan = values[parameterIndexes.get("Scanning")].getContents();
        double scanningAngle = Double.parseDouble( values[parameterIndexes.get("ScanningAngle")].getContents() );
        double fov_at = Double.parseDouble( values[parameterIndexes.get("FOV-AT")].getContents() );
        double fov_ct = Double.parseDouble( values[parameterIndexes.get("FOV-CT")].getContents() );
        double lookAngle = Double.parseDouble( values[parameterIndexes.get("LookAngle")].getContents() );

        OffNadirRectangularFOV fieldOfRegard;
        switch(scan){
            case "conical":
                fieldOfRegard = new OffNadirRectangularFOV(0.0,
                        FastMath.toRadians(fov_ct/2.0 + 2*lookAngle) , FastMath.toRadians(fov_at/2.0),
                        0.0, earthShape);

                return new SAR(name, fieldOfRegard, mass, peakPower * dc, freq, peakPower, dc, pw, prf,
                        nLooks, rb, nominalOps, antenna);
            case "side":
                fieldOfRegard = new OffNadirRectangularFOV(FastMath.toRadians(lookAngle),
                        FastMath.toRadians(fov_ct/2.0 + scanningAngle) , FastMath.toRadians(fov_at/2.0),
                        0.0, earthShape);

                return new SAR(name, fieldOfRegard, mass, peakPower * dc, freq, peakPower, dc, pw, prf,
                        nLooks, rb, nominalOps, antenna);
            case "none":
                fieldOfRegard = new OffNadirRectangularFOV(FastMath.toRadians( lookAngle ),
                        FastMath.toRadians(fov_ct/2.0) , FastMath.toRadians(fov_at/2.0) , 0.0, earthShape);

                return new SAR(name, fieldOfRegard, mass, peakPower * dc, freq, peakPower, dc, pw, prf,
                        nLooks, rb, nominalOps, antenna);
            default:
                throw new InputMismatchException("Instrument Database input error on instrument " + name + ". " + scan + " scanning type not yet supported");
        }
    }

    /**
     * Getters for coverage metrics
     */
    public HashMap<Constellation, HashMap<Satellite,
            HashMap<Satellite, TimeIntervalArray>>> getAccessesCL() { return accessesCL; }
    public HashMap<CoverageDefinition, HashMap<Satellite,
            HashMap<TopocentricFrame, TimeIntervalArray>>> getAccessesGP() { return accessesGP; }
    public HashMap<Satellite,
            HashMap<GndStation, TimeIntervalArray>> getAccessesGS() { return accessesGS; }

    /**
     * Getters for simulation constellation, coverage definitions, or ground station assignments
     */
    public ArrayList<Constellation> getConstellations() { return constellations; }
    public HashSet<CoverageDefinition> getCovDefs() { return covDefs; }
    public HashMap<Satellite, Set<GndStation>> getStationAssignment() { return stationAssignment; }
}