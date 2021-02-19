package modules.measurements;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

public class Measurement {
    private AbsoluteDate availableDate = null;
    private AbsoluteDate measurementDate = null;
    private double utility = -1.0;
    private double res = -1.0;
    private double acc = -1.0;

    public Measurement(){
        // TODO create proper measurement metrics
    }

    public void randomize() throws OrekitException {
        TimeScale utc = TimeScalesFactory.getUTC();
        availableDate = new AbsoluteDate(2021, 1, 1, 0, 0, 0, utc);
        double duration = 24*3600 * Math.random();
        measurementDate = availableDate.shiftedBy(duration);
        utility = 100 * Math.random();
        res = 1000 * Math.random();
        acc = Math.random();
    }


    public AbsoluteDate getAvailableDate() {
        return availableDate;
    }

    public AbsoluteDate getMeasurementDate() {
        return measurementDate;
    }

    public double getUtility(){
        return  utility;
    }

    public double getRes() {
        return res;
    }

    public double getAcc() {
        return acc;
    }
}
