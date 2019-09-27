package CCBBA;

import madkit.kernel.Message;

import java.util.Vector;

public class myMessage extends Message {
    public IterationResults myResults;
    public boolean consensus;
    public String senderName;

    public myMessage(IterationResults newResults, String myName) {
        this.myResults = newResults;
        this.senderName = myName;
    }
}
