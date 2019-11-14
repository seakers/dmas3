package CCBBA.source;

import java.awt.*;
import java.util.Vector;

import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.StrictMath.sqrt;

public class SubtaskBid {
    private double c_aj;                // self bid
    private double t_aj;                // subtask start time
    private Dimension x_aj;             // location of realization
    private int i_opt;                  // index of optimal path
    private double cost_aj;             // cost of task
    private double score;               // score of a task

    public SubtaskBid(){
        int inf = Integer.MIN_VALUE;
        this.c_aj = 0.0;
        this.t_aj = 0.0;
        this.x_aj = new Dimension(inf,inf);
        this.i_opt = 0;
        this.cost_aj = 0;
        this.score = 0.0;
    }

    void calcBidForSubtask(Subtask j, SimulatedAbstractAgent agent){
        Vector<Subtask> oldBundle = agent.getBundle();
        Vector<Subtask> oldPath = agent.getPath();
        PathUtility oldUtility = calcPathUtility(oldPath, agent);

        Vector<Vector<Subtask>> possiblePaths = generateNewPaths(oldPath, j);

        Vector<Subtask> newBundle = new Vector<>();
        newBundle.addAll(oldBundle);
        newBundle.add(j);

        // find optimal placement in path
        double maxPathBid = 0.0;
        for (int i = 0; i < possiblePaths.size(); i++) { // Calculate utility for each new path
            // get new path and calc utility
            Vector<Subtask> newPath = possiblePaths.get(i);
            PathUtility newPathUtility = calcPathUtility(newPath, agent);

            // substract path utilities to obtain subtask utility
            double newPathBid = newPathUtility.getUtility() - oldUtility.getUtility();
            if(i != possiblePaths.size()-1){  // if path modifies previously agreed order, deduct points
                newPathBid = newPathBid - 5.0;
            }

            //get max bid from all new paths
            if(newPathBid > maxPathBid){
                maxPathBid = newPathBid;
                this.c_aj = newPathBid;
                this.t_aj = newPathUtility.getTz().get( newPath.indexOf(j) );
                this.x_aj = j.getParentTask().getLocation();
                this.i_opt = newPath.indexOf(j);


                // calculate path's coalition matrix - omega
                Vector<Subtask> localJ = agent.getLocalResults().getJ();
                Vector<SimulatedAbstractAgent> localZ = agent.getLocalResults().getZ();
                Vector<Vector<SimulatedAbstractAgent>> pathOmega = new Vector<>();

                for(int k = 0; k < agent.getM(); k++) {
                    Vector<SimulatedAbstractAgent> tempCoal = new Vector<>();

                    if( newPath.size() >= k+1 ) {
                        for (int i_j = 0; i_j < localJ.size(); i_j++) {
                            if ((localZ.get(i_j) != agent)
                                    && (localZ.get(i_j) != null)
                                    && (newPath.get(k).getParentTask() == localJ.get(i_j).getParentTask())) {
                                tempCoal.add(localZ.get(i_j));
                            }
                        }
                    }
                    pathOmega.add(tempCoal);
                }

                // calculate task's cost and score
                PathUtility subtaskUtility = calcSubtaskUtility(newPath, j, this.t_aj, agent, pathOmega);
                this.cost_aj = subtaskUtility.getCost();
                this.score = subtaskUtility.getScore();
            }
        }
    }

