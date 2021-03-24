package modules.planner.CCBBA;

import modules.agents.SatelliteAgent;
import org.orekit.time.AbsoluteDate;

public class SubtaskResult {
    /**
     * Current bid winner
     */
    private SatelliteAgent z;

    /**
     * Expected time of arrival to task
     */
    private AbsoluteDate tz;

    /**
     * Subtask being bid on
     */
    private Subtask j;

    /**
     * Self bid
     */
    private double c;

    /**
     * Winning bid
     */
    private double y;

    /**
     * Last update date
     */
    private AbsoluteDate s;
}
