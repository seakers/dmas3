package modules.environment;

import constants.JSONFields;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Watcher;
import madkit.simulation.probe.PropertyProbe;
import modules.measurements.MeasurementRequest;
import modules.measurements.Requirement;
import modules.simulation.OrbitData;
import modules.simulation.SimGroups;
import modules.simulation.Simulation;
import org.hipparchus.util.FastMath;
import org.json.simple.JSONObject;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.object.CoverageDefinition;
import seakers.orekit.object.CoveragePoint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;

/**
 * Environment class in charge of generating measurement requests and making them available to satellites upon requests and coverage/time availability
 */
public class Environment extends Watcher {
    private JSONObject input;
    private OrbitData orbitData;
    private Simulation parentSim;
    private String simDirectoryAddress;
    private HashMap<String, HashMap<String, Requirement>> measurementTypes;
    private ArrayList<MeasurementRequest> requests;

    public Environment(JSONObject input, OrbitData orbitData, Simulation parentSim) throws IOException, BiffException {
        // Save coverage and cross link data
        this.input = input;
        this.orbitData = orbitData;
        this.parentSim = parentSim;
        this.simDirectoryAddress = parentSim.getSimDirectoryAddress();
    }

    @Override
    protected void activate(){

        try {
            // load scenario data
            String scenarioDir = orbitData.getScenarioDir();
            String scenarioStr = input.get(JSONFields.SCENARIO).toString();
            Workbook scenarioWorkbook = Workbook.getWorkbook(new File( scenarioDir + scenarioStr + ".xls"));

            // load requirements
            this.measurementTypes = loadRequirements(scenarioWorkbook);

            // generate measurement requests
            this.requests = generateRequests(scenarioWorkbook);

            // print measurement requests
            printRequests();

            // request my role so that the viewers can probe me
            SimGroups myGroups = parentSim.getSimGroups();
            requestRole(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.ENV_ROLE);

            // give probe access to agents - Any agent within the group agent can access this environment's properties
            addProbe(new Environment.AgentsProbe(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.SATELLITE, "environment"));
            addProbe(new Environment.AgentsProbe(myGroups.MY_COMMUNITY, myGroups.SIMU_GROUP, myGroups.GNDSTAT, "environment"));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }

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

                        MeasurementRequest request = new MeasurementRequest(j, location, announceDate, startDate, endDate, type, requirements);
                        requests.add(request);
                    }
                }
            }
        }

        return requests;
    }

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

    private CoveragePoint randomPoint(CoverageDefinition covDef){
        int i_rand = (int) (Math.random()*covDef.getNumberOfPoints());
        int i = 0;

        for(CoveragePoint pt : covDef.getPoints()){
            if(i == i_rand) return pt;
            i++;
        }

        return null;
    }

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

    private Requirement readRequirement(Cell[] reqRow, HashMap<String, Integer> reqIndexes){
        String name = reqRow[reqIndexes.get("Requirement")].getContents();
        String units = reqRow[reqIndexes.get("Units")].getContents();
        String boundsStr = reqRow[reqIndexes.get("Bounds")].getContents();
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
                String reqStr = genReqStr(request);
                printWriter.print(reqStr);
            }

            printWriter.close();
        }
    }

    private String genReqStr(MeasurementRequest req){
        StringBuilder out = new StringBuilder();

        int id = req.getId();
        String type = req.getType();
        CoveragePoint location = req.getLocation();
        double lat = FastMath.toDegrees( location.getPoint().getLatitude() );
        double lon = FastMath.toDegrees( location.getPoint().getLongitude() );
        double announceDate = req.getAnnounceDate().durationFrom( this.orbitData.getStartDate() );
        double startDate = req.getStartDate().durationFrom( this.orbitData.getStartDate() );
        double endDate = req.getEndDate().durationFrom( this.orbitData.getStartDate() );

        HashMap<String, Requirement> requirements = req.getRequirements();

        out.append(id + "," + type + "," + lat + "," + lon + "," + announceDate + "," + startDate + "," + endDate + ",");

        int i = 0;
        for(String requirementName : requirements.keySet()){
            Requirement requirement = requirements.get(requirementName);

            String name = requirement.getName();
            double goal = requirement.getGoal();
            double breakThrough = requirement.getBreakThrough();
            double threshold = requirement.getThreshold();
            String units = requirement.getUnits();

            out.append(name + "," + goal + "," +breakThrough + "," + threshold + "," + units);

            if(i < requirements.keySet().size() - 1) out.append(",");
            i++;
        }
        out.append("\n");

        return out.toString();
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
}
