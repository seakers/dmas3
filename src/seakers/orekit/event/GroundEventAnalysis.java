/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import java.util.Map;
import org.orekit.frames.TopocentricFrame;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.CoverageDefinition;

/**
 * Event analysis for the ground-based analyses (e.g. field-of-view access,
 * ground point sun angle)
 *
 * @author nhitomi
 */
public interface GroundEventAnalysis extends EventAnalysis {

    /**
     * Gets the time intervals when events occurred for each coverage point for
     * each coverage definition
     *
     * @return
     */
    public Map<CoverageDefinition, Map<TopocentricFrame, TimeIntervalArray>> getEvents();
    
    /**
     * Gets the time intervals when events occurred for each coverage point for
     * a specified coverage definition
     *
     * @param covDef the coverage definition of interest
     * @return
     */
    public Map<TopocentricFrame, TimeIntervalArray> getEvents(CoverageDefinition covDef);
}
