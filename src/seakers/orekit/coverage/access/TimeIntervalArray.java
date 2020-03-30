/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.coverage.access;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import seakers.orekit.object.CoveragePoint;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.util.RawSafety;

/**
 * Array to store intervals of rise and set times (e.g. gaps or accesses). The
 * class allows users to combine arrays with logical OR or AND to find the union
 * or intersection of intervals, respectively.
 *
 * @author nozomihitomi
 */
public class TimeIntervalArray implements Iterable<RiseSetTime>, Serializable {

    private static final long serialVersionUID = 394081713593567685L;

    protected final ArrayList<RiseSetTime> timeArray;

    protected boolean accessing; //boolean to track if a rise time has a corresponding set time

    /**
     * Head is the beginning date of the timeline
     */
    private final AbsoluteDate head;
    /**
     * Head is the ending date of the timeline
     */
    private final AbsoluteDate tail;

    /**
     * Time between the head and the tail dates
     */
    protected final double simulationLength;

    /**
     * Creates a time interval array instance with the head and tail date times
     * to define the entire timeline. If no rise or set times are added, assumes
     * that entire array is closed (i.e. array starts with set time).
     *
     * @param head
     * @param tail
     */
    public TimeIntervalArray(AbsoluteDate head, AbsoluteDate tail) {
        this.head = head;
        this.tail = tail;
        this.timeArray = new ArrayList<>();
        this.simulationLength = tail.durationFrom(head);
        this.accessing = false;
    }

    /**
     * Creates a time interval array instance with the head and tail date times
     * to define the entire timeline. Can specify whether head of the array is
     * open (i.e. true = array starts with rise time) or closed (i.e. false =
     * array starts with set time).
     *
     * @param head
     * @param tail
     * @param headOpen Can specify whether head of the array is open (i.e. true
     * = array starts with rise time) or closed (i.e. false = array starts with
     * set time).
     */
    public TimeIntervalArray(AbsoluteDate head, AbsoluteDate tail, boolean headOpen) {
        this.head = head;
        this.tail = tail;
        this.timeArray = new ArrayList<>();
        this.simulationLength = tail.durationFrom(head);
        this.accessing = false;
        if (headOpen) {
            this.addRise(0.0);
        }
    }

    /**
     * Adds a new rise time to the array to the start of a time interval. If
     * previous intervals exists, the newly added rise time must occur after the
     * set time of the previous interval. If an interval remains "open" (i.e.
     * has no set time) then the interval must be "closed" before a new rise
     * time can be added.
     *
     * @param riseTime
     * @return true if the interval is open, false if it is closed.
     */
    public boolean addRiseTime(AbsoluteDate riseTime) {
        return addRiseTime(riseTime.durationFrom(head));
    }

    /**
     * Adds a new rise time to the array to the start of a time interval. If
     * previous intervals exists, the newly added rise time must occur after the
     * set time of the previous interval. If an interval remains "open" (i.e.
     * has no set time) then the interval must be "closed" before a new rise
     * time can be added.
     *
     * @param riseTime
     * @return true if the interval is open, false if it is closed.
     */
    public boolean addRiseTime(double riseTime) {
        return addRise(riseTime);
    }

    /**
     * Constructor needs access to non-over-rideable method for adding a rise
     * time
     *
     * @param riseTime
     * @return
     */
    private boolean addRise(double riseTime) {
        if (accessing) {
            throw new IllegalArgumentException(String.format("Cannot add rise time %f since interval is not closed yet.\nLast rise time = %f",
                                riseTime, this.timeArray.get(this.timeArray.size()-1).getTime()));
        }

        if (riseTime < 0) {
            throw new IllegalArgumentException(String.format("Cannot add rise time %f before the head of the timeline.", riseTime));
        }

        timeArray.add(new RiseSetTime(riseTime, true));
        accessing = true;
        return accessing;
    }

