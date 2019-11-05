package CCBBA.source;

import madkit.kernel.Message;

import java.util.Vector;

public class myMessage extends Message {
    public IterationResults myResults;
    public String senderName;

    public myMessage(IterationResults newResults, String myName, SimulatedAbstractAgent agent) {
        this.myResults = new IterationResults(newResults, false, agent);
        this.senderName = myName;
    }
}
