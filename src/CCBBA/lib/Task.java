package CCBBA.lib;

import jmetal.encodings.variable.Int;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.util.ArrayList;

public class Task {
    /**
     * Task parameters
     */
    private String name;                        // Task name
    private double S_Max;                       // Maximum score
    private double cost;                        // Inherent cost of executing the task
    private String cost_type;                   // Describes if the cost is constant or not
    private ArrayList<String> req_sensors;      // List of required sensors for task
    private ArrayList<Double> location;         // Location of the task
    private String locationType;                // How the location of the task is expressed
    private double t_start;                     // Start of availability time
    private double t_end;                       // End of availability time
    private double duration;                    // Duration of task
    private double t_corr;                      // Correlation time
    private double lambda;                      // Score time decay parameter
    private double gamma;                       // Proximity parameter
    private ArrayList<Subtask> J;               // Subtask list
    private ArrayList<Integer> K;               // Level of partiality
    private int[][] D;                          // Dependency matrix
    private double[][] T;                       // Correlation time matrix
    private int N_sub;                          // Number of subtasks in task
    private int I;                              // Number of sensors needed
    private boolean completeness;               // Completeness of task

    /**
     * Constructor
     * @param taskData - Data received from JSON input file
     */
    Task(JSONObject taskData, JSONObject worldData) throws Exception {
        // Check if all required information is contained in the input file
        checkInputFormat(taskData);

        // Unpack data from JSON input file
        unpackInput(taskData, worldData);

        // Create subtask list from sensor list
        createSensorList();

        // Fill in Dependency and Correlation time matrices
        createDependencies();
    }

