/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import seakers.orekit.object.CoveragePoint;
import seakers.orekit.object.linkbudget.LinkBudget;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_MAX_ITER;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author paugarciabuzzi
 */
public class LBDetector extends AbstractEventDetector<LBDetector> {

    private static final long serialVersionUID = -6354518769897706495L;

    /**
     * The ground point the satellite wants to comunciate with
     */
    private final CoveragePoint pt;

    private final LinkBudget lb;

    /**
     * Constructor for the detector. Threshold for event detection is set to
     * default 1e-6 seconds
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param pt ground point the satellite wants to comunciate with
     * @param lb link budget params provider
     */
    public LBDetector(final SpacecraftState initialState,
            final AbsoluteDate startDate, final AbsoluteDate endDate, CoveragePoint pt, LinkBudget lb) {
        super(initialState, startDate, endDate);
        this.pt = pt;
        this.lb = lb;
    }

    /**
     * Constructor for the detector. Can set the resolution at which access time
     * is computed
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param maxCheck maximum checking interval (s)
     * @param threshold threshold in seconds that determines the temporal
     * @param pt ground point the satellite wants to comunciate with
     * @param lb link budget params provider
     * @param action specifies action after event is detected.
     */
    public LBDetector(final SpacecraftState initialState,
            final AbsoluteDate startDate, final AbsoluteDate endDate, CoveragePoint pt, LinkBudget lb, double maxCheck, double threshold, EventHandler.Action action) {
        super(initialState, startDate, endDate, action, maxCheck, threshold, DEFAULT_MAX_ITER);
        this.pt = pt;
        this.lb = lb;
    }

    /**
     * This g() function is positive when the link budget is closed (distance
     * smaller than dmax) and negative if the satellite cannot communicate with
     * the ground point with enough power margin.
     *
     * @param s Spacecraft state
     * @return
     * @throws OrekitException
     */
    @Override
    public double g(SpacecraftState s) throws OrekitException {
        double distance1 = s.getPVCoordinates().getPosition().distance1(pt.getPVCoordinates(s.getDate(), s.getFrame()).getPosition());
        double maxdistance = this.lb.MaxDistance();
        return maxdistance - distance1;
    }

    @Override
    protected LBDetector create(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate, EventHandler.Action action, double maxCheck, double threshold, int maxIter) {
        return new LBDetector(initialState, startDate, endDate, pt, lb, maxCheck, threshold, action);
    }

}
