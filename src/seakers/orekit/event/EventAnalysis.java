/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.event;

import java.util.concurrent.Callable;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author nhitomi
 */
public interface EventAnalysis extends Callable<EventAnalysis> {
    
    /**
     * Gets the start date of the analysis
     * @return the start date of the analysis
     */
    public AbsoluteDate getStartDate();

    /**
     * Gets the end date of the analysis
     * @return the end date of the analysis
     */
    public AbsoluteDate getEndDate();
    
    /**
     * Gets the inertial frame used in the analysis
     * @return the inertial frame used in the analysis
     */
    public Frame getInertialFrame();
    
    /**
     * The header to write to file when saving the results.
     * @return 
     */
    public String getHeader();
}
