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
public interface Transmitter extends Antenna {
    /**
     * Gets the transmitting gain of the antenna
     *
     * @return the transmitting gain of the antenna
     */
    public double getGainT();
    
    /**
     * Compares this transmitter with a receiver antenna to see if they are
     * compatible in terms of operating frequencies.
     *
     * @param receiver the receiver emitting the signal for communication
     * @return true if this transmitter and the receiver have compatible
     * operating frequencies. else false
     */
    public boolean compatible(Receiver receiver);
}
