/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.object.communications;

/**
 *
 * @author nhitomi
 */
public interface Receiver extends Antenna {

    /**
     * Gets the receiving gain of the antenna
     *
     * @return the receiving gain of the antenna
     */
    public double getGainR();

    /**
     * Compares this receiver with a transmitter antenna to see if they are
     * compatible in terms of operating frequencies.
     *
     * @param transmitter the transmitter emitting the signal for communication
     * @return true if this receiver and the transmitter have compatible
     * operating frequencies. else false
     */
    public boolean compatible(Transmitter transmitter);
}