    /**
     * Adds a new set time to the array to the end of a time interval. The newly
     * added set time must occur after the rise time of the current interval. If
     * an interval is not "open" (i.e. has no rise time) then the interval must
     * be "opened" before a new set time can be added.
     *
     * @param setTime
     * @return true if the interval is open, false if it is closed.
     */
    public boolean addSetTime(AbsoluteDate setTime) {
        return addSetTime(setTime.durationFrom(head));
    }

    /**
     * Adds a new set time to the array to the end of a time interval. The newly
     * added set time must occur after the rise time of the current interval. If
     * an interval is not "open" (i.e. has no rise time) then the interval must
     * be "opened" before a new set time can be added.
     *
     * @param setTime
     * @return true if the interval is open, false if it is closed.
     */
    public boolean addSetTime(double setTime) {
        if (timeArray.isEmpty()) {
            addRiseTime(head);
        } else {

            if (!accessing) {
                throw new IllegalArgumentException(
                        String.format("Cannot add set time %f since interval is not open yet.\nLast set time = %f",
                                setTime, this.timeArray.get(this.timeArray.size()-1).getTime()));
            }

            if (setTime > simulationLength) {
                throw new IllegalArgumentException(String.format("Cannot add set time %s after the tail of the timeline.", setTime));
            }
        }
        timeArray.add(new RiseSetTime(setTime, false));
        accessing = false;
        return accessing;
    }

    /**
     * Check to see if there is currently an open time interval at the end of
     * the array (i.e. last item in array is a rise time).
     *
     * @return true if last item in array is a rise time. Else false.
     */
    public boolean isAccessing() {
        return accessing;
    }

    /**
     * Returns the number of closed intervals. Open intervals at the ends of the
     * array are counted as they are automatically closed with the head or tail
     * times.
     *
     * @return
     */
    public int numIntervals() {
        if (!accessing) {
            return FastMath.floorDiv(timeArray.size(), 2);
        } else {
            return FastMath.floorDiv(timeArray.size(), 2) + 1;
        }
    }

    /**
     * Returns a copy of the time intervals ordered chronologically, and the
     * rise and set times alternate within the array.
     *
     * @return
     */
    public ArrayList<RiseSetTime> getRiseSetTimes() {
        return new ArrayList<>(timeArray);
    }

    /**
     * Returns an array of the durations of each interval stored in the array.
     * The returned array will contain the durations in chronological order of
     * the time intervals
     *
     * @return
     */
    public double[] getDurations() {
        int nIntervals = numIntervals();
        double[] durations = new double[nIntervals];
        int durationIndex = 0;

        int endInd = timeArray.size();

        if (isAccessing()) {
            endInd--;
            durations[nIntervals - 1] = timeArray.get(endInd).getTime() - timeArray.get(endInd - 1).getTime();
        }

        for (int i = 0; i < endInd; i += 2) {
            durations[durationIndex] = timeArray.get(i + 1).getTime() - timeArray.get(i).getTime();
            durationIndex++;
        }

        return durations;
    }

    /**
     * Returns an array of the rise and set times of each interval stored in the array.
     * The returned array will contain the rise and set times in chronological order of
     * the time intervals
     *
     * @return
     */
    public double[] getRiseAndSetTimesList() {
        int nIntervals = numIntervals();
        double[] eventsList = new double[2*nIntervals];
        int eventIndex = 0;

        int endInd = timeArray.size();

        for (int i = 0; i < endInd; i += 1) {
            eventsList[eventIndex] = timeArray.get(i).getTime();
            eventIndex++;
        }

        return eventsList;
    }
    
