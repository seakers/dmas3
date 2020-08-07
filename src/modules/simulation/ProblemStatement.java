package modules.simulation;

import jmetal.encodings.variable.Int;
import jxl.read.biff.BiffException;
import madkit.kernel.AbstractAgent;
import modules.environment.Environment;
import modules.planner.Task;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import jxl.*;

public class ProblemStatement extends AbstractAgent {
    /**
     *  Reads Problem Statements folder location. Initiates Environment and the Tasks Located in it
     */
    private Environment environment;
    private String problemStatementDir;
    private JSONObject inputDataSettings;
    private JSONObject inputDataMission;
    private TimeScale utc;

    public ProblemStatement(String inputFile, String problemStatement) throws Exception {
        this.problemStatementDir = "./src/scenarios/" + problemStatement;
        this.inputDataSettings = (JSONObject) readJSON(inputFile).get("settings");
        this.inputDataMission = (JSONObject) readJSON(inputFile).get("mission");

        // load orekit-data
        File orekitData = new File("./src/data/orekit-data");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        try {
            manager.addProvider(new DirectoryCrawler(orekitData));
        } catch (OrekitException e) {
            e.printStackTrace();
        }

        this.environment = initiateEnvironment();
    }

    private JSONObject readJSON(String inputFileName){
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(
                    "src/inputs/" + inputFileName));
            return (JSONObject) obj;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void executeEnvironmentAgent(){
        launchAgent(this.environment, false);
    }

    private Environment initiateEnvironment() throws Exception {
        this.utc = TimeScalesFactory.getUTC();
        String logger = inputDataSettings.get("logger").toString();
        AbsoluteDate startDate = stringToDate( inputDataMission.get("start").toString() );
        double duration = stringToDuration( inputDataMission.get("duration").toString() );
        AbsoluteDate endDate = startDate.shiftedBy(duration);
        double timeStep = (double) inputDataMission.get("timeStep");

        ArrayList<Task> environmentTasks = initiateTasks();
        return new Environment(logger, startDate, endDate, timeStep, environmentTasks);
    }

    private AbsoluteDate stringToDate(String startDate) throws Exception {
        if(startDate.length() != 20){
            throw new Exception("Date format not supported");
        }

        int YYYY = Integer.parseInt(String.valueOf(startDate.charAt(0))
                                    + String.valueOf(startDate.charAt(1))
                                    + String.valueOf(startDate.charAt(2))
                                    + String.valueOf(startDate.charAt(3)));
        int MM = Integer.parseInt(String.valueOf(startDate.charAt(5))
                + String.valueOf(startDate.charAt(6)));
        int DD = Integer.parseInt(String.valueOf(startDate.charAt(8))
                + String.valueOf(startDate.charAt(9)));

        int hh = Integer.parseInt(String.valueOf(startDate.charAt(11))
                + String.valueOf(startDate.charAt(12)));
        int mm = Integer.parseInt(String.valueOf(startDate.charAt(14))
                + String.valueOf(startDate.charAt(15)));
        int ss = Integer.parseInt(String.valueOf(startDate.charAt(17))
                + String.valueOf(startDate.charAt(18)));

        return new AbsoluteDate(YYYY, MM, DD, hh, mm, ss, utc);
    }

    private double stringToDuration(String duration) throws Exception {
        if(duration.length() != 10){
            throw new Exception("Mission duration format not supported");
        }

        int YY = Integer.parseInt(String.valueOf(duration.charAt(1))
                + String.valueOf(duration.charAt(2)));
        int MM = Integer.parseInt(String.valueOf(duration.charAt(4))
                + String.valueOf(duration.charAt(5)));
        int DD = Integer.parseInt(String.valueOf(duration.charAt(7))
                + String.valueOf(duration.charAt(8)));

        double yy = YY * 365.25 * 24 * 3600;
        double mm = MM * 365.25/12 * 24 * 3600;
        double dd = DD * 24 * 3600;
        return yy + mm + dd;
    }

    private ArrayList<Task> initiateTasks() throws Exception {
        ArrayList<Task> tasks = new ArrayList<>();
        Workbook taskDataXls = Workbook.getWorkbook(new File(problemStatementDir + "/Measurement Requirements.xls"));
        Sheet data = taskDataXls.getSheet("Measurements");
        int nRows = data.getRows();
        int nCols = data.getColumns();
        for(int i = 1; i < nRows; i++){
            // Create a task per each row
            Cell[] row = data.getRow(i);
            String name = row[0].getContents();
            double score = Double.parseDouble( row[1].getContents() );
            double lat = Double.parseDouble( row[2].getContents() );
            double lon = Double.parseDouble( row[3].getContents() );
            double alt = Double.parseDouble( row[4].getContents() );
            String[] freqString = row[5].getContents().split(",");
            ArrayList<Double> freqs = new ArrayList<>(freqString.length);
            for(int j = 0; j < freqString.length; j++){
                freqs.add(Double.parseDouble(freqString[j]));
            }
            double spatialResReq = Double.parseDouble( row[6].getContents() );
            double swathReq = Double.parseDouble( row[7].getContents() );
            double lossReq = Double.parseDouble( row[8].getContents() );
            int numLooks = Integer.parseInt( row[9].getContents() );
            double tempResReqLooks = Double.parseDouble( row[10].getContents() );
            String startTimeString = row[11].getContents();
            String endTimeString = row[11].getContents();
            String tempResMeasurementsString = row[12].getContents();

            tasks.add( new Task(name, score, lat, lon, alt, freqs,
                                spatialResReq, swathReq, lossReq, numLooks, tempResReqLooks,
                                stringToDate(startTimeString), stringToDate(startTimeString), stringToDuration(tempResMeasurementsString)));
        }

        return tasks;
    }
}
