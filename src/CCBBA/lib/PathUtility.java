package CCBBA.lib;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.PV;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Math.*;
import static java.lang.Math.exp;

public class PathUtility{
    private double utility;                     // path total utility
    private double cost;                        // path total cost
    private double score;                       // path total score
    private ArrayList<Double> utilityList;
    private ArrayList<Double> costList;
    private ArrayList<Double> scoreList;
    private ArrayList<Double> tz;               // list of arrival times
    private ArrayList<ArrayList<Double>> x;     // list of measurement locations

    public PathUtility(){
        this.utility = 0.0;
        this.cost = 0.0;
        this.score = 0.0;
        this.utilityList = new ArrayList<>();
        this.costList = new ArrayList<>();
        this.scoreList = new ArrayList<>();
        this.tz = new ArrayList<>();
        this.x = new ArrayList<>();
    }

    public PathUtility(PathUtility oldPathUtility){
        this.utility = oldPathUtility.getUtility();
        this.cost = oldPathUtility.getCost();
        this.score = oldPathUtility.getScore();
        this.utilityList = new ArrayList<>();
        this.costList = new ArrayList<>();
        this.scoreList = new ArrayList<>();
        this.tz = new ArrayList<>();
        this.x = new ArrayList<>();

        this.utilityList.addAll(oldPathUtility.getUtilityList());
        this.costList.addAll(oldPathUtility.getCostList());
        this.scoreList.addAll(oldPathUtility.getScoreList());
        this.tz.addAll(oldPathUtility.getTz());
        this.x.addAll(oldPathUtility.getX());
    }

    void calcPathUtility(ArrayList<Subtask> path, ArrayList<ArrayList<SimulatedAgent>> omega, SimulatedAgent agent) throws Exception {
        for(Subtask j : path){
            // calc subtask utility
            this.calcSubtaskUtility(j, path, omega, agent);
        }
    }