    /**
     * Returns a time interval array that contains the gaps between the time
     * intervals stored in this array. For example, if this array was a time
     * storing rise and set times of accesses between a grid point and a
     * satellite, this method would return the gap times.
     *
     * @return Returns an immutable time interval array that contains the gaps
     * between the time intervals stored in this array.
     */
    public TimeIntervalArray complement() {
        TimeIntervalArray out = new TimeIntervalArray(head, tail);
        if (timeArray.isEmpty()) {
            out.addRiseTime(head);
            out.addSetTime(tail);

        } else {
            Iterator<RiseSetTime> iter = timeArray.iterator();

            RiseSetTime current = iter.next();
            if (current.getTime() > 0) {
                out.addRiseTime(head);
                out.addSetTime(current.getTime());
            }

            while (iter.hasNext()) {
                current = iter.next();
                if (current.isRise()) {
                    out.addSetTime(current.getTime());
                } else {
                    out.addRiseTime(current.getTime());
                }
            }
        }

        return new ImmutableTimeIntervalArray(out);
    }

    /**
     * Gets the head time stamp where the timeline starts
     *
     * @return the head time stamp where the timeline starts
     */
    public AbsoluteDate getHead() {
        return head;
    }

    /**
     * Gets the head time stamp where the timeline ends
     *
     * @return the head time stamp where the timeline ends
     */
    public AbsoluteDate getTail() {
        return tail;
    }

    @Override
    public Iterator<RiseSetTime> iterator() {
        return timeArray.iterator();
    }

    /**
     * Checks to see if there are any intervals in this array
     *
     * @return
     */
    public boolean isEmpty() {
        return timeArray.isEmpty();
    }

    /**
     * This method returns a time interval array instance that is immutable. In
     * other words, the methods to add times will no longer be available to the
     * user. Attempts to add new times will throw an
     * UnsupportedOperationException. In addition time arrays that have no
     * intervals but have an "accessing" or "open" head, will be converted into
     * an interval that extends from the given start and end times of this
     * object. Note that this instance will remain mutable.
     *
     *
     * @return a time interval array instance that is immutable
     */
    public TimeIntervalArray createImmutable() {
        return new ImmutableTimeIntervalArray(this);
    }
    
    public static void save(File file, HashMap<CoveragePoint,TimeIntervalArray> arrays){
        try(ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file))){
            os.writeObject(arrays);
        } catch (IOException ex) {
            Logger.getLogger(TimeIntervalArray.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static HashMap<CoveragePoint,TimeIntervalArray> load(File file){
        HashMap<CoveragePoint, TimeIntervalArray> out = null;
        try(ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))){
            out = RawSafety.castHashMap(is.readObject());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TimeIntervalArray.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TimeIntervalArray.class.getName()).log(Level.SEVERE, null, ex);
        }
        return out;
    }

    /**
     * This class of time interval array is immutable, so add methods are not
     * supported and will throw an unsupported operation exception
     *
     * @author nozomihitomi
     */
    private class ImmutableTimeIntervalArray extends TimeIntervalArray {

        private static final long serialVersionUID = 2589419421517789562L;

        public ImmutableTimeIntervalArray(TimeIntervalArray original) {
            super(original.getHead(), original.getTail());
            this.timeArray.addAll(original.getRiseSetTimes());
            //if tail is open close the interval with the end of the simulation
            if (original.isAccessing()) {
                this.timeArray.add(new RiseSetTime(this.simulationLength, false));
            }
        }

        @Override
        public TimeIntervalArray createImmutable() {
            //return this since it is already immutable
            return this;
        }

        @Override
        public boolean addSetTime(double setTime) {
            throw new UnsupportedOperationException("Attempted to modifiy an immutable instance of time interval array.");
        }

        @Override
        public boolean addSetTime(AbsoluteDate setTime) {
            throw new UnsupportedOperationException("Attempted to modifiy an immutable instance of time interval array.");
        }

        @Override
        public boolean addRiseTime(double riseTime) {
            throw new UnsupportedOperationException("Attempted to modifiy an immutable instance of time interval array.");
        }

        @Override
        public boolean addRiseTime(AbsoluteDate riseTime) {
            throw new UnsupportedOperationException("Attempted to modifiy an immutable instance of time interval array.");
        }

    }

}
