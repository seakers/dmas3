package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import modules.environment.Dependencies;
import modules.environment.Subtask;
import modules.environment.Task;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class IterationResults {
    private HashMap<Subtask, IterationDatum> results;
    private AbstractAgent parentAgent;
    private CCBBAPlanner parentPlanner;

    public IterationResults(Spacecraft parentAgent, ArrayList<Subtask> subtasks, CCBBASettings settings){
        this.results = new HashMap<>();
        this.parentAgent = parentAgent;
        this.parentPlanner = (CCBBAPlanner) parentAgent.getPlanner();
        AbsoluteDate startDate = parentAgent.getStartDate();
        for(Subtask subtask : subtasks){
            IterationDatum datum = new IterationDatum(subtask,settings,startDate);
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
                return true;
            }
        }
        return false;
    }
    public ArrayList<Bid> calcBidList(CCBBAPlanner planner, AbstractAgent parentAgent) throws Exception {
        ArrayList<Bid> bidList = new ArrayList<>();
        Set<Subtask> subtasks = this.results.keySet();
        ArrayList<Subtask> bundle = planner.getBundle();

        for(Subtask j : subtasks){
            Bid localBid = new Bid(j);
            int h = 0;
            if(canBid(j, bundle, planner)){
                localBid.calcBid((Spacecraft) parentAgent, planner);

                h = coalitionTest(localBid,j,planner,parentAgent);
                if(h == 1) h = mutexTest(localBid,j,planner,parentAgent);
                if(h == 1 && localBid.getC() <= 0.0) h = 0;
            }
            this.getIterationDatum(j).setH(h);
            bidList.add(localBid);
        }
        return  bidList;
    }

    private boolean canBid(Subtask j, ArrayList<Subtask> bundle, CCBBAPlanner planner){
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

        // check if dependent subtasks are about to reach coalition violation timeout
        for(Subtask q : j.getParentTask().getSubtasks()){
            boolean depends = dep.depends(j,q);
            boolean violationLimitReached = results.get(q).getV() >= planner.getSettings().O_kq;
            if(depends && violationLimitReached) return  false;
        }

        // check if datum is available
        if(planner.getIterationDatum(j).getH() == 0) {
            return false;
        }

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

    private int coalitionTest(Bid localBid, Subtask j, CCBBAPlanner planner, AbstractAgent agent){
        double new_bid = 0.0;
        double coalition_bid = 0.0;

        ArrayList<Subtask> relatedSubtasks = j.getParentTask().getSubtasks();
        Dependencies dep = j.getParentTask().getDependencies();

        for(Subtask q : relatedSubtasks){
            if(q == j) continue;

            boolean sameWinner = this.getIterationDatum(q).getZ() == this.getIterationDatum(j).getZ();
            boolean noConstraintsJQ = dep.noDependency(j,q);
            boolean constraintsJQ = dep.depends(j,q);
            boolean agentIsWinner = this.getIterationDatum(q).getZ() == agent;

            if( sameWinner && (noConstraintsJQ || constraintsJQ)){
                coalition_bid += this.getIterationDatum(q).getY();
            }

            if( constraintsJQ && agentIsWinner){
                new_bid += this.getIterationDatum(q).getY();
            }
        }
        new_bid += localBid.getC();

        if(new_bid > coalition_bid) return 1;
        else return 0;
    }

    private int mutexTest(Bid localBid, Subtask j, CCBBAPlanner planner, AbstractAgent agent){
        ArrayList<Subtask> relatedSubtasks = j.getParentTask().getSubtasks();
        Dependencies dep = j.getParentTask().getDependencies();

        double new_bid = 0.0;
        ArrayList<ArrayList<Subtask>> coalitionMembers = new ArrayList<>();
        for(Subtask q : relatedSubtasks){
            if( q != j && dep.depends(j,q)) new_bid += this.getIterationDatum(q).getY();

            ArrayList<Subtask> Jv = new ArrayList<>();
            for(Subtask k : relatedSubtasks){
                if(dep.depends(q,k)) Jv.add(k);
            }
            Jv.add(q);
            coalitionMembers.add(Jv);
        }
        new_bid += localBid.getC();

        ArrayList<ArrayList<Subtask>> exclusiveCoalitionMembers = new ArrayList<>();
        for(ArrayList<Subtask> Jv : coalitionMembers){
            ArrayList<Subtask> Jv_p = new ArrayList<>();
            for(Subtask q : Jv){
                for(Subtask k : relatedSubtasks){
                    if(dep.mutuallyExclusive(k,q) && !Jv_p.contains(k)) {
                        Jv_p.add(k);
                    }
                }
            }
            exclusiveCoalitionMembers.add(Jv_p);
        }

        double max_bid = 0.0;
        double y_coalition;
        for(ArrayList<Subtask> Jv_p : exclusiveCoalitionMembers){
            y_coalition = 0.0;
            for(Subtask k : Jv_p){
                y_coalition += this.getIterationDatum(k).getY();
            }
            if(y_coalition > max_bid) max_bid = y_coalition;
        }

        if(new_bid > max_bid) return 1;
        else return 0;
    }

    public boolean isOptimistic(Subtask j){
        // determines if j can be bid on using an optimistic or pessimistic biding strategy
        Dependencies dep = j.getParentTask().getDependencies();
        ArrayList<Subtask> dependentTasks = new ArrayList<>();

        for(Subtask q : j.getParentTask().getSubtasks()){
            // if mutually dependent, then
            if(dep.depends(j,q) && dep.depends(q,j)) dependentTasks.add(q);
        }

        return  dependentTasks.size() > 0;
    }

    public void updateResults(Bid bid, Spacecraft winner){
        for(Subtask j : bid.getWinnerPath()){
            IterationDatum datum = this.getIterationDatum(j);
            int i_path = bid.getWinnerPath().indexOf(j);

            datum.setY( bid.getWinningPathUtility().getUtilityList().get(i_path) );
            datum.setZ( winner );
            datum.setTz( bid.getWinningPathUtility().getTz().get(i_path) );
            datum.setC( bid.getWinningPathUtility().getUtilityList().get(i_path) );
            if(j == bid.getJ()) datum.setS( winner.getCurrentDate().getDate() );

            datum.setCost( bid.getWinningPathUtility().getCostList().get(i_path) );
            datum.setScore( bid.getWinningPathUtility().getScoreList().get(i_path) );
            datum.setPerformance( bid.getPerformanceList().get(i_path) );
        }
    }

    public void updateResults(IterationDatum newDatum){
        this.parentPlanner.releaseTaskFromBundle(newDatum);

        IterationDatum updatedDatum = newDatum.copy();
        updatedDatum.setW_any( this.getIterationDatum(newDatum).getW_any() );
        updatedDatum.setW_solo( this.getIterationDatum(newDatum).getW_solo() );
        updatedDatum.setW_all( this.getIterationDatum(newDatum).getW_all() );
        updatedDatum.setC( this.getIterationDatum(newDatum).getC() );

        this.results.put(newDatum.getSubtask(), updatedDatum);
    }

    public void resetResults(IterationDatum newDatum, AbsoluteDate currentDate){
        this.parentPlanner.releaseTaskFromBundle(newDatum);

        IterationDatum updatedDatum = new IterationDatum(newDatum.getSubtask(), parentPlanner.getSettings(), currentDate);
        updatedDatum.setW_any( this.getIterationDatum(newDatum).getW_any() );
        updatedDatum.setW_solo( this.getIterationDatum(newDatum).getW_solo() );
        updatedDatum.setW_all( this.getIterationDatum(newDatum).getW_all() );
        updatedDatum.setC( this.getIterationDatum(newDatum).getC() );

        this.results.put(newDatum.getSubtask(), updatedDatum);
    }

    public void resetResults(Subtask j, AbsoluteDate currentDate){
        IterationDatum updatedDatum = new IterationDatum(j, parentPlanner.getSettings(), currentDate);
        updatedDatum.setW_any( this.getIterationDatum(j).getW_any() );
        updatedDatum.setW_solo( this.getIterationDatum(j).getW_solo() );
        updatedDatum.setW_all( this.getIterationDatum(j).getW_all() );
        updatedDatum.setC( this.getIterationDatum(j).getC() );

        this.results.put(j, updatedDatum);
    }

    public void resetResults(Subtask j, AbsoluteDate currentDate, boolean override){
        IterationDatum updatedDatum = new IterationDatum(j, parentPlanner.getSettings(), currentDate);
        if(!override) {
            updatedDatum.setW_any(this.getIterationDatum(j).getW_any());
            updatedDatum.setW_solo(this.getIterationDatum(j).getW_solo());
            updatedDatum.setW_all(this.getIterationDatum(j).getW_all());
            updatedDatum.setC(this.getIterationDatum(j).getC());
        }

        this.results.put(j, updatedDatum);
    }

    public void leaveResults(IterationDatum newDatum){
        // does nothing
    }
    public boolean containsKey(Subtask j){
        Set<Subtask> keyset = results.keySet();
        return keyset.contains(j);
    }
    public void put(IterationDatum datum){
        Subtask j = datum.getSubtask();
        this.results.put(j,datum.copy());
    }
    public boolean checkForChanges(IterationResults receivedResults){
        // returns true if changes were made
        Set<Subtask> myKeyset = this.results.keySet();
        Set<Subtask> itsKeyset = receivedResults.getResults().keySet();

        if(myKeyset.size() != itsKeyset.size()) return true;
        else{
            for(Subtask j : myKeyset){
                IterationDatum myDatum = this.getIterationDatum(j);
                IterationDatum itsDatum = receivedResults.getIterationDatum(j);
                if(!myDatum.equals(itsDatum)) return true;
                else if(myDatum.getV() != 0) return true;
            }
            return false;
        }
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
    public IterationDatum getIterationDatum(IterationDatum datum){
        return this.getIterationDatum(datum.getSubtask());
    }

//    public boolean checkForChanges(IterationResults prevResults) {
//
//
//    }
}
