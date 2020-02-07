package CCBBA.lib;

import java.util.ArrayList;

import static java.lang.Math.*;
import static java.lang.Math.exp;

public class PathUtility{
    private double utility;                     // path total utility
    private double cost;                        // path total cost
    private double score;                       // path total score
    private ArrayList<Double> tz;               // list of arrival times
    private ArrayList<ArrayList<Double>> x;     // list of measurement locations

    PathUtility(){
        this.utility = 0.0;
        this.cost = 0.0;
        this.score = 0.0;
        this.tz = new ArrayList<>();
        this.x = new ArrayList<>();
    }

    void calcPathUtility(ArrayList<Subtask> path, ArrayList<ArrayList<SimulatedAgent>> omega, SimulatedAgent agent) throws Exception {
        for(Subtask j : path){
            // calc subtask utility
            this.calcSubtaskUtility(j, path, omega, agent);
        }
    }

    void calcSubtaskUtility(Subtask j, ArrayList<Subtask> path, ArrayList<ArrayList<SimulatedAgent>> omega, SimulatedAgent agent) throws Exception {
        double t_a = Double.NEGATIVE_INFINITY;
        ArrayList<Double> x_a = new ArrayList<>();

        if(j.getParentTask().getGamma() == Double.NEGATIVE_INFINITY){
            x_a = j.getParentTask().getLocation();
            t_a = calcTimeOfArrival(path, j, agent, x_a);
        }
        else{
            throw new Exception("ERROR: optimal measurement location determination not yet supported.");
        }

        double S = calcSubtaskScore(path, j, t_a, x_a, agent);
        double g = calcTravelCost(path, j, x_a, agent);
        double p = calcMergePenalty(path, j, agent, omega);
        double c_v = calcSubtaskCost(j, g, p, agent);

        this.utility += S - g - p - c_v;
        this.cost += g + p + c_v;
        this.score += S;
        this.tz.add(t_a);
        this.x.add(x_a);
    }

    private double calcTimeOfArrival(ArrayList<Subtask> path, Subtask j, SimulatedAgent agent, ArrayList<Double> x_a) throws Exception {
        //Calculate time of arrival
        IterationResults localResults = agent.getLocalResults();
        double t_a;
        double t_corr;
        double t_quickest = calcQuickestArrivalTime(path, j, agent, x_a);

        ArrayList<Subtask> timeConstraints = getTimeConstraints(j, path, agent);
        if(timeConstraints.size() > 0 ) {
            // if there are time constraints, consider them
            Task parentTask = j.getParentTask();
            double[][] T = parentTask.getT();
            double maxTz = Double.NEGATIVE_INFINITY;
            int i_max = 0;

            for (Subtask timeConstraint : timeConstraints) {
                double thisTz;
                boolean pathContainsTimeConstraint = path.contains( timeConstraint );
                boolean isBehindInPath =  path.indexOf(j) > path.indexOf( timeConstraint );

                if( pathContainsTimeConstraint && isBehindInPath ){
                    int i_const = path.indexOf( timeConstraint );
                    thisTz = this.tz.get(i_const);
                }
                else {
                    thisTz = localResults.getIterationDatum(timeConstraint).getTz();
                }

                if (thisTz > maxTz) {
                    maxTz = thisTz;
                    i_max = timeConstraint.getI_q();
                }
            }

            // looks to maximize utility by getting there as quick as possible
            t_corr = T[j.getI_q()][ i_max ];
            t_a = maxTz - t_corr;

            if(t_a < t_quickest){ // if the
                t_a = t_quickest;
            }
        }
        else{
            t_a = t_quickest;
        }

        return t_a;
    }

    private double calcQuickestArrivalTime(ArrayList<Subtask> path, Subtask j, SimulatedAgent agent, ArrayList<Double> x_a) throws Exception{
        double delta_x;
        ArrayList<Double> x_0;
        int i = path.indexOf(j);
        double t_0;

        if(i == 0){ // task is at the beginning of the path
            if(agent.getOverallPath().size() > 0){ // there exists a path before new path
                Subtask j_p = agent.getOverallPath().get( agent.getOverallPath().size() - 1 ); // last task in previous path

                x_0 = j_p.getParentTask().getLocation();
                t_0 = agent.getLocalResults().getIterationDatum(j_p).getTz() + j_p.getParentTask().getDuration();
            }
            else{ // there was no previous path
                x_0 = agent.getPosition();
                t_0 = agent.getT_0();
            }
        }
        else{ // there is a task before the current task
            x_0 = this.x.get(i - 1);
            t_0 = this.tz.get(i - 1);
        }

        if(agent.getEnvironment().getWorldType().equals("2D_Grid") || agent.getEnvironment().getWorldType().equals("3D_Grid")){
            delta_x = 0.0;
            for(int i_x = 0; i_x < x_0.size(); i_x++){
                delta_x += pow( x_a.get(i_x) - x_0.get(i_x) , 2);
            }
            return sqrt(delta_x)/agent.getSpeed() + t_0;
        }
        else{
            throw new Exception("INPUT ERROR: World-type not supported");
        }
    }

