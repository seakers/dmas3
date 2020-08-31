package modules.planner.messages;

import madkit.kernel.Message;
import modules.environment.Subtask;
import modules.planner.CCBBA.IterationDatum;
import modules.planner.CCBBA.IterationResults;
import modules.spacecraft.Spacecraft;
import org.orekit.utils.PVCoordinates;

import java.util.HashMap;

public class CCBBAResultsMessage extends Message {
    private HashMap<Subtask, IterationDatum> results;
    private PVCoordinates pv;
    private PVCoordinates pvEarth;

    public CCBBAResultsMessage(HashMap<Subtask, IterationDatum> results, PVCoordinates pv, PVCoordinates pvEarth){
        this.results = new HashMap<>(); this.results.putAll(results);
        this.pv = pv;
        this.pvEarth = pvEarth;
    }

    public HashMap<Subtask, IterationDatum> getResults(){return this.results;}
}
