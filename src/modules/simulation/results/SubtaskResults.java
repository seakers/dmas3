package modules.simulation.results;

import madkit.kernel.AbstractAgent;
import modules.environment.*;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.measurements.Measurement;
import modules.spacecraft.instrument.measurements.MeasurementCapability;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;

public class SubtaskResults {
    private Subtask j;
    private Task parentTask;
    private MeasurementCapability capability;
    private AbstractAgent winner;
    private double utility;
    private double score;
    private double cost;
    private AbsoluteDate startDate;
    private int n_req;
    private int n_sat;
    private ArrayList<Instrument> instrumentsUsed;
    private Measurement measurement;
    private ArrayList<Measurement> depMeasurements;
    private int maxDep;

    public  SubtaskResults(Subtask j, HashMap<Task, TaskCapability> capabilities){
//        this.j = j;
//        this.parentTask = j.getParentTask();
//        this.capability = capabilities.get(j.getParentTask()).getSubtaskCapability(j);
//        for(Subtask k : capability.getParentSubtasks()){
//
//        }
//
//        this.winner = datum.getZ();
//        this.utility = datum.getY();
//        this.score = datum.getScore();
//        this.cost = datum.getCost();
//        this.startDate = capability.getPerformance().getDate();
//        this.instrumentsUsed = new ArrayList<>();
//        if(capability.getInstrumentsUsed() != null){
//            this.instrumentsUsed = new ArrayList<>(capability.getInstrumentsUsed());
//        }
//        this.measurement = j.getMainMeasurement();
//        this.depMeasurements = new ArrayList<>(j.getDepMeasurements());
//
//        this.n_req = 0;
//        this.n_sat = 0;
//        Dependencies dep = parentTask.getDependencies();
//        for(Subtask q : parentTask.getSubtasks()){
//            if(dep.depends(j,q)) {
//                n_req++;
//                IterationDatum datum_q = results.getIterationDatum(q);
//                if(datum_q.getZ() != null){
//                    // dependency has winner, check if time constraints are met
//                    AbsoluteDate tz_j = datum.getTz();
//                    AbsoluteDate tz_q = datum_q.getTz();
//
//                    boolean req1 = Math.abs( tz_j.durationFrom(tz_q) ) <= dep.Tmax(j,q) && Math.abs( tz_q.durationFrom(tz_j) ) <= dep.Tmax(q,j);
//                    boolean req2 = Math.abs( tz_j.durationFrom(tz_q) ) >= dep.Tmin(j,q) && Math.abs( tz_q.durationFrom(tz_j) ) >= dep.Tmin(q,j);
//
//                    if(req1 && req2) n_sat++;
//                }
//            }
//        }
//
//        this.maxDep = 0;
//        for(Subtask q : results.getResults().keySet()){
//            int deps = q.getDepMeasurements().size();
//            if(deps > maxDep) maxDep = deps;
//        }
    }

    public String toString(){
        // TASK SUBTASK MAIN_MEASUREMENT DEP_MEASUREMENTS NSAT/NREQ WINNER START_DATE MEASURE_DATE END_DATE INS_USED RES/REQ SNR/REQ INCIDENCE AT CT UTILITY SCORE COST

        StringBuilder results = new StringBuilder();
//        try {
//            if(parentTask.getSubtasks().indexOf(j) == 0){
//                results.append("---------------------------------------------------------------------------------------------------------------------------------------\n");
//            }
//            results.append(parentTask.toString() + "\t" + j.toString() + "\t" + this.measurement.toString() + "\t");
//            int count = 0;
//            for (Measurement m : depMeasurements) {
//                if (depMeasurements.indexOf(m) != 0) {
//                    results.append("," + m.toString());
//                } else {
//                    results.append(m.toString());
//                }
//                count++;
//            }
//            if(count < maxDep){
//                for(int i = 0; i < maxDep-count; i++) results.append("\t");
//            }
//            if(winner == null){
//                results.append("\n");
//            }
//            else {
//                results.append("\t" + n_sat + "/" + n_req + "\t" + ((Spacecraft) this.winner).toString() + "\t"
//                        + parentTask.getRequirements().getStartDate().toString() + "\t" + startDate.toString()
//                        + "\t" + parentTask.getRequirements().getEndDate().toString() + "\t");
//
//
//                for (Instrument ins : instrumentsUsed) {
//                    if (instrumentsUsed.indexOf(ins) != 0) {
//                        results.append("," + ins.toString());
//                    } else {
//                        results.append(ins.toString());
//                    }
//                }
//
//                MeasurementPerformance perf = capability.getPerformance();
//                Requirements req = capability.getRequirements();
//                results.append("\t" + perf.getSpatialRes() + "/" + req.getSpatialResReq() + "\t" + perf.getSNR() + "/" + req.getLossReq()
//                        + "\t" + FastMath.toDegrees(perf.getIncidence()) + "\t" + FastMath.toDegrees(perf.getAngleAT()) + "\t" + FastMath.toDegrees(perf.getAngleCT())
//                        + "\t" + utility + "\t" + score + "\t" + cost + "\n");
//            }
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }

        return results.toString();
    }
}
