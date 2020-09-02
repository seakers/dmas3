package modules.planner.messages;

import madkit.kernel.Message;
import modules.environment.Subtask;
import modules.planner.CCBBA.IterationDatum;
import modules.planner.CCBBA.IterationResults;
import org.orekit.utils.PVCoordinates;

import java.util.HashMap;

public class CCBBAResultsMessage extends Message {
    private IterationResults results;
    private PVCoordinates pv;
    private PVCoordinates pvEarth;

    public CCBBAResultsMessage(IterationResults results, PVCoordinates pv, PVCoordinates pvEarth){
        this.results = results.copy();
        this.pv = pv;
        this.pvEarth = pvEarth;
    }

    public IterationResults getResults(){return this.results;}
}
