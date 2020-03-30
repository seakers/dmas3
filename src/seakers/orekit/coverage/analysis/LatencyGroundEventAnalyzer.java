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
import seakers.orekit.coverage.access.RiseSetTime;
import seakers.orekit.object.GndStation;
import seakers.orekit.object.Satellite;

/**
 * This class computes several standard metrics regarding events (occurring and
 * not occurring) computed during a coverage simulation
 *
 * @author nozomihitomi
 */
public class LatencyGroundEventAnalyzer implements Serializable {
    
    private static final long serialVersionUID = 5796954223682164296L;    

    private final HashMap<Satellite,HashMap<TopocentricFrame, TimeIntervalArray>> events;
    
    /**
     * Collection of GroundStations
     */
    private final HashMap<Satellite, HashMap<GndStation, TimeIntervalArray>> gndstations;
    
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
     * of event occurrences assigned for each satellite in the constellation
     * and a set of GS locations and time interval array of access to these GS
     * for each satellite in the constellation.
     * of contacts
     *
     * @param fovEvents accesses of all satellites in the constellation to a collection of coverage points
     * @param gsEvents accesses of all  satellites in the constellation to a set of ground stations
     */
    public LatencyGroundEventAnalyzer(HashMap<Satellite,HashMap<TopocentricFrame, TimeIntervalArray>> fovEvents, HashMap<Satellite,HashMap<GndStation, TimeIntervalArray>> gsEvents) {
        this.events = fovEvents;
        this.gndstations = gsEvents;
        Set<Satellite> sats = fovEvents.keySet();
        Map<TopocentricFrame, TimeIntervalArray> sat1_accesses=fovEvents.get(sats.iterator().next());
        Set<TopocentricFrame> topos = sat1_accesses.keySet();
        TopocentricFrame tp = topos.iterator().next();
        this.startDate = sat1_accesses.get(tp).getHead();
        this.endDate = sat1_accesses.get(tp).getTail();
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
     * Gets the collection of satellites in this analysis. 
     * @return
     */
    public Set<Satellite> getSatellites() {
        return events.keySet();
    }
    
    /**
     * Gets the collection of coverage points covered in this analysis. The
     * returned collection is sorted by latitude first then by longitude in
     * ascending order
     * @return
     */
    public ArrayList<CoveragePoint> getCoveragePoints() {
        Set<Satellite> sats = events.keySet();
        Map<TopocentricFrame, TimeIntervalArray> sat1_accesses = events.get(sats.iterator().next());
        ArrayList<CoveragePoint> points = new ArrayList<>();
        for (TopocentricFrame frame: sat1_accesses.keySet()) {
            if (frame instanceof CoveragePoint) {
                points.add((CoveragePoint)frame);
            }
        }
        Collections.sort(points);
        return points;
    }

    /**
     * Computes the latency statistics over all points given to the analyzer
     * @return latency statistics
     */
    public DescriptiveStatistics getStatistics() {
        return this.getStatistics(new double[]{-Math.PI / 2, Math.PI / 2}, new double[]{-Math.PI, Math.PI});
    }

    /**
     * Computes the latency statistics on the points in the given bounds 
     * for latitude and longitude
     * @param latBounds latitude bounds
     * @param lonBounds longitude bounds
     * @return latency statistics
     */
    public DescriptiveStatistics getStatistics(double[] latBounds, double[] lonBounds) {
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
        for (TopocentricFrame pt : getCoveragePoints()) {
            if (pt.getPoint().getLatitude() >= latBounds[0]
                    && pt.getPoint().getLatitude() <= latBounds[1]
                    && pt.getPoint().getLongitude() >= lonBounds[0]
                    && pt.getPoint().getLongitude() <= lonBounds[1]) {
                points.add(pt);
            }
        }
        return this.getStatistics(points);
    }

    /**
     * Computes the latency statistics on the specified point
     * @param point the specific point to compute latency
     *
     * @return latency statistics
     */
    public DescriptiveStatistics getStatistics(TopocentricFrame point) {
        ArrayList<TopocentricFrame> points = new ArrayList<>();
        points.add(point);
        return this.getStatistics(points);
    }

    /**
     * Computes the latency statistics on the specified subset of given points
     * @param points the subset of points to compute latency on
     * @return latency statistics
     */
    public DescriptiveStatistics getStatistics(Collection<TopocentricFrame> points) {
        DescriptiveStatistics ds = new DescriptiveStatistics();
        //for each coverage point, we will calculate the latency of each rise time event
        for (TopocentricFrame cp : points) {
            for (Satellite sat : getSatellites()){
                //Obtain rise and set times of the different accesses of the coverage point
                ArrayList<RiseSetTime> eventsCP = events.get(sat).get(cp).getRiseSetTimes();
                for (RiseSetTime tcp:eventsCP){
                    //check if it is a rise time
                    if(tcp.isRise()){
                        //default initialization of latency of an specific coverage point and specific rise time
                        double latencyopt=getEndDate().durationFrom(getStartDate());
                        //Loop around the different ground stations to see which one is accessed first and determines shortest latency
                        for(GndStation gs: gndstations.get(sat).keySet()){
                            //Obtain rise and set times of the different contacts with ground station gs
                            ArrayList<RiseSetTime> eventsGS = gndstations.get(sat).get(gs).getRiseSetTimes();
                            //If on coverage point access rise time tcp, there is contact with ground station gs, latency is 0.
                            if(isAccessing(eventsGS,tcp.getTime())){
                                latencyopt=0;
                                break;
                            }
                            //Find the latency for the rise time tcp of coverage point cp and ground station gs
                            for(RiseSetTime tgs:eventsGS){
                                if(tgs.isRise()){
                                    double latency=tgs.getTime()-tcp.getTime();
                                    if (latency>0 && latency<latencyopt){
                                        //Update the latency of a specific rise time of a specific cov point if previous ground stations had longer latency
                                        latencyopt=latency;
                                    }
                                }
                            }
                        }
                        //Add the latency with the "best" ground station of a specific rise time of a specific cov point
                        ds.addValue(latencyopt);
                    }
                }
            }

        }
        return ds;
    }

    
     /**
     * Returns true if a ground station is in contact at the time when a coverage point is accessed
     *
     * @param eventGS List of rise and set times of ground station contacts
     * @param time Access rise time of coverage point
     */
    private boolean isAccessing(ArrayList<RiseSetTime> eventsGS, double time) {
        int ind=0;
        while(ind<eventsGS.size()-1){
            if(eventsGS.get(ind).isRise() && (time-eventsGS.get(ind).getTime()>0)){
                if(time-eventsGS.get(ind+1).getTime()<0){
                    return true;
                }
            }
            ind=ind+1;
        }
        return false;
    }
}