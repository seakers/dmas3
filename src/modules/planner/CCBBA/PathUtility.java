package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import modules.environment.Dependencies;
import modules.environment.Subtask;
import modules.environment.Task;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.maneuvers.Maneuver;
import modules.spacecraft.orbits.TimeInterval;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PathUtility {
    public double utility;                      // total path utility
    public double cost;                         // total path cost
    public double score;                        // total path score
    private ArrayList<Double> utilityList;      // list of utilities
    private ArrayList<Double> costList;         // list of costs
    private ArrayList<Double> scoreList;        // list of scores
    private ArrayList<AbsoluteDate> tz;         // list of arrival times
    private ArrayList<Maneuver> maneuvers;      // list of maneuvers done to achieve respective subtask
    private ArrayList<ArrayList<AbstractAgent>> pathOmega;

    public PathUtility(Spacecraft parentSpacecraft, CCBBAPlanner planner, ArrayList<Subtask> path) throws Exception {
        utilityList = new ArrayList<>();
        costList  = new ArrayList<>();
        scoreList = new ArrayList<>();
        tz = new ArrayList<>();
        maneuvers = new ArrayList<>();
        pathOmega = calcCoalitionMatrix(parentSpacecraft,planner,path);
        ArrayList<Instrument> payload = parentSpacecraft.getDesign().getPayload();

        // calculate the utility of each subtask
        for(Subtask j : path){
            // Get accesses time intervals to subtask j
            ArrayList<TimeInterval> lineOfSightTimes = parentSpacecraft.getLineOfSightTimeS(j);
            int i_path = path.indexOf(j);

            // get the maximum utility from all line of sight time intervals
            for(TimeInterval interval : lineOfSightTimes){
                // get date information from access and environment
                AbsoluteDate startDate = interval.getAccessStart();
                AbsoluteDate endDate = interval.getAccessEnd();
                double timeStep = planner.getTimeStep();
                AbsoluteDate stepDate;

                // check if interval happens at or after last measurement
                if(i_path == 0){
                    // if the first one in the path, check previously finished plans
                    int overall_path_size = planner.getOverallPath().size();

                    if(overall_path_size == 0){
                        // if no finished plan, start at interval start
                        stepDate = startDate.getDate();
                    }
                    else {
                        // else if there was a finished plan, check it's time of arrival to determine start date
                        Subtask j_last = planner.getOverallPath().get(overall_path_size - 1);
                        IterationDatum datum = planner.getIterationDatum(j_last);

                        if (datum.getTz_date().compareTo(endDate) > 0) {
                            // if the start time of the previous measurement is later than the end of this time interval,
                            // skip to the next interval
                            continue;
                        } else if (datum.getTz_date().compareTo(startDate) > 0) {
                            // if the start date of the previous measurement is after the start date, then skip all previous
                            // dates in the time interval;
                            stepDate = datum.getTz_date();
                        } else {
                            stepDate = startDate.getDate();
                        }
                    }
                }
                else{
                    if( tz.get(i_path-1).compareTo(endDate) > 0){
                        // if the start time of the previous measurement is later than the end of this time interval,
                        // skip to the next interval
                        continue;
                    }
                    else if( tz.get(i_path-1).compareTo(startDate) > 0){
                        // if the start date of the previous measurement is after the start date, then skip all previous
                        // dates in the time interval;
                        stepDate = tz.get(i_path-1);
                    }
                    else {
                        stepDate = startDate.getDate();
                    }
                }

                // linear search each interval for maximum utility
                while(startDate.compareTo(endDate) <= 0){
                    // check time if constraints exist
                    ArrayList<Subtask> timeConstraints = getTimeConstraints(j, path, planner);
                    boolean dateFulfillsConstraints = meetsTimeConstraints(j,path,stepDate,timeConstraints,planner);

                    // if constraints are satisfied, then calculate utility at this point in time
                    if(dateFulfillsConstraints){
                        ArrayList<Integer> visibilityList = calcVisibilityMatrix(parentSpacecraft,j,stepDate);
                    }

                    stepDate.shiftedBy(timeStep);
                }
            }

            double S = 0.0;
            double sig = 0.0;
            double manuverCost = 0.0;
            double coalPenalties = 0.0;
            double measurementCost = 0.0;
            AbsoluteDate ta = new AbsoluteDate();
            Maneuver maneuver = null;

            this.utility += (S*sig - manuverCost - coalPenalties - measurementCost);
            this.cost += (manuverCost + coalPenalties + measurementCost);
            this.score += (S*sig);

            this.utilityList.add(S*sig - manuverCost - coalPenalties - measurementCost);
            this.costList.add(manuverCost + coalPenalties + measurementCost);
            this.scoreList.add(S*sig);
            this.tz.add(ta);
            this.maneuvers.add(maneuver);
        }
    }

    private ArrayList<ArrayList<AbstractAgent>> calcCoalitionMatrix(Spacecraft parentSpacecraft, CCBBAPlanner planner, ArrayList<Subtask> path){
        ArrayList<ArrayList<AbstractAgent>> coalMatrix = new ArrayList<>();

        for(Subtask j : path){
            ArrayList<AbstractAgent> tempCoal = new ArrayList<>();
            Set<Subtask> keys = planner.getIterationResults().getResults().keySet();
            Dependencies dep = j.getParentTask().getDependencies();

            for(Subtask q : keys){
                AbstractAgent tempWinner = planner.getIterationDatum(q).getZ();
                if( (tempWinner != parentSpacecraft) && (tempWinner != null) && (dep.depends(j,q)) ){
                    tempCoal.add(tempWinner);
                }
            }

            coalMatrix.add(tempCoal);
        }

        return coalMatrix;
    }

    private ArrayList<Subtask> getTimeConstraints(Subtask j, ArrayList<Subtask> path, CCBBAPlanner planner) throws Exception {
        ArrayList<Subtask> timeConstraints = new ArrayList<>();
        Dependencies dep = j.getParentTask().getDependencies();

        for(Subtask q : j.getParentTask().getSubtasks()){
            int i_j = path.indexOf(j);
            int i_q = path.indexOf(q);

            boolean isDependent = dep.depends(j,q);
            boolean qHaswinner = planner.getIterationDatum(q).getZ() != null;
            boolean isInPath = path.contains(q) && (j != q);
            boolean isBehindPath = i_q < i_j;

            if( isDependent && ( qHaswinner || (isInPath && isBehindPath) )) timeConstraints.add(q);
        }

        return timeConstraints;
    }

    private boolean meetsTimeConstraints(Subtask j, ArrayList<Subtask> path, AbsoluteDate date, ArrayList<Subtask> timeConstraints, CCBBAPlanner planner){
        if(timeConstraints.size() == 0) return true;
        else{
            // from the constraints, get the task with the latest arrival time
            Dependencies dep = j.getParentTask().getDependencies();

            AbsoluteDate maxTz = null;
            Subtask maxSubtask = null;
            for(Subtask constraint : timeConstraints){
                AbsoluteDate tempTz;
                boolean pathContainsTimeConstraint = path.contains( constraint );
                boolean isBehindInPath = path.indexOf(j) > path.indexOf( constraint );

                if( pathContainsTimeConstraint && isBehindInPath ){
                    tempTz = this.tz.get( path.indexOf( constraint ) );
                }
                else{
                    tempTz = planner.getIterationDatum( constraint ).getTz_date();
                }

                if(maxTz == null) {
                    maxTz = tempTz;
                    maxSubtask = constraint;
                }
                else if(tempTz.compareTo(maxTz) > -1) {
                    maxTz = tempTz;
                    maxSubtask = constraint;
                }
            }

            double t_corr_max = dep.Tmax(j,maxSubtask);
            double t_corr_min = dep.Tmin(j,maxSubtask);
            AbsoluteDate maxUp  = maxTz.shiftedBy(t_corr_max);
            AbsoluteDate maxLow = maxTz.shiftedBy(-t_corr_max);
            AbsoluteDate minUp  = maxTz.shiftedBy(t_corr_min);
            AbsoluteDate minLow = maxTz.shiftedBy(-t_corr_min);

            boolean meetsTmax = (date.compareTo(maxLow) <= 0) && (date.compareTo(maxUp) >= 0);
            boolean meetsTminLow = (date.compareTo(minLow) >= 0);
            boolean meetsTminUp = (date.compareTo(minUp) <= 0);
            boolean happensAfterConstraint = date.compareTo(maxTz) < 0;

            if(meetsTmax && (meetsTminLow || meetsTminUp)) return true;
            else if(meetsTmax && happensAfterConstraint) return true;
            return false;
        }
    }

    private ArrayList<ArrayList<Integer>> calcVisibilityMatrix(Subtask j, ArrayList<Subtask> path, Spacecraft spacecraft, AbsoluteDate date) throws OrekitException {
        ArrayList<ArrayList<Integer>> visibilityMatrix = new ArrayList<>();
        ArrayList<Instrument> payload = spacecraft.getDesign().getPayload();

        for(int i = 0; i <= payload.size(); i++){
            ArrayList<Integer> visibilityTemp = new ArrayList<>();
            if(i == 0){
                // check for visibility without maneuvers
                for(Instrument ins : payload){
                    boolean visibleToInstrument = false;
                    Vector3D taskPos = j.getParentTask().getPVEarth(date).getPosition();
                    int k = payload.indexOf(ins);
                    int path_i = path.indexOf(j);
                    ArrayList<Vector3D> bodyFrame;
                    if(path_i == 0){
                        bodyFrame = spacecraft.getBodyFrame();
                    }
                    else{

                    }

                    visibleToInstrument = spacecraft.isVisible(ins, bodyFrame, date, taskPos);
                    if(visibleToInstrument) visibilityTemp.add(k);
                }
            }
            else{
                if(!visibilityMatrix.get(0).contains(1)){
                    // if all instruments can see subtask j without maneuver, then finish iterations
                    for(Instrument ins : payload){
                        visibilityTemp = new ArrayList<>();
                        visibilityMatrix.add(visibilityTemp);
                    }
                    break;
                }

                // calculate maneuver necessary for instrument i-1 to be able to see subtask j
                Instrument ins_i = payload.get(i-1);

                // check visibility for all sensors after maneuver
                for(Instrument ins : payload){

                }
            }
            visibilityMatrix.add(visibilityTemp);
        }

        return visibilityMatrix;
    }

    /**
     * Copy Constructors
     * @param utility
     * @param cost
     * @param score
     * @param utilityList
     * @param costList
     * @param scoreList
     * @param tz
     * @param maneuvers
     */
    private PathUtility(double utility, double cost, double score, ArrayList<Double> utilityList, ArrayList<Double> costList,
                       ArrayList<Double> scoreList, ArrayList<AbsoluteDate> tz, ArrayList<Maneuver> maneuvers){
        this.utility = utility;
        this.cost = cost;
        this.score = score;
        this.utilityList = new ArrayList<>(utilityList);
        this.costList = new ArrayList<>(costList);
        this.scoreList = new ArrayList<>(scoreList);
        this.tz = new ArrayList<>(tz);
        this.maneuvers = new ArrayList<>(maneuvers);
    }

    public PathUtility copy(){
        return new PathUtility(utility, cost, score, utilityList, costList, scoreList, tz, maneuvers);
    }

    /**
     * Getters
     */
    public double getPathUtility(){return this.utility; }
    public double getPathCost() { return cost; }
    public double getPathScore() { return score; }
    public ArrayList<Double> getUtilityList() { return utilityList;    }
    public ArrayList<Double> getCostList() { return costList; }
    public ArrayList<Double> getScoreList() { return scoreList; }
    public ArrayList<AbsoluteDate> getTz() { return tz; }
    public ArrayList<Maneuver> getManeuvers(){return maneuvers;}
}