    private PathUtility calcPathUtility(Vector<Subtask> path, SimulatedAbstractAgent agent){
        PathUtility pathUtility = new PathUtility();
        PathUtility subtaskUtility;

        // calculate path's coalition matrix - omega
        Vector<Subtask> localJ = agent.getLocalResults().getJ();
        Vector<SimulatedAbstractAgent> localZ = agent.getLocalResults().getZ();
        Vector<Vector<SimulatedAbstractAgent>> pathOmega = new Vector<>();

        for(int i = 0; i < path.size(); i++) {
            Vector<SimulatedAbstractAgent> tempCoal = new Vector<>();
            for (int i_j = 0; i_j < localJ.size(); i_j++) {
                if ((localZ.get(i_j) != agent)
                        && (localZ.get(i_j) != null)
                        && (path.get(i).getParentTask() == localJ.get(i_j).getParentTask())) {
                    tempCoal.add(localZ.get(i_j));
                }
            }

            pathOmega.add(tempCoal);
        }

        // calculate total path utility
        for(int i = 0; i < path.size(); i++){
            Subtask j = path.get(i);

            //Calculate time of arrival
            double t_a = calcTimeOfArrival(path, j, agent, pathUtility);

            // Calculate subtask utility within path
            subtaskUtility = calcSubtaskUtility(path,j,t_a, agent, pathOmega);

            // Add to subtask utility to path utility
            pathUtility.setUtility( pathUtility.getUtility() + subtaskUtility.getUtility() );

            // Add time of arrival to path
            pathUtility.addTz(t_a);
        }
        return pathUtility;
    }

