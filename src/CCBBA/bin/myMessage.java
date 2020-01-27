package CCBBA.bin;

import madkit.kernel.Message;

import java.util.Vector;

public class myMessage extends Message {
    public IterationResults myResults;
    public IterationLists myLists;
    public String senderName;

    public myMessage(IterationResults newResults, String myName, SimulatedAbstractAgent agent) {
        this.myResults = new IterationResults(newResults, false, agent);
        this.senderName = myName;
    }

    public myMessage(IterationLists newResults, AbstractSimulatedAgent agent) {
        this.myLists = new IterationLists(newResults, false, agent);
        this.senderName = agent.getName();
    }
}
