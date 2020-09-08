package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import modules.environment.Dependencies;
import modules.environment.Subtask;
import modules.environment.Task;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class IterationResults {
    private HashMap<Subtask, IterationDatum> results;
    private AbstractAgent parentAgent;

    public IterationResults(AbstractAgent parentAgent, ArrayList<Subtask> subtasks, CCBBASettings settings){
        this.results = new HashMap<>();
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
    public ArrayList<Bid> calcBidList(CCBBAPlanner planner) throws Exception {
        ArrayList<Bid> bidList = new ArrayList<>();
        Set<Subtask> subtasks = this.results.keySet();
        ArrayList<Subtask> bundle = planner.getBundle();
        ArrayList<Subtask> path = planner.getPath();

        for(Subtask j : subtasks){
            Bid localBid = new Bid(j);
            int h = 0;
            if(canBid(j, bundle)){
                localBid.calcBid((Spacecraft) parentAgent, planner);

                h = coalitionTest();
                if(h == 1) h = mutexTest();
            }
            this.getIterationDatum(j).setH(h);
            bidList.add(localBid);
        }
        return  bidList;
    }

    private boolean canBid(Subtask j, ArrayList<Subtask> bundle){
        Spacecraft agent = (Spacecraft) this.parentAgent;
        ArrayList<Instrument> instruments = agent.getDesign().getPayload();
        Dependencies dep = j.getParentTask().getDependencies();

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

        // Check if Pessimistic or Optimistic bidding strategy applies
        // Start by counting number of coalitions and requirements
        int N_req = 0;
        int N_sat = 0;

        int i_j = j.getI_q();
        for(Subtask q : j.getParentTask().getSubtasks()){
            int i_q = q.getI_q();

            // if j = q, then continue
            if(i_j == i_q) continue;

            // if j depends on q, then a requirement exists
            if(dep.depends(j,q)) N_req++;

            // if j depends on q and q has a winner, then that requirement is met
            if( results.get(q).getZ() != null && dep.depends(j,q) ) N_sat++;
        }

        int w_any_j = this.getIterationDatum(j).getW_any();
        int w_solo_j = this.getIterationDatum(j).getW_solo();
        int w_all_j = this.getIterationDatum(j).getW_all();

        if(isOptimistic(j)){
            // Agent has NOT yet spent all possible tries biding on this task with dependencies
            // Optimistic bidding strategy to be used to determine bid
            return (((w_any_j > 0)&&(N_sat > 0)) || (w_solo_j > 0) || (N_sat == N_req)) && (w_all_j > 0);
        }
        else{
            // Agent has spent all of its possible tries biding on this tasks with dependencies
            // Pessimistic bidding strategy is used to determine bid
            return (N_req == N_sat && w_all_j > 0);
        }
    }

    private int coalitionTest(){
        return -1;
    }

    private int mutexTest(){
        return -1;
    }

    private boolean isOptimistic(Subtask j){
        // determines if j can be bid on using an optimistic or pessimistic biding strategy
        Dependencies dep = j.getParentTask().getDependencies();
        ArrayList<Subtask> dependentTasks = new ArrayList<>();

        for(Subtask q : j.getParentTask().getSubtasks()){
            // if mutually dependent, then
            if(dep.depends(j,q) && dep.depends(q,j)) dependentTasks.add(q);
        }

        return  dependentTasks.size() > 0;
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
