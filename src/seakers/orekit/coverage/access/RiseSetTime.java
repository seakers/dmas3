/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.coverage.access;

import java.io.Serializable;

/**
 * Method that stores information about an event start/rise or stop/set time.
 *
 * @author nozomihitomi
 */
public class RiseSetTime implements Serializable {

    private static final long serialVersionUID = 6893042201775522394L;

    private final boolean isRise;

    private final double time;

    /**
     * Creates new instance of a time stamp to mark the start or end of an event
     *
     * @param time in epoch seconds
     * @param isRise true if the event is starting (rise time) or false if the
     * event is ending (set time)
     */
    public RiseSetTime(final double time, final boolean isRise) {
        this.time = time;
        this.isRise = isRise;
    }

    /**
     * Check to see if the event is a rise time or a set time.
     *
     * @return true if event is a rise time. false if event is a set time.
     */
    public boolean isRise() {
        return isRise;
    }

    /**
     * Returns the time elapsed in this epoch in seconds
     *
     * @return the time elapsed in this epoch in seconds
     */
    public double getTime() {
        return time;
    }
}
