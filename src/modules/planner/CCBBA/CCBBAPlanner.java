package modules.planner.CCBBA;

import modules.agents.Spacecraft;
import modules.environment.Subtask;
import modules.planner.Planner;

import java.util.ArrayList;

public class CCBBAPlanner extends Planner {
    private String planner;
    private PlannerSettings settings;
    private ArrayList<Subtask> bundle;
    private ArrayList<Subtask> overallBundle;
    private ArrayList<Subtask> path;
    private ArrayList<Subtask> overallPath;
    private ArrayList<ArrayList<Spacecraft>> omega;
    private ArrayList<ArrayList<Spacecraft>> overallOmega;
    private IterationResults iterationResults;
    private ArrayList<IterationResults> receivedResults;
}
