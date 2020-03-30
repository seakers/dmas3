/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.coverage.access.TimeIntervalArray;

/**
 * The abstract class for an event analysis.
 *
 * @author nhitomi
 */
public abstract class AbstractEventAnalysis implements EventAnalysis {

    /**
     * Analysis start date
     */
    private final AbsoluteDate startDate;

    /**
     * Analysis end date
     */
    private final AbsoluteDate endDate;

    /**
     * the inertial frame
     */
    private final Frame inertialFrame;

    /**
     * Constructor to create new event analysis.
     *
     * @param startDate Analysis start date
     * @param endDate Analysis end date
     * @param inertialFrame the inertial frame
     */
    public AbstractEventAnalysis(AbsoluteDate startDate, AbsoluteDate endDate, Frame inertialFrame) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.inertialFrame = inertialFrame;
    }

    /**
     * Returns a new TimeIntervalArray object that contains no events but has
     * its bounds set to the start and end dates
     *
     * @return
     */
    protected TimeIntervalArray getEmptyTimeArray() {
        return new TimeIntervalArray(startDate, endDate);
    }

    @Override
    public abstract EventAnalysis call() throws Exception;

    @Override
    public AbsoluteDate getStartDate() {
        return startDate;
    }

    @Override
    public AbsoluteDate getEndDate() {
        return endDate;
    }

    @Override
    public Frame getInertialFrame() {
        return inertialFrame;
    }

}
