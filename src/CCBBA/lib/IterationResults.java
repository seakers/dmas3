package CCBBA.lib;

import com.lowagie.text.pdf.BidiLine;
import madkit.kernel.AgentAddress;

import java.util.ArrayList;
import java.util.List;

public class IterationResults {
    private ArrayList<IterationDatum> results;
    public SimulatedAgent parentAgent;

    /**
     * Constructor
     */
    public IterationResults(SimulatedAgent agent){
        this.results = new ArrayList<>();
        this.parentAgent = agent;
        ArrayList<Subtask> subtaskList = agent.getWorldSubtasks();

        for(Subtask j : subtaskList){ addResult(j, agent); }
    }

    public IterationResults(IterationResults newResults, SimulatedAgent agent){
        this.results = new ArrayList<>();
        this.parentAgent = agent;
        for(IterationDatum datum : newResults.getResults()){
            IterationDatum tempDatum = new IterationDatum(datum);
            this.results.add(tempDatum);
        }
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

    public IterationDatum getIterationDatum(int i){
        return this.results.get(i);
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
            SubtaskBid localBid = new SubtaskBid(j);
            int h;

            if(canBid(j, biddingAgent)){
                // if agent can bid, calculate bid for subtask
                localBid.calcSubtaskBid(j, biddingAgent);

                // coalition and mutex test
                h = coalitionTest(localBid, datum.getJ(), biddingAgent) ;
                if(h == 1){
                    h = mutexTest(localBid, datum.getJ(), biddingAgent);
                }

                // check if agent has enough resources to execute task
                // MISSING
            }
            else{
                h = 0;
            }
            bidList.add(localBid);
            datum.setH(h);
        }
        return bidList;
    }

    private int coalitionTest(SubtaskBid localBid, Subtask j, SimulatedAgent agent) throws Exception {
        IterationDatum testDatum = this.getIterationDatum(j);
        Task parentTask = j.getParentTask();
        int[][] D = j.getParentTask().getD();

        double new_bid = 0.0;
        double coalition_bid = 0.0;

        for(IterationDatum datum : this.results){
            // Check if j and q are in the same task
            if(datum.getJ().getParentTask() == parentTask && datum.getJ() != j){
                //Check if bid outmatches coalition bid
                int i_j = j.getI_q();
                boolean sameWinner = (datum.getZ() == testDatum.getZ());
                boolean noConstraintsUQ = (D[testDatum.getI_q()][datum.getI_q()] == 0);
                boolean constraintsQU = (D[datum.getI_q()][testDatum.getI_q()] >= 1);

                if( sameWinner && (noConstraintsUQ || constraintsQU)){
                    coalition_bid = coalition_bid + datum.getY();
                }

                if( D[testDatum.getI_q()][datum.getI_q()] == 1 ){
                    if(datum.getZ() == agent){
                        new_bid = new_bid + datum.getY();
                    }
                }
            }
        }
        new_bid = new_bid + localBid.getC();;

        if(new_bid > coalition_bid){ return 1; }
        else{ return 0; }
    }
    private int mutexTest(SubtaskBid localBid, Subtask j, SimulatedAgent agent) throws Exception {
        Task parentTask = j.getParentTask();
        ArrayList<Subtask> J_parent = parentTask.getSubtaskList();
        double c = localBid.getC();
        int[][] D = j.getParentTask().getD();

        double new_bid = 0.0;
        for(int q = 0; q < J_parent.size(); q++) {
            //if q != j and D(j,q) == 1, then add y_q to new bid
            if( (J_parent.get(q) != j) && (D[J_parent.indexOf(j)][q] == 1) ){
                new_bid = new_bid + this.getIterationDatum(J_parent.get(q)).getY();
            }
        }
        new_bid = new_bid + c;

        ArrayList<ArrayList<Integer>> coalitionMembers = new ArrayList<>();
        for(int i_j = 0; i_j < J_parent.size(); i_j++){
            ArrayList<Integer> Jv = new ArrayList<>();
            for(int i_q = 0; i_q < J_parent.size(); i_q++){
                if( (D[i_j][i_q] == 1) ){
                    Jv.add(i_q);
                }
            }
            Jv.add(J_parent.indexOf(j));

            coalitionMembers.add(Jv);
        }

        double max_bid = 0.0;
        double y_coalition;
        ArrayList<Integer> Jv;
        for(int i_c = 0; i_c < coalitionMembers.size(); i_c++) {
            y_coalition = 0.0;
            Jv = coalitionMembers.get(i_c);

            for (int i = 0; i < Jv.size(); i++) {
                y_coalition += this.getIterationDatum( parentTask.getSubtaskList().get(Jv.get(i)) ).getY();
            }
            y_coalition += this.getIterationDatum( parentTask.getSubtaskList().get(i_c) ).getY();

            if (y_coalition > max_bid) {
                max_bid = y_coalition;
            }
        }

        if(new_bid > max_bid){ return 1; }
        else{ return 0; }
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

    public void updateResults(SubtaskBid maxBid, SimulatedAgent agent) throws Exception {
        IterationDatum datum = this.getIterationDatum(maxBid.getJ_a());
        datum.setY( maxBid.getC() );
        datum.setZ( agent );
        datum.setTz(maxBid.getTz());
        datum.setC( maxBid.getC() );
        datum.setS( agent.getIteration() );
        datum.setCost( maxBid.getCost() );
        datum.setScore( maxBid.getScore() );
        datum.setX( maxBid.getX() );
    }

    public String toString(){
        StringBuilder output = new StringBuilder(new String());
        output = new StringBuilder("#j\ty\t\tz\t\t\t\t\ttz\t\tc\t\ts\th\tv\tw_any\tw_solo\tcost\tscore\n" +
                                   "========================================================================================\n");
        Task j_current = this.results.get(0).getJ().getParentTask();
        for(IterationDatum datum : this.results){
            if(datum.getJ().getParentTask() != j_current){
                output.append("----------------------------------------------------------------------------------------\n");
            }

            String winnerName;
            if(datum.getZ() == null){
                winnerName = "-";
                output.append( String.format("%d\t%.2f\t%s\t\t\t\t\t%.2f\t%.2f\t%d\t%d\t%d\t%d\t\t%d\t\t%.2f\t%.2f\n",
                        this.results.indexOf(datum)+1,
                        datum.getY(),
                        winnerName,
                        datum.getTz(),
                        datum.getC(),
                        datum.getS(),
                        datum.getH(),
                        datum.getV(),
                        datum.getW_any(),
                        datum.getW_solo(),
                        datum.getCost(),
                        datum.getScore()
                        )
                );
            }
            else{
                winnerName = datum.getZ().getName();
                output.append( String.format("%d\t%.2f\t%s\t%.2f\t%.2f\t%d\t%d\t%d\t%d\t\t%d\t\t%.2f\t%.2f\n",
                        this.results.indexOf(datum)+1,
                        datum.getY(),
                        winnerName,
                        datum.getTz(),
                        datum.getC(),
                        datum.getS(),
                        datum.getH(),
                        datum.getV(),
                        datum.getW_any(),
                        datum.getW_solo(),
                        datum.getCost(),
                        datum.getScore()
                        )
                );
            }
        }
        return output.toString();
    }

    public ArrayList<IterationDatum> getResults(){ return this.results; }
}