    private double calcTimeOfArrival(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent, PathUtility pathUtility){
        //Calculate time of arrival
        IterationResults localResults = agent.getLocalResults();
        double t_a;
        double t_corr;
        double t_quickest = calcQuickestArrivalTime(path, j, agent, pathUtility);

        Vector<Integer> timeConstraints = getTimeConstraints(j, path, agent);
        if(timeConstraints.size() > 0 ) { // if there are time constraints, consider them
            Task parentTask = j.getParentTask();
            double[][] T = parentTask.getT();
            double maxTz = Double.NEGATIVE_INFINITY;
            int i_max = 0;

            for (Integer timeConstraint : timeConstraints) {
                double thisTz;
                boolean pathContainsTimeConstraint = path.contains( agent.getLocalResults().getJ().get(timeConstraint) );
                boolean isBehindInPath =  path.indexOf(j) > path.indexOf( agent.getLocalResults().getJ().get(timeConstraint) );

                if( pathContainsTimeConstraint && isBehindInPath ){
                    int i_const = path.indexOf( agent.getLocalResults().getJ().get(timeConstraint) );
                    thisTz = pathUtility.getTz().get(i_const);
                }
                else {
                    thisTz = localResults.getTz().get(timeConstraint);
                }


                if (thisTz > maxTz) {
                    maxTz = thisTz;
                    i_max = timeConstraint;
                }
            }

            // looks to maximize utility by getting there as quick as possible
            t_corr = T[parentTask.getJ().indexOf(j)][parentTask.getJ().indexOf( localResults.getJ().get(i_max) )];
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

    private Vector<Vector<Subtask>> generateNewPaths(Vector<Subtask> oldPath, Subtask j){
        Vector<Vector<Subtask>> newPaths = new Vector<>();

        for(int i = 0; i < (oldPath.size()+1); i++){
            Vector<Subtask> tempPath = new Vector<>();
            tempPath.addAll(oldPath);
            tempPath.add(i,j);
            newPaths.add(tempPath);
        }

        return newPaths;
    }

    private Vector<Integer> getTimeConstraints(Subtask j, Vector<Subtask> path, SimulatedAbstractAgent agent){
        Vector<Integer> timeConstraints = new Vector<>();
        Task parentTask = j.getParentTask();
        int[][] D = parentTask.getD();

        // evaluate each dependent task
        for(int i = 0; i < parentTask.getJ().size(); i++){
            int i_j = parentTask.getJ().indexOf(j);
            int i_u = agent.getLocalResults().getJ().indexOf(parentTask.getJ().get(i));
            int i_j_path = path.indexOf(j);
            int i_u_path = path.indexOf( parentTask.getJ().get(i) );

            // if j depends on u and u has a winner, then add relationship to time constraints list
            boolean isDependent = D[i_j][i] >= 1;
            boolean hasWinner = agent.getLocalResults().getZ().get(i_u) != null;
            boolean isInPath = path.contains( parentTask.getJ().get(i_j) ) && (parentTask.getJ().get(i_j) != j);
            boolean isBehindInPath = i_j_path > i_u_path;

            if( isDependent && (hasWinner || (isInPath && isBehindInPath)) ){
                timeConstraints.add( agent.getJ().indexOf(j) - parentTask.getJ().indexOf(j) + i );
            }
        }

        return timeConstraints;
    }

    private double calcQuickestArrivalTime(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent, PathUtility pathUtility){
        double delta_x;
        Dimension x_0;
        int i = path.indexOf(j);
        double t_0;

        if(i == 0){ // task is at the beginning of the path
            if(agent.getLocalResults().getOverallPath().size() > 0){ // there exists a path before new path
                Subtask j_p = agent.getLocalResults().getOverallPath().get( agent.getLocalResults().getOverallPath().size() - 1 ); // last task in previous path
                int i_p = agent.getLocalResults().getJ().indexOf( j_p ); // index of las path task in J results vector

                x_0 = j_p.getParentTask().getLocation();
                t_0 = agent.getLocalResults().getTz().get(i_p);
            }
            else{ // there was no previous path
                x_0 = agent.getPosition();
                t_0 = agent.getT_0();
            }
        }
        else{ // there is a task before the current task
            x_0 = path.get(i-1).getParentTask().getLocation();
            t_0 =  pathUtility.getTz().get(i-1);
        }

        Dimension x_f = j.getParentTask().getLocation();
        delta_x = pow( (x_f.getHeight() - x_0.getHeight()) , 2) + pow( (x_f.getWidth() - x_0.getWidth()) , 2);

        return sqrt(delta_x)/agent.getSpeed() + t_0;
    }

    private PathUtility calcSubtaskUtility(Vector<Subtask> path, Subtask j, double t_a, SimulatedAbstractAgent agent, Vector<Vector<SimulatedAbstractAgent>> pathOmega){
        PathUtility pathUtility = new PathUtility();

        double S = calcSubtaskScore(path, j, t_a, agent);
        double g = calcTravelCost(path, j, agent);
        double p = calcMergePenalty(path, j, agent, pathOmega);
        double c_v = calcSubtaskCost(j, g, p, agent);

        pathUtility.setUtility(S - g - p - c_v);
        pathUtility.setCost(g + p + c_v);
        pathUtility.setScore(S);

        return pathUtility;
    }

    private double calcSubtaskScore(Vector<Subtask> path, Subtask j, double t_a, SimulatedAbstractAgent agent){
        double S_max = j.getParentTask().getS_max();
        double K = j.getK();
        double e = calcUrgency(j, t_a, agent);
        double alpha = calcAlpha(j.getK(), j.getParentTask().getI());
        double sigmoid = calcSigmoid(path, j, agent);

        return (S_max/K)*e*alpha*sigmoid;
    }

    private double calcUrgency(Subtask j, double t_a, SimulatedAbstractAgent agent){
        double lambda = j.getParentTask().getLambda();
        double t_start = agent.getT_0();

        return exp(- lambda * (t_a-t_start) );
//        return 1.0; // <- USED ONLY IN APPENDIX B EXAMPLE
    }

    private double calcAlpha(double K, double I){
        if((K/I) == 1.0){
            return 1.0;
        }
        else return (1.0 / 3.0);
    }

    private double calcSigmoid(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        int i = path.indexOf(j);
        double delta_x;
        if(i == 0){
            Dimension x_i = agent.getInitialPosition();
            Dimension x_f = j.getParentTask().getLocation();
            delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);
        }
        else{
            Dimension x_i = path.get(i-1).getParentTask().getLocation();
            Dimension x_f = j.getParentTask().getLocation();
            delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);
        }

        double gamma = j.getParentTask().getGamma();
        double distance =  sqrt(delta_x);

        double e;
        if( gamma == Double.NEGATIVE_INFINITY ) {
            e = 0.0;
        }
        else {
            e = exp(gamma * distance);
        }
//        return 1.0/( 1 + e);
        return 1.0;
    }