    private ArrayList<Subtask> getTimeConstraints(Subtask j, ArrayList<Subtask> path, SimulatedAgent agent) throws Exception {
        ArrayList<Subtask> timeConstraints = new ArrayList<>();
        Task parentTask = j.getParentTask();
        int[][] D = parentTask.getD();
        int i_q = j.getI_q();
        int i_q_path = path.indexOf(j);

        // evaluate each dependent task
        for(Subtask j_u : parentTask.getSubtaskList()){
            int i_u = j_u.getI_q();
            int i_u_path = path.indexOf(j_u);

            // if j depends on u and u has a winner, then add relationship to time constraints list
            boolean isDependent = D[i_q][i_u] >= 1;
            boolean hasWinner = (agent.getLocalResults().getIterationDatum(j_u).getZ() != null);
            boolean isInPath = path.contains(j_u) && (j_u != j);
            boolean isBehindInPath = i_q_path > i_u_path;

            if( isDependent && (hasWinner || (isInPath && isBehindInPath)) ){
                timeConstraints.add(j_u);
            }
        }

        return timeConstraints;
    }

    private double calcSubtaskScore(ArrayList<Subtask> path, Subtask j, double t_a, ArrayList<Double> x_a, SimulatedAgent agent){
        double S_max = j.getParentTask().getS_Max();
        double K = j.getK();
        double e = calcUrgency(j, t_a, agent);
        double alpha = calcAlpha(j.getK(), j.getParentTask().getI());
        double sigmoid = calcSigmoid(path, j, agent, x_a);

        return (S_max/K)*e*alpha*sigmoid;
    }

    private double calcUrgency(Subtask j, double t_a, SimulatedAgent agent){
        double lambda = j.getParentTask().getLambda();
        double t_start = agent.getT_0();

        if( lambda == 0.0 ){
            return 1.0;
        }
        else{
            return exp(- lambda * (t_a - t_start) );
        }
    }

    private double calcAlpha(double K, double I){
        if((K/I) == 1.0){
            return 1.0;
        }
        else return (1.0 / 3.0);
    }

    private double calcSigmoid(ArrayList<Subtask> path, Subtask j, SimulatedAgent agent, ArrayList<Double> x_a){
        int i = path.indexOf(j);
        double delta_x = 0.0;
        ArrayList<Double> x_i;
        double gamma = j.getParentTask().getGamma();
        double e;

        if(i == 0){ // task is at the beginning of the path
            if(agent.getOverallPath().size() > 0){ // there exists a path before new path
                x_i = agent.getOverallX_path().get( agent.getOverallX_path().size() - 1 ); // last location in previous path
            }
            else{ // there was no previous path
                x_i = agent.getPosition();
            }
        } else{ // there is a task before the current task
            x_i = this.x.get(i-1);
        }

        for(int i_x = 0; i_x < x_a.size(); i_x++){
            delta_x += pow( x_a.get(i_x) - x_i.get(i_x), 2);
        }

        double distance =  sqrt(delta_x);
        if( gamma == Double.NEGATIVE_INFINITY ) { e = 0.0; }
        else { e = exp(gamma * distance); }

        return 1.0/( 1.0 + e);
    }

    private double calcTravelCost(ArrayList<Subtask> path, Subtask j, ArrayList<Double> x_a, SimulatedAgent agent){
        int i = path.indexOf(j);
        double delta_x = 0.0;
        ArrayList<Double> x_i;

        if(i == 0){ // task is at the beginning of the path
            if(agent.getOverallPath().size() > 0){ // there exists a path before new path
                x_i = agent.getOverallX_path().get( agent.getOverallX_path().size() - 1 ); // last location in previous path
            }
            else{ // there was no previous path
                x_i = agent.getPosition();
            }
        } else{ // there is a task before the current task
            x_i = this.x.get(i-1);
        }

        for(int i_x = 0; i_x < x_a.size(); i_x++){
            delta_x += pow( (x_a.get(i_x) - x_i.get(i_x)), 2);
        }

        double distance =  sqrt(delta_x);

        return distance*agent.getResources().getMiu();
    }

