package modules.environment;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.kernel.Watcher;
import madkit.simulation.probe.PropertyProbe;
import modules.agents.SatelliteAgent;
import modules.components.instruments.SAR;
import modules.measurements.*;
import modules.messages.BookkeepingMessage;
import modules.messages.MeasurementMessage;
import modules.messages.filters.MeasurementFilter;
import modules.messages.filters.PauseFilter;
import modules.orbitData.OrbitData;
import modules.planner.NominalPlanner;
import modules.simulation.SimGroups;
import modules.simulation.Simulation;
import modules.utils.Statistics;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.json.simple.JSONObject;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.object.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static constants.JSONFields.*;

/**
 * Environment class in charge of generating measurement requests and making them available to satellites upon requests and coverage/time availability
 *
 * @author a.aguilar
 */
public class Environment extends Watcher {
    /**
     * Name of the environment
     */
    private final String name;

    /**
     * json file object containing input information
     */
    private final JSONObject input;

    /**
     * coverage data of all satellites and ground stations in the scenario
     */
    private final OrbitData orbitData;

    /**
     * Simulation start and end dates
     */
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    /**
     * Organization groups and roles available for agents in the simulation
     */
    private final SimGroups myGroups;

    /**
     * Directory where results of this simulation will be printed to
     */
    private final String simDirectoryAddress;

    /**
     * Types of measurements to be performed in the simulation
     */
    private HashMap<String, HashMap<String, Requirement>> measurementTypes;

    /**
     * List of measurement requests to be done during the simulation
     */
    private ArrayList<MeasurementRequest> requests;

    /**
     * List of measurement request to be done during the simulation in chronological order
     */
    private ArrayList<MeasurementRequest> orderedRequests;

    /**
     * List of measurements performed by satellites throughout the simulation
     */
    private ArrayList<Measurement> measurements;

    /**
     * Simulation Global Time
     */
    private double GVT;

    /**
     * Simulation fixed time-step*
     *  *this time step can be varied to accelerate simulation runtime
     */
    private final double dt;

    /**
     * Simulation of which this environment is a part of
     */
    private final Simulation parentSimulation;

    /**
     * Addresses of all satellite and ground station agents present in the simulation
     */
    private HashMap<Satellite, AgentAddress> satAddresses;
    private HashMap<GndStation, AgentAddress> gndAddresses;

    /**
     * Creates an instance of and Environment to be simulated
     * @param input : JSON input file for simulation
     * @param orbitData : coverage and trajectory data for chosen constellation and coverage definitions
     * @param myGroups : organization groups and roles available for agents in the simulation
     * @throws IOException
     * @throws BiffException
     */
    public Environment(JSONObject input, OrbitData orbitData, SimGroups myGroups, String simDirectoryAddress, Simulation parentSimulation) {
        // Save coverage and cross link data
        this.name = ((JSONObject) input.get(SIM)).get(SCENARIO).toString() + " - Environment";
        this.input = input;
        this.orbitData = orbitData;
        this.startDate = orbitData.getStartDate();
        this.endDate = orbitData.getEndDate();
        this.myGroups = myGroups;
        this.simDirectoryAddress = simDirectoryAddress;
        this.GVT = orbitData.getStartDate().durationFrom(orbitData.getStartDate().getDate());
        this.dt = Double.parseDouble( ((JSONObject) input.get(SETTINGS)).get(TIMESTEP).toString() );
        this.measurements = new ArrayList<>();
        this.parentSimulation = parentSimulation;
    }

