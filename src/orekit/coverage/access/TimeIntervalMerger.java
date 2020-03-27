/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.coverage.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import seakers.orekit.util.SortedLinkedList;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author nozomihitomi
 */
public class TimeIntervalMerger {

    private final ArrayList<RiseSetTime> sortedRiseSetTimes;
    private final ArrayList<Integer> overlaps;
    private final AbsoluteDate head;
    private final AbsoluteDate tail;
    private final int nTotalArrays;
    
    public TimeIntervalMerger(TimeIntervalArray array0, TimeIntervalArray array1) {
        this(Arrays.asList(new TimeIntervalArray[]{array0, array1}));
    }

    public TimeIntervalMerger(Collection<TimeIntervalArray> timeArrays) {
        this.nTotalArrays = timeArrays.size();

        //Find earliest head date and latest tail date
        Iterator<TimeIntervalArray> iter = timeArrays.iterator();
        TimeIntervalArray currentArray = iter.next();
        AbsoluteDate earliest = currentArray.getHead();
        AbsoluteDate latest = currentArray.getTail();
        while (iter.hasNext()) {
            currentArray = iter.next();
            if (currentArray.getHead().compareTo(earliest) < 0) {
                earliest = currentArray.getHead();
            }
            if (currentArray.getTail().compareTo(latest) > 0) {
                latest = currentArray.getTail();
            }
        }
        head = earliest;
        tail = latest;

        //compute the ordered/sorted rise and set times to be used for the future so they don't have to be recomputed
        this.sortedRiseSetTimes = sortRiseSetTimes(timeArrays);

        //find where the time intervals overlap
        this.overlaps = findOverlaps(sortedRiseSetTimes);
    }

    /**
     * Sorts all the rise and set times from all the time interval arrays. In
     * mode = 0, creates a sorted array for all the rise times. In mode = 1
     * creates sorted array of all the set times. A mode of any other number
     * except for 0 or 1 will cause exception to be thrown.
     *
     * @param mode
     * @return
     */
    private ArrayList<RiseSetTime> sortRiseSetTimes(Collection<TimeIntervalArray> timeArrays) {
        //set all time arrays into immutable arrays to close off any open intervals
        ArrayList<TimeIntervalArray> arrays = new ArrayList<>(timeArrays.size());
        for(TimeIntervalArray array : timeArrays){
            arrays.add(array.createImmutable());
        }

        //Give each array an index
        HashMap<Integer, ArrayList<RiseSetTime>> timeArrayMap = new HashMap<>(timeArrays.size());
        //map to store the current index for each array <id,index>
        HashMap<Integer, Integer> id_indexMap = new HashMap<>(timeArrays.size());
        Iterator<TimeIntervalArray> iter = arrays.iterator();
        int arrayID = 0;
        int totalSize = 0;
        while (iter.hasNext()) {
            timeArrayMap.put(arrayID, iter.next().getRiseSetTimes());
            totalSize += timeArrayMap.get(arrayID).size();
            id_indexMap.put(arrayID, 0);
            arrayID++;
        }

        ArrayList<RiseSetTime> resultant = new ArrayList<>(totalSize);

        SortedLinkedList<RiseSetTimeID> currentDates = new SortedLinkedList<>(new TimeComparator());
        for (Integer id : id_indexMap.keySet()) {
            if(!timeArrayMap.get(id).isEmpty()){
                //skip over any time interval arrays that are empty 
                RiseSetTime time = timeArrayMap.get(id).get(id_indexMap.get(id));
                currentDates.add(new RiseSetTimeID(time, id));
            }
        }
        
        //if all arrays are empty, return an empty array
        if (currentDates.isEmpty()){
            return resultant;
        }
        
        int numOpenIntervals = 0;
        while (true) {
            //search for the next earliest rise or set time
            RiseSetTimeID earliestDate = currentDates.get(0);
            RiseSetTime minDate = earliestDate.getTime();
            Integer minID = earliestDate.getId();
            currentDates.remove(0);

            //update index
            //check if update would exceed array size
            int nextIndex = id_indexMap.get(minID) + 1;
            if (nextIndex < timeArrayMap.get(minID).size()) {
                id_indexMap.put(minID, nextIndex);
                currentDates.add(new RiseSetTimeID(timeArrayMap.get(minID).get(nextIndex), minID));
            } else if (nextIndex == timeArrayMap.get(minID).size() && minDate.isRise()) {
                numOpenIntervals++;
            }
            
            resultant.add(minDate);

            //if there are no more dates to sort, break from loop
            if (currentDates.isEmpty()) {
                break;
            }
        }
        //maybe no longer need numOpenIntevals since makeImmutable takes cares of open intervals
        for (int i = 0; i < numOpenIntervals; i++) {
            resultant.add(new RiseSetTime(tail.durationFrom(head), false));
        }
        return resultant;
    }

