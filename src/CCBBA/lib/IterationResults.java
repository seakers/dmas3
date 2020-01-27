package CCBBA.lib;

import java.util.ArrayList;

public class IterationResults {
    // Info used with other agents*********************
    private ArrayList<Subtask> J = new ArrayList<>();                   // available task list
    private ArrayList<Double> y = new ArrayList<>();                    // winner bid list
    private ArrayList<SimulatedAgent> z = new ArrayList<>();            // winner agent list
    private ArrayList<Double> tz = new ArrayList<>();                   // arrival time list

    public IterationResults(SimulatedAgent parentAgent){

    }
}
