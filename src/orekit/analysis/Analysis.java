/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis;

import java.util.Collection;
import java.util.concurrent.Callable;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;

/**
 * An analysis is used to record certain states at fixed time steps during the
 * propagation of an orbit. The states must be observable from information
 * stored in the SpacecraftSate class. Information retrievable from
 * SpacecraftState include the spacecraft's mass, attitude, position, and
 * velocity.
 *
 * Each analysis records an object of a generic class T at each time step and
 * the history can be retrieved after propagation.
 *
 * @author nozomihitomi
 * @param <T> the generic object for the record
 */
public interface Analysis<T> extends Callable<Analysis<T>>, OrekitFixedStepHandler{

    /**
     * Gets the recorded value of an analysis from each time step. The returned
     * history shall be sorted in chronological order with the earliest record
     * as the first element
     *
     * @return the recorded value of an analysis from each time step.
     */
    public Collection<Record<T>> getHistory();
    
    /**
     * Gets the start date of the analysis
     *
     * @return the start date of the analysis
     */
    public AbsoluteDate getStartDate();
    
    /**
     * Gets the end date of the analysis
     *
     * @return the end date of the analysis
     */
    public AbsoluteDate getEndDate();
    
    /**
     * Gets the fixed time step at which values are recorded
     *
     * @return the fixed time step at which values are recorded
     */
    public double getTimeStep();

    /**
     * The header to use in a saved file to inform a user of what the data are
     *
     * @return the header to use in a saved file to inform a user of what the data are
     */
    public String getHeader();

    /**
     * The file extension required to save the analysis
     *
     * @return the file extension required to save the analysis
     */
    public String getExtension();
    
    /**
     * Gets the name of the analysis
     * @return  the name of the analysis
     */
    public String getName();
}