    /**
     * Keeps track of when and how many overlaps occur between the time
     * intervals. Also removes duplicate rise or set times from the given list.
     *
     * @param riseSetTimes the sorted set of rise and set times
     * @return
     */
    private ArrayList<Integer> findOverlaps(ArrayList<RiseSetTime> riseSetTimes) {
        ArrayList<Integer> overlap = new ArrayList<>();
        
        //if there are no time intervals, there are no overlapping intervals
        if(riseSetTimes.isEmpty()){
            overlap.add(0);
            return overlap;
        }

        double currentTime = 0.0;
        int currentOverlap = 0;
        //check beginning condition
        int ind = 0;
        if(riseSetTimes.get(0).getTime() > currentTime){
            overlap.add(currentOverlap);
        }else if(riseSetTimes.get(0).getTime() == currentTime){
            currentOverlap++;
            while(riseSetTimes.get(1).getTime() == currentTime){
                //remove duplicate rise times that occur at the beginning
                riseSetTimes.remove(1);
            }
            overlap.add(currentOverlap);
            ind = 1;
        }
        
        for (int i = ind; i < riseSetTimes.size(); i++) {
            RiseSetTime event = riseSetTimes.get(i);

            if (event.isRise()) {
                currentOverlap++;
            } else {
                currentOverlap--;
            }

            if (event.getTime() != currentTime) {
                currentTime = event.getTime();
                overlap.add(currentOverlap);
            } else {
                overlap.remove(overlap.size()-1); //remove duplicate rise set times
                overlap.add(currentOverlap);
                riseSetTimes.remove(i);
                i--;
            }
        }

        return overlap;
    }

    /**
     * This method uses a logical AND to combine the time intervals. Resulting
     * time interval array uses the earliest head date and the latest tail date
     * in the collection of TimeIntervalSets. The returned array is immutable.
     *
     * @return the intersection of all the time intervals. The returned array is immutable.
     */
    public TimeIntervalArray andCombine() {
        return nOverlapping(this.nTotalArrays);
    }

    /**
     * This method uses a logical OR to combine the time intervals. Resulting
     * time interval array uses the earliest head date and the latest tail date
     * in the collection of TimeIntervalSets. The returned array is immutable.
     *
     * @return the union of all the time intervals. The returned array is immutable.
     */
    public TimeIntervalArray orCombine() {
        return nOverlapping(1);
    }

    /**
     * This method finds the intervals where at least n intervals overlap at a
     * given time. Resulting time interval array uses the earliest head date and
     * the latest tail date in the collection of TimeIntervalSets. The returned array is immutable.
     *
     * @param n the minimum number of intervals that need to overlap to be
     * included in the set.
     * @return An array when there are at least n overlapping intervals. The returned array is immutable.
     */
    public TimeIntervalArray nOverlapping(int n) {
        TimeIntervalArray out = new TimeIntervalArray(head, tail);
        //return the array if there are no accesses
        if(sortedRiseSetTimes.isEmpty()){
            return out;
        }
        
         //check beginning condition
        int ind = 0;
        if(sortedRiseSetTimes.get(0).getTime() != 0.0){
            ind = 1;
        }
        
        for (int i = 0; i < this.overlaps.size(); i++) {
            if (overlaps.get(i) >= n) {
                if (!out.isAccessing()) {
                    out.addRiseTime(sortedRiseSetTimes.get(i - ind).getTime());
                }
            } else {
                if (out.isAccessing()) {
                    out.addSetTime(sortedRiseSetTimes.get(i - ind).getTime());
                }
            }
        }
        
        return out.createImmutable();
    }

    private class RiseSetTimeID {

        private final RiseSetTime time;
        private final int id;

        public RiseSetTimeID(RiseSetTime time, int id) {
            this.time = time;
            this.id = id;
        }

        public RiseSetTime getTime() {
            return time;
        }

        public int getId() {
            return id;
        }
    }

    private class TimeComparator implements Comparator<RiseSetTimeID> {

        @Override
        public int compare(RiseSetTimeID a, RiseSetTimeID b) {
            double val = a.getTime().getTime()-b.getTime().getTime();
            if(val < 0)
                return -1;
            else if(val > 0)
                return 1;
            else
                return 0;
        }
    }

}
