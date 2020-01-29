package CCBBA.lib;

import com.lowagie.text.pdf.BidiLine;

import java.util.ArrayList;

public class IterationResults {
    private ArrayList<IterationDatum> results;

    /**
     * Constructor
     */
    public IterationResults(SimulatedAgent agent){
        this.results = new ArrayList<>();
        ArrayList<Subtask> subtaskList = agent.getWorldSubtasks();

        for(Subtask j : subtaskList){ addResult(j, agent); }
    }

    public IterationResults(IterationResults newResults){

    }

    /**
     * Data Access Funtions
     */
    public int size(){ return results.size(); }

    public IterationDatum getIterationDatum(Subtask j) throws Exception{
        for(IterationDatum datum : this.results){
            if( datum.getJ().equals(j) ){
                return datum;
            }
        }
        throw new Exception("Subtask not contained in results");
    }

    public boolean contains(Subtask j){
        for(IterationDatum datum : this.results){
            if( datum.getJ().equals(j) ){
                return true;
            }
        }
        return false;
    }

    public void addResult(Subtask j, SimulatedAgent agent){
        this.results.add( new IterationDatum(j, agent) );
    }

    public boolean checkAvailability(){
        for(IterationDatum datum : results){
            if(datum.getH() == 1){
                return true;
            }
        }
        return false;
    }

    public ArrayList<SubtaskBid> calcBidList(SimulatedAgent biddingAgent) throws Exception {
        ArrayList<SubtaskBid> bidList = new ArrayList<>( results.size() );
        for(IterationDatum datum : results){
            // calculate bid for each task
            Subtask j = datum.getJ();
            SubtaskBid localBid = new SubtaskBid();
            if(canBid(j, biddingAgent)){
                localBid.calcSubtaskBid(j, biddingAgent);
            }
            bidList.add(localBid);

            // coalition and mutex test

            // check if agent has enough resources to execute task
        }
        return bidList;
    }

    private boolean canBid(Subtask j, SimulatedAgent biddingAgent) throws Exception {
        ArrayList<Subtask> agentBundle = biddingAgent.getBundle();
        if( !biddingAgent.getSensorList().contains( j.getMain_task() ) ){
            // If I don't have the required sensors for this subtask, I can't bid
            return false;
        }
        else if(j.getParentTask().getCompleteness()){
            // if parent task is completed, I can't bid
            return false;
        }
        else if(j.getCompleteness()){
            // if subtask is completed, I can't bid
            return false;
        }
        else if(agentBundle.contains(j)) {
            // if I have already bid for this subtask, I can't bid
            return false;
        }

        // check if bid for a subtask of the same task is in the bundle
        Task parentTask = j.getParentTask();
        int[][] D = parentTask.getD();
        int i_q = j.getI_q();

        // check if subtask in question is mutually exclusive with a bid already in the bundle
        for(Subtask bundleSubtask : agentBundle){
            if(bundleSubtask.getParentTask() == parentTask) {
                int i_b = bundleSubtask.getI_q();
                if (D[i_q][i_b] == -1) { // if subtask j has a mutually exclusive task in bundle, you cannot bid
                    return false;
                }
            }
        }

        // check if dependent task is about to reach coalition violation timeout
        for(Subtask subtask : parentTask.getSubtaskList()){
            int i_j = subtask.getI_q();

            if( (this.getIterationDatum(subtask).getV() >= biddingAgent.getMaxItersInViolation() )
                    && (D[i_q][i_j] >= 1) ){
                // if dependent subtask is about to be timed out, then don't bid
                return false;
            }
        }

        //check if pessimistic or optimistic strategy -> if w_solo(i_j) = 0 & w_any(i_j) = 0, then PBS. Else OBS.
        // Count number of requirements and number of completed requirements
        int N_req = 0;
        int n_sat = 0;

        for(Subtask subtask : j.getParentTask().getSubtaskList()){
            int i_j = subtask.getI_q();

            if(i_q == i_j){ continue; }
            if(D[i_q][i_j] >= 0){ N_req++; }
            if( (this.getIterationDatum(subtask).getZ() != null) && ( D[i_q][i_j] == 1)){ n_sat++; }
        }

        if(!isOptimistic(j)){
            // Agent has spent all possible tries biding on this task with dependencies
            // Pessimistic Bidding Strategy to be used
            return (N_req == n_sat);
        }
        else{
            // Agent has NOT spent all possible tries biding on this task with dependencies
            // Optimistic Bidding Strategy to be used
            int w_any_j = this.getIterationDatum(j).getW_any();
            int w_solo_j = this.getIterationDatum(j).getW_solo();

            return ((w_any_j > 0)&&(n_sat> 0)) || (w_solo_j > 0) || (n_sat == N_req);
        }
    }

    private boolean isOptimistic(Subtask j){
        //check if pessimistic or optimistic strategy
        Task parentTask = j.getParentTask();
        int[][] D = parentTask.getD();
        int i_q = j.getI_q();
        ArrayList<Subtask> dependentTasks = new ArrayList<>();

        for(Subtask subtask : parentTask.getSubtaskList()){
            int i_j = subtask.getI_q();

            if( (D[i_q][i_j] >= 1) && (D[i_j][i_q] == 1) ){
                dependentTasks.add( subtask );
            }
        }

        return dependentTasks.size() > 0;
    }
}
