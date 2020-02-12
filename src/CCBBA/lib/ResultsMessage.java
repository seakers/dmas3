package CCBBA.lib;

import madkit.kernel.Message;

public class ResultsMessage extends Message {
    private IterationResults results;

    public ResultsMessage(IterationResults results, SimulatedAgent agent){
        this.results = new IterationResults(results, agent);

    }
    public IterationResults getResults(){ return this.results; }
    public String getSenderName(){
        return results.getParentAgent().getName();
    }
}