    void calcSubtaskUtility(Subtask j, ArrayList<Subtask> path, ArrayList<ArrayList<SimulatedAgent>> omega, SimulatedAgent agent) throws Exception {
        double t_a = 0;
        ArrayList<Double> x_a = new ArrayList<>(3);

        double S = 0.0;
        double sigmoid = 0.0;
        double g = 0.0;
        double p = 0.0;
        double c_v = 0.0;
        double n = 0.0;

        if(j.getParentTask().getGamma() == Double.NEGATIVE_INFINITY){
            if(agent.getEnvironment().getWorldType().equals("2D_World") || agent.getEnvironment().getWorldType().equals("3D_World")){
                x_a = j.getParentTask().getLocation();
                t_a = calcTimeOfArrival(path, j, agent, x_a);

                S = calcSubtaskScore(j, t_a, agent);
                sigmoid = calcSigmoid(j, x_a);
                g = calcTravelCost(path, j, x_a, agent);
                p = calcMergePenalty(path, j, agent, omega);
                c_v = calcSubtaskCost(j, agent);
                n = calcCostNoise(j);
            }
            else if(agent.getEnvironment().getWorldType().equals("3D_Earth")){
                throw new Exception("ERROR: maneuvering satellites not yet supported for this world-type");
            }
            else{
                throw new Exception("ERROR: optimal measurement location determination not yet supported for this world-type.");
            }
        }
        else{
            if(agent.getEnvironment().getWorldType().equals("3D_Earth")){
                if(j.getParentTask().getGamma() == Double.NEGATIVE_INFINITY){
                    throw new Exception("ERROR: optimal measurement location determination not yet supported for set gamma value.");
                }
                if(!agent.getManeuver()){
                    // agent cant maneuver, find optimal place and time for measurement
                    int i_j = path.indexOf(j);
                    AbsoluteDate simStartDate = agent.getEnvironment().getStartDate();
                    ArrayList<AccessTime> agentAccessTimes = agent.getAgentOrbit().getAccessTimes(j);
                    AbsoluteDate  prevPathSubtaskDate;
                    if(i_j == 0){
                        if(agent.getOverallPath().size() > 0){ // there existed a path before new path
                            Subtask j_p = agent.getOverallPath().get( agent.getOverallPath().size() - 1 ); // last task in previous path
                            double t_prev = agent.getLocalResults().getIterationDatum(j_p).getTz() + j_p.getParentTask().getDuration();
                            prevPathSubtaskDate = agent.getEnvironment().getStartDate().shiftedBy(t_prev);
                        }
                        else{ // there was no previous path
                            prevPathSubtaskDate = agent.getEnvironment().getStartDate();
                        }
                    }
                    else{
                        int[][] D = j.getParentTask().getD();
                        // check if any previous tasks are mutually exclusive with j
                        ArrayList<Integer> exclusivePathSubtasks = new ArrayList<>();
                        for(int i_p = i_j; i_p >= 0; i_p--){
                            if(path.get(i_p).getParentTask() != j.getParentTask()){
                                break;
                            }
                            else{
                                int i_q = path.get(i_p).getI_q();
                                int i_u = j.getI_q();
                                if(D[i_u][i_q] <= - 1) {
                                    exclusivePathSubtasks.add(i_p);
                                }
                            }
                        }

                        if(exclusivePathSubtasks.size() > 0){
                            // if there are mutually exclusive subtasks in bundle, chose most recent nonexclusive task as the initial position
                            int i_p = exclusivePathSubtasks.get(exclusivePathSubtasks.size() - 1) - 1;
                            if(i_p > 0){
                                double t_prev = this.tz.get(i_p) + path.get(i_p).getParentTask().getDuration();
                                prevPathSubtaskDate = agent.getEnvironment().getStartDate().shiftedBy(t_prev);
                            }
                            else{
                                if(agent.getOverallPath().size() > 0){ // there exists a path before new path
                                    Subtask j_p = agent.getOverallPath().get( agent.getOverallPath().size() - 1 ); // last task in previous path
                                    double t_prev = agent.getLocalResults().getIterationDatum(j_p).getTz() + j_p.getParentTask().getDuration();
                                    prevPathSubtaskDate = agent.getEnvironment().getStartDate().shiftedBy(t_prev);
                                }
                                else{ // there was no previous path
                                    prevPathSubtaskDate = agent.getEnvironment().getStartDate();
                                }
                            }
                        }
                        else {

                            double t_prev = this.tz.get(i_j - 1) + path.get(i_j - 1).getParentTask().getDuration();
                            prevPathSubtaskDate = agent.getEnvironment().getStartDate().shiftedBy(t_prev);
                        }
                    }

                    // agent might have one or multiple accesses to ground point. Find optimal measurement
                    double utilityAccessMax = 0.0;
                    for(AccessTime agentAccess : agentAccessTimes){
                        // calculate best measurement time and position for each access time
                        AbsoluteDate start = agentAccess.getAccessStart();
                        AbsoluteDate end = agentAccess.getAccessEnd();

                        if( prevPathSubtaskDate.compareTo(start) > 0){
                            if( prevPathSubtaskDate.compareTo(end) > 0) {
                                // access window is before last time of arrival
                                continue;
                            }
                            else{
                                // access window start date is before last time of arrival but end date is still after last time of arrival
                                start = prevPathSubtaskDate;
                            }
                        }

                        // define access window
                        int i_start = agent.getAgentOrbit().getDateData().indexOf(start);
                        int i_end = agent.getAgentOrbit().getDateData().indexOf(end);

                        // for each access time within access window, find the best utility
                        double localAccessMax = 0.0;
                        double S_local = 0.0, sigmoid_local = 0.0, p_local = 0.0, c_v_local = 0.0, n_local = 0.0, t_a_local = 0.0;
                        ArrayList<Double> x_a_loc = new ArrayList<>();
                        for(int i = i_start; i <= i_end; i++){
                            // get task arrival time
                            AbsoluteDate date_i = agent.getAgentOrbit().getDateData().get(i);
                            double t_quickest = date_i.durationFrom(simStartDate);
                            double t_a_access = t_quickest;

                            // get task measurement location
                            PVCoordinates pv_a_local = agent.getAgentOrbit().getGroundTrack(date_i);
                            ArrayList<Double> x_a_accs = new ArrayList<>();
                            x_a_accs.add(pv_a_local.getPosition().getX());
                            x_a_accs.add(pv_a_local.getPosition().getY());
                            x_a_accs.add(pv_a_local.getPosition().getZ());

                            // check if time constraints exist
                            ArrayList<Subtask> timeConstraints = getTimeConstraints(j, path, agent);

                            if( timeConstraints.size() > 0 ){
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
                                        thisTz = agent.getLocalResults().getIterationDatum(timeConstraint).getTz();
                                    }

                                    if (thisTz > maxTz) {
                                        maxTz = thisTz;                     // <- slowest time of arrival in the coalition
                                        i_max = timeConstraint.getI_q();
                                    }
                                }

                                // looks to maximize utility by getting there as quickly as correlation time will allow
                                double t_corr = T[j.getI_q()][ i_max ];
                                if(abs( maxTz - t_quickest ) >= t_corr){
                                    // measurement is taking place outside of correlation time
                                    if( t_quickest > maxTz){
                                        // I am the slowest one in the coalition, the other agents must adjust to my time
                                        t_a_access = t_quickest;
                                    }
                                    else{
                                        // I am getting there too quickly for other dependent agents, I must look for another measurement time
                                        t_a_access = - 1.0;
                                    }
                                }
                            }
                            else{
                                // no time constraints, get there as soon as possible
                                t_a_access = t_quickest;
                            }

                            double S_access = 0.0, sigmoid_access = 0.0, p_access = 0.0, c_v_access = 0.0, n_access = 0.0;
                            if(t_a_access >= 0.0) {
                                // if time constraints were able to be met, calc utility. Else, utility will be set to 0.0
                                S_access = calcSubtaskScore(j, t_a_access, agent);
                                sigmoid_access = calcSigmoid(j, agent, pv_a_local, date_i);
                                p_access = calcMergePenalty(path, j, agent, omega);
                                c_v_access = calcSubtaskCost(j, agent);
                                n_access = calcCostNoise(j);
                            }

                            double localUtility = S_access + sigmoid_access - p_access - c_v_access - n_access;

                            if(localUtility > localAccessMax){
                                localAccessMax = localUtility;
                                S_local = S_access;
                                sigmoid_local = sigmoid_access;
                                p_local = p_access;
                                c_v_local = c_v_access;
                                n_local = n_access;
                                x_a_loc = new ArrayList<>();
                                x_a_loc.addAll(x_a_accs);
                                t_a_local = t_a_access;
                            }
                        }

                        if(localAccessMax > utilityAccessMax){
                            utilityAccessMax = localAccessMax;
                            S = S_local;
                            sigmoid = sigmoid_local;
                            p = p_local;
                            c_v = c_v_local;
                            n = n_local;
                            x_a = new ArrayList<>();
                            x_a.addAll(x_a_loc);
                            t_a = t_a_local;
                        }
                    }
                }
                else{
                    throw new Exception("ERROR: optimal measurement location not yet supported for maneuvering satellites");
                }
            }
            else{
                throw new Exception("ERROR: optimal measurement location determination not yet supported for this world-type.");
            }
        }

        this.utility += (S*sigmoid - g - p - c_v - n);
        this.cost += (g + p + c_v);
        this.score += S;

        this.utilityList.add(S*sigmoid - g - p - c_v - n);
        this.costList.add(g + p + c_v + n);
        this.scoreList.add(S);
        this.tz.add(t_a);
        this.x.add(x_a);
    }

    private double calcCostNoise(Subtask j){
        Task parentTask = j.getParentTask();
        double noise = (parentTask.getS_Max()/100) * parentTask.getNoise() * Math.random();
        if(Math.random() > 5.0){
            return noise;
        }
        else{
            return -noise;
        }
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

            // looks to maximize utility by getting there as quickly as correlation time will allow
            t_corr = T[j.getI_q()][ i_max ];
            t_a = maxTz - t_corr;

            if(t_a < t_quickest){ // if the agreed arrival time is less than the fastest time of arrival
                // I am the slowest in the coalition, they must adjust to my time
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
        int[][] D = j.getParentTask().getD();

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
            // check if any previous tasks are mutually exclusive with j
            ArrayList<Integer> exclusivePathSubtasks = new ArrayList<>();
            for(int i_p = i; i_p >= 0; i_p--){
                if(path.get(i_p).getParentTask() != j.getParentTask()){
                    break;
                }
                else{
                    int i_q = path.get(i_p).getI_q();
                    int i_j = j.getI_q();
                    if(D[i_j][i_q] <= - 1) {
                        exclusivePathSubtasks.add(i_p);
                    }
                }
            }

            if(exclusivePathSubtasks.size() > 0){
                // if there are mutually exclusive subtasks in bundle, chose most recent nonexclusive task as the initial position
                int i_p = exclusivePathSubtasks.get(exclusivePathSubtasks.size() - 1) - 1;
                if(i_p > 0){
                    x_0 = this.x.get(i_p);
                    t_0 = this.tz.get(i_p) + path.get(i_p).getParentTask().getDuration();
                }
                else{
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
            }
            else {
                x_0 = this.x.get(i - 1);
                t_0 = this.tz.get(i - 1) + path.get(i - 1).getParentTask().getDuration();
            }
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

    private double calcSubtaskScore(Subtask j, double t_a, SimulatedAgent agent){
        double S_max = j.getParentTask().getS_Max();
        double K = j.getK();
        double e = calcUrgency(j, t_a, agent);
        double alpha = calcAlpha(j.getK(), j.getParentTask().getI());


        return (S_max/K)*e*alpha;
    }

    private double calcUrgency(Subtask j, double t_a, SimulatedAgent agent){
        double lambda = j.getParentTask().getLambda();
        double t_start = agent.getT_0();

        if( lambda == 0.0 ){
            return 1.0;
        }
        else{
            if(t_a == Double.POSITIVE_INFINITY){
                return 0.0;
            }
            else {
                return exp(-lambda * (t_a - t_start));
            }
        }
    }

    private double calcAlpha(double K, double I){
        if((K/I) == 1.0){
            return 1.0;
        }
        else return (1.0 / 3.0);
    }

    private double calcSigmoid(Subtask j, ArrayList<Double> x_a){
        double delta_x = 0.0;
        ArrayList<Double> x_i;
        double gamma = j.getParentTask().getGamma();
        double e;

        if( gamma == Double.NEGATIVE_INFINITY ) {
            return 1;
        }
        else {
            x_i = j.getParentTask().getLocation();

            for(int i_x = 0; i_x < x_a.size(); i_x++){
                delta_x += pow( x_a.get(i_x) - x_i.get(i_x), 2);
            }

            double distance =  sqrt(delta_x);
            e = exp(gamma * distance);
        }

        return 1.0/( 1.0 + e);
    }

    private double calcSigmoid(Subtask j, SimulatedAgent agent, PVCoordinates pv_a_local, AbsoluteDate date) throws Exception {
        double gamma = j.getParentTask().getGamma();

        if( gamma == Double.NEGATIVE_INFINITY ) {
            return 1;
        }
        else {
            PVCoordinates taskPV = agent.getLocalResults().getIterationDatum(j).getTaskOrbitData(date);
            Vector3D agentLocation = pv_a_local.getPosition();
            Vector3D taskLocation = taskPV.getPosition();

            double distance = agentLocation.distance(taskLocation);
            double e = exp(gamma * distance);
            return 1.0/( 1.0 + e);
        }
    }

    private double calcTravelCost(ArrayList<Subtask> path, Subtask j, ArrayList<Double> x_a, SimulatedAgent agent){
        int i = path.indexOf(j);
        double delta_x = 0.0;
        ArrayList<Double> x_i;
        int[][] D = j.getParentTask().getD();

        if(i == 0){ // task is at the beginning of the path
            if(agent.getOverallPath().size() > 0){ // there exists a path before new path
                x_i = agent.getOverallX_path().get( agent.getOverallX_path().size() - 1 ); // last location in previous path
            }
            else{ // there was no previous path
                x_i = agent.getPosition();
            }
        } else{ // there is a task before the current task
            // check if any previous tasks are mutually exclusive with j
            ArrayList<Integer> exclusivePathSubtasks = new ArrayList<>();
            for(int i_p = i; i_p >= 0; i_p--){
                if(path.get(i_p).getParentTask() != j.getParentTask()){
                    break;
                }
                else{
                    int i_q = path.get(i_p).getI_q();
                    int i_j = j.getI_q();
                    if(D[i_j][i_q] <= - 1) {
                        exclusivePathSubtasks.add(i_p);
                    }
                }
            }

            if(exclusivePathSubtasks.size() > 0){
                // if there are mutually exclusive subtasks in bundle, chose most recent nonexclusive task as the initial position
                int i_p = exclusivePathSubtasks.get(exclusivePathSubtasks.size() - 1) - 1;
                if(i_p > 0){
                    x_i = this.x.get(i_p);
                }
                else{
                    if(agent.getOverallPath().size() > 0){ // there exists a path before new path
                        x_i = agent.getOverallX_path().get( agent.getOverallX_path().size() - 1 ); // last location in previous path
                    }
                    else{ // there was no previous path
                        x_i = agent.getPosition();
                    }
                }
            }
            else {
                x_i = this.x.get(i - 1);
            }
        }

        for(int i_x = 0; i_x < x_a.size(); i_x++){
            delta_x += pow( (x_a.get(i_x) - x_i.get(i_x)), 2);
        }

        double distance =  sqrt(delta_x);

        return distance * agent.getInitialResources().getMiu();
    }

    private double calcMergePenalty(ArrayList<Subtask> path, Subtask j, SimulatedAgent agent, ArrayList<ArrayList<SimulatedAgent>> pathOmega){
        int i = path.indexOf(j);

        double C_split;
        double C_merge;

        ArrayList<Subtask> overallBundle = agent.getOverallBundle();
        ArrayList<Subtask> overallPath = agent.getOverallPath();
        ArrayList<ArrayList<SimulatedAgent>> overallOmega = agent.getOverallOmega();

        if( i == 0 ){ // j is at beginning of new path
            if( overallOmega.size() <= 0) { // no previous coalitions, no split cost
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

    private double calcSubtaskCost(Subtask j, SimulatedAgent agent) throws Exception {

        if(j.getParentTask().getCost_type().equals("Const")){
            return j.getParentTask().getCost();
        }
        else if(j.getParentTask().getCost_type().equals("Proportional")){
            return j.getParentTask().getCost() * (agent.getInitialResources().getValue());
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
    public ArrayList<Double> getUtilityList() { return this.utilityList; }
    public ArrayList<Double> getScoreList() { return this.scoreList; }
    public ArrayList<Double> getCostList() { return this.costList; }

    public void setUtility(double utility){ this.utility = utility; }
    public void setCost(double cost){ this.cost = cost; }
    public void setScore(double score){ this.score = score; }

}
