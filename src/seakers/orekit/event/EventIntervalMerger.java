/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.orekit.frames.TopocentricFrame;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.coverage.access.TimeIntervalMerger;

/**
 * This class is used to merge event time series together (e.g. access times,
 * solar illumination times, availability times etc.)
 *
 * @author nozomihitomi
 */
public class EventIntervalMerger implements Serializable {

    private static final long serialVersionUID = 9046345352941432741L;

    /**
     * Merges the events in two sets of event time series. The inputs are two
     * sets of events computed at each point in the same coverage grid
     * definition.
     *
     * @param events1 a set of events that is to be merged. Must have the same
     * coverage points as events2
     * @param events2 a set of events that is to be merged. Must have the same
     * coverage points as events1
     * @param andCombine true if events should be combined with logical AND
     * (i.e. intersection). False if events should be combined with a logical OR
     * (i.e. union)
     * @return the merged event time series
     */
    public static Map<TopocentricFrame, TimeIntervalArray> merge(
            Map<TopocentricFrame, TimeIntervalArray> events1,
            Map<TopocentricFrame, TimeIntervalArray> events2,
            boolean andCombine) throws IllegalArgumentException {

        ArrayList<Map<TopocentricFrame, TimeIntervalArray>> accessesCollection = new ArrayList<>(2);
        accessesCollection.add(events1);
        accessesCollection.add(events2);
        return EventIntervalMerger.merge(accessesCollection, andCombine);
    }

    /**
     * Merges multiple accesses from different simulations. The inputs are sets
     * of access computed at each point in the same coverage grid definition.
     *
     * @param eventCollection a collection of events from multiple scenario that
     * is to be merged. Each item in collection must have the same coverage
     * points
     * @param andCombine true if events should be combined with logical AND
     * (i.e. intersection). False if events should be combined with a logical OR
     * (i.e. union)
     * @return the merged event time series
     */
    public static Map<TopocentricFrame, TimeIntervalArray> merge(
            Collection<Map<TopocentricFrame, TimeIntervalArray>> eventCollection,
            boolean andCombine) throws IllegalArgumentException {

        Set<TopocentricFrame> ptKeys = eventCollection.iterator().next().keySet();
        Map<TopocentricFrame, TimeIntervalArray> out = new HashMap<>(ptKeys.size());

        //Check that all accesses sets have the same coverage grid definition
        for (Map<TopocentricFrame, TimeIntervalArray> accesses : eventCollection) {
            if (!accesses.keySet().equals(ptKeys)) {
                throw new IllegalArgumentException("Failed to merge event time series. Expected grid points between sets to be equal. Found sets containing different points.");
            }
        }

        for (TopocentricFrame pt : ptKeys) {
            ArrayList<TimeIntervalArray> accessArrays = new ArrayList<>();
            for (Map<TopocentricFrame, TimeIntervalArray> events : eventCollection) {
                accessArrays.add(events.get(pt));
            }
            TimeIntervalMerger merger = new TimeIntervalMerger(accessArrays);
            TimeIntervalArray mergedArray;
            if (andCombine) {
                mergedArray = merger.andCombine();
            } else {
                mergedArray = merger.orCombine();
            }
            out.put(pt, mergedArray);
        }
        return out;
    }
}