    private double calcTravelCost(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        int i = path.indexOf(j);
        double delta_x;
        Dimension x_i;
        if(i == 0){
            Vector<Subtask> overallPath = agent.getLocalResults().getOverallPath();
            if(overallPath.size() == 0){
                x_i = agent.getInitialPosition();
            }
            else {
                int i_last = overallPath.size() - 1;
                x_i = overallPath.get(i_last).getParentTask().getLocation();
            }
        }
        else{
            x_i = path.get(i-1).getParentTask().getLocation();
        }
        Dimension x_f = j.getParentTask().getLocation();
        delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);

        double distance = sqrt(delta_x);

        return distance*agent.getMiu();
    }

    private double calcMergePenalty(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent, Vector<Vector<SimulatedAbstractAgent>> pathOmega){
        int i = path.indexOf(j);
        double C_split;
        double C_merge;

        IterationResults localResults = agent.getLocalResults();
        Vector<Subtask> overallBundle = localResults.getOverallBundle();
        Vector<Subtask> overallPath = localResults.getOverallPath();
        Vector<Vector<SimulatedAbstractAgent>> overallOmega = localResults.getOverallOmega();

        if( i == 0 ){ // j is at beginning of new path
            if( overallOmega.size() == 0) { // no previous coalitions, no split cost
                if (pathOmega.get(i).size() > 0) { // Is there a coalition at i_bundle?
                    C_merge = agent.getC_merge();
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
                            for(SimulatedAbstractAgent coalMember : pathOmega.get(i)){ // check member by member
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
                            C_split = agent.getC_split();
                            C_merge = agent.getC_merge();
                        }
                        else{
                            // coalition is the same
                            C_split = 0.0;
                            C_merge = 0.0;
                        }
                    }
                    else{ // there is NO coalition at i
                        C_split = agent.getC_split();
                        C_merge = 0.0;
                    }
                }
                else{ // there is no coalition at i - 1
                    if(pathOmega.get(i).size() > 0) { // Is there a coalition at i?
                        // new coalition at i
                        C_split = 0.0;
                        C_merge = agent.getC_merge();
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
                        for(SimulatedAbstractAgent coalMember : pathOmega.get(i)){ // check member by member
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
                        C_split = agent.getC_split();
                        C_merge = agent.getC_merge();
                    }
                    else{
                        // coalition is the same
                        C_split = 0.0;
                        C_merge = 0.0;
                    }
                }
                else{ // there is NO coalition at i
                    C_split = agent.getC_split();
                    C_merge = 0.0;
                }
            }
            else{ // There was NO coalition at i-1
                if(pathOmega.get(i).size() > 0) { // Is there a coalition at i?
                    C_split = 0.0;
                    C_merge = agent.getC_merge();
                }
                else{
                    C_split = 0.0;
                    C_merge = 0.0;
                }
            }
        }
        return C_split + C_merge;
    }

    private double calcSubtaskCost(Subtask j, Double g, Double p, SimulatedAbstractAgent agent){
        double cost_const = j.getParentTask().getCostConst();
        double cost_prop = j.getParentTask().getCostProp();
        if( (cost_prop > 0.0)&&(cost_const <= 0.0) ){
            return (agent.readResources() - g - p ) * cost_prop;
        }
        else{
            return cost_const;
        }
    }

    private double calcStartTime(double local_bid, Subtask j, Vector<Subtask> path, SimulatedAbstractAgent agent){
        double t_start = 0.0;

        return t_start;
    }

    private Dimension calcLocation(double local_bid, Subtask j, Vector<Subtask> path, SimulatedAbstractAgent agent){
        // Performs task at location of task.
        // IMPROVEMENT OPPORTUNITY: decide where to do task using sigmoid function
        Dimension location = j.getParentTask().getLocation();
        return location;
    }


    /**
     * Getters and setters
     */
    public double getC(){ return this.c_aj; }
    public double getTStart(){return this.t_aj;}
    public Dimension getX_aj(){return this.x_aj;}
    public int getI_opt(){return this.i_opt;}
    public double getCost_aj(){ return this.cost_aj; }
    public double getScore(){ return this.score; }
}
