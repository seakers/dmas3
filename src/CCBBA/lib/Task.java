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
    private double S_Max;                       // Maximum score
    private double cost;                        // Inherent cost of executing the task
    private String cost_type;                   // Describes if the cost is constant or not
    private ArrayList<String> req_sensors;      // List of required sensors for task
    private ArrayList<Double> location;         // Location of the task
    private String locationType;                // How the location of the task is expressed
    private double t_start;                     // Start of availability time
    private double t_end;                       // End of availability time
    private double duration;                    // Duration of task
    private double t_corr;                      // Decorrelation time
    private double lambda;                      // Score time decay parameter
    private double gamma;                       // Proximity parameter
    private ArrayList<Subtask> J;               // Subtask list
    private ArrayList<Integer> K;               // Level of partiality
    private int[][] D;                          // Dependency matrix
    private double[][] T;                       // Decorrelation time matrix
    private int N_sub;                          // Number of subtasks in task
    private int I;                              // Number of sensors needed

    /**
     * Constructor
     * @param taskData - Data received from JSON input file
     */
    Task(JSONObject taskData, JSONObject worldData) throws Exception {
        int x = 1;

        // Check if all required information is contained in the input file
        checkInputFormat(taskData);

        // Unpack data from JSON input file
        unpackInput(taskData, worldData);

        x = 1;
    }

    private void checkInputFormat(JSONObject taskData){
        try {
            if(taskData.get("MaxScore") == null){
                throw new NullPointerException("INPUT ERROR: Task max score not contained in input file.");
            }
            else if(taskData.get("Cost") == null){
                throw new NullPointerException("INPUT ERROR: Task cost not contained in input file.");
            }
            else if( ((JSONObject) taskData.get("Cost")).get("Value") == null){
                throw new NullPointerException("INPUT ERROR: Task cost value not contained in input file.");
            }
            else if( ((JSONObject) taskData.get("Cost")).get("Type") == null){
                throw new NullPointerException("INPUT ERROR: Task cost type not contained in input file.");
            }
            else if(taskData.get("SensorList") == null){
                throw new NullPointerException("INPUT ERROR: Task Sensor List not contained in input file.");
            }
            else if(taskData.get("Location") == null){
                throw new NullPointerException("INPUT ERROR: Task Location not contained in input file.");
            }
            else if(taskData.get("t_start") == null){
                throw new NullPointerException("INPUT ERROR: Task start time not contained in input file.");
            }
            else if(taskData.get("t_end") == null){
                throw new NullPointerException("INPUT ERROR: Task end time not contained in input file.");
            }
            else if(taskData.get("duration") == null){
                throw new NullPointerException("INPUT ERROR: Task duration time not contained in input file.");
            }
            else if(taskData.get("t_corr") == null){
                throw new NullPointerException("INPUT ERROR: Task correlation time not contained in input file.");
            }
            else if(taskData.get("lambda") == null){
                throw new NullPointerException("INPUT ERROR: Task score time decay parameter (lambda) not contained in input file.");
            }
            else if(taskData.get("gamma") == null){
                throw new NullPointerException("INPUT ERROR: Task proximity parameter (gamma) not contained in input file.");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpackInput(JSONObject taskData, JSONObject worldData) throws Exception {
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
                throw new Exception("INPUT ERROR: Task max score distribution not supported.");
            }
        }
        // -Cost
        JSONObject costData = (JSONObject) taskData.get("Cost");
        this.cost = (double) costData.get("Value");
        this.cost_type = (String) costData.get("Type");
        if(!this.cost_type.equals("const")){
            if(!this.cost_type.equals("proportional")){
                throw new Exception("Cost type not supported.");
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
                throw new Exception("INPUT ERROR: Task location format not supported.");
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
//                else if(worldType.equals("3D_World")){
//                    // ---Task is on earth's surface
//                    // IMPLEMENTATION NEEDED
//                }
                else{
                    throw new Exception("INPUT ERROR: Task world not supported.");
                }

            }
//            else if(locationDist.equals("Normal")){
//                // ---Normal distribution
//                // NEEDS IMPLEMENTATION
//            }
            else{
                throw new Exception("INPUT ERROR: Task location distribution not supported.");
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
            throw new Exception("INPUT ERROR: Task end time entry not supported.");
        }
        if( taskData.get("t_corr").toString().equals("INF") ){
            this.t_corr = Double.POSITIVE_INFINITY;
        }
        else if( taskData.get("t_corr").getClass().equals(Double.valueOf(1.0).getClass()) ){
            this.t_corr = (double) taskData.get("t_corr");
        }
        else{
            throw new Exception("INPUT ERROR: Task correlation time entry not supported.");
        }

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
            throw new Exception("INPUT ERROR: Task score decay parameter entry not supported.");
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
            throw new Exception("INPUT ERROR: Task proximity parameter entry not supported.");
        }
    }

}
