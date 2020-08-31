package modules.environment;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Watcher;
import madkit.simulation.probe.PropertyProbe;
import modules.simulation.ProblemStatement;
import modules.simulation.SimGroups;
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
            // Initiate environment tasks
            environmentTasks = new ArrayList<>();
            environmentTasks.addAll( initiateTasks() );

            // Calculate Task Trajectory

            // Add probes
            // 1 : request my role so that the viewer can probe me
            requestRole(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.ENV_ROLE);

            // 2 : give probe access to agents - Any agent within the group agent can access this environment's properties
            addProbe(new Environment.AgentsProbe(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.AGENT, "environment"));
            addProbe(new Environment.AgentsProbe(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.PLANNER, "environment"));
            addProbe(new Environment.AgentsProbe(SimGroups.MY_COMMUNITY, SimGroups.SIMU_GROUP, SimGroups.SCH_ROLE, "environment"));

        } catch (Exception e) {
            e.printStackTrace();
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
            double snrReq = Double.parseDouble( row[7].getContents() );
            int numLooks = (int) Double.parseDouble( row[8].getContents() );
            String tempResReqMin =  row[9].getContents();
            String tempResReqMax = row[10].getContents();
            String startTimeString = row[11].getContents();
            String endTimeString = row[12].getContents();
            double urgencyFactor = Double.parseDouble( row[13].getContents() );

            tasks.add( new Task(name, score, lat, lon, alt, freqs,
                    spatialResReq, snrReq, numLooks, stringToDuration(tempResReqMin), stringToDuration(tempResReqMax),
                    stringToDate(startTimeString), stringToDate(endTimeString), timeStep, urgencyFactor));
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
        StringBuilder YYs = new StringBuilder();
        StringBuilder MMs = new StringBuilder();
        StringBuilder DDs = new StringBuilder();
        int stage = 0;

        for(int i = 0; i < duration.length(); i++){
            String c = String.valueOf(duration.charAt(i));
            switch (c) {
                case "P":
                    stage = 1;
                    continue;
                case "Y":
                    stage = 2;
                    continue;
                case "M":
                    stage = 3;
                    continue;
                case "D":
                    stage = 4;
                    continue;
            }

            switch(stage){
                case 1:
                    YYs.append(c);
                    break;
                case 2:
                    MMs.append(c);
                    break;
                case 3:
                    DDs.append(c);
                case 4:
                    break;
            }
        }
        double YY = Double.parseDouble(YYs.toString());
        double MM = Double.parseDouble(MMs.toString());
        double DD = Double.parseDouble(DDs.toString());

        double yy = YY * 365.25 * 24 * 3600;
        double mm = MM * 365.25/12 * 24 * 3600;
        double dd = DD * 24 * 3600;

        return yy + mm + dd;
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

    protected void tic(){
        this.currentDate = this.currentDate.shiftedBy(this.timeStep);
    }

    // Getters and Setters
    public ArrayList<Task> getScenarioTasks(){ return this.environmentTasks; }
    public AbsoluteDate getStartDate(){ return this.startDate; }
    public AbsoluteDate getEndDate(){ return this.endDate; }
    public double getTimeStep(){return this.timeStep;}
    public ArrayList<Task> getEnvironmentTasks(){return environmentTasks;}
    public double getGVT(){ return this.currentDate.durationFrom(this.startDate); }
    public AbsoluteDate getCurrentDate(){return this.currentDate;}
    public ArrayList<Subtask> getEnvironmentSubtasks(){
        ArrayList<Subtask> subtasks = new ArrayList<>();
        for(Task task : this.environmentTasks){
            ArrayList<Subtask> taskSubtasks = task.getSubtasks();
            subtasks.addAll(taskSubtasks);
        }
        return subtasks;
    }
}