    private void checkInputFormat(JSONObject taskData) throws Exception{
        if(taskData.get("MaxScore") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " max score not contained in input file.");
        }
        else if(taskData.get("Cost") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " cost not contained in input file.");
        }
        else if( ((JSONObject) taskData.get("Cost")).get("Value") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " cost value not contained in input file.");
        }
        else if( ((JSONObject) taskData.get("Cost")).get("Type") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " cost type not contained in input file.");
        }
        else if(taskData.get("SensorList") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " Sensor List not contained in input file.");
        }
        else if(taskData.get("Location") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " Location not contained in input file.");
        }
        else if(taskData.get("t_start") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " start time not contained in input file.");
        }
        else if(taskData.get("t_end") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " end time not contained in input file.");
        }
        else if(taskData.get("duration") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " duration time not contained in input file.");
        }
        else if(taskData.get("t_corr") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " correlation time not contained in input file.");
        }
        else if(taskData.get("lambda") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " score time decay parameter (lambda) not contained in input file.");
        }
        else if(taskData.get("gamma") == null){
            throw new NullPointerException("INPUT ERROR:" + taskData.get("Name").toString() + " proximity parameter (gamma) not contained in input file.");
        }
    }

    private void unpackInput(JSONObject taskData, JSONObject worldData) throws Exception {
        // -Task Name
        this.name = taskData.get("Name").toString();

        // -Max Score
        if(taskData.get("MaxScore").getClass().equals( Double.valueOf("1.0").getClass() ) ){
            // --Constant Max Score
            this.S_Max = Double.valueOf(taskData.get("MaxScore").toString());
        }
        else{
            // --Random Max Score
            JSONObject maxScoreData = (JSONObject) taskData.get("MaxScore");
            if( maxScoreData.get("Dist").toString().equals("Linear") ){
                // ---Linear distribution
                double maxVal = (double) maxScoreData.get("MaxValue");
                double minVal = (double) maxScoreData.get("MinValue");
                this.S_Max = (maxVal - minVal) * Math.random() + minVal;
            }
//            else if( maxScoreData.get("Dist").toString().equals("Normal") ){
//                // ---Normal distribution
//                // NEEDS IMPLEMENTATION
//            }
            else{
                throw new Exception("INPUT ERROR:" + this.name + " max score distribution not supported.");
            }
        }

        // -Cost
        JSONObject costData = (JSONObject) taskData.get("Cost");
        this.cost = (double) costData.get("Value");
        this.cost_type = (String) costData.get("Type");
        if(!this.cost_type.equals("Const")){
            if(!this.cost_type.equals("Proportional")){
                throw new Exception("INPUT ERROR:" + this.name + " cost type not supported.");
            }
        }

        // -Sensor List
        JSONArray sensorData = (JSONArray) taskData.get("SensorList");
        this.req_sensors = new ArrayList<>();
        for (Object sensorDatum : sensorData) {
            this.req_sensors.add(sensorDatum.toString());
        }

        // -Location
        this.location = new ArrayList<>();
        if( taskData.get("Location").getClass().equals(sensorData.getClass()) ){
            // --Location is predetermined
            JSONArray locationData = (JSONArray) taskData.get("Location");
            if(locationData.size() == 2){
                // ---Task location is in coordinates
                this.locationType = "coordinates";
            }
            else if(locationData.size() == 3){
                // ---Task location is in 3D vector wrt the Earth's Center
                this.locationType = "vector";
            }
            else{
                throw new Exception("INPUT ERROR:" + this.name + " location format not supported.");
            }
            for (Object locationDatum : locationData) {
                this.location.add((double) locationDatum);
            }
        }
        else if( taskData.get("Location").getClass().equals(costData.getClass()) ){
            // --Location is random
            String locationDist = ((JSONObject) taskData.get("Location")).get("Dist").toString();
            String worldType = worldData.get("Type").toString();

            if(locationDist.equals("Linear")){
                // ---Linear distribution
                if( worldType.equals("2D_Grid") ){
                    // ---Task is in a 2D grid world
                    JSONArray boundsData = (JSONArray) worldData.get("Bounds");
                    double x_max = (double) boundsData.get(0);
                    double y_max = (double) boundsData.get(1);

                    double x = x_max * Math.random();
                    double y = y_max * Math.random();

                    this.location.add(x);
                    this.location.add(y);
                    this.location.add(0.0);
                    this.locationType = "vector";
                }
                else if(worldType.equals("3D_Grid")){
                    // ---Task is in a 3D grid world
                    JSONArray boundsData = (JSONArray) worldData.get("Bounds");
                    double x_max = (double) boundsData.get(0);
                    double y_max = (double) boundsData.get(1);
                    double z_max = (double) boundsData.get(2);

                    double x = x_max * Math.random();
                    double y = y_max * Math.random();
                    double z = z_max * Math.random();

                    this.location.add(x);
                    this.location.add(y);
                    this.location.add(z);
                    this.locationType = "vector";
                }
//                else if(worldType.equals("3D_Earth")){
//                    // ---Task is on earth's surface
//                    // IMPLEMENTATION NEEDED
//                }
                else{
                    throw new Exception("INPUT ERROR:" + this.name + " world not supported.");
                }

            }
//            else if(locationDist.equals("Normal")){
//                // ---Normal distribution
//                // NEEDS IMPLEMENTATION
//            }
            else{
                throw new Exception("INPUT ERROR:" + this.name + " location distribution not supported.");
            }
        }

        // -Time Constraints
        this.t_start = (double) taskData.get("t_start");
        this.duration = (double) taskData.get("duration");
        if( taskData.get("t_end").toString().equals("INF") ){
            this.t_end = Double.POSITIVE_INFINITY;
        }
        else if( taskData.get("t_end").getClass().equals(Double.valueOf(1.0).getClass()) ){
            this.t_end = (double) taskData.get("t_end");
        }
        else{
            throw new Exception("INPUT ERROR:" + this.name + " end time entry not supported.");
        }
        if( taskData.get("t_corr").toString().equals("INF") ){
            this.t_corr = Double.POSITIVE_INFINITY;
        }
        else if( taskData.get("t_corr").getClass().equals(Double.valueOf(1.0).getClass()) ){
            this.t_corr = (double) taskData.get("t_corr");
        }
        else{
            throw new Exception("INPUT ERROR:" + this.name + " correlation time entry not supported.");
        }

        this.t_start += (double) worldData.get("t_0");
        this.t_end += (double) worldData.get("t_0");

        // -Score time decay parameter
        if( taskData.get("lambda").toString().equals("INF") ){
            this.lambda = Double.POSITIVE_INFINITY;
        }
        else if( taskData.get("lambda").toString().equals("NEG_INF") ){
            this.lambda = Double.NEGATIVE_INFINITY;
        }
        else if( taskData.get("lambda").getClass().equals(Double.valueOf(1.0).getClass()) ){
            this.lambda = (double) taskData.get("lambda");
        }
        else{
            throw new Exception("INPUT ERROR:" + this.name + " score decay parameter entry not supported.");
        }

        // -Proximity parameter
        if( taskData.get("gamma").toString().equals("INF") ){
            this.gamma = Double.POSITIVE_INFINITY;
        }
        else if( taskData.get("gamma").toString().equals("NEG_INF") ){
            this.gamma = Double.NEGATIVE_INFINITY;
        }
        else if( taskData.get("gamma").getClass().equals(Double.valueOf(1.0).getClass()) ){
            this.gamma = (double) taskData.get("gamma");
        }
        else{
            throw new Exception("INPUT ERROR:" + this.name + " proximity parameter entry not supported.");
        }

        this.I = req_sensors.size();
    }

    private void createSensorList(){
        this.J = new ArrayList<>();
        this.K = new ArrayList<>();

        for(int i = 0; i < this.req_sensors.size(); i++) {
            ArrayList<String> remainingSensors = new ArrayList<>();
            String mainSensor = this.req_sensors.get(i);

            // create list of dependent sensors
            for(int j = 0; j < this.req_sensors.size(); j++){
                if(i != j){ remainingSensors.add(this.req_sensors.get(j)); }
            }

            // create combinations of dependencies
            ArrayList<ArrayList<String>> combinations = getCombinations( remainingSensors );

            for (ArrayList<String> depTasks : combinations) {
                int j = combinations.indexOf( depTasks );
                Subtask mainSubtask = new Subtask(mainSensor, i + 1, this, (depTasks.size()+1), this.J.size());

                for (String depTask : depTasks) {
                    if (depTask.length() > 0) {
                        mainSubtask.addDep_task(depTask, this.req_sensors.indexOf(depTask) + 1);
                    }
                }

                this.J.add(mainSubtask);
                this.K.add((depTasks.size() + 1));
            }
        }
        N_sub = J.size();
    }

    private ArrayList<ArrayList<String>> getCombinations(ArrayList<String> remainingSensors){
        ArrayList<ArrayList<String>> combinations = new ArrayList<>();

        for(int i = 0; i < (int) Math.pow(2, remainingSensors.size()); i++ ){
            String bitRepresentation = Integer.toBinaryString(i);
            ArrayList<String> tempSet = new ArrayList<>();

            for(int j = 0; j < bitRepresentation.length(); j++){
                int delta = remainingSensors.size() - bitRepresentation.length();

                if (bitRepresentation.charAt(j) == '1') {
                    tempSet.add(remainingSensors.get(j + delta));
                }
            }

            combinations.add(tempSet);
        }
        return combinations;
    }

    private void createDependencies(){
        // Create T and D matrices
        D = new int[N_sub][N_sub];
        T = new double[N_sub][N_sub];

        for (int j = 0; j < N_sub; j++){
            Subtask temp_task1 = J.get(j);
            for (int q = 0; q < N_sub; q++){
                Subtask temp_task2 = J.get(q);
                if (j == q){
                    // Subtask has no dependency with itself
                    D[j][q] = 0;
                    T[j][q] = Double.POSITIVE_INFINITY;
                }
                else if(temp_task1.getDep_nums().size() == 0){
                    // Subtask j has no dependent subtasks
                    D[j][q] = -1;
                    T[j][q] = Double.POSITIVE_INFINITY;
                }
                else if(temp_task2.getDep_nums().size() == 0){
                    // Subtask q has no dependent subtasks
                    D[j][q] = -1;
                    T[j][q] = Double.POSITIVE_INFINITY;
                }
                else{
                    // Checks for dependency constraints
                    for (int i = 0; i < temp_task1.getDep_nums().size(); i++){
                        if (temp_task1.getDep_nums().get(i) == temp_task2.getMain_num()){
                            D[j][q] = 1;
                            T[j][q] = this.t_corr;
                        }
                        else{
                            T[j][q] = Double.POSITIVE_INFINITY;
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return String.format(
                "-Name: \t\t\t%s \n" +
                "-Max Score: \t%f \n" +
                "-Sensor List: \t%s \n" +
                "-Location: \t\t%s \n", this.name, this.S_Max, this.req_sensors, this.location);
    }

    /**
     * Getters and Setters
     */
    public String getName(){ return this.name; }
    public ArrayList<Subtask> getSubtaskList(){ return this.J; }
    public int[][] getD(){ return D; }
    public double[][] getT(){ return T; }
    public boolean getCompleteness(){ return this.completeness; }
    public ArrayList<Double> getLocation(){ return this.location; }
    public double getLambda(){ return this.lambda; }
    public double getGamma(){ return this.gamma; }
    public double getS_Max(){ return this.S_Max; }
    public int getI(){ return this.I; }
    public double getDuration(){ return this.duration; }
    public String getCost_type(){ return this.cost_type; }
    public double getCost(){ return this.cost; }
}
