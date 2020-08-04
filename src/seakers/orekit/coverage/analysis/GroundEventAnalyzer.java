/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.coverage.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.CoveragePoint;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;

/**
 * This class computes several standard metrics regarding events (occurring and
 * not occurring) computed during a coverage simulation
 *
 * @author nozomihitomi
 */
public class GroundEventAnalyzer implements Serializable {

    private static final long serialVersionUID = -1289132465835079824L;

    /**
     * Collection of the CoveragePoints
     */
    private final Map<TopocentricFrame, TimeIntervalArray> events;
    
    /**
     * Start of simulation
     */
    private final AbsoluteDate startDate;
    
    /**
     * End of simulation
     */
    private final AbsoluteDate endDate;

    /**
     * Creates an analyzer from a set of coverage points and time interval array
     * of event occurrences
     *
     * @param fovEvents
     */
    public GroundEventAnalyzer(Map<TopocentricFrame, TimeIntervalArray> fovEvents) {
        this.events = fovEvents;
        Set<TopocentricFrame> topos = fovEvents.keySet();
        TopocentricFrame tp = topos.iterator().next();
        this.startDate = fovEvents.get(tp).getHead();
        this.endDate = fovEvents.get(tp).getTail();
    }

    /**
     * Gets the start date of the simulation
     * @return the start date of the simulation
     */
    public AbsoluteDate getStartDate() {
        return startDate;
    }

    /**
     * Gets the end date of the simulation
     * @return the end date of the simulation
     */
    public AbsoluteDate getEndDate() {
        return endDate;
    }
    
    /**
     * Gets the collection of  coverage points and their time interval array
     * of event occurrences
     *
     * @return
     */
    public Map<TopocentricFrame, TimeIntervalArray> getEvents() {
        return events;
    }
    /**
     * Gets the collection of coverage points covered in this analysis. The
     * returned collection is sorted by latitude first then by longitude in
     * ascending order
     *
     * @return
     */
    public ArrayList<CoveragePoint> getCoveragePoints() {
        ArrayList<CoveragePoint> points = new ArrayList<>();
        for (TopocentricFrame frame: events.keySet()) {
            if (frame instanceof CoveragePoint) {
                points.add((CoveragePoint)frame);
            }
        }
        Collections.sort(points);
        return points;
    }
    
    /**
     * gets the map of specified coverage points and 
     * their time interval array of their occurrences
     */
    public Map<TopocentricFrame, TimeIntervalArray> getEvents(Collection<TopocentricFrame> points) {

        Map<TopocentricFrame, TimeIntervalArray> data = new HashMap<>();
        for (TopocentricFrame tp : points) {
            data.put(tp, events.get(tp));
        }
        return data;
    }

    /**
     * Computes the statistics for a specified metric over all points given to
     * the analyzer
     *
     * @param metric the metric to compute
     * @param occurrences flag to declare whether to use the saved time
     * intervals [true] or its complement [false].
     * @param properties properties that declare the input arguments of the
     * metrics
     * @return statistics computed on the specified metric
     */
    public DescriptiveStatistics getStatistics(AnalysisMetric metric, boolean occurrences, Properties properties) {
        return this.getStatistics(metric, occurrences,
                new double[]{-Math.PI / 2, Math.PI / 2},
                new double[]{-Math.PI, Math.PI},
                properties);
    }

    /**
     * Computes the statistics for a specified metric on the points in the given
     * bounds for latitude and longitude
     *
     * @param metric the metric to compute
     * @param occurrences flag to declare whether to use the saved time
     * intervals [true] or its complement [false].
     * @param latBounds latitude bounds
     * @param lonBounds longitude bounds
     * @param properties properties that declare the input arguments of the
     * metrics
     * @return statistics computed on the specified metric
     */
    public DescriptiveStatistics getStatistics(AnalysisMetric metric, boolean occurrences, double[] latBounds, double[] lonBounds, Properties properties) {
        if (latBounds[0] > latBounds[1] || latBounds[0] < -Math.PI / 2 || latBounds[0] > Math.PI / 2
                || latBounds[1] < -Math.PI / 2 || latBounds[1] > Math.PI / 2) {
            throw new IllegalArgumentException(
                    String.format("Expected latitude bounds to be within [-pi/2,pi/2]. Found [%f,%f]", latBounds[0], latBounds[1]));
        }
        if (lonBounds[0] > lonBounds[1] || lonBounds[0] < -Math.PI || lonBounds[0] > Math.PI
                || lonBounds[1] < -Math.PI || lonBounds[1] > Math.PI) {
            throw new IllegalArgumentException(
                    String.format("Expected longitude bounds to be within [-pi,pi]. Found [%f,%f]", lonBounds[0], lonBounds[1]));
        }
        ArrayList<TopocentricFrame> points = new ArrayList<>();
        for (TopocentricFrame pt : events.keySet()) {
            if (pt.getPoint().getLatitude() >= latBounds[0]
                    && pt.getPoint().getLatitude() <= latBounds[1]
                    && pt.getPoint().getLongitude() >= lonBounds[0]
                    && pt.getPoint().getLongitude() <= lonBounds[1]) {
                points.add(pt);
            }
        }
        return this.getStatistics(metric, occurrences, points, properties);
    }

