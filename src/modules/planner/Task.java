package modules.planner;

import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;

public class Task {

    private String name;                        // Task name
    private double Smax;                        // Maximum score
    private ArrayList<Double> location;         // Task location in {lat, lon, alt}
    private ArrayList<Double> frequencies;      // List of frequencies required for measurement
    private int numLooks;                       // Amount of measurements
    private Requirements requirements;          // List of measurement requirements
    private HashMap<Double, Subtask> subtasks;  // List of subtasks
    private double[][] D;                       // Coalition Dependency Matrix
    private double[][] T;                       // Coalition Correlation Time Matrix

    public Task(String name, double Score, double lat, double lon, double alt, ArrayList<Double> freq,
                double spatialResReq, double swathReq, double lossReq, int numLooks, double tempResReqLooks,
                AbsoluteDate startTime, AbsoluteDate endTime, double tempResMeasurements){

    }
}
