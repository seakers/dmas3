package modules.environment;

import constants.JSONFields;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Watcher;
import madkit.simulation.probe.PropertyProbe;
import modules.measurement.MeasurementRequest;
import modules.measurement.Requirement;
import modules.orbitData.OrbitData;
import modules.simulation.SimGroups;
import modules.simulation.Simulation;
import org.json.simple.JSONObject;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static constants.JSONFields.*;

/**
 * Environment class in charge of generating measurement requests and making them available to satellites upon requests and coverage/time availability
 */
public class Environment extends Watcher {
    private final JSONObject input;
    private final OrbitData orbitData;
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;
    private final Simulation parentSim;
    private final String simDirectoryAddress;
    private HashMap<String, HashMap<String, Requirement>> measurementTypes;
    private ArrayList<MeasurementRequest> requests;
    private ArrayList<MeasurementRequest> orderedRequests;
    private double GVT;
    private final double dt;

    /**
     * Constructor
     * @param input JSON input file for simulation
     * @param orbitData coverage and trajectory data for chosen constellation and coverage definitions
     * @param parentSim simulation that houses this environment
     * @throws IOException
     * @throws BiffException
     */
    public Environment(JSONObject input, OrbitData orbitData, Simulation parentSim) throws IOException, BiffException {
        // Save coverage and cross link data
        this.input = input;
        this.orbitData = orbitData;
        this.startDate = orbitData.getStartDate();
        this.endDate = orbitData.getEndDate();
        this.parentSim = parentSim;
        this.simDirectoryAddress = parentSim.getSimDirectoryAddress();
        this.GVT = orbitData.getStartDate().durationFrom(orbitData.getStartDate().getDate());
        this.dt = Double.parseDouble( ((JSONObject) input.get(SETTINGS)).get(TIMESTEP).toString() );
    }

    /**
     * Initializes the measurement requests to be performed during the simulation. Claims environment role in simulation
     * and creates proves so that ground stations and satellites can observe the environment. Triggered only when envi-
     * ronment agent is launched.
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
            SimGroups myGroups = parentSim.getSimGroups();
            requestRole(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.ENVIRONMENT);

            // give probe access to agents - Any agent within the group agent can access this environment's properties
            addProbe(new Environment.AgentsProbe(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SATELLITE, "environment"));
            addProbe(new Environment.AgentsProbe(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.GNDSTAT, "environment"));
            addProbe(new Environment.AgentsProbe(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SCHEDULER, "environment"));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates list of measurement requests to be made in the duration of the simulation
     * @param scenarioWorkbook open excelsheet workbook containing scenario information
     * @return requests : ArrayList containing generated measurement requests
     */
    private ArrayList<MeasurementRequest> generateRequests(Workbook scenarioWorkbook){
        ArrayList<MeasurementRequest> requests = new ArrayList<>();

        Sheet regionsSheet = scenarioWorkbook.getSheet("Regions");
        HashMap<String, Integer> rowIndexes = readIndexes(regionsSheet.getRow(0));

        // for every coverage definition, look for its measurement requirement information in scenario excel sheet
        for(CoverageDefinition covDef : orbitData.getCovDefs()){
            for(int i = 1; i < regionsSheet.getRows(); i++){
                Cell[] row = regionsSheet.getRow(i);
                String regionName = row[rowIndexes.get("Name")].getContents();

                if(covDef.getName().equals(regionName)){
                    // generate specified number of requests at random times and locations within
                    // the coverage definition and simulation start and end times
                    String type = row[rowIndexes.get("Type")].getContents();
                    int reqPerDay = Integer.parseInt( row[rowIndexes.get("RequestsPerDay")].getContents() );
                    double numDays = (orbitData.getEndDate().durationFrom(orbitData.getStartDate()) / (24*3600));

                    for(int j = 0; j < reqPerDay*numDays; j++){
                        CoveragePoint location = randomPoint(covDef);
                        HashMap<String, Requirement> requirements = this.measurementTypes.get(type);

                        ArrayList<AbsoluteDate> dates = randomDates(requirements.get("Temporal"));
                        AbsoluteDate announceDate = dates.get(0);
                        AbsoluteDate startDate = dates.get(1);
                        AbsoluteDate endDate = dates.get(2);

                        MeasurementRequest request = new MeasurementRequest(j, location, announceDate, startDate, endDate, type, requirements, orbitData.getStartDate());
                        requests.add(request);
                    }
                }
            }
        }

        return requests;
    }

    /**
     * Generates random dates for the announcement, start, and ending of a measurement request. Duration determined by
     * temporal requirement of measurement.
     * @param tempRequirement
     * @return
     */
    private ArrayList<AbsoluteDate> randomDates(Requirement tempRequirement){
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

        double dt = simDuration * Math.random();
        AbsoluteDate startDate = simStartDate.shiftedBy(dt);
        AbsoluteDate announceDate = simStartDate.shiftedBy(dt);
        AbsoluteDate endDate = startDate.shiftedBy(availability);

        ArrayList<AbsoluteDate> dates = new ArrayList<>();
        dates.add(startDate); dates.add(announceDate); dates.add(endDate);

        return dates;
    }

    /**
     * Chooses a random point from the coverage definition that belongs to the type of measurement to be generated
     * @param covDef coverage definition of desired measurement
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
     * Reads scenario information excel sheet and generates list of requirements for the different types of measurements
     * to be requested in the simulation
     * @param scenarioWorkbook excel sheet workbook containing information of the chosen scenario
     * @return requirements : hashmap giving a list of requirements of a given type of measurement
     */
    private HashMap<String, HashMap<String, Requirement>> loadRequirements( Workbook scenarioWorkbook ){
        HashMap<String, HashMap<String, Requirement>> requirements = new HashMap<>();

        Sheet weightsSheet = scenarioWorkbook.getSheet("Weights");
        HashMap<String, Integer> weightsRowIndexes = readIndexes(weightsSheet.getRow(0));
        for(int i = 1; i < weightsSheet.getRows(); i++){
            Cell[] row = weightsSheet.getRow(i);
            String type = row[weightsRowIndexes.get("MeasurementType")].getContents();

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
     * @param reqRow row from scenario excel sheet containing info about a particular requirement including its type,
     *               bounds, and units.
     * @param reqIndexes hashmap that returns the index of a particular metric in reqRow
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

    public void update(){

    }

    /**
     * Prints the list of generated measurement requests to a csv file located in the results folder of this
     * simulation run
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
     * Updates simulation time
     */
    public void tic(){
        // TODO  allow for time to step forward to next action performed by a ground station, satellite, or comms satellite
        this.GVT += this.dt;
    }

    public double getGVT(){ return this.GVT; }
    public double getDt(){ return dt; }
    public AbsoluteDate getCurrentDate(){ return this.startDate.shiftedBy(this.GVT); }
}
