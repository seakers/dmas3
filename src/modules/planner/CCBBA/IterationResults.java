package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class IterationResults {
    private HashMap<Subtask, IterationDatum> results;
    private AbstractAgent parentAgent;

    public IterationResults(AbstractAgent parentAgent, ArrayList<Subtask> subtasks, CCBBASettings settings){
        this.parentAgent = parentAgent;
        for(Subtask subtask : subtasks){
            IterationDatum datum = new IterationDatum(subtask,settings);
            results.put(subtask, datum);
        }
    }

    private IterationResults(AbstractAgent parentAgent, HashMap<Subtask, IterationDatum> results){
        this.parentAgent = parentAgent;
        this.results = new HashMap<>();
        Set<Subtask> keys = results.keySet();
        for(Subtask subtask : keys){
            IterationDatum datum = results.get(subtask);
            this.results.put(subtask, datum.copy());
        }
    }

    public IterationResults copy(){
        return new IterationResults(this.parentAgent, this.results);
    }

    /**
     * Helping functions
     */
    public void resetTaskAvailability(){
        Set<Subtask> keys = results.keySet();
        for(Subtask subtask : keys){
            this.results.get(subtask).resetAvailability();
        }
    }
    public boolean subtasksAvailable(){
        Set<Subtask> keys = results.keySet();
        for(Subtask subtask : keys){
            IterationDatum datum = results.get(subtask);
            if(datum.getH() == 1){
                return false;
            }
        }
        return false;
    }
    public ArrayList<Bid> calcBidList(ArrayList<Subtask> bundle, ArrayList<Subtask> path){
        ArrayList<Bid> bidList = new ArrayList<>();
        Set<Subtask> subtasks = this.results.keySet();

        for(Subtask j : subtasks){
            Bid localBid = new Bid(j);
            int h = 0;
            if(canBid(j)){
                localBid.calcBid();

                h = coalitionTest();
                if(h == 1) h = mutexTest();
            }
            this.getIterationDatum(j).setH(h);
        }
        return  bidList;
    }

    private boolean canBid(Subtask j, ArrayList<Subtask> bundle){
        Spacecraft agent = (Spacecraft) this.parentAgent;
        ArrayList<Instrument> instruments = agent.getDesign().getPayload();

        // if I don't have the sensors to measure this, I can't bid
        boolean sensorsAvailable = false;
        for(Instrument ins : instruments){
            if(ins.getFreq().getBand().equals(j.getMainMeasurement().getBand())){
                sensorsAvailable = true;
                break;
            }
        }
        if(!sensorsAvailable) return false;
        else if(j.getCompletion()) return false;
        else if(j.getParentTask().getCompletion()) return false;
        else if(bundle.contains(j)) return false;
        else if(!agent.hasAccess(j)) return false;


        return true;
    }

    private int coalitionTest(){
        return -1;
    }

    private int mutexTest(){
        return -1;
    }

    /**
     * Getters and Setters
     */
    public AbstractAgent getParentAgent() {
        return parentAgent;
    }

    public HashMap<Subtask, IterationDatum> getResults() {
        return results;
    }
    public IterationDatum getIterationDatum(Subtask subtask){
        return this.results.get(subtask);
    }
}
