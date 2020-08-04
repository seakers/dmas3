/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event.detector;

import org.orekit.propagation.events.EventDetector;
import seakers.orekit.coverage.access.TimeIntervalArray;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/**
 * the g() function for the FOVDetector assumes that the target enters the
 * FieldOfView when the value is positive and exits when the value is negative.
 * Therefore, when an event is detected and the value is increasing, the target
 * is entering the FieldOfView.
 *
 * The access times are stored within the handler and can be retrieved at any
 * time during the simulation
 *
 * @author nozomihitomi
 */
public class HandlerTimeInterval<T extends EventDetector> implements EventHandler<T> {

    private final TimeIntervalArray timeArray;
    
    private EventHandler.Action action;

    /**
     * Creates a field of view handler for a specific coverage point. Default
     * handler will continue after event is detects. If there are no events
     * detected, assumes that gvalues were negative for entire simulation.
     *
     * @param startDate the start date of the simulation
     * @param endDate the end date of the simulation
     */
    public HandlerTimeInterval(AbsoluteDate startDate, AbsoluteDate endDate) {
        this(startDate, endDate, EventHandler.Action.CONTINUE);
    }

    /**
     * Creates a field of view handler for a specific coverage point. If there
     * are no events detected, assumes that gvalues were negative for entire
     * simulation.
     *
     * @param startDate the start date of the simulation
     * @param endDate the end date of the simulation
     * @param action set the action to execute after event is detected
     * {CONTINUE, STOP, RESET_DERIVATIVES, RESET_STATE}
     */
    public HandlerTimeInterval(AbsoluteDate startDate, AbsoluteDate endDate, EventHandler.Action action) {
        this.timeArray = new TimeIntervalArray(startDate, endDate);
        this.action = action;
    }

    /**
     * Creates a field of view handler for a specific coverage point. Can
     * specify if the simulation starts with a positive or negative gvalue. If
     * there are no events detected, assumes that gvalues were there same sign
     * as initGVal for entire simulation.
     *
     * @param startDate the start date of the simulation
     * @param endDate the end date of the simulation
     * @param initGVal the initial g value of the spacecraft state
     * @param action set the action to execute after event is detected
     * {CONTINUE, STOP, RESET_DERIVATIVES, RESET_STATE}
     */
    public HandlerTimeInterval(AbsoluteDate startDate, AbsoluteDate endDate, double initGVal, EventHandler.Action action) {
        if(initGVal > 0){
            this.timeArray = new TimeIntervalArray(startDate, endDate, true);
        }else{
            this.timeArray = new TimeIntervalArray(startDate, endDate, false);
        }
        this.action = action;
    }

    @Override
    public EventHandler.Action eventOccurred(final SpacecraftState s, final T detector,
            final boolean increasing) throws OrekitException {

        if (increasing) {
            //Access begins
            timeArray.addRiseTime(s.getDate());
        } else {
            //Access ends
            timeArray.addSetTime(s.getDate());
        }

        return action;
    }

    public TimeIntervalArray getTimeArray() {
        return timeArray;
    }
    
    }