    /**
     * Computes the statistics for a specified metric on the specified point
     *
     * @param metric the metric to compute
     * @param occurrences flag to declare whether to use the saved time
     * intervals [true] or its complement [false].
     * @param point the specific point to compute the metrics on
     * @param properties properties that declare the input arguments of the
     * metrics
     *
     * @return statistics computed on the specified metric
     */
    public DescriptiveStatistics getStatistics(AnalysisMetric metric, boolean occurrences, TopocentricFrame point, Properties properties) {
        ArrayList<TopocentricFrame> list = new ArrayList<>();
        list.add(point);
        return this.getStatistics(metric, occurrences, list, properties);
    }

    /**
     * Computes the statistics for a specified metric on the specified subset of
     * given points
     *
     * @param metric the metric to compute
     * @param occurrences flag to declare whether to use the saved time
     * intervals [true] or its complement [false].
     * @param points the subset of points to compute the metrics on
     * @param properties properties that declare the input arguments of the
     * metrics
     * @return statistics computed on the specified metric
     */
    public DescriptiveStatistics getStatistics(AnalysisMetric metric, boolean occurrences, Collection<TopocentricFrame> points, Properties properties) {
        Map<TopocentricFrame, TimeIntervalArray> data = new HashMap<>();
        for (TopocentricFrame cp : points) {
            if (occurrences) {
                data.put(cp, events.get(cp));
            } else {
                data.put(cp, events.get(cp).complement());
            }
        }

        DescriptiveStatistics ds = new DescriptiveStatistics();

        switch (metric) {
            case DURATION:
                for (TopocentricFrame cp : data.keySet()) {
                    for (double duration : data.get(cp).getDurations()) {
                        ds.addValue(duration);
                    }
                }
                break;
            case DURATION_GEQ:
                double thresholdGEQ = Double.parseDouble(properties.getProperty("threshold"));
                for (TopocentricFrame cp : data.keySet()) {
                    for (double duration : data.get(cp).getDurations()) {
                        if (duration > thresholdGEQ) {
                            ds.addValue(duration);
                        }
                    }
                }
                break;
            case DURATION_LEQ:
                double thresholdLEQ = Double.parseDouble(properties.getProperty("threshold"));
                for (TopocentricFrame cp : data.keySet()) {
                    for (double duration : data.get(cp).getDurations()) {
                        if (duration < thresholdLEQ) {
                            ds.addValue(duration);
                        }
                    }
                }
                break;
            case MEAN_TIME_TO_T:
                for (TopocentricFrame cp : data.keySet()) {
                    DescriptiveStatistics responseTime = new DescriptiveStatistics();
                    for (double duration : data.get(cp).getDurations()) {
                        responseTime.addValue(duration);
                    }
                    double sumsquares = responseTime.getSumOfSquares();
                    double totalTime = data.get(cp).getTail().durationFrom(
                            data.get(cp).getHead());
                    ds.addValue(sumsquares / (2 * totalTime));
                }
                break;
            case TIME_AVERAGE:
                for (TopocentricFrame cp : data.keySet()) {
                    DescriptiveStatistics respTime = new DescriptiveStatistics();
                    for (double duration : data.get(cp).getDurations()) {
                        respTime.addValue(duration);
                    }
                    double sumsquares = respTime.getSumOfSquares();
                    double totalTime = data.get(cp).getTail().durationFrom(
                            data.get(cp).getHead());
                    ds.addValue(sumsquares / totalTime); //only thing different than mean time to T
                }
                break;
            case PERCENT_TIME:
                for (TopocentricFrame cp : data.keySet()) {
                    double sumDuration = 0;
                    for (double duration : data.get(cp).getDurations()) {
                        sumDuration += duration;
                    }
                    double totalTime = data.get(cp).getTail().durationFrom(
                            data.get(cp).getHead());
                    ds.addValue(sumDuration / totalTime);
                }
                break;
            case LIST_RISE_SET_TIMES:
                for (TopocentricFrame cp : data.keySet()) {
                    for (double event : data.get(cp).getRiseAndSetTimesList()) {
                        ds.addValue(event);
                    }
                }
                break;
            case OCCURRENCES:
                for (TopocentricFrame cp : data.keySet()) {
                    ds.addValue(data.get(cp).getDurations().length);
                }
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupported metric %s.", metric));
        }
        return ds;
    }
}
