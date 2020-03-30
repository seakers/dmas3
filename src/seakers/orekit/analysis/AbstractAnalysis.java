/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis;

import java.util.Collection;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.propagation.PropagatorFactory;

/**
 * An abstract class for the Analysis interface
 * @author nozomihitomi
 * @param <T> The generic for the record object to record 
 */ 
public abstract class AbstractAnalysis<T> implements Analysis<T> {

    /**
     * Gets the start date of the analysis
     */
    private final AbsoluteDate startDate;

    /**
     * Gets the end date of the analysis
     */
    private final AbsoluteDate endDate;

    /**
     * the fixed time step at which values are recorded
     */
    private final double timeStep;

    protected final RecordHistory<T> history;

    public AbstractAnalysis(AbsoluteDate startDate, AbsoluteDate endDate,
            double timeStep, PropagatorFactory propagatorFactory) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.timeStep = timeStep;
        this.history = new RecordHistory<>();
    }

    protected void addRecord(Record<T> record) {
        history.add(record);
    }

    @Override
    public AbsoluteDate getEndDate() {
        return endDate;
    }

    @Override
    public AbsoluteDate getStartDate() {
        return startDate;
    }

    @Override
    public String getHeader() {
        return String.format("Date", timeStep);
    }

    @Override
    public double getTimeStep() {
        return timeStep;
    }

    /**
     * Returns the recorded data structures from the analysis
     *
     * @return the recorded data structures from the analysis
     */
    @Override
    public Collection<Record<T>> getHistory() {
        history.sortByDate();
        return history;
    }

}
