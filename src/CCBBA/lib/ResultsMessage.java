package CCBBA.lib;

import madkit.kernel.Message;

public class ResultsMessage extends Message {
    private IterationResults myResults;

    public ResultsMessage(IterationResults results, SimulatedAgent agent){
        this.myResults = new IterationResults(results, agent);

    }
    public IterationResults getResults(){ return this.myResults; }
    public String getSenderName(){
        return myResults.getParentAgent().getName();
    }
}
