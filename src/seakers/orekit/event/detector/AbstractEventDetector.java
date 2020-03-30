/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import java.util.logging.Level;
import java.util.logging.Logger;
import seakers.orekit.coverage.access.TimeIntervalArray;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_MAXCHECK;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_MAX_ITER;
import static org.orekit.propagation.events.AbstractDetector.DEFAULT_THRESHOLD;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.time.AbsoluteDate;

/**
 * This abstract class shall be extended for any event detectors used within
 * this orekit package. It contains methods for detecting events and storing the
 * event start and stop times within a TimeIntervalArray object. It is assumed
 * that positive values in the g function indicate that a time interval shall be
 * open.
 *
 * @author nozomihitomi
 * @param <T>
 */
public abstract class AbstractEventDetector<T extends EventDetector> extends AbstractDetector<T> {

    private static final long serialVersionUID = 1500574292575623469L;

    /**
     * initial state of the spacecraft given at the start date
     */
    private final SpacecraftState initialState;

    /**
     * the start date of the simulation or propagation
     */
    private final AbsoluteDate startDate;

    /**
     * the end date of the simulation or propagation
     */
    private final AbsoluteDate endDate;

    /**
     * Flag for if the default initializer should be used to initialize the time
     * interval handler;
     */
    private boolean timeIntervalInit;

    /**
     * specifies action after event is detected.
     */
    private final EventHandler.Action action;

    /**
     * Event handler for the time intervals
     */
    private HandlerTimeInterval<? super T> handlerTimeInterval;

    /**
     * Constructor for the detector.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     */
    public AbstractEventDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate) {
        this(initialState, startDate, endDate, EventHandler.Action.STOP, DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER);
    }

    /**
     * Constructor for the detector.
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     */
    public AbstractEventDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate, double maxCheck, double threshold) {
        this(initialState, startDate, endDate, EventHandler.Action.STOP, maxCheck, threshold, DEFAULT_MAX_ITER);
    }

    /**
     * Private constructor with full parameters. Attaches a time interval
     * handler that records event start and stop times.
     * <p>
     * This constructor is private as users are expected to use the builder API
     * with the various {@code withXxx()} methods to set up the instance in a
     * readable manner without using a huge amount of parameters.
     * </p>
     *
     * @param initialState initial state of the spacecraft given at the start
     * date
     * @param startDate the start date of the simulation or propagation
     * @param endDate the end date of the simulation or propagation
     * @param action specifies action after event is detected.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     */
    public AbstractEventDetector(SpacecraftState initialState, AbsoluteDate startDate, AbsoluteDate endDate, EventHandler.Action action, double maxCheck, double threshold, int maxIter) {
        super(maxCheck, threshold, maxIter, new StopOnIncreasing<>());
        this.initialState = initialState;
        this.startDate = startDate;
        this.endDate = endDate;
        this.timeIntervalInit = true;
        this.action = action;
    }

    @Override
    public void init(SpacecraftState s0, AbsoluteDate t) {
        super.init(s0, t);
        if (timeIntervalInit) {
            //check that the spacecraft state date matches start date
            if (!initialState.getDate().equals(startDate)) {
                throw new IllegalArgumentException("Spacecraft state must be given at the provided start date");
            }
            try {
                this.handlerTimeInterval = new HandlerTimeInterval<>(startDate, endDate, g(initialState), action);
            } catch (OrekitException ex) {
                Logger.getLogger(AbstractEventDetector.class.getName()).log(Level.SEVERE, null, ex);
            }
            //only initialize time interval array once
            timeIntervalInit = false;
        }
    }

    /**
     * Abstract Event Detector does not utilize the given event handler. It is
     * set up to use a default time interval handler (HandlerTimeInterval)
     *
     * @param newMaxCheck
     * @param newThreshold
     * @param newMaxIter
     * @param newHandler
     * @return
     */
    @Override
    protected final T create(double newMaxCheck, double newThreshold, int newMaxIter, EventHandler<? super T> newHandler) {
        return create(initialState, startDate, endDate, action, newMaxCheck, newThreshold, newMaxIter);
    }

    protected abstract T create(SpacecraftState initialState,
            AbsoluteDate startDate, AbsoluteDate endDate,
            EventHandler.Action action, double maxCheck, double threshold, int maxIter);

    @Override
    public EventHandler<? super T> getHandler() {
        if (this.handlerTimeInterval == null) {
            return super.getHandler();
        } else {
            return this.handlerTimeInterval;
        }
    }

    /**
     * If the event handler is the default HandlreTimeInterval, the recorded
     * time intervals are returned. Else null is returned
     *
     * @return
     */
    public TimeIntervalArray getTimeIntervalArray() {
        if (this.handlerTimeInterval == null) {
            return null;
        } else {
            return this.handlerTimeInterval.getTimeArray().createImmutable();
        }
    }

    /**
     * Checks to see if the latest time interval in the time interval array is open or closed
     *
     * @return true if the latest time interval in the time interval array is open or closed
     */
    public boolean isOpen() {
        return handlerTimeInterval.getTimeArray().isAccessing();
    }

}
