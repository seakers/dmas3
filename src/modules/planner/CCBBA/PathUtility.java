package modules.planner.CCBBA;

import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.maneuvers.Maneuver;
import modules.spacecraft.orbits.TimeInterval;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;

public class PathUtility {
    public double utility;                      // total path utility
    public double cost;                         // total path cost
    public double score;                        // total path score
    private ArrayList<Double> utilityList;      // list of utilities
    private ArrayList<Double> costList;         // list of costs
    private ArrayList<Double> scoreList;        // list of scores
    private ArrayList<AbsoluteDate> tz;               // list of arrival times
    private ArrayList<Maneuver> maneuvers;      // list of maneuvers done to achieve respective subtask

    public PathUtility(Spacecraft parentSpacecraft, CCBBAPlanner planner, ArrayList<Subtask> path) throws Exception {
        utilityList = new ArrayList<>();
        costList  = new ArrayList<>();
        scoreList = new ArrayList<>();
        tz = new ArrayList<>();
        maneuvers = new ArrayList<>();

        // calculate the utility of each subtask
        for(Subtask j : path){
            // Get accesses time intervals to subtask j
            ArrayList<TimeInterval> lineOfSightTimes = parentSpacecraft.getLineOfSightTimeS(j);
            ArrayList<Instrument> payload = parentSpacecraft.getDesign().getPayload();
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
                    IterationDatum datum = planner.getIterationDatum(j);
                    if( datum.getTz_date().compareTo(endDate) > 0){
                        // if the start time of the previous measurement is later than the end of this time interval,
                        // skip to the next interval
                        continue;
                    }
                    else if( datum.getTz_date().compareTo(startDate) > 0){
                        // if the start date of the previous measurement is after the start date, then skip all previous
                        // dates in the time interval;
                        stepDate = datum.getTz_date();
                    }
                    else{
                        stepDate = startDate.getDate();
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
                    ArrayList<Boolean> instrumentFovAT = new ArrayList<>(); // i is true if task is in instrument[i]'s fov across track
                    ArrayList<Boolean> instrumentFovCT = new ArrayList<>(); // i is true if task is in instrument[i]'s fov across track
                    Vector3D satPos = parentSpacecraft.getPV(stepDate).getPosition();
                    Vector3D satVel = parentSpacecraft.getPV(stepDate).getVelocity();
                    Vector3D taskPos = j.getParentTask().getPVEarth(stepDate).getPosition();

                    for(Instrument ins : payload){
                        if(ins.getScanningType().equals("side")){
                            throw new Exception("Sensor scanning type not yet supported");
                        }
                        // calculate if instrument[i] can see task j in the accross track direction
                        instrumentFovAT.add( parentSpacecraft.isInFovAT(ins, satPos, satVel, taskPos) );
                        instrumentFovCT.add( parentSpacecraft.isInFovCT(ins, satPos, satVel, taskPos) );
                    }

                    if(instrumentFovAT.contains(true)){
                        if(instrumentFovCT.contains(true)){
                            // no maneuver needed to observe subtask j
                            
                        }
                        else{
                            // maneuver needed to observe subtask j
                        }
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
