/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author nozomihitomi
 */
public class RecordHistory<T> extends ArrayList<Record<T>> {

    private static final long serialVersionUID = -3805527690941619591L;

    /**
     * flag to keep track if the history is sorted by date
     */
    private boolean sorted;

    public RecordHistory() {
        super();
    }

    @Override
    public boolean addAll(int index, Collection<? extends Record<T>> c) {
        sorted = false;
        return super.addAll(index, c);
    }

    @Override
    public boolean addAll(Collection<? extends Record<T>> c) {
        sorted = false;
        return super.addAll(c); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void add(int index, Record<T> element) {
        sorted = false;
        super.add(index, element); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean add(Record<T> e) {
        sorted = false;
        return super.add(e); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Sorts the ephemeris history by date, starting from the earliest event.
     */
    public void sortByDate() {
        if (!sorted) {
            Collections.sort(this);
            sorted = true;
        }
    }
}
