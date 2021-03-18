package modules.orbitData;

import modules.utils.Statistics;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Describes the coverage statistics of a given scenario
 */
public class CoverageStats {
    private final double maxRevTime;
    private final double minRevTime;
    private final double avgRevTime;
    private final double stdRevTime;
    private final double cvgPtg;

    private HashMap<TopocentricFrame, ArrayList<GPAccess>> orderedAccesses;
    private ArrayList<Double> revTimes;

    public CoverageStats( OrbitData orbitData ){
        orderedAccesses = orbitData.orderGPAccesses();
        revTimes = calcRevTimes(orbitData.getStartDate());
        maxRevTime = calcMaxRevTime();
        minRevTime = calcMinRevTime();
        avgRevTime = calcAvgRevTime();
        stdRevTime = calcStdRevTime();
        cvgPtg = calcCoveragePercentage();
    }

    private ArrayList<Double> calcRevTimes(AbsoluteDate startDate){
        ArrayList<Double> revTimes = new ArrayList<>();

        for(TopocentricFrame point : orderedAccesses.keySet()){
            for(int i = 0; i < orderedAccesses.get(point).size()-1; i++){

                GPAccess acc_i = orderedAccesses.get(point).get(i);
                GPAccess acc_ip = orderedAccesses.get(point).get(i+1);
                AbsoluteDate acc_iEnd = acc_i.getEndDate();
                AbsoluteDate acc_ipStart = acc_ip.getStartDate();

                double duration = acc_ipStart.durationFrom(acc_iEnd);

                revTimes.add(duration);
            }
        }

        return revTimes;
    }

    private double calcMaxRevTime(){
        return Statistics.getMax(revTimes);
    }

    private double calcMinRevTime(){
        return Statistics.getMin(revTimes);
    }

    private double calcAvgRevTime(){
        return Statistics.getMean(revTimes);
    }

    private double calcStdRevTime(){
        return Statistics.getStd(revTimes);
    }

    private double calcCoveragePercentage(){
        double n_acc = 0.0;
        double n_total = orderedAccesses.keySet().size();

        for(TopocentricFrame point : orderedAccesses.keySet()){
            if(orderedAccesses.get(point).size() > 0) n_acc++;
        }

        return n_acc/n_total;
    }

    public double getMaxRevTime() { return maxRevTime; }
    public double getMinRevTime() { return minRevTime; }
    public double getAvgRevTime() { return avgRevTime; }
    public double getStdRevTime() { return stdRevTime; }
    public double getCoveragePercentage(){ return cvgPtg; }
}
