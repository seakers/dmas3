package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import modules.environment.Dependencies;
import modules.environment.Requirements;
import modules.environment.Subtask;
import modules.environment.Task;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.maneuvers.AttitudeManeuver;
import modules.spacecraft.maneuvers.Maneuver;
import modules.spacecraft.maneuvers.NoManeuver;
import modules.spacecraft.maneuvers.SlewingManeuver;
import modules.spacecraft.orbits.SpacecraftOrbit;
import modules.spacecraft.orbits.TimeInterval;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.Math.exp;

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
    private ArrayList<ArrayList<Instrument>> instrumentsUsed;

    public PathUtility(Spacecraft parentSpacecraft, CCBBAPlanner planner, ArrayList<Subtask> path) throws Exception {
        utilityList = new ArrayList<>();
        costList  = new ArrayList<>();
        scoreList = new ArrayList<>();
        tz = new ArrayList<>();
        maneuvers = new ArrayList<>();
        pathOmega = calcCoalitionMatrix(parentSpacecraft,planner,path);
        instrumentsUsed = new ArrayList<>();
        ArrayList<Instrument> payload = parentSpacecraft.getDesign().getPayload();

        // calculate the utility of each subtask
        for(Subtask j : path){
            // Get accesses time intervals to subtask j
            ArrayList<TimeInterval> lineOfSightTimes = parentSpacecraft.getLineOfSightTimeS(j);
            int i_path = path.indexOf(j);

            double S_max= 0.0;
            double sig_max = 0.0;
            double maneuverCost_max = 0.0;
            double coalPenalties_max = 0.0;
            double measurementCost_max = 0.0;
            AbsoluteDate ta_max = new AbsoluteDate();
            Maneuver maneuver_max = new NoManeuver(new AbsoluteDate());
            double utility_max = 0.0;

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
                // Initialize local search for max utility
                double S_interval = 0.0;
                double sig_interval = 0.0;
                double maneuverCost_interval = 0.0;
                double coalPenalties_interval = 0.0;
                double measurementCost_interval = 0.0;
                AbsoluteDate ta_interval = new AbsoluteDate();
                Maneuver maneuver_interval = new NoManeuver(ta_interval);
                double utility_interval = 0.0;


                // linear search each interval for maximum utility
                while(stepDate.compareTo(endDate) <= 0){
                    // check time if constraints exist
                    ArrayList<Subtask> timeConstraints = getTimeConstraints(j, path, planner);
                    boolean dateFulfillsConstraints = meetsTimeConstraints(j,path,stepDate,timeConstraints,planner);

                    double S_date = 0.0;
                    double sig_date = 0.0;
                    double maneuverCost_date = 0.0;
                    double coalPenalties_date = 0.0;
                    double measurementCost_date = 0.0;
                    AbsoluteDate ta_date = stepDate.getDate();
                    Maneuver maneuver_date = new NoManeuver(stepDate.getDate());
                    double utility_date = 0.0;

                    // if constraints are satisfied, then calculate utility at this point in time
                    if(dateFulfillsConstraints){
                        // Maneuver list contains the list of maneuvers done allow for all sensors to see subtask j
                        ArrayList<Maneuver> maneuverList = new ArrayList<>();

                        // visibility list contains list of sensors that can see subtask j with no maneuvers or if
                        // maneuvers are required to see it
                        ArrayList<ArrayList<Instrument>> visibilityList = calcVisibilityMatrix(j,path,parentSpacecraft,planner,stepDate,maneuverList);

                        for(ArrayList<Instrument> visibleToSensorsList : visibilityList){
                            int i = visibilityList.indexOf(visibleToSensorsList);
                            Maneuver maneuverTemp = maneuverList.get(i);

                            double S_maneuver = 0.0;
                            double sig_maneuver = 0.0;
                            double maneuverCost_maneuver = 0.0;
                            double coalPenalties_maneuver = 0.0;
                            double measurementcost_maneuver = 0.0;
                            double utility_maneuver = 0.0;

                            ArrayList<ArrayList<Instrument>> instrumentCombinations = calcInstrumentCombinations(visibleToSensorsList);
                            for(ArrayList<Instrument> sensorsUsed : instrumentCombinations){
                                double S_combination = calcSubtaskScore(j,stepDate,parentSpacecraft);
                                double sig_combination = calcRequirementSatisfaction(j,sensorsUsed,parentSpacecraft,maneuverTemp,stepDate);
                                double maneuverCost_combination = calcManeuverCost(maneuverTemp,parentSpacecraft);
                                double coalPenalties_combination = 0.0;
                                double measurementCost_combination = 0.0;

                                double utility_combination = S_combination*sig_combination -
                                        maneuverCost_combination - coalPenalties_combination - measurementCost_combination;

                                if(utility_combination > utility_maneuver){
                                    S_maneuver = S_combination;
                                    sig_maneuver = sig_combination;
                                    maneuverCost_maneuver = maneuverCost_combination;
                                    coalPenalties_maneuver = coalPenalties_combination;
                                    measurementcost_maneuver = measurementCost_combination;
                                    utility_maneuver = utility_combination;
                                }
                            }

                            if(utility_maneuver > utility_date){
                                S_date = S_maneuver;
                                sig_date = sig_maneuver;
                                maneuverCost_date = maneuverCost_maneuver;
                                coalPenalties_date = coalPenalties_maneuver;
                                measurementCost_date = measurementcost_maneuver;
                                maneuver_date = maneuverTemp;
                                utility_date = utility_maneuver;
                            }
                        }
                    }

                    if(utility_date > utility_interval){
                        S_interval = S_date;
                        sig_interval = sig_date;
                        maneuverCost_interval = maneuverCost_date;
                        coalPenalties_interval = coalPenalties_date;
                        measurementCost_interval = measurementCost_date;
                        ta_interval = ta_date.getDate();
                        maneuver_interval = maneuver_date;
                        utility_interval = utility_date;
                    }

                    stepDate = stepDate.shiftedBy(timeStep).getDate();
                }

                if(utility_interval > utility_max){
                    S_max= S_interval;
                    sig_max = sig_interval;
                    maneuverCost_max = maneuverCost_interval;
                    coalPenalties_max = coalPenalties_interval;
                    measurementCost_max = measurementCost_interval;
                    ta_max = ta_interval.getDate();
                    utility_max = utility_interval;
                }
            }

            double S = S_max;
            double sig = sig_max;
            double manuverCost = maneuverCost_max;
            double coalPenalties = coalPenalties_max;
            double measurementCost = measurementCost_max;
            AbsoluteDate ta = ta_max.getDate();
            Maneuver maneuver = maneuver_max;

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

    private ArrayList<ArrayList<Instrument>> calcInstrumentCombinations(ArrayList<Instrument> visibleToSensorsList){
        ArrayList<ArrayList<Instrument>> combinations = new ArrayList<>();

        for(int i = 1; i < (int) Math.pow(2, visibleToSensorsList.size()); i++ ){
            String bitRepresentation = Integer.toBinaryString(i);
            ArrayList<Instrument> tempSet = new ArrayList<>();

            for(int j = 0; j < bitRepresentation.length(); j++){
                int delta = visibleToSensorsList.size() - bitRepresentation.length();

                if (bitRepresentation.charAt(j) == '1') {
                    tempSet.add(visibleToSensorsList.get(j + delta));
                }
            }

            combinations.add(tempSet);
        }

        return combinations;
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

    private ArrayList<ArrayList<Instrument>> calcVisibilityMatrix(Subtask j, ArrayList<Subtask> path, Spacecraft spacecraft, CCBBAPlanner planner, AbsoluteDate date, ArrayList<Maneuver> maneuverList) throws Exception {
        ArrayList<ArrayList<Instrument>> visibilityMatrix = new ArrayList<>();
        ArrayList<Maneuver> maneuverListTemp = new ArrayList<>();
        ArrayList<Instrument> payload = spacecraft.getDesign().getPayload();

        // get last body frame and maneuver start time
        int path_i = path.indexOf(j);
        ArrayList<Vector3D> bodyFrame;
        AbsoluteDate maneuverStartTime;
        if(path_i == 0){
            // if first in path, get the spacecraft's latest body frame
            bodyFrame = spacecraft.getBodyFrame();

            // if first in path, check if previous plan has been performed
            if(planner.getOverallPath().size() == 0){
                // if not, then take the start of the simulation as the start date
                maneuverStartTime = spacecraft.getStartDate();
            }
            else{
                // if a previous plan was performed, then use it's arrival time as the start of the maneuver
                int i_last_overall = planner.getOverallPath().size() -1;
                Subtask lastSubtask = planner.getOverallPath().get(i_last_overall);
                maneuverStartTime = planner.getIterationDatum(lastSubtask).getTz_date();
            }
        }
        else{
            // if subtasks in path exist, then use the status of the maneuvers of the previous task as starting points
            AttitudeManeuver lastManeuver = ((AttitudeManeuver) (maneuvers.get(path_i-1)));
            bodyFrame = lastManeuver.getFinalBodyFrame();
            maneuverStartTime = lastManeuver.getEndDate();
        }

        // check for visibility for no maneuvers and for all maneuvers
        Vector3D taskPos = j.getParentTask().getPVEarth(date).getPosition();
        for(int i = 0; i <= payload.size(); i++){
            ArrayList<Instrument> visibilityTemp = new ArrayList<>();

            if(i == 0){
                // check for visibility without maneuvers
                for(Instrument ins : payload){
                    boolean visibleToInstrument = spacecraft.isVisible(ins, bodyFrame, date, taskPos);
                    if(visibleToInstrument) visibilityTemp.add(ins);
                }
                maneuverListTemp.add( new AttitudeManeuver( bodyFrame, bodyFrame, maneuverStartTime, date) );
                visibilityMatrix.add(visibilityTemp);

                if(visibilityMatrix.get(0).size() == payload.size()){
                    // if all instruments can see subtask j without maneuver, then finish iterations
                    for(Instrument ins : payload){
                        visibilityTemp = new ArrayList<>();
                        visibilityMatrix.add(visibilityTemp);
                        maneuverListTemp.add( new AttitudeManeuver( bodyFrame, bodyFrame, maneuverStartTime, date) );
                    }
                    break;
                }
            }
            else{
                // some instruments may need maneuvers to do maneuvers to

                // calculate maneuver necessary for instrument[i-1] to be able to see subtask j
                double slew = spacecraft.calcSlewAngleReq(payload.get(i-1), bodyFrame, spacecraft.getOrbit(), date, taskPos);
                if(slew != Double.POSITIVE_INFINITY){
                    // there exist a slew maneuver than can allow for instrument[i-1] to access subtask j
                    // check visibility for all sensors after maneuver
                    for (Instrument ins : payload) {
                        Vector3D insPoint = spacecraft.getPointingWithSlew(slew, ins, spacecraft.getOrbit(), date);
                        boolean visibleToInstrument = spacecraft.isVisible(ins, insPoint, taskPos, spacecraft.getOrbit(), date);
                        if (visibleToInstrument) visibilityTemp.add(ins);
                    }
                }
                maneuverListTemp.add( new SlewingManeuver(bodyFrame, slew, maneuverStartTime, date) );
                visibilityMatrix.add(visibilityTemp);
            }

        }
        maneuverList.addAll(maneuverListTemp);
        return visibilityMatrix;
    }

    /**
     * Utility Function Calculation
     */
    private double calcSubtaskScore(Subtask j, AbsoluteDate t_a, Spacecraft parentSparecraft) throws Exception {
        double S_max = j.getParentTask().getMaxScore();
        double K = j.getLevelOfPartiality();
        double I = j.getParentTask().getMeasurements().size();
        double e = calcUrgency(j,t_a,parentSparecraft);
        double alpha = calcAlpha(K,I);

        return (S_max/K)*e*alpha;
    }

    private double calcUrgency(Subtask j, AbsoluteDate t_a, Spacecraft parentSpacecraft){
        Requirements req = j.getParentTask().getRequirements();
        double lambda = req.getUrgencyFactor();
        AbsoluteDate t_start = req.getStartDate();
        AbsoluteDate t_end = req.getEndDate();

        if(t_a.compareTo(t_end) > 0){
            // selected time is past the end time of the task
            return 0.0;
        }
        else{
            if( lambda == 0.0 ){
                return 1.0;
            }
            else{
                return exp(-lambda * t_a.durationFrom(t_start));
            }
        }
    }

    private double calcAlpha(double K, double I) throws Exception{
        if(K > I) throw new Exception("level of partiality greater than measurements required. Check Task formation");

        if(K/I == 1){
            return 1.0;
        }
        else{
            return 1.0/(I+1.0);
        }
    }

    private double calcRequirementSatisfaction(Subtask j, ArrayList<Instrument> instrumenstUsed, Spacecraft spacecraft, Maneuver maneuver, AbsoluteDate date) throws Exception {
        // Calc measurement performance
        double spatialRes = spacecraft.calcSpatialRes(j, instrumenstUsed, spacecraft, maneuver, date);
        double snr = spacecraft.calcSNR(j,instrumenstUsed,date);

        // Get measurement requirements
        Requirements req = j.getParentTask().getRequirements();
        double spatialResReq = req.getSpatialResReq();
        double spatialResReqSlope = req.getSpatialResReqSlope();
        double spatialResReqWeight = req.getSpatialResReqWeight();

        double snrReq = req.getLossReq();
        double snrReqSlope = req.getLossReqSlope();
        double snrReqWeight = req.getLossReqWeight();

        double delta_spat = spatialRes - spatialResReq;
        double e_spat = exp(spatialResReqSlope * delta_spat);

        double delta_snr = snrReq - snr;
        double e_snr = exp(snrReqSlope * delta_snr);

        return (spatialResReqWeight/( 1.0 + e_spat)) + (snrReqWeight/( 1.0 + e_snr));
    }

    private double calcManeuverCost(Maneuver maneuver, Spacecraft spacecraft) throws Exception {
        double specificTorque = maneuver.getSpecificTorque();
        double I = spacecraft.getDesign().getMassProperties().getIx();
        double M = I*specificTorque;
        double powerReq = spacecraft.getDesign().getAdcs().getPower();
        double scale = 1.0;

        return M*powerReq*scale;
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
