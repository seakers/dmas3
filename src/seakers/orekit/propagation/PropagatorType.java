/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.propagation;

/**
 * These enums enumerate the propagator types supported by the
 * PropagatorFactory. More can be added as the code base is developed further
 *
 * @author nozomihitomi
 */
public enum PropagatorType {
    /**
     * for keplerian propagation
     */
    KEPLERIAN,
    /**
     * for J2 propagation
     */
    J2,
    /**
     * Use if TLE is available
     */
    TLE,
    /**
     * numerical propagation for when using force models (i.e. atmospheric drag)
     */
    NUMERICAL
}
