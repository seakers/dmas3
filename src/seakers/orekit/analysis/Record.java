/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis;

import java.io.Serializable;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author nozomihitomi
 */
public class Record<T> implements Comparable<Record<T>>, Serializable {

    private static final long serialVersionUID = 1580288073315494198L;

    private final AbsoluteDate date;

    private final T value;

    public Record(AbsoluteDate date, T value) {
        this.date = date;
        this.value = value;
    }

    /**
     * Gets the generic value stored in this record
     *
     * @return the generic value stored in this record
     */
    public T getValue() {
        return value;
    }

    /**
     * Gets the date of this record
     *
     * @return the date of this record
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /**
     * Use to compare the dates of two ephemeris data points
     *
     * @param o the other record to compare this record with
     * @return a negative integer, zero, or a positive integer as this date is
     * before, simultaneous, or after the specified date.
     */
    @Override
    public int compareTo(Record<T> o) {
        return this.getDate().compareTo(o.getDate());
    }

}
