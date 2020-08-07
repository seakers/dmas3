package modules.agents;

import madkit.kernel.AbstractAgent;
import modules.planner.IterationResults;
import modules.planner.PlannerSettings;
import modules.planner.Subtask;
import modules.planner.Task;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.HashMap;

public class Spacecraft extends AbstractAgent {
    private String name;
    private ArrayList<Instrument> payload;
    private HashMap<AbsoluteDate, PVCoordinates> orbitalPV;
    private HashMap<AbsoluteDate, PVCoordinates> groundPV;
    private HashMap<Task, TaskAccess> accessTimes;
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