    /**
     * Initializes the measurement requests to be performed during the simulation. Claims environment
     * role in simulation and creates proves so that ground stations and satellites can observe the
     * environment.
     *
     * Triggered only when environment agent is launched.
     */
    @Override
    protected void activate(){

        try {
            // load scenario data
            String scenarioDir = orbitData.getScenarioDir();
            String scenarioStr = ((JSONObject) input.get(SIM)).get(SCENARIO).toString();
            Workbook scenarioWorkbook = Workbook.getWorkbook(new File( scenarioDir + scenarioStr + ".xls"));

            // load requirements
            this.measurementTypes = loadRequirements(scenarioWorkbook);

            // generate measurement requests
            this.requests = generateRequests(scenarioWorkbook);
            this.orderedRequests = getOrderedRequests();

            // print measurement requests
            printRequests();

            // request my role so that the viewers can probe me
            requestRole(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.ENVIRONMENT);

            // give probe access to agents - Any agent within the group agent can access this environment's properties
            addProbe(new Environment.AgentsProbe(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SATELLITE, "environment"));
            addProbe(new Environment.AgentsProbe(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.GNDSTAT, "environment"));
            addProbe(new Environment.AgentsProbe(myGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCHEDULER, "environment"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates list of measurement requests to be made in the duration of the simulation
     * @param scenarioWorkbook : open excel sheet workbook containing scenario information
     * @return requests : ArrayList containing generated measurement requests
     */
    private ArrayList<MeasurementRequest> generateRequests(Workbook scenarioWorkbook) throws Exception {
        ArrayList<MeasurementRequest> requests = new ArrayList<>();

        double arrivalRate = Double.parseDouble(((JSONObject) input.get(PLNR)).get(TASK_ARRIVAL_R).toString());
        double meanInterval = Double.parseDouble(((JSONObject) input.get(PLNR)).get(MEAN_INTERVAL_R).toString());
        double planningHorizon = Double.parseDouble(((JSONObject) input.get(PLNR)).get(PLN_HRZN).toString());
        double numReqs = endDate.durationFrom(startDate)*arrivalRate;

        for(int i = 0; i < numReqs; i++){
            CoverageDefinition covDef = randomCovDef();
            assert covDef != null;
            CoveragePoint location = randomPoint(covDef);

            String type = covDefMeasurementType(covDef, scenarioWorkbook);
            HashMap<String, Requirement> requirements = this.measurementTypes.get(type);

            ArrayList<AbsoluteDate> dates;
            if(i==0){
                dates = randomDates(requirements.get("Temporal"), null, planningHorizon, arrivalRate, meanInterval);
            }
            else {
                dates = randomDates(requirements.get("Temporal"), requests.get(i-1), planningHorizon, arrivalRate, meanInterval);
            }
            AbsoluteDate announceDate = dates.get(0);
            AbsoluteDate startDate = dates.get(1);
            AbsoluteDate endDate = dates.get(2);

            MeasurementRequest request = new MeasurementRequest(i, covDef, location,
                    announceDate, startDate, endDate, type,
                    requirements, orbitData.getStartDate(), 100);
            requests.add(request);
        }

        return requests;
    }

    private String covDefMeasurementType(CoverageDefinition covDef, Workbook scenarioWorkbook) throws Exception {
        Sheet regionsSheet = scenarioWorkbook.getSheet("Regions");
        HashMap<String, Integer> rowIndexes = readIndexes(regionsSheet.getRow(0));

        for(int i = 1; i < regionsSheet.getRows(); i++){
            Cell[] row = regionsSheet.getRow(i);
            String name = row[rowIndexes.get("Name")].getContents();

            if(name.equals(covDef.getName())) return row[rowIndexes.get("Measurement Types")].getContents();
        }

        throw new Exception("Coverage Deginition " + covDef.getName() + " not found in database");
    }

    /**
     * Generates random dates for the announcement, start, and ending of a measurement request.
     * Duration determined by temporal requirement of measurement.
     * @param tempRequirement : temporal requirement for measurement
     * @return dates : array of dates containing when the request starts, when it can be announced,
     * and when it stops being available.
     */
    private ArrayList<AbsoluteDate> randomDates(Requirement tempRequirement,
                                                MeasurementRequest request,
                                                double planningHorizon,
                                                double arrivalRate, double meanInterval){
        AbsoluteDate simStartDate = orbitData.getStartDate();
        AbsoluteDate simEndDate = orbitData.getEndDate();
        double simDuration = simEndDate.durationFrom(simStartDate);

        double availability = tempRequirement.getThreshold();
        if(tempRequirement.getUnits().equals("hrs")){
            availability *= 3600;
        }
        else if(tempRequirement.getUnits().equals("min")){
            availability *= 60;
        }
        else if(!tempRequirement.getUnits().equals("s")){
            throw new InputMismatchException("Temporal requirement units not supported");
        }


        ExponentialDistribution startDist = new ExponentialDistribution(1/arrivalRate);
        ExponentialDistribution intervalDist = new ExponentialDistribution(planningHorizon*meanInterval);

        double dt_start = startDist.sample();
        double dt_interval = intervalDist.sample();

        AbsoluteDate startDate;
        AbsoluteDate announceDate;

        if(request == null){
            startDate = simStartDate.shiftedBy(dt_start);
            announceDate = startDate.shiftedBy(dt_interval);
        }
        else{
            startDate = request.getAnnounceDate().shiftedBy(dt_start);
            announceDate = startDate.shiftedBy(dt_interval);
        }

        AbsoluteDate endDate = startDate.shiftedBy(availability);

        ArrayList<AbsoluteDate> dates = new ArrayList<>();
        dates.add(startDate); dates.add(announceDate); dates.add(endDate);

        return dates;
    }

    /**
     * Chooses a random point from the coverage definition that belongs to the type of measurement
     * to be generated
     * @return pt : a random coverage definition belonging to the scenario
     */
    private CoverageDefinition randomCovDef(){
        int i_rand = (int) (Math.random()*orbitData.getCovDefs().size());
        int i = 0;

        for(CoverageDefinition covDef : orbitData.getCovDefs()){
            if(i == i_rand) return covDef;
            i++;
        }

        return null;
    }

    /**
     * Chooses a random point from the coverage definition that belongs to the type of measurement
     * to be generated
     * @param covDef : coverage definition of desired measurement
     * @return pt : a coverage point belonging to covDef
     */
    private CoveragePoint randomPoint(CoverageDefinition covDef){
        int i_rand = (int) (Math.random()*covDef.getNumberOfPoints());
        int i = 0;

        for(CoveragePoint pt : covDef.getPoints()){
            if(i == i_rand) return pt;
            i++;
        }

        return null;
    }

    /**
     * Reads scenario information excel sheet and generates list of requirements for the different
     * types of measurements to be requested in the simulation
     * @param scenarioWorkbook : excel sheet workbook containing information of the chosen scenario
     * @return requirements : hashmap giving a list of requirements of a given type of measurement
     */
    private HashMap<String, HashMap<String, Requirement>> loadRequirements( Workbook scenarioWorkbook ){
        HashMap<String, HashMap<String, Requirement>> requirements = new HashMap<>();

        Sheet weightsSheet = scenarioWorkbook.getSheet("Weights");
        HashMap<String, Integer> weightsRowIndexes = readIndexes(weightsSheet.getRow(0));
        for(int i = 1; i < weightsSheet.getRows(); i++){
            Cell[] row = weightsSheet.getRow(i);
            String type = row[weightsRowIndexes.get("Measurement Type")].getContents();

            HashMap<String, Requirement> reqs = new HashMap<>();
            Sheet reqSheet = scenarioWorkbook.getSheet(type);
            HashMap<String, Integer> reqIndexes = readIndexes(reqSheet.getRow(0));
            for(int j = 1; j < reqSheet.getRows(); j++){
                Cell[] reqRow = reqSheet.getRow(j);
                Requirement req = readRequirement(reqRow, reqIndexes);
                reqs.put(req.getName(), req);
            }

            requirements.put(type, reqs);
        }

        return requirements;
    }

    /**
     * Reads measurement requirements from scenario excel sheet row
     * @param reqRow : row from scenario excel sheet containing info about a particular requirement
     *               including its type, bounds, and units.
     * @param reqIndexes : hashmap that returns the index of a particular metric in reqRow
     * @return a new object of type requirement containing the information from the excel sheet
     */
    private Requirement readRequirement(Cell[] reqRow, HashMap<String, Integer> reqIndexes){
        String name = reqRow[reqIndexes.get("Requirement")].getContents();
        String boundsStr = reqRow[reqIndexes.get("Bounds")].getContents();
        String units = reqRow[reqIndexes.get("Units")].getContents();

        String[] bounds = boundsStr.substring(1,boundsStr.length()-1).split(",");

        double goal = Math.min( Double.parseDouble( bounds[2] ), Double.parseDouble( bounds[0]));
        double breakthrough = Double.parseDouble( bounds[1] );
        double threshold = Math.max( Double.parseDouble( bounds[2] ), Double.parseDouble( bounds[0]));;

        return new Requirement(name, goal, breakthrough, threshold, units);
    }

    /**
     * Creates a hashmap that stores the indexes of the different parameters for databases
     * @param row : first row or column of data that contains field names
     * @return hashmap that, given a parameter name, returns the column or row index of said parameter
     */
    private HashMap<String, Integer> readIndexes(Cell[] row){
        HashMap<String,Integer> indexes = new HashMap<>();
        for(int i = 0; i < row.length; i++){
            indexes.put(row[i].getContents(), i);
        }
        return indexes;
    }

    /**
     * Prints the list of generated measurement requests to a csv file located in the results folder of .
     * this simulation run
     */
    private void printRequests(){
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = simDirectoryAddress + "/" + "requests.csv";
        File f = new File(outAddress);
        if(!f.exists()) {
            // if file does not exist yet, save data
            try{
                fileWriter = new FileWriter(outAddress, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            printWriter = new PrintWriter(fileWriter);

            for(MeasurementRequest request : requests){
                String reqStr = request.toString();
                printWriter.print(reqStr);
            }

            printWriter.close();
        }
    }

    /**
     * Prove Class that allows for other agents to access the properties and methods of the environment.
     */
    class AgentsProbe extends PropertyProbe<AbstractAgent, Environment> {

        public AgentsProbe(String community, String group, String role, String fieldName) {
            super(community, group, role, fieldName);
        }

        @Override
        protected void adding(AbstractAgent agent) {
            super.adding(agent);
            setPropertyValue(agent, Environment.this);
        }
    }

    /**
     * Returns the list of measurements requests in chronological order
     */
    private ArrayList<MeasurementRequest> getOrderedRequests(){
        ArrayList<MeasurementRequest> orderedRequests = new ArrayList<>();
        for(MeasurementRequest req : this.requests){
            if(orderedRequests.size() == 0) {
                orderedRequests.add(req);
                continue;
            }

            int i = 0;
            for(MeasurementRequest reqOrd : orderedRequests){
                if(req.getStartDate().compareTo(reqOrd.getStartDate()) <= 0) break;
                i += 1;
            }
            orderedRequests.add(i,req);
        }

        ArrayList<MeasurementRequest> reqQueue = new ArrayList<>();
        for(MeasurementRequest req : orderedRequests){
            reqQueue.add(req);
        }
        return reqQueue;
    }

    /**
     * Returns the list of all available measurements requests in chronological order
     */
    public LinkedList<MeasurementRequest> getAvailableRequests(){
        LinkedList<MeasurementRequest> availableRequests = new LinkedList<>();

        for(MeasurementRequest request : this.orderedRequests){
            if(this.getCurrentDate().compareTo(request.getAnnounceDate()) >= 0
                    && this.getCurrentDate().compareTo(request.getEndDate()) <= 0){
                availableRequests.add(request);
            }
        }

        return availableRequests;
    }

    /**
     * Returns the list of all available measurements requests within a given time window and
     * returns them in chronological order
     */
    public LinkedList<MeasurementRequest> getAvailableRequests(AbsoluteDate startDate, AbsoluteDate endDate){
        LinkedList<MeasurementRequest> availableRequests = new LinkedList<>();

        for(MeasurementRequest request : this.orderedRequests){
            if(request.getEndDate().compareTo(startDate) >= 0 &&
                request.getAnnounceDate().compareTo(endDate) <= 0){
                availableRequests.add(request);
            }
        }

        return availableRequests;
    }

    public void registerMeasurements(ArrayList<Measurement> measurements){
        this.measurements.addAll(measurements);
    }

    /**
     * Calculates the performance of a measurements performed by a given instrument at the current simulation time
     * @param agent : agent performing the measurement
     * @param instrument : instrument performing the measurement
     * @param request : measurement request being satisfied by measurement
     */
    public HashMap<Requirement, RequirementPerformance> calculatePerformance(SatelliteAgent agent,
                                                                             Instrument instrument,
                                                                             TopocentricFrame target,
                                                                             MeasurementRequest request) throws Exception {
        return calculatePerformance(agent, instrument, target, request, this.getCurrentDate());
    }

    /**
     * Calculates the performance of a measurements performed by a given instrument at the a given simulation time
     * @param agent : agent performing the measurement
     * @param instrument : instrument performing the measurement
     * @param request : measurement request being satisfied by measurement
     * @param date : date of measurement
     */
    public HashMap<Requirement, RequirementPerformance> calculatePerformance(SatelliteAgent agent,
                                                                             Instrument instrument,
                                                                             TopocentricFrame target,
                                                                             MeasurementRequest request,
                                                                             AbsoluteDate date) throws Exception {
        HashMap<Requirement, RequirementPerformance> measurementPerformance = new HashMap<>();

        HashMap<String, Requirement> requirements;
        if(request == null){
            requirements = new HashMap<>();
            requirements.put(Requirement.SPATIAL, new Requirement(Requirement.SPATIAL, 500, 500, 500, Units.KM));
        }
        else{
            requirements = request.getRequirements();
        }

        for(String reqName : requirements.keySet()){
            Requirement req = requirements.get(reqName);

            double score;
            switch(reqName.toLowerCase()){
                case Requirement.SPATIAL:
                    score = calcSpatialResolution(agent, target, instrument, date);
                    break;
                case Requirement.TEMPORAL:
                    double startDelay = date.durationFrom(request.getStartDate());
                    double unitsFactor = getUnitsFactor( req.getUnits() );
                    score = startDelay * unitsFactor;
                    break;
                case Requirement.ACCURACY:
                    score = calcAccuracy(agent, target, instrument, date);
                    break;
                default:
                    throw new Exception("Performance requirement of type " + req.getName() + " not yet supported.");
            }

            measurementPerformance.put(req, new RequirementPerformance(req, score));
        }

        return measurementPerformance;
    }

    private double calcSpatialResolution(SatelliteAgent agent, TopocentricFrame target,
                                         Instrument instrument, AbsoluteDate date) throws Exception {
        // TODO create instrument specific spatial resolution estimation
        if(instrument.getClass().toString().equals(SAR.class.toString())){
            Orbit orbit = agent.getSat().getOrbit();

            Vector3D satPos = orbitData.propagateSatPos(agent.getSat(), date);
            Vector3D targetPos = orbitData.propagateGPPos(target, date);
            Vector3D targetRel = targetPos.subtract(satPos);

            double th_look = Vector3D.angle(satPos.scalarMultiply(-1), targetRel);
            double pulse_width = ((SAR) instrument).getPulseWidth();
            double D = ((SAR) instrument).getAntenna().getDims().get(0);

            double satResAT = D/2;
            double satResCT = 3e8 * pulse_width / (2 * Math.sin(th_look));

            return Math.max(satResAT, satResCT);
        }
        else{
            throw new Exception("Instrument of type " + instrument.getClass().toString()
                    + " not yet supported for Spatial Resolution calculations");
        }
    }

    private double calcAccuracy(SatelliteAgent agent, TopocentricFrame target,
                                Instrument instrument, AbsoluteDate date) throws Exception {

        // TODO create instrument specific spatial resolution estimation
        if(instrument.getClass().toString().equals(SAR.class.toString())){
            Orbit orbit = agent.getSat().getOrbit();

            Vector3D satPos = orbitData.propagateSatPos(agent.getSat(), date);
            Vector3D targetPos = orbitData.propagateGPPos(target, date);
            Vector3D targetRel = targetPos.subtract(satPos);

            double Pt = ((SAR) instrument).getPeakPower();
            double n = 0.6;
            double W = ((SAR) instrument).getAntenna().getDims().get(0);
            double L = ((SAR) instrument).getAntenna().getDims().get(0);
            double c = 3e8;
            double th = Vector3D.angle(satPos.scalarMultiply(-1), targetRel);
            double lambda = 3e8 / ((SAR) instrument).getFreq();
            double h = satPos.getNorm() - Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
            double B = ((SAR) instrument).getBandwidth();
            double k = 1.380649e-23;
            double T = 290;

            double N = Pt * n * n * W * W * L * c * Math.pow(Math.cos(th), 4);
            double D = 8 * Math.PI * lambda * Math.pow(h,3) * B * Math.sin(th) * k * T * B;
            double sigma_dec = D/N;
            double sigma = 10*Math.log10(sigma_dec);

            return sigma;
        }
        else{
            throw new Exception("Instrument of type " + instrument.getClass().toString()
                    + " not yet supported for Spatial Resolution calculations");
        }
    }

    private double getUnitsFactor(String units) throws Exception {
        switch (units){
            case Units.SECONDS:
                return 1;
            case Units.MINS:
                return 1.0/60.0;
            case Units.HRS:
                return 1.0/3600.0;
        }

        throw new Exception("Units of " + units + " for temporal resolution requirement not yet supported");
    }

    /**
     * Updates simulation time
     */
    public void tic(){
        // TODO  allow for time to step forward to next action performed by a ground station,
        //  satellite, or comms satellite
        List<Message> pauseMessages = nextMessages( new PauseFilter());

        if(pauseMessages.size() == 0) this.GVT += this.dt;
        else getLogger().finer("Pause message received. Stopping time for one simulation step");

        getLogger().finest("Current simulation epoch: " + this.GVT);
    }

    public void printResults() throws Exception {
        List<Message> measurementMessages = nextMessages( new MeasurementFilter() );
        List<Message> bookKeepingMessages = nextMessages(null);

        ArrayList<Measurement> measurements = readMeasurementMessages(measurementMessages);
        JSONObject simPerformance = processResults(measurements);

        printPerformance(simPerformance);
        printMeasurements(measurements);
        printMessages(measurementMessages, bookKeepingMessages);
    }

    private void printMessages(List<Message> measurementMessages, List<Message> bookKeepingMessages) throws Exception {
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = simDirectoryAddress + "/" + "messages.csv";
        File f = new File(outAddress);

        if(!f.exists()) {
            // if file does not exist yet, save data
            try{
                fileWriter = new FileWriter(outAddress, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            printWriter = new PrintWriter(fileWriter);

            List<Message> orderedMessages = sortMessages(measurementMessages, bookKeepingMessages);

            for(Message message : orderedMessages){
                BookkeepingMessage messageBook = (BookkeepingMessage) message;
                printWriter.print(messageBook.toString(satAddresses, gndAddresses, startDate));
                printWriter.print("\n");
            }
            printWriter.close();
        }
    }

    private ArrayList<Message> sortMessages(List<Message> measurementMessages, List<Message> bookKeepingMessages){
        ArrayList<Message> sorted = new ArrayList<>();
        ArrayList<Message> unsorted = new ArrayList<>();
        unsorted.addAll(measurementMessages); unsorted.addAll(bookKeepingMessages);

        for(Message unsort : unsorted){
            if(sorted.isEmpty()) {
                sorted.add(unsort);
                continue;
            }

            int i = 0;
            for(Message sort : sorted){
                if(((BookkeepingMessage) unsort).getSendDate().compareTo( ((BookkeepingMessage) sort).getSendDate() ) < 0){
                    break;
                }
                i++;
            }
            sorted.add(i,unsort);
        }

        return sorted;
    }

    private JSONObject processResults(ArrayList<Measurement> measurements){
        JSONObject out = new JSONObject();
        JSONObject inputs = parentSimulation.getInput();

        out.put("name", parentSimulation.getName());
        out.put("planer", ((JSONObject) inputs.get(PLNR)).get(PLNR_NAME).toString());
        JSONObject dates = new JSONObject();
            dates.put("start", startDate.toString());
            dates.put("end", endDate.toString());
        out.put("dates", dates);
        out.put("scenario", ((JSONObject) inputs.get(SIM)).get(SCENARIO).toString());

        JSONObject resultsJSON = new JSONObject();

        resultsJSON.put("coverage", orbitData.coverageStats());
        resultsJSON.put("tasks", this.taskStats(measurements));
        resultsJSON.put("utility", this.utilityStats(measurements));
        resultsJSON.put("coalitionStats", this.coalStats(measurements));

        out.put("results", resultsJSON);

        return out;
    }

    private JSONObject coalStats(ArrayList<Measurement> measurements){
        JSONObject out = new JSONObject();
        HashMap<MeasurementRequest, ArrayList<Measurement>> map = new HashMap<>();

        for(MeasurementRequest req : requests){
            map.put(req, new ArrayList<>());
        }

        for(Measurement measurement : measurements){
            if(measurement.getRequest() == null) continue;

            map.get(measurement.getRequest()).add(measurement);
        }

        int coalCount = 0;
        for(MeasurementRequest req : requests){
            if(map.get(req).size() > 1) coalCount++;
            // TODO add checks for time constraints and coalition requirements
        }

        out.put("coalsFormed", coalCount);
        out.put("coalsAvailable", countAvailableCoals());
        out.put("coalsFormedPtg", ((double) coalCount)/((double) countAvailableCoals()));

        return out;
    }

    private int countAvailableCoals(){
        // TODO make this count dependent to the type of measurement being requested
        //  and the instruments available in the constellation
        int count = 0;
        for(MeasurementRequest request : requests){
            count++;
        }
        return count;
    }

    private JSONObject utilityStats(ArrayList<Measurement> measurements){
        JSONObject out = new JSONObject();
        ArrayList<Double> utilityAchieved = new ArrayList<>();

        double totalUtility = 0.0;
        for(Measurement measurement : measurements){
            utilityAchieved.add(measurement.getUtility());
            totalUtility += measurement.getUtility();
        }
        double availableUtility = calcAvailableUtility();

        out.put("totalUtility", totalUtility);
        out.put("availableUtility", availableUtility);
        out.put("utilityPtg", totalUtility/availableUtility);

        JSONObject taskUtility = new JSONObject();

        double max = Statistics.getMax(utilityAchieved);
        double min = Statistics.getMin(utilityAchieved);
        double avg = Statistics.getMean(utilityAchieved);
        double std = Statistics.getStd(utilityAchieved);

        taskUtility.put("max", max);
        taskUtility.put("min", min);
        taskUtility.put("avg", avg);
        taskUtility.put("std", std);

        out.put("achievedUtility", taskUtility);

        return out;
    }

    private double calcAvailableUtility(){
        double availableUtility = 0.0;

        // Calculate the total utility that would be achieved if agents only did nominal measurements
        for(CoverageDefinition covDef : orbitData.getAccessesGPIns().keySet()){
            for(Satellite sat : orbitData.getAccessesGPIns().get(covDef).keySet()){
                if(orbitData.isCommsSat(sat)) continue;
                for(Instrument ins : orbitData.getAccessesGPIns().get(covDef).get(sat).keySet()){
                    if(ins.getName().contains("FOR")) continue;

                    for(TopocentricFrame point : orbitData.getAccessesGPIns().get(covDef).get(sat).get(ins).keySet()){
                        for(RiseSetTime time : orbitData.getAccessesGPIns().get(covDef).get(sat).get(ins)
                                .get(point).getRiseSetTimes()){

                            if(time.isRise()){
                                availableUtility += NominalPlanner.NominalUtility;
                            }
                        }
                    }
                }
            }
        }

        // calculate the total maximum utility available from measurement requests
        for(MeasurementRequest request : this.requests){
            availableUtility += request.getMaxUtility();
        }

        return availableUtility;
    }

    private JSONObject taskStats(ArrayList<Measurement> measurements){
        JSONObject out = new JSONObject();

        out.put("urgentTaskRequests", this.requests.size());
        out.put("urgentTasksDone", this.getUrgentMeasurements(measurements).size());
        out.put("urgentTaskDonePtg", ((double) this.getUrgentMeasurements(measurements).size())/( (double) this.requests.size() ));
        out.put("nominalTasksDone", this.getNominalMeasurements(measurements).size());

        JSONObject response = this.calcResponseTimes(measurements);
        out.put("responseTime", response);

        return out;
    }

    private JSONObject calcResponseTimes(ArrayList<Measurement> measurements){
        JSONObject out = new JSONObject();

        ArrayList<Double> responseTimes = new ArrayList<>();
        for(Measurement measurement : getUrgentMeasurements(measurements)){
            MeasurementRequest request = measurement.getRequest();
            double response = measurement.getDownloadDate().durationFrom(request.getAnnounceDate());

            responseTimes.add(response);
        }

        double max = Statistics.getMax(responseTimes);
        double min = Statistics.getMin(responseTimes);
        double avg = Statistics.getMean(responseTimes);
        double std = Statistics.getStd(responseTimes);

        out.put("max", max);
        out.put("min", min);
        out.put("avg", avg);
        out.put("std", std);

        return out;
    }

    private ArrayList<Measurement> getNominalMeasurements(ArrayList<Measurement> measurements){
        ArrayList<Measurement> nominal = new ArrayList<>();
        for(Measurement measurement : measurements){
            if(measurement.getRequest() == null) nominal.add(measurement);
        }
        return nominal;
    }

    private ArrayList<Measurement> getUrgentMeasurements(ArrayList<Measurement> measurements){
        ArrayList<Measurement> urgent = new ArrayList<>();
        for(Measurement measurement : measurements){
            if(measurement.getRequest() != null) urgent.add(measurement);
        }
        return urgent;
    }

    private void printPerformance(JSONObject performance){

        try{
            String filename = "results.json";
            FileWriter writer = new FileWriter(simDirectoryAddress + "/" + filename);
            writer.write(performance.toJSONString());
            writer.close();

            getLogger().info("Results saved as " + filename + "\n\tin directory\n\t" + simDirectoryAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void printMeasurements( ArrayList<Measurement> measurements ){
        FileWriter fileWriter = null;
        PrintWriter printWriter;
        String outAddress = simDirectoryAddress + "/" + "measurements.csv";
        File f = new File(outAddress);

        if(!f.exists()) {
            // if file does not exist yet, save data
            try{
                fileWriter = new FileWriter(outAddress, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            printWriter = new PrintWriter(fileWriter);

            for (Measurement measurement : measurements) {
                printWriter.print(measurement.toString(startDate));
                printWriter.print("\n");
            }
            printWriter.close();
        }
    }

    private ArrayList<Measurement> readMeasurementMessages(List<Message> measurementMessages){
        ArrayList<Measurement> measurements = new ArrayList<>();

        for(Message message : measurementMessages){
            Message originalMessage = ((BookkeepingMessage) message).getOriginalMessage();
            ArrayList<Measurement> receivedMeasurements = ((MeasurementMessage) originalMessage).getMeasurements();
            measurements.addAll(receivedMeasurements);
        }

        return measurements;
    }

    /**
     * Saves addresses of all agents in the simulation for future use
     * @param satAdd : map of each satellite to an address
     * @param gndAdd : map of each ground station to an address
     */
    public void registerAddresses(HashMap<Satellite, AgentAddress> satAdd, HashMap<GndStation, AgentAddress> gndAdd){
        this.satAddresses = new HashMap<>(satAdd);
        this.gndAddresses = new HashMap<>(gndAdd);
    }

    /**
     * General property getters
     */
    public double getGVT(){ return this.GVT; }
    public double getDt(){ return dt; }
    public AbsoluteDate getStartDate(){return this.startDate;}
    public AbsoluteDate getEndDate(){return this.endDate;}
    public AbsoluteDate getCurrentDate(){ return this.startDate.shiftedBy(this.GVT); }
    public OrbitData getOrbitData(){ return this.orbitData; }
}
