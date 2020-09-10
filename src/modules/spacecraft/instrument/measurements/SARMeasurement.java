package modules.spacecraft.instrument.measurements;

import modules.environment.Subtask;
import modules.spacecraft.Spacecraft;
import modules.spacecraft.instrument.Instrument;
import modules.spacecraft.instrument.InstrumentAntenna;
import modules.spacecraft.maneuvers.Maneuver;
import org.orekit.time.AbsoluteDate;

import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class SARMeasurement extends Measurement {

    public SARMeasurement(Subtask j, Instrument ins, Spacecraft spacecraft, Maneuver maneuver, AbsoluteDate date) throws Exception {
        super(j.getMainMeasurement().getF());
        double th = calMeasurementAngle(j,ins,spacecraft,maneuver,date);
        InstrumentAntenna antenna = ins.getAnt();

        this.agent = spacecraft;
        this.B = ins.getBandwidth();
        this.dtheta = rad2deg( lambda/antenna.getDimEl() );
        this.rangeRes = c/(2.0 * B * sin( th ));
        this.swadth = calcSwadth(spacecraft,date);
        double nLooks = 1.0;
        this.spatialResAT = rangeRes*sqrt(nLooks);
        this.spatialResCT = rangeRes*sqrt(nLooks)/sin(th);
    }

    @Override
    public double calcSpatialResolution() {
        return this.spatialResCT;
    }

    @Override
    public double calcSNR(){
        return -1.0;
    }

}
