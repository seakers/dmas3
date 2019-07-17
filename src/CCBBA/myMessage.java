package CCBBA;

import madkit.kernel.Message;

import java.util.Vector;

public class myMessage extends Message {
    public IterationResults myResults;
    public boolean consensus;
    public Vector<Double> y_cast = new Vector<>();

    /*
    public myMessage(IterationResults newResults, boolean newConsensus){
        myResults = newResults;
        consensus = newConsensus;
    }
    */
    public myMessage(Vector<Double> y) {
        for(int i = 0; i < y.size(); i++){
            this.y_cast.add(y.get(i));
        }
    }
}