    private double calcMergePenalty(ArrayList<Subtask> path, Subtask j, SimulatedAgent agent, ArrayList<ArrayList<SimulatedAgent>> pathOmega){
        int i = path.indexOf(j);
        double C_split;
        double C_merge;

        IterationResults localResults = agent.getLocalResults();
        ArrayList<Subtask> overallBundle = agent.getOverallBundle();
        ArrayList<Subtask> overallPath = agent.getOverallPath();
        ArrayList<ArrayList<SimulatedAgent>> overallOmega = agent.getOverallOmega();

        if( i == 0 ){ // j is at beginning of new path
            if( overallOmega.size() == 0) { // no previous coalitions, no split cost
                if (pathOmega.get(i).size() > 0) { // Is there a coalition at i_bundle?
                    C_merge = agent.getResources().getC_merge();
                } else {
                    C_merge = 0.0;
                }
                C_split = 0.0;
            }
            else{ // previous coalitions might exist
                Subtask j_last = overallPath.get( overallPath.size() - 1);
                int i_last = overallBundle.indexOf(j_last);

                if(overallOmega.get(i_last).size() > 0) { // is there a coalition at i - 1?
                    if(pathOmega.get(i).size() > 0) { // Is there a coalition at i?
                        boolean sameCoalition = true;

                        //check if coalition is the same at i-1 and i
                        if(pathOmega.get(i).size() == overallOmega.get(i_last).size()){ // check size
                            for(SimulatedAgent coalMember : pathOmega.get(i)){ // check member by member
                                if(!overallOmega.get(i_last).contains(coalMember) ){
                                    // previous coalition does not contain all of the current coalition members
                                    sameCoalition = false;
                                    break;
                                }
                            }
                        }
                        else{
                            // coalition size is different
                            sameCoalition = false;
                        }

                        if(!sameCoalition){
                            // coalition is different
                            C_split = agent.getResources().getC_split();
                            C_merge = agent.getResources().getC_merge();
                        }
                        else{
                            // coalition is the same
                            C_split = 0.0;
                            C_merge = 0.0;
                        }
                    }
                    else{ // there is NO coalition at i
                        C_split = agent.getResources().getC_split();
                        C_merge = 0.0;
                    }
                }
                else{ // there is no coalition at i - 1
                    if(pathOmega.get(i).size() > 0) { // Is there a coalition at i?
                        // new coalition at i
                        C_split = 0.0;
                        C_merge = agent.getResources().getC_merge();
                    }
                    else{
                        // no coalition at i
                        C_split = 0.0;
                        C_merge = 0.0;
                    }
                }
            }

        }
        else {  // j has a previous subtask in the path
            if(pathOmega.get(i - 1).size() > 0){ // Is there was a coalition at i - 1?
                if(pathOmega.get(i).size() > 0){ // Is there a coalition at i?
                    boolean sameCoalition = true;

                    //check if coalition is the same at i-1 and i
                    if(pathOmega.get(i).size() == pathOmega.get(i - 1).size()){ // check size
                        for(SimulatedAgent coalMember : pathOmega.get(i)){ // check member by member
                            if( !pathOmega.get(i - 1).contains(coalMember) ){
                                // previous coalition does not contain all of the current coalition members
                                sameCoalition = false;
                                break;
                            }
                        }
                    }
                    else{
                        // coalition size is different
                        sameCoalition = false;
                    }

                    if(!sameCoalition){
                        // coalition is different
                        C_split = agent.getResources().getC_split();
                        C_merge = agent.getResources().getC_merge();
                    }
                    else{
                        // coalition is the same
                        C_split = 0.0;
                        C_merge = 0.0;
                    }
                }
                else{ // there is NO coalition at i
                    C_split = agent.getResources().getC_split();
                    C_merge = 0.0;
                }
            }
            else{ // There was NO coalition at i-1
                if(pathOmega.get(i).size() > 0) { // Is there a coalition at i?
                    C_split = 0.0;
                    C_merge = agent.getResources().getC_merge();
                }
                else{
                    C_split = 0.0;
                    C_merge = 0.0;
                }
            }
        }
        return C_split + C_merge;
    }

    private double calcSubtaskCost(Subtask j, Double g, Double p, SimulatedAgent agent) throws Exception {

        if(j.getParentTask().getCost_type().equals("Const")){
            return j.getParentTask().getCost();
        }
        else if(j.getParentTask().getCost_type().equals("Const")){
            return j.getParentTask().getCost() * (agent.getResources().getValue() - g - p);
        }
        else{
            throw new Exception("INPUT ERROR: task cost type not supported");
        }
    }

    public double getUtility(){ return this.utility; }
    public double getCost(){ return this.cost;}
    public double getScore(){ return this.score; }
    public ArrayList<Double> getTz(){ return this.tz; }
    public ArrayList<ArrayList<Double>> getX(){ return this.x; }

    public void setUtility(double utility){ this.utility = utility; }
    public void setCost(double cost){ this.cost = cost; }
    public void setScore(double score){ this.score = score; }

}
