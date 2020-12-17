package modules.planner.messages;

import madkit.kernel.Message;
import modules.environment.Subtask;
import modules.planner.CCBBA.IterationDatum;
import modules.planner.CCBBA.IterationResults;
import org.orekit.utils.PVCoordinates;

import java.util.HashMap;

public class CCBBAResultsMessage extends Message {
    private IterationResults results;
    private PVCoordinates pvEarth;

    public CCBBAResultsMessage(IterationResults results, PVCoordinates pvEarth){
        this.results = results.copy();
        this.pvEarth = pvEarth;
    }

    public IterationResults getResults(){return results;}
    public PVCoordinates getAgentPV(){return pvEarth;}
}
