package modules.planner.CCBBA;

import madkit.kernel.AbstractAgent;
import modules.environment.Dependencies;
import modules.environment.Requirements;
import modules.environment.Subtask;
import modules.environment.Task;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.measurements.MeasurementPerformance;
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
    private ArrayList<Subtask> path;
    private ArrayList<MeasurementPerformance> performanceList;

    public PathUtility(Spacecraft parentSpacecraft, CCBBAPlanner planner, ArrayList<Subtask> path) throws Exception {
        utilityList = new ArrayList<>();
        costList  = new ArrayList<>();
        scoreList = new ArrayList<>();
        tz = new ArrayList<>();
        maneuvers = new ArrayList<>();
        pathOmega = calcCoalitionMatrix(parentSpacecraft,planner,path);
        instrumentsUsed = new ArrayList<>();
        this.path = new ArrayList<>(path);
        performanceList = new ArrayList<>();

        // calculate the utility of each subtask
        for(Subtask j : path){
//            System.out.println("Subtask: " + j.toString());

            // Get accesses time intervals to subtask j
            ArrayList<TimeInterval> lineOfSightTimes = parentSpacecraft.getLineOfSightTimeS(j);
            int i_path = path.indexOf(j);

            // Get the initial body frame prior to subtask j
            int path_i = path.indexOf(j);
            ArrayList<Vector3D> bodyFrame;
            if(path_i == 0){
                // if first in path, get the spacecraft's latest body frame
                bodyFrame = parentSpacecraft.getBodyFrame();
            }
            else{
                // if subtasks in path exist, then use the status of the maneuvers of the previous task as starting points
                AttitudeManeuver lastManeuver = ((AttitudeManeuver) (maneuvers.get(path_i-1)));
                bodyFrame = lastManeuver.getFinalBodyFrame();
            }

            double S_max= 0.0;
            double sig_max = 0.0;
            double maneuverCost_max = 0.0;
            double coalPenalties_max = 0.0;
            double measurementCost_max = 0.0;
            AbsoluteDate ta_max = new AbsoluteDate();
            Maneuver maneuver_max = new NoManeuver(bodyFrame,new AbsoluteDate());
            double utility_max = 0.0;
            ArrayList<Instrument> sensors_max = new ArrayList<>();
            MeasurementPerformance performance_max = new MeasurementPerformance(j);

            // calc time in which having the lowest power sensor on for a measurement would cost more than decayed utility
            AbsoluteDate t_limit = calcTlimit(j,parentSpacecraft);

            // get the maximum utility from all line of sight time intervals
            for(TimeInterval interval : lineOfSightTimes){
                // get date information from access and environment
                AbsoluteDate startDate = interval.getAccessStart();
                AbsoluteDate endDate = interval.getAccessEnd();
                double timeStep = planner.getTimeStep();
                AbsoluteDate stepDate;

//                System.out.println("Interval: " + startDate.toString() + " -> " + endDate.toString());

                // check if time interval has already passed the simulation time
                AbsoluteDate simTcurr = parentSpacecraft.getCurrentDate();
                if(simTcurr.compareTo(endDate) > 0){
                    // if current simulation time is past the interval, then skip to next interval
                    continue;
                }
                else if(simTcurr.compareTo(startDate) > 0){
                    // if current simulation time is within the access interval, chane start time to current sim time
                    startDate = simTcurr.getDate();
                }

                // check if interval happens before or after the lifespan of the task
                AbsoluteDate taskStartTime = j.getParentTask().getRequirements().getStartDate();
                AbsoluteDate taskEndTime = j.getParentTask().getRequirements().getEndDate();
                if(endDate.compareTo(taskStartTime) < 0){
                    continue;
                }
                else if(startDate.compareTo(taskEndTime) > 0){
                    continue;
                }

                // check if interval happens at or after last measurement
                if(i_path == 0){
                    stepDate = startDate.getDate();
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
                        stepDate = tz.get(i_path-1).shiftedBy(planner.getTimeStep());
                    }
                    else {
                        stepDate = startDate.getDate();
                    }
                }
//                if(stepDate.compareTo(t_limit) > 0){
//                    continue;
//                }
//                if(endDate.compareTo(t_limit) > 0){
//                    endDate = t_limit.getDate();
//                }

                // Initialize local search for max utility
                Utility uInterval = new Utility();
                // perform linear search
                while(stepDate.compareTo(endDate) <= 0) {
                    Utility uDate = new Utility(); uDate.calcUtility(j, path, stepDate, bodyFrame, parentSpacecraft, planner);

                    if(uDate.utility > uInterval.utility) {
                        uInterval = uDate.copy();
                    }
                    stepDate = stepDate.shiftedBy(timeStep).getDate();
                }

                if(uInterval.utility > utility_max){
                    S_max= uInterval.utility;
                    sig_max = uInterval.sig;
                    maneuverCost_max = uInterval.maneuverCost;
                    coalPenalties_max = uInterval.coalPenalties;
                    measurementCost_max = uInterval.measurementCost;
                    ta_max = uInterval.ta.getDate();
                    maneuver_max = uInterval.maneuver;
                    utility_max = uInterval.utility;
                    sensors_max = uInterval.sensors;
                    performance_max = uInterval.performance;
                }
            }

            double S = S_max;
            double sig = sig_max;
            double manuverCost = maneuverCost_max;
            double coalPenalties = coalPenalties_max;
            double measurementCost = measurementCost_max;
            AbsoluteDate ta = ta_max.getDate();
            Maneuver maneuver = maneuver_max;
            ArrayList<Instrument> sensors = sensors_max;
            MeasurementPerformance performance = performance_max;

            this.utility += (S*sig - manuverCost - coalPenalties - measurementCost);
            this.cost += (manuverCost + coalPenalties + measurementCost);
            this.score += (S*sig);

            this.utilityList.add(S*sig - manuverCost - coalPenalties - measurementCost);
            this.costList.add(manuverCost + coalPenalties + measurementCost);
            this.scoreList.add(S*sig);
            this.tz.add(ta);
            this.maneuvers.add(maneuver);
            this.instrumentsUsed.add(sensors);
            this.performanceList.add(performance);
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
                if( (j.getParentTask() == q.getParentTask()) && (tempWinner != parentSpacecraft)
                        && (tempWinner != null) && (dep.depends(j,q)) ){
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
                    tempTz = planner.getIterationDatum( constraint ).getTz();
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

            boolean meetsTmax = (date.compareTo(maxLow) >= 0) && (date.compareTo(maxUp) <= 0);
            boolean meetsTminLow = (date.compareTo(minLow) <= 0);
            boolean meetsTminUp = (date.compareTo(minUp) >= 0);
            boolean happensAfterConstraint = date.compareTo(maxTz) > 0;

            if(meetsTmax && (meetsTminLow || meetsTminUp)) {
                return true;
            }
            else if(meetsTmax && happensAfterConstraint) {
                return true;
            }
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
            maneuverStartTime = spacecraft.getCurrentDate();
        }
        else{
            // if subtasks in path exist, then use the status of the maneuvers of the previous task as starting points
            AttitudeManeuver lastManeuver = ((AttitudeManeuver) (maneuvers.get(path_i-1)));
            bodyFrame = lastManeuver.getFinalBodyFrame();
            maneuverStartTime = lastManeuver.getEndDate().shiftedBy(planner.getTimeStep());
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
                maneuverListTemp.add( new NoManeuver(bodyFrame, maneuverStartTime, date) );
                visibilityMatrix.add(visibilityTemp);

                if(visibilityMatrix.get(0).size() == payload.size()){
                    // if all instruments can see subtask j without maneuver, then finish iterations
                    for(Instrument ins : payload){
                        visibilityTemp = new ArrayList<>();
                        visibilityMatrix.add(visibilityTemp);
                        maneuverListTemp.add( new NoManeuver( bodyFrame, maneuverStartTime, date) );
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

    private AbsoluteDate calcTlimit(Subtask j, Spacecraft spacecraft) throws Exception {
        double S_max = j.getParentTask().getMaxScore();
        double K = j.getLevelOfPartiality();
        double I = j.getParentTask().getMeasurements().size();
        double alpha = calcAlpha(K,I);

        double S = (S_max/K)*alpha;

        ArrayList<Instrument> payload = spacecraft.getDesign().getPayload();
        double costMin = 1e5;
        for(Instrument ins : payload){
            double costIns = calcMeasurementCost(ins);
            if(costIns < costMin) costMin = costIns;
        }

        AbsoluteDate t_0 = j.getParentTask().getRequirements().getStartDate();
        double lambda = j.getParentTask().getRequirements().getUrgencyFactor();
        if( lambda == 0.0 ){
            return j.getParentTask().getRequirements().getEndDate();
        }
        else{
            return t_0.shiftedBy( (1.0/lambda) * Math.log(S/costMin) );
        }
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
        else if(t_a.compareTo(t_start) < 0){
            // selected time is before the start time of the task
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

    private double calcRequirementSatisfaction(Subtask j, MeasurementPerformance performance) throws Exception {
        // Calc measurement performance
        double spatialRes = performance.getSpatialResAz();
        double snr = performance.getSNR();

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
        double sigmoid_spat;
        if(e_spat == Double.POSITIVE_INFINITY){
            sigmoid_spat = 0.0;
        }
        else if(e_spat == Double.NEGATIVE_INFINITY){
            sigmoid_spat = spatialResReqWeight;
        }
        else{
            sigmoid_spat = (spatialResReqWeight/( 1.0 + e_spat));
        }

        double delta_snr = snrReq - snr;
        double e_snr = exp(snrReqSlope * delta_snr);
        double sigmoid_snr;
        if(e_snr == Double.POSITIVE_INFINITY){
            sigmoid_snr = 0.0;
        }
        else if(e_snr == Double.NEGATIVE_INFINITY){
            sigmoid_snr = snrReqWeight;
        }
        else{
            sigmoid_snr = (snrReqWeight/( 1.0 + e_snr));
        }

        return (sigmoid_spat + sigmoid_snr);
    }

    private double calcManeuverCost(Maneuver maneuver, Spacecraft spacecraft) throws Exception {
        double specificTorque = maneuver.getSpecificTorque();
        double I = spacecraft.getDesign().getMassProperties().getIx();
        double M = I*specificTorque;
        double powerReq = spacecraft.getDesign().getAdcs().getPower();
        double scale = 1.0;

        return M*powerReq*scale;
    }

    private double calcMeasurementCost(ArrayList<Instrument> instruments, double duration){
        double v = 1.5e-1;
        double totalAveragePower = 0.0;

        for(Instrument ins : instruments){
            totalAveragePower += ins.getPavg();
        }
        return totalAveragePower*v;
    }

    private double calcMeasurementCost(Instrument instrument){
        double v = 1e-1;
        double totalAveragePower = 0.0;

        totalAveragePower += instrument.getPavg();
        return totalAveragePower*v;
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
                       ArrayList<Double> scoreList, ArrayList<AbsoluteDate> tz, ArrayList<Maneuver> maneuvers,ArrayList<ArrayList<Instrument>> instrumentsUsed,
                        ArrayList<Subtask> path, ArrayList<MeasurementPerformance> performanceList){
        this.utility = utility;
        this.cost = cost;
        this.score = score;
        this.utilityList = new ArrayList<>(utilityList);
        this.costList = new ArrayList<>(costList);
        this.scoreList = new ArrayList<>(scoreList);
        this.tz = new ArrayList<>(tz);
        this.maneuvers = new ArrayList<>(maneuvers);
        this.instrumentsUsed = new ArrayList<>(instrumentsUsed);
        this.path = new ArrayList<>(path);
        this.performanceList = new ArrayList<>(performanceList);
    }

    public PathUtility copy(){
        return new PathUtility(utility, cost, score, utilityList, costList, scoreList, tz, maneuvers, instrumentsUsed, path, performanceList);
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
    public ArrayList<Subtask> getPath(){return path;}
    public ArrayList<MeasurementPerformance> getPerformanceList(){return performanceList;}
    public ArrayList<ArrayList<Instrument>> getInstrumentsUsed(){return instrumentsUsed;}

    private class Utility{
        double S = 0.0;
        double sig = 0.0;
        double maneuverCost = 0.0;
        double coalPenalties= 0.0;
        double measurementCost = 0.0;
        AbsoluteDate ta = new AbsoluteDate();
        Maneuver maneuver = null;
        double utility = 0.0;
        ArrayList<Instrument> sensors = new ArrayList<>();
        MeasurementPerformance performance = null;

        public Utility(){

        }

        private Utility(double s, double sig, double maneuverCost, double coalPenalties, double measurementCost, AbsoluteDate ta, Maneuver maneuver, double utility, ArrayList<Instrument> sensors, MeasurementPerformance performance) {
            S = s;
            this.sig = sig;
            this.maneuverCost = maneuverCost;
            this.coalPenalties = coalPenalties;
            this.measurementCost = measurementCost;
            this.ta = ta.getDate();
            this.maneuver = maneuver;
            this.utility = utility;
            this.sensors = new ArrayList<>(sensors);
            if(performance == null) this.performance = null;
            else this.performance = performance.copy();
        }

        public Utility copy(){
            return new Utility(S, sig, maneuverCost, coalPenalties, measurementCost, ta, maneuver, utility, sensors, performance);
        }

        public void calcUtility(Subtask j, ArrayList<Subtask> path, AbsoluteDate stepDate, ArrayList<Vector3D> bodyFrame, Spacecraft parentSpacecraft, CCBBAPlanner planner) throws Exception {
                // check time if constraints exist
                ArrayList<Subtask> timeConstraints = getTimeConstraints(j, path, planner);
                boolean dateFulfillsConstraints = meetsTimeConstraints(j,path,stepDate,timeConstraints,planner);

                double S_date = 0.0;
                double sig_date = 0.0;
                double maneuverCost_date = 0.0;
                double coalPenalties_date = 0.0;
                double measurementCost_date = 0.0;
                this.ta = stepDate.getDate();
                Maneuver maneuver_date = new NoManeuver(bodyFrame, stepDate.getDate());
                double utility_date = 0.0;
                ArrayList<Instrument> sensors_date = new ArrayList<>();
                MeasurementPerformance performance_date = new MeasurementPerformance(j);

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
                        ArrayList<Instrument> sensors_maneuver = new ArrayList<>();
                        MeasurementPerformance performance_maneuver = new MeasurementPerformance(j);

                        ArrayList<ArrayList<Instrument>> instrumentCombinations = calcInstrumentCombinations(visibleToSensorsList);
                        for(ArrayList<Instrument> sensorsUsed : instrumentCombinations){
                            if(sensorsUsed.size() == 0) continue;

                            MeasurementPerformance performance = new MeasurementPerformance(j,sensorsUsed,parentSpacecraft,stepDate);

                            double S_combination = calcSubtaskScore(j,stepDate,parentSpacecraft);
                            double sig_combination = calcRequirementSatisfaction(j,performance);
                            double maneuverCost_combination = calcManeuverCost(maneuverTemp,parentSpacecraft);
                            double coalPenalties_combination = 0.0;
                            double measurementCost_combination = calcMeasurementCost(sensorsUsed, planner.getTimeStep());

                            double utility_combination = S_combination*sig_combination -
                                    maneuverCost_combination - coalPenalties_combination - measurementCost_combination;

                            if(utility_combination > utility_maneuver){
                                S_maneuver = S_combination;
                                sig_maneuver = sig_combination;
                                maneuverCost_maneuver = maneuverCost_combination;
                                coalPenalties_maneuver = coalPenalties_combination;
                                measurementcost_maneuver = measurementCost_combination;
                                utility_maneuver = utility_combination;
                                sensors_maneuver = sensorsUsed;
                                performance_maneuver = performance;
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
                            sensors_date = sensors_maneuver;
                            performance_date = performance_maneuver;
                        }
                    }
                }

                if(utility_date > utility){
                    S = S_date;
                    sig = sig_date;
                    maneuverCost = maneuverCost_date;
                    coalPenalties = coalPenalties_date;
                    measurementCost = measurementCost_date;
                    maneuver = maneuver_date;
                    utility = utility_date;
                    sensors = sensors_date;
                    performance = performance_date;
                }
        }
    }
}
