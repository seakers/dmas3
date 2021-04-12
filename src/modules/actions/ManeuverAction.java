package modules.actions;

import madkit.kernel.AbstractAgent;
import modules.orbitData.Attitude;
import org.orekit.time.AbsoluteDate;

public class ManeuverAction extends SimulationAction{

    /**
     * Initial and final roll angles in radians
     */
    private final double initialRollAngle;
    private final double finalRollAngle;

    /**
     * Duration of the maneuver
     */
    private final double duration;

    protected ManeuverAction(AbstractAgent agent, Attitude attitude, double initialRollAngle, double finalRollAngle,
                             AbsoluteDate startDate, AbsoluteDate endDate) {
        super(agent, startDate, endDate);
        this.initialRollAngle = initialRollAngle;
        this.finalRollAngle = finalRollAngle;
        this.duration = Math.sqrt( 8.0 * Math.abs(initialRollAngle - finalRollAngle)
                                    /attitude.getPeakRollAcc() );
    }

    public double getInitialRollAngle() { return initialRollAngle; }
    public double getFinalRollAngle() { return finalRollAngle; }
    public double getDuration() { return duration; }
}
