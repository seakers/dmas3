package modules.environment;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import madkit.kernel.Watcher;
import modules.planner.Measurement;
import modules.planner.Task;
import modules.simulation.ProblemStatement;
import org.json.simple.JSONObject;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

public class Environment extends Watcher {
    private Level loggerLevel;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;
    private AbsoluteDate currentDate;
    private double timeStep;
    private ArrayList<Task> environmentTasks;
    private String problemStatementDir;
    private TimeScale utc;

    // Constructor
    public Environment(ProblemStatement prob) throws Exception {
        // load info from problem statements
        setUpLogger(prob.getLoggerLevel());
        this.startDate = prob.getStartDate().getDate();
        this.currentDate = prob.getCurrentDate().getDate();
        this.endDate = prob.getEndDate().getDate();
        this.timeStep = prob.getTimeStep();
        this.problemStatementDir = prob.getProblemStatementDir();
        this.utc = prob.getUtc();
    }

    @Override
    protected void activate() {
        try {
            environmentTasks = new ArrayList<>();
            environmentTasks.addAll( initiateTasks() );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper functions
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
            ArrayList<Measurement> freqs = new ArrayList<>(freqString.length);
            for(int j = 0; j < freqString.length; j++){
                double f = Double.parseDouble(freqString[j]);
                freqs.add(new Measurement(f));
            }
            double spatialResReq = Double.parseDouble( row[6].getContents() );
            double swathReq = Double.parseDouble( row[7].getContents() );
            double lossReq = Double.parseDouble( row[8].getContents() );
            int numLooks = (int) Double.parseDouble( row[9].getContents() );
            double tempResReqLooks = Double.parseDouble( row[10].getContents() );
            String startTimeString = row[11].getContents();
            String endTimeString = row[12].getContents();
            String tempResMeasurementsString = row[13].getContents();
            double urgencyFactor = Double.parseDouble( row[14].getContents() );

            tasks.add( new Task(name, score, lat, lon, alt, freqs,
                    spatialResReq, swathReq, lossReq, numLooks, tempResReqLooks, urgencyFactor,
                    stringToDate(startTimeString), stringToDate(startTimeString), stringToDuration(tempResMeasurementsString)));
        }

        return tasks;
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

    private void stepTime(){
        this.currentDate = this.currentDate.shiftedBy(this.timeStep);
    }

    private double stringToDuration(String duration) throws Exception {
        if(duration.length() == 10) {
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
        else if(duration.length() == 7) {
            int YY = Integer.parseInt(String.valueOf(duration.charAt(1)));
            int MM = Integer.parseInt(String.valueOf(duration.charAt(3)));
            int DD = Integer.parseInt(String.valueOf(duration.charAt(5)));

            double yy = YY * 365.25 * 24 * 3600;
            double mm = MM * 365.25/12 * 24 * 3600;
            double dd = DD * 24 * 3600;
            return yy + mm + dd;
        }
        else{
            throw new Exception("Mission duration format not supported");
        }


    }

    private void setUpLogger(String logger) throws Exception {
        if(logger.equals("OFF")){
            this.loggerLevel = Level.OFF;
        }
        else if(logger.equals("SEVERE")){
            this.loggerLevel = Level.SEVERE;
        }
        else if(logger.equals("WARNING")){
            this.loggerLevel = Level.WARNING;
        }
        else if(logger.equals("INFO")){
            this.loggerLevel = Level.INFO;
        }
        else if(logger.equals("CONFIG")){
            this.loggerLevel = Level.CONFIG;
        }
        else if(logger.equals("FINE")){
            this.loggerLevel = Level.FINE;
        }
        else if(logger.equals("FINER")){
            this.loggerLevel = Level.FINER;
        }
        else if(logger.equals("FINEST")){
            this.loggerLevel = Level.FINEST;
        }
        else if(logger.equals("ALL")){
            this.loggerLevel = Level.ALL;
        }
        else{
            throw new Exception("INPUT ERROR: Logger type not supported");
        }

        getLogger().setLevel(this.loggerLevel);
    }

    // Getters and Setters
    public ArrayList<Task> getScenarioTasks(){ return this.environmentTasks; }
    public AbsoluteDate getStartDate(){ return this.startDate; }
    public AbsoluteDate getEndDate(){ return this.endDate; }
}
