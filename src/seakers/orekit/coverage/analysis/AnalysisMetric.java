/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.coverage.analysis;

/**
 *
 * @author nhitomi
 */
public enum AnalysisMetric {

    /**
     * Used when obtaining the duration of the event occurrences or
     * non-occurrences (e.g. mean access time or max gap time)
     */
    DURATION,
    /**
     * Used when obtaining the duration of the event occurrences or
     * non-occurrences that exceed a certain duration. Takes in input parameter "threshold".
     */
    DURATION_GEQ,
    /**
     * Used when obtaining the duration of the event occurrences or
     * non-occurrences that are less than a certain duration. Takes in input parameter "threshold".
     */
    DURATION_LEQ,
    /**
     * used for mean time to access or mean response time
     */
    MEAN_TIME_TO_T,
    /**
     * Analytically, it is simply double the the mean time to T
     */
    TIME_AVERAGE,
    /**
     * the quotient between the sum of the durations when the event is occurring
     * and the total simulation time
     */
    PERCENT_TIME,
    /**
     * used when obtaining the rise and set times of event ocurrences (start and end times of accesses or gaps)
     */
    LIST_RISE_SET_TIMES,
    /**
     * Number of occurrences (passes or gaps)
     */
    OCCURRENCES,
}
